package com.servicecenter.app.data.local

object SyncState {
    const val PENDING = "PENDING"
    const val SYNCED = "SYNCED"
}

object Roles {
    const val OWNER = "OWNER"
    const val STAFF = "STAFF"
}

/** Permission names are fixed in code. The Admin Panel later decides which role gets which. */
object Permissions {
    const val CREATE_CUSTOMER = "create_customer"
    const val CREATE_JOB = "create_job"
    const val UPDATE_STATUS = "update_status"
    const val RECORD_PAYMENT = "record_payment"
    const val ADD_EXPENSE = "add_expense"
    const val LOG_ENQUIRY = "log_enquiry"
    const val VIEW_REPORTS = "view_reports"
    const val EDIT_FINAL_AMOUNT = "edit_final_amount"
    const val CORRECT_PAYMENT = "correct_payment"
    const val CORRECT_EXPENSE = "correct_expense"
    const val MANAGE_STAFF = "manage_staff"
    const val MANAGE_SETTINGS = "manage_settings"

    val STAFF_DEFAULT = listOf(
        CREATE_CUSTOMER, CREATE_JOB, UPDATE_STATUS, RECORD_PAYMENT, ADD_EXPENSE, LOG_ENQUIRY
    )
    val OWNER_ONLY_DEFAULT = listOf(
        VIEW_REPORTS, EDIT_FINAL_AMOUNT, CORRECT_PAYMENT, CORRECT_EXPENSE, MANAGE_STAFF, MANAGE_SETTINGS
    )
    val ALL = STAFF_DEFAULT + OWNER_ONLY_DEFAULT
}

/** Fixed IDs for the seeded defaults, so the Supabase seed in Phase 2 matches and nothing duplicates. */
object DefaultIds {
    const val STATUS_RECEIVED = "status-received"
    const val STATUS_DIAGNOSING = "status-diagnosing"
    const val STATUS_WAITING_PARTS = "status-waiting-parts"
    const val STATUS_IN_REPAIR = "status-in-repair"
    const val STATUS_READY = "status-ready"
    const val STATUS_DELIVERED = "status-delivered"
    const val STATUS_CANCELLED = "status-cancelled"

    const val MODE_UPI = "mode-upi"
    const val MODE_CASH = "mode-cash"

    const val DEVICE_MOBILE = "device-mobile"
    const val DEVICE_LAPTOP = "device-laptop"
}

/** Keys in the settings table. */
object SettingKeys {
    const val SHOP_NAME = "shop_name"
    const val DEVICE_PREFIX = "device_prefix"
    const val CUSTOMER_PREFIX = "customer_prefix"
    const val JOB_PREFIX = "job_prefix"
    const val PIN_LENGTH = "pin_length"
    const val CUSTOMER_SEQ = "customer_seq" // device-local counter, never synced
    const val JOB_SEQ = "job_seq"           // device-local counter, never synced
}
