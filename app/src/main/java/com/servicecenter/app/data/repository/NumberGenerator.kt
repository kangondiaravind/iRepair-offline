package com.servicecenter.app.data.repository

import com.servicecenter.app.core.TimeProvider
import com.servicecenter.app.data.local.AppDatabase
import com.servicecenter.app.data.local.SettingKeys
import com.servicecenter.app.data.local.entity.SettingEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Readable numbers like C-A-0001 and J-A-0001. The middle part is this device's prefix,
 * so two phones never produce the same number. Call inside db.withTransaction.
 */
@Singleton
class NumberGenerator @Inject constructor(
    private val db: AppDatabase,
    private val time: TimeProvider
) {
    suspend fun nextCustomerCode(): String = next(SettingKeys.CUSTOMER_PREFIX, SettingKeys.CUSTOMER_SEQ, "C")
    suspend fun nextJobNo(): String = next(SettingKeys.JOB_PREFIX, SettingKeys.JOB_SEQ, "J")

    private suspend fun next(prefixKey: String, seqKey: String, fallbackPrefix: String): String {
        val settings = db.settingDao()
        val prefix = settings.getValue(prefixKey) ?: fallbackPrefix
        val device = settings.getValue(SettingKeys.DEVICE_PREFIX) ?: "A"
        val seq = (settings.getValue(seqKey)?.toLongOrNull() ?: 0L) + 1
        settings.upsert(SettingEntity(seqKey, seq.toString(), time.now()))
        return "$prefix-$device-${seq.toString().padStart(4, '0')}"
    }
}
