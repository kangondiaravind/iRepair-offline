package com.servicecenter.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.servicecenter.app.data.local.entity.DeviceTypeEntity
import com.servicecenter.app.data.local.entity.PaymentModeEntity
import com.servicecenter.app.data.local.entity.RolePermissionEntity
import com.servicecenter.app.data.local.entity.SettingEntity
import com.servicecenter.app.data.local.entity.StaffEntity
import com.servicecenter.app.data.local.entity.StatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Upsert
    suspend fun upsert(staff: StaffEntity)

    @Query("SELECT * FROM staff WHERE id = :id")
    suspend fun getById(id: String): StaffEntity?

    /** Used at login: the repository checks the entered PIN against each active member's salted hash. */
    @Query("SELECT * FROM staff WHERE isDeleted = 0 AND active = 1")
    suspend fun getActive(): List<StaffEntity>

    @Query("SELECT * FROM staff WHERE isDeleted = 0 ORDER BY name")
    fun observeAll(): Flow<List<StaffEntity>>

    @Query("SELECT COUNT(*) FROM staff WHERE role = :role AND isDeleted = 0 AND active = 1")
    suspend fun countActiveByRole(role: String): Int
}

@Dao
interface StatusDao {
    @Upsert
    suspend fun upsertAll(items: List<StatusEntity>)

    @Query("SELECT * FROM statuses WHERE isDeleted = 0 AND active = 1 ORDER BY sortOrder")
    fun observeActive(): Flow<List<StatusEntity>>

    @Query("SELECT * FROM statuses WHERE id = :id")
    suspend fun getById(id: String): StatusEntity?
}

@Dao
interface PaymentModeDao {
    @Upsert
    suspend fun upsertAll(items: List<PaymentModeEntity>)

    @Query("SELECT * FROM payment_modes WHERE isDeleted = 0 AND active = 1 ORDER BY name")
    fun observeActive(): Flow<List<PaymentModeEntity>>
}

@Dao
interface DeviceTypeDao {
    @Upsert
    suspend fun upsertAll(items: List<DeviceTypeEntity>)

    @Query("SELECT * FROM device_types WHERE isDeleted = 0 AND active = 1 ORDER BY name")
    fun observeActive(): Flow<List<DeviceTypeEntity>>
}

@Dao
interface RolePermissionDao {
    @Upsert
    suspend fun upsertAll(items: List<RolePermissionEntity>)

    @Query("SELECT permission FROM role_permissions WHERE role = :role AND allowed = 1 AND isDeleted = 0")
    fun observeAllowed(role: String): Flow<List<String>>

    @Query(
        "SELECT allowed FROM role_permissions " +
            "WHERE role = :role AND permission = :permission AND isDeleted = 0 LIMIT 1"
    )
    suspend fun isAllowed(role: String, permission: String): Boolean?
}

@Dao
interface SettingDao {
    @Upsert
    suspend fun upsert(setting: SettingEntity)

    @Query("SELECT settingValue FROM settings WHERE settingKey = :key AND isDeleted = 0")
    suspend fun getValue(key: String): String?

    @Query("SELECT * FROM settings WHERE isDeleted = 0")
    fun observeAll(): Flow<List<SettingEntity>>
}
