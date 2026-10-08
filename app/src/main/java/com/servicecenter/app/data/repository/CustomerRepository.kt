package com.servicecenter.app.data.repository

import androidx.room.withTransaction
import com.servicecenter.app.core.AppException
import com.servicecenter.app.core.TimeProvider
import com.servicecenter.app.core.newId
import com.servicecenter.app.data.auth.SessionManager
import com.servicecenter.app.data.local.AppDatabase
import com.servicecenter.app.data.local.Permissions
import com.servicecenter.app.data.local.entity.CustomerEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Singleton
class CustomerRepository @Inject constructor(
    private val db: AppDatabase,
    private val session: SessionManager,
    private val numbers: NumberGenerator,
    private val time: TimeProvider
) {
    /** Home search: by phone or name. An empty box shows nothing. */
    fun search(query: String): Flow<List<CustomerEntity>> =
        if (query.isBlank()) flowOf(emptyList()) else db.customerDao().search(query.trim())

    fun observe(customerId: String): Flow<CustomerEntity?> = db.customerDao().observeById(customerId)

    /** Lets the screen warn "a customer with this number exists" before creating a second one. */
    suspend fun findByPhone(phone: String): CustomerEntity? = db.customerDao().findByPhone(normalizePhone(phone))

    /**
     * Family members can share a phone, so duplicates are allowed only when the
     * screen passes allowDuplicatePhone = true after warning the user.
     */
    suspend fun create(name: String, phone: String, allowDuplicatePhone: Boolean = false): CustomerEntity {
        val staff = session.require(Permissions.CREATE_CUSTOMER)
        val cleanPhone = normalizePhone(phone)
        if (name.isBlank()) throw AppException.Validation("Enter the customer's name")
        if (cleanPhone.length !in 10..15) throw AppException.Validation("Enter a valid phone number")

        return db.withTransaction {
            if (!allowDuplicatePhone && db.customerDao().findByPhone(cleanPhone) != null) {
                throw AppException.Validation("A customer with this phone number already exists")
            }
            val now = time.now()
            val customer = CustomerEntity(
                id = newId(),
                customerCode = numbers.nextCustomerCode(),
                name = name.trim(),
                phone = cleanPhone,
                createdAt = now,
                createdBy = staff.id,
                updatedAt = now
            )
            db.customerDao().upsert(customer)
            customer
        }
    }

    private fun normalizePhone(phone: String): String = phone.filter { it.isDigit() || it == '+' }
}
