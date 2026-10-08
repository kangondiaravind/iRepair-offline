package com.servicecenter.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.servicecenter.app.data.local.SyncState

/*
 * Rules for every table here:
 * - id is a UUID string generated on the phone (safe for offline devices and later sync)
 * - amounts are Long, in paise
 * - times are epoch milliseconds
 * - rows are never hard-deleted: isDeleted = true, so the deletion can sync and history stays
 */

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["customerCode"], unique = true),
        Index("phone"),
        Index("name")
    ]
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    val customerCode: String,          // e.g. C-A-0001 (device prefix avoids clashes)
    val name: String,
    val phone: String,
    val createdAt: Long,
    val createdBy: String,             // staff id
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val syncState: String = SyncState.PENDING
)

@Entity(
    tableName = "jobs",
    foreignKeys = [
        ForeignKey(CustomerEntity::class, ["id"], ["customerId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(StatusEntity::class, ["id"], ["statusId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(DeviceTypeEntity::class, ["id"], ["deviceTypeId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [
        Index(value = ["jobNo"], unique = true),
        Index("customerId"),
        Index("statusId"),
        Index("deviceTypeId"),
        Index("createdAt"),
        Index("deliveredAt")
    ]
)
data class JobEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val jobNo: String,                 // e.g. J-A-0001
    val deviceTypeId: String,
    val model: String,
    val problem: String,
    val accessories: String,
    val estimate: Long,                // first quote, never changed
    val finalAmount: Long,             // starts equal to estimate, Owner can change (bargaining)
    val discountNote: String? = null,
    val statusId: String,
    val createdAt: Long,
    val deliveredAt: Long? = null,
    val createdBy: String,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val syncState: String = SyncState.PENDING
)

/** A refund is a payment with a negative amount. */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(JobEntity::class, ["id"], ["jobId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(PaymentModeEntity::class, ["id"], ["paymentModeId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("jobId"), Index("paymentModeId"), Index("paidAt")]
)
data class PaymentEntity(
    @PrimaryKey val id: String,
    val jobId: String,
    val paymentModeId: String,
    val amount: Long,
    val paidAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val syncState: String = SyncState.PENDING
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(JobEntity::class, ["id"], ["jobId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("jobId"), Index("createdAt")]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val jobId: String,
    val description: String,
    val amount: Long,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val syncState: String = SyncState.PENDING
)

/** Walk-in enquiry. jobId stays null unless the enquiry is converted into a job. */
@Entity(
    tableName = "enquiries",
    foreignKeys = [
        ForeignKey(JobEntity::class, ["id"], ["jobId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("jobId"), Index("createdAt")]
)
data class EnquiryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val model: String,
    val notes: String,
    val jobId: String? = null,
    val createdAt: Long,
    val createdBy: String,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val syncState: String = SyncState.PENDING
)
