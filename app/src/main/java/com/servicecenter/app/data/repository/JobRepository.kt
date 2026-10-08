package com.servicecenter.app.data.repository

import androidx.room.withTransaction
import com.servicecenter.app.core.AppException
import com.servicecenter.app.core.Money
import com.servicecenter.app.core.TimeProvider
import com.servicecenter.app.core.newId
import com.servicecenter.app.data.auth.SessionManager
import com.servicecenter.app.data.local.AppDatabase
import com.servicecenter.app.data.local.DefaultIds
import com.servicecenter.app.data.local.Permissions
import com.servicecenter.app.data.local.Roles
import com.servicecenter.app.data.local.dao.JobSummary
import com.servicecenter.app.data.local.entity.JobEntity
import com.servicecenter.app.data.local.entity.PaymentEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

data class NewJob(
    val customerId: String,
    val deviceTypeId: String,
    val model: String,
    val problem: String,
    val accessories: String,
    val estimate: Long
)

/** One payment line. "Paid by both" is simply two parts: one UPI, one Cash. */
data class PaymentPart(val paymentModeId: String, val amount: Long)

@Singleton
class JobRepository @Inject constructor(
    private val db: AppDatabase,
    private val session: SessionManager,
    private val numbers: NumberGenerator,
    private val time: TimeProvider
) {
    fun observe(jobId: String): Flow<JobSummary?> = db.jobDao().observeSummary(jobId)
    fun observeForCustomer(customerId: String): Flow<List<JobSummary>> = db.jobDao().observeForCustomer(customerId)
    fun observeByStatus(statusId: String): Flow<List<JobSummary>> = db.jobDao().observeByStatus(statusId)
    fun observeOpen(): Flow<List<JobSummary>> = db.jobDao().observeOpen()
    fun observeRecent(limit: Int = 100): Flow<List<JobSummary>> = db.jobDao().observeRecent(limit)

    /**
     * Creates the job card (status Received, final amount = estimate) and any advance payments
     * in one transaction, so a job never exists without the advance that was taken for it.
     */
    suspend fun createJob(input: NewJob, advances: List<PaymentPart> = emptyList()): JobEntity {
        val staff = session.require(Permissions.CREATE_JOB)
        if (advances.isNotEmpty()) session.require(Permissions.RECORD_PAYMENT)

        if (input.model.isBlank()) throw AppException.Validation("Enter the device model")
        if (input.problem.isBlank()) throw AppException.Validation("Enter the problem description")
        if (input.estimate < 0) throw AppException.Validation("Estimation cannot be negative")
        if (advances.any { it.amount <= 0 }) throw AppException.Validation("Advance amount must be more than zero")
        if (advances.sumOf { it.amount } > input.estimate) {
            throw AppException.Validation("Advance is more than the estimation")
        }

        return db.withTransaction {
            val customer = db.customerDao().getById(input.customerId)?.takeIf { !it.isDeleted }
                ?: throw AppException.NotFound("Customer")
            val now = time.now()
            val job = JobEntity(
                id = newId(),
                customerId = customer.id,
                jobNo = numbers.nextJobNo(),
                deviceTypeId = input.deviceTypeId,
                model = input.model.trim(),
                problem = input.problem.trim(),
                accessories = input.accessories.trim(),
                estimate = input.estimate,
                finalAmount = input.estimate,
                statusId = DefaultIds.STATUS_RECEIVED,
                createdAt = now,
                createdBy = staff.id,
                updatedAt = now
            )
            db.jobDao().upsert(job)
            advances.forEach { part ->
                db.paymentDao().upsert(
                    PaymentEntity(
                        id = newId(),
                        jobId = job.id,
                        paymentModeId = part.paymentModeId,
                        amount = part.amount,
                        paidAt = now,
                        createdBy = staff.id,
                        updatedAt = now
                    )
                )
            }
            job
        }
    }

    /**
     * Delivered needs a zero balance. A closed job (Delivered or Cancelled) can be
     * changed only by an Owner, for example to reopen it.
     */
    suspend fun updateStatus(jobId: String, newStatusId: String) {
        val staff = session.require(Permissions.UPDATE_STATUS)
        db.withTransaction {
            val job = db.jobDao().getById(jobId)?.takeIf { !it.isDeleted } ?: throw AppException.NotFound("Job")
            if (job.statusId == newStatusId) return@withTransaction
            val current = db.statusDao().getById(job.statusId)
            db.statusDao().getById(newStatusId)?.takeIf { !it.isDeleted && it.active }
                ?: throw AppException.NotFound("Status")

            if (current?.isFinal == true && staff.role != Roles.OWNER) {
                throw AppException.Validation("This job is closed. Ask the owner to reopen it.")
            }
            if (newStatusId == DefaultIds.STATUS_DELIVERED) {
                val balance = job.finalAmount - db.paymentDao().totalPaid(jobId)
                if (balance > 0) {
                    throw AppException.Validation("Balance of ${Money.format(balance)} is still pending")
                }
            }
            val now = time.now()
            val deliveredAt = if (newStatusId == DefaultIds.STATUS_DELIVERED) now else null
            db.jobDao().updateStatus(jobId, newStatusId, deliveredAt, now)
        }
    }

    /** Owner only. Used for bargaining: the estimate stays, the final amount changes. */
    suspend fun updateFinalAmount(jobId: String, finalAmount: Long, note: String?) {
        session.require(Permissions.EDIT_FINAL_AMOUNT)
        if (finalAmount < 0) throw AppException.Validation("Final amount cannot be negative")
        db.withTransaction {
            db.jobDao().getById(jobId)?.takeIf { !it.isDeleted } ?: throw AppException.NotFound("Job")
            val paid = db.paymentDao().totalPaid(jobId)
            if (finalAmount < paid) {
                throw AppException.Validation(
                    "Already paid ${Money.format(paid)}, which is more than this amount. Record a refund first."
                )
            }
            db.jobDao().updateFinalAmount(jobId, finalAmount, note?.trim()?.ifBlank { null }, time.now())
        }
    }
}
