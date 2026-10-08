package com.servicecenter.app.data.repository

import com.servicecenter.app.core.AppException
import com.servicecenter.app.core.TimeProvider
import com.servicecenter.app.core.newId
import com.servicecenter.app.data.auth.SessionManager
import com.servicecenter.app.data.local.AppDatabase
import com.servicecenter.app.data.local.Permissions
import com.servicecenter.app.data.local.entity.ExpenseEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Per-job expenses only (parts, courier, outside technician). No shop-level expenses. */
@Singleton
class ExpenseRepository @Inject constructor(
    private val db: AppDatabase,
    private val session: SessionManager,
    private val time: TimeProvider
) {
    fun observeForJob(jobId: String): Flow<List<ExpenseEntity>> = db.expenseDao().observeForJob(jobId)

    suspend fun add(jobId: String, description: String, amount: Long): ExpenseEntity {
        val staff = session.require(Permissions.ADD_EXPENSE)
        if (description.isBlank()) throw AppException.Validation("Enter what the expense was for")
        if (amount <= 0) throw AppException.Validation("Enter an amount greater than zero")
        db.jobDao().getById(jobId)?.takeIf { !it.isDeleted } ?: throw AppException.NotFound("Job")
        val now = time.now()
        val expense = ExpenseEntity(
            id = newId(), jobId = jobId, description = description.trim(), amount = amount,
            createdAt = now, createdBy = staff.id, updatedAt = now
        )
        db.expenseDao().upsert(expense)
        return expense
    }

    /** Owner only. The row stays in the database, marked as voided. */
    suspend fun void(expenseId: String) {
        session.require(Permissions.CORRECT_EXPENSE)
        db.expenseDao().markVoided(expenseId, time.now())
    }
}
