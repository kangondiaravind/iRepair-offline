package com.servicecenter.app.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Upsert
import com.servicecenter.app.data.local.entity.CustomerEntity
import com.servicecenter.app.data.local.entity.EnquiryEntity
import com.servicecenter.app.data.local.entity.ExpenseEntity
import com.servicecenter.app.data.local.entity.JobEntity
import com.servicecenter.app.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Upsert
    suspend fun upsert(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE id = :id AND isDeleted = 0")
    fun observeById(id: String): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: String): CustomerEntity?

    /** Home screen search: partial match on phone or name. */
    @Query(
        "SELECT * FROM customers WHERE isDeleted = 0 AND " +
            "(phone LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%') " +
            "ORDER BY name LIMIT 50"
    )
    fun search(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE isDeleted = 0 AND phone = :phone LIMIT 1")
    suspend fun findByPhone(phone: String): CustomerEntity?
}

/** A job plus the figures the screens need. Balance, profit and discount are calculated, never stored. */
data class JobSummary(
    @Embedded val job: JobEntity,
    val statusName: String,
    val statusIsFinal: Boolean,
    val deviceTypeName: String,
    val customerName: String,
    val customerPhone: String,
    val totalPaid: Long,
    val totalExpenses: Long
) {
    val balance: Long get() = job.finalAmount - totalPaid
    val profit: Long get() = totalPaid - totalExpenses
    val discount: Long get() = maxOf(job.estimate - job.finalAmount, 0L)
    val extraCharge: Long get() = maxOf(job.finalAmount - job.estimate, 0L)
}

internal const val JOB_SUMMARY_SELECT = """
SELECT j.*,
       s.name AS statusName,
       s.isFinal AS statusIsFinal,
       d.name AS deviceTypeName,
       c.name AS customerName,
       c.phone AS customerPhone,
       COALESCE((SELECT SUM(p.amount) FROM payments p
                 WHERE p.jobId = j.id AND p.isDeleted = 0), 0) AS totalPaid,
       COALESCE((SELECT SUM(e.amount) FROM expenses e
                 WHERE e.jobId = j.id AND e.isDeleted = 0), 0) AS totalExpenses
FROM jobs j
JOIN statuses s ON s.id = j.statusId
JOIN device_types d ON d.id = j.deviceTypeId
JOIN customers c ON c.id = j.customerId
"""

@Dao
interface JobDao {
    @Upsert
    suspend fun upsert(job: JobEntity)

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun getById(id: String): JobEntity?

    @Query(JOB_SUMMARY_SELECT + " WHERE j.isDeleted = 0 AND j.id = :id")
    fun observeSummary(id: String): Flow<JobSummary?>

    /** Customer history, newest first. */
    @Query(JOB_SUMMARY_SELECT + " WHERE j.isDeleted = 0 AND j.customerId = :customerId ORDER BY j.createdAt DESC")
    fun observeForCustomer(customerId: String): Flow<List<JobSummary>>

    /** Status filter, for example all jobs that are Ready. */
    @Query(JOB_SUMMARY_SELECT + " WHERE j.isDeleted = 0 AND j.statusId = :statusId ORDER BY j.createdAt DESC")
    fun observeByStatus(statusId: String): Flow<List<JobSummary>>

    /** Jobs that are not yet in a final status (Delivered or Cancelled). */
    @Query(JOB_SUMMARY_SELECT + " WHERE j.isDeleted = 0 AND s.isFinal = 0 ORDER BY j.createdAt DESC")
    fun observeOpen(): Flow<List<JobSummary>>

    @Query(JOB_SUMMARY_SELECT + " WHERE j.isDeleted = 0 ORDER BY j.createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<JobSummary>>

    @Query(
        "UPDATE jobs SET statusId = :statusId, deliveredAt = :deliveredAt, " +
            "updatedAt = :now, syncState = 'PENDING' WHERE id = :id"
    )
    suspend fun updateStatus(id: String, statusId: String, deliveredAt: Long?, now: Long)

    /** Owner only (permission edit_final_amount). Used for bargaining. */
    @Query(
        "UPDATE jobs SET finalAmount = :finalAmount, discountNote = :note, " +
            "updatedAt = :now, syncState = 'PENDING' WHERE id = :id"
    )
    suspend fun updateFinalAmount(id: String, finalAmount: Long, note: String?, now: Long)
}

data class PaymentWithMode(
    @Embedded val payment: PaymentEntity,
    val modeName: String
)

@Dao
interface PaymentDao {
    @Upsert
    suspend fun upsert(payment: PaymentEntity)

    @Query(
        "SELECT p.*, m.name AS modeName FROM payments p " +
            "JOIN payment_modes m ON m.id = p.paymentModeId " +
            "WHERE p.jobId = :jobId AND p.isDeleted = 0 ORDER BY p.paidAt"
    )
    fun observeForJob(jobId: String): Flow<List<PaymentWithMode>>

    /** Net amount received for a job (refunds are negative, voided rows are ignored). */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE jobId = :jobId AND isDeleted = 0")
    suspend fun totalPaid(jobId: String): Long

    /** Owner correction: the row is voided, not erased, so the history stays. */
    @Query("UPDATE payments SET isDeleted = 1, updatedAt = :now, syncState = 'PENDING' WHERE id = :id")
    suspend fun markVoided(id: String, now: Long)
}

@Dao
interface ExpenseDao {
    @Upsert
    suspend fun upsert(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE jobId = :jobId AND isDeleted = 0 ORDER BY createdAt")
    fun observeForJob(jobId: String): Flow<List<ExpenseEntity>>

    @Query("UPDATE expenses SET isDeleted = 1, updatedAt = :now, syncState = 'PENDING' WHERE id = :id")
    suspend fun markVoided(id: String, now: Long)
}

@Dao
interface EnquiryDao {
    @Upsert
    suspend fun upsert(enquiry: EnquiryEntity)

    @Query("SELECT * FROM enquiries WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<EnquiryEntity>>

    @Query("SELECT * FROM enquiries WHERE id = :id")
    suspend fun getById(id: String): EnquiryEntity?

    @Query("UPDATE enquiries SET jobId = :jobId, updatedAt = :now, syncState = 'PENDING' WHERE id = :id")
    suspend fun markConverted(id: String, jobId: String, now: Long)
}
