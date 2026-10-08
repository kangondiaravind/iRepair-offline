package com.servicecenter.app.data.repository

import androidx.room.withTransaction
import com.servicecenter.app.core.AppException
import com.servicecenter.app.core.TimeProvider
import com.servicecenter.app.core.newId
import com.servicecenter.app.data.auth.SessionManager
import com.servicecenter.app.data.local.AppDatabase
import com.servicecenter.app.data.local.Permissions
import com.servicecenter.app.data.local.entity.EnquiryEntity
import com.servicecenter.app.data.local.entity.JobEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class EnquiryRepository @Inject constructor(
    private val db: AppDatabase,
    private val session: SessionManager,
    private val jobs: JobRepository,
    private val time: TimeProvider
) {
    fun observeAll(): Flow<List<EnquiryEntity>> = db.enquiryDao().observeAll()

    suspend fun get(id: String): EnquiryEntity? = db.enquiryDao().getById(id)?.takeIf { !it.isDeleted }

    /** A walk-in who left no device. No job is created. */
    suspend fun log(name: String, phone: String, model: String, notes: String): EnquiryEntity {
        val staff = session.require(Permissions.LOG_ENQUIRY)
        if (name.isBlank() && phone.isBlank()) throw AppException.Validation("Enter a name or phone number")
        val now = time.now()
        val enquiry = EnquiryEntity(
            id = newId(), name = name.trim(), phone = phone.trim(), model = model.trim(),
            notes = notes.trim(), createdAt = now, createdBy = staff.id, updatedAt = now
        )
        db.enquiryDao().upsert(enquiry)
        return enquiry
    }

    /** The enquiry customer decided to repair: create the job and link it to the enquiry. */
    suspend fun convertToJob(
        enquiryId: String,
        job: NewJob,
        advances: List<PaymentPart> = emptyList()
    ): JobEntity = db.withTransaction {
        val enquiry = db.enquiryDao().getById(enquiryId)?.takeIf { !it.isDeleted }
            ?: throw AppException.NotFound("Enquiry")
        if (enquiry.jobId != null) throw AppException.Validation("This enquiry is already converted")
        val created = jobs.createJob(job, advances)
        db.enquiryDao().markConverted(enquiryId, created.id, time.now())
        created
    }
}
