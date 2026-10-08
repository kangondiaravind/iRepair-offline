package com.servicecenter.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.servicecenter.app.data.local.dao.CustomerDao
import com.servicecenter.app.data.local.dao.DeviceTypeDao
import com.servicecenter.app.data.local.dao.EnquiryDao
import com.servicecenter.app.data.local.dao.ExpenseDao
import com.servicecenter.app.data.local.dao.JobDao
import com.servicecenter.app.data.local.dao.PaymentDao
import com.servicecenter.app.data.local.dao.PaymentModeDao
import com.servicecenter.app.data.local.dao.ReportDao
import com.servicecenter.app.data.local.dao.RolePermissionDao
import com.servicecenter.app.data.local.dao.SettingDao
import com.servicecenter.app.data.local.dao.StaffDao
import com.servicecenter.app.data.local.dao.StatusDao
import com.servicecenter.app.data.local.entity.CustomerEntity
import com.servicecenter.app.data.local.entity.DeviceTypeEntity
import com.servicecenter.app.data.local.entity.EnquiryEntity
import com.servicecenter.app.data.local.entity.ExpenseEntity
import com.servicecenter.app.data.local.entity.JobEntity
import com.servicecenter.app.data.local.entity.PaymentEntity
import com.servicecenter.app.data.local.entity.PaymentModeEntity
import com.servicecenter.app.data.local.entity.RolePermissionEntity
import com.servicecenter.app.data.local.entity.SettingEntity
import com.servicecenter.app.data.local.entity.StaffEntity
import com.servicecenter.app.data.local.entity.StatusEntity

@Database(
    entities = [
        StaffEntity::class,
        StatusEntity::class,
        PaymentModeEntity::class,
        DeviceTypeEntity::class,
        RolePermissionEntity::class,
        SettingEntity::class,
        CustomerEntity::class,
        JobEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class,
        EnquiryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun staffDao(): StaffDao
    abstract fun statusDao(): StatusDao
    abstract fun paymentModeDao(): PaymentModeDao
    abstract fun deviceTypeDao(): DeviceTypeDao
    abstract fun rolePermissionDao(): RolePermissionDao
    abstract fun settingDao(): SettingDao

    abstract fun customerDao(): CustomerDao
    abstract fun jobDao(): JobDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun enquiryDao(): EnquiryDao
    abstract fun reportDao(): ReportDao

    companion object {
        const val NAME = "service_center.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        DefaultData.seed(db)
                    }
                })
                .build()
    }
}
