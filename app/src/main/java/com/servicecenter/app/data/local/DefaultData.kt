package com.servicecenter.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Default configuration, inserted once when the database is first created.
 * updatedAt = 0 on purpose: when the web Admin Panel syncs later, its (newer) values win.
 * The Owner account is NOT seeded: it is created on first launch, when the Owner chooses a PIN.
 */
internal object DefaultData {

    fun seed(db: SupportSQLiteDatabase) {
        seedStatuses(db)
        seedPaymentModes(db)
        seedDeviceTypes(db)
        seedPermissions(db)
        seedSettings(db)
    }

    private fun seedStatuses(db: SupportSQLiteDatabase) {
        val rows = listOf(
            Triple(DefaultIds.STATUS_RECEIVED, "Received", false),
            Triple(DefaultIds.STATUS_DIAGNOSING, "Diagnosing", false),
            Triple(DefaultIds.STATUS_WAITING_PARTS, "Waiting for Parts", false),
            Triple(DefaultIds.STATUS_IN_REPAIR, "In Repair", false),
            Triple(DefaultIds.STATUS_READY, "Ready", false),
            Triple(DefaultIds.STATUS_DELIVERED, "Delivered", true),
            Triple(DefaultIds.STATUS_CANCELLED, "Cancelled", true)
        )
        rows.forEachIndexed { index, (id, name, isFinal) ->
            db.execSQL(
                "INSERT INTO statuses (id, name, sortOrder, isFinal, active, updatedAt, isDeleted) " +
                    "VALUES (?, ?, ?, ?, 1, 0, 0)",
                arrayOf<Any>(id, name, index + 1, if (isFinal) 1 else 0)
            )
        }
    }

    private fun seedPaymentModes(db: SupportSQLiteDatabase) {
        listOf(DefaultIds.MODE_UPI to "UPI", DefaultIds.MODE_CASH to "Cash").forEach { (id, name) ->
            db.execSQL(
                "INSERT INTO payment_modes (id, name, active, updatedAt, isDeleted) VALUES (?, ?, 1, 0, 0)",
                arrayOf<Any>(id, name)
            )
        }
    }

    private fun seedDeviceTypes(db: SupportSQLiteDatabase) {
        listOf(DefaultIds.DEVICE_MOBILE to "Mobile", DefaultIds.DEVICE_LAPTOP to "Laptop").forEach { (id, name) ->
            db.execSQL(
                "INSERT INTO device_types (id, name, active, updatedAt, isDeleted) VALUES (?, ?, 1, 0, 0)",
                arrayOf<Any>(id, name)
            )
        }
    }

    private fun seedPermissions(db: SupportSQLiteDatabase) {
        Permissions.ALL.forEach { permission ->
            val staffAllowed = permission in Permissions.STAFF_DEFAULT
            insertPermission(db, Roles.OWNER, permission, true)
            insertPermission(db, Roles.STAFF, permission, staffAllowed)
        }
    }

    private fun insertPermission(db: SupportSQLiteDatabase, role: String, permission: String, allowed: Boolean) {
        db.execSQL(
            "INSERT INTO role_permissions (id, role, permission, allowed, updatedAt, isDeleted) " +
                "VALUES (?, ?, ?, ?, 0, 0)",
            arrayOf<Any>("perm-$role-$permission".lowercase(), role, permission, if (allowed) 1 else 0)
        )
    }

    private fun seedSettings(db: SupportSQLiteDatabase) {
        mapOf(
            SettingKeys.SHOP_NAME to "",
            SettingKeys.DEVICE_PREFIX to "A",
            SettingKeys.CUSTOMER_PREFIX to "C",
            SettingKeys.JOB_PREFIX to "J",
            SettingKeys.PIN_LENGTH to "4",
            SettingKeys.CUSTOMER_SEQ to "0",
            SettingKeys.JOB_SEQ to "0"
        ).forEach { (key, value) ->
            db.execSQL(
                "INSERT INTO settings (settingKey, settingValue, updatedAt, isDeleted) VALUES (?, ?, 0, 0)",
                arrayOf<Any>(key, value)
            )
        }
    }
}
