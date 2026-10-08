package com.servicecenter.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.servicecenter.app.data.local.SyncState

/**
 * Staff is the only configuration table written on the phone in Phase 1
 * (the Owner manages PINs locally), so it carries syncState.
 * The PIN is never stored: only a salted hash.
 */
@Entity(tableName = "staff", indices = [Index("name")])
data class StaffEntity(
    @PrimaryKey val id: String,
    val name: String,
    val pinHash: String,
    val pinSalt: String,
    val role: String,
    val active: Boolean = true,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val syncState: String = SyncState.PENDING
)

// The tables below are edited on the web Admin Panel later. The phone only reads them.

@Entity(tableName = "statuses")
data class StatusEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int,
    val isFinal: Boolean,
    val active: Boolean = true,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Entity(tableName = "payment_modes")
data class PaymentModeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val active: Boolean = true,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Entity(tableName = "device_types")
data class DeviceTypeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val active: Boolean = true,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "role_permissions",
    indices = [Index(value = ["role", "permission"], unique = true)]
)
data class RolePermissionEntity(
    @PrimaryKey val id: String,
    val role: String,
    val permission: String,
    val allowed: Boolean,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val settingKey: String,
    val settingValue: String,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)
