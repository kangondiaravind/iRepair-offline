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
import com.servicecenter.app.data.local.dao.PaymentWithMode
import com.servicecenter.app.data.local.entity.PaymentEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class PaymentRepository @Inject constructor(
    private val db: AppDatabase,
    private val session: SessionManager,
    private val time: TimeProvider
) {
    fun observeForJob(jobId: String): Flow<List<PaymentWithMode>> = db.paymentDao().observeForJob(jobId)

    /** Staff and Owner. Several parts at once covers "paid by UPI and Cash". */
    suspend fun record(jobId: String, parts: List<PaymentPart>) {
        val staff = session.require(Permissions.RECORD_PAYMENT)
        if (parts.isEmpty() || parts.any { it.amount <= 0 }) {
            throw AppException.Validation("Enter an amount greater than zero")
        }
        db.withTransaction {
            val job = db.jobDao().getById(jobId)?.takeIf { !it.isDeleted } ?: throw AppException.NotFound("Job")
            if (job.statusId == DefaultIds.STATUS_CANCELLED) throw AppException.Validation("This job is cancelled")
            val paid = db.paymentDao().totalPaid(jobId)
            val balance = job.finalAmount - paid
            if (parts.sumOf { it.amount } > balance) {
                throw AppException.Validation("That is more than the balance of ${Money.format(balance)}")
            }
            val now = time.now()
            parts.forEach { part ->
                db.paymentDao().upsert(
                    PaymentEntity(
                        id = newId(), jobId = jobId, paymentModeId = part.paymentModeId,
                        amount = part.amount, paidAt = now, createdBy = staff.id, updatedAt = now
                    )
                )
            }
        }
    }

    /** Owner only. Stored as a negative payment so the reports subtract it automatically. */
    suspend fun refund(jobId: String, paymentModeId: String, amount: Long) {
        val staff = session.require(Permissions.CORRECT_PAYMENT)
        if (amount <= 0) throw AppException.Validation("Enter an amount greater than zero")
        db.withTransaction {
            db.jobDao().getById(jobId)?.takeIf { !it.isDeleted } ?: throw AppException.NotFound("Job")
            val paid = db.paymentDao().totalPaid(jobId)
            if (amount > paid) {
                throw AppException.Validation("Only ${Money.format(paid)} has been received on this job")
            }
            val now = time.now()
            db.paymentDao().upsert(
                PaymentEntity(
                    id = newId(), jobId = jobId, paymentModeId = paymentModeId,
                    amount = -amount, paidAt = now, createdBy = staff.id, updatedAt = now
                )
            )
        }
    }

    /** Owner only. The row stays in the database, marked as voided. */
    suspend fun void(paymentId: String) {
        session.require(Permissions.CORRECT_PAYMENT)
        db.paymentDao().markVoided(paymentId, time.now())
    }
}
