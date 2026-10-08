package com.servicecenter.app.data.repository

import com.servicecenter.app.data.local.AppDatabase
import com.servicecenter.app.data.local.entity.DeviceTypeEntity
import com.servicecenter.app.data.local.entity.PaymentModeEntity
import com.servicecenter.app.data.local.entity.StatusEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Read-only lists that the Admin Panel will manage later (statuses, payment modes, device types). */
@Singleton
class ConfigRepository @Inject constructor(private val db: AppDatabase) {
    fun statuses(): Flow<List<StatusEntity>> = db.statusDao().observeActive()
    fun paymentModes(): Flow<List<PaymentModeEntity>> = db.paymentModeDao().observeActive()
    fun deviceTypes(): Flow<List<DeviceTypeEntity>> = db.deviceTypeDao().observeActive()
}
