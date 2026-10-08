package com.servicecenter.app.di

import android.content.Context
import com.servicecenter.app.core.SystemTimeProvider
import com.servicecenter.app.core.TimeProvider
import com.servicecenter.app.data.local.AppDatabase
import com.servicecenter.app.data.local.dao.RolePermissionDao
import com.servicecenter.app.data.local.dao.SettingDao
import com.servicecenter.app.data.local.dao.StaffDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase = AppDatabase.build(context)

    @Provides fun provideStaffDao(db: AppDatabase): StaffDao = db.staffDao()
    @Provides fun provideSettingDao(db: AppDatabase): SettingDao = db.settingDao()
    @Provides fun provideRolePermissionDao(db: AppDatabase): RolePermissionDao = db.rolePermissionDao()

    @Provides @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider
}
