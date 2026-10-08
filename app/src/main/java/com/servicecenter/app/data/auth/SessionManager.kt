package com.servicecenter.app.data.auth

import com.servicecenter.app.core.AppException
import com.servicecenter.app.data.local.dao.RolePermissionDao
import com.servicecenter.app.data.local.entity.StaffEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Who is using the shop device right now. Every repository asks this before acting. */
@Singleton
class SessionManager @Inject constructor(
    private val rolePermissionDao: RolePermissionDao
) {
    private val _current = MutableStateFlow<StaffEntity?>(null)
    val current: StateFlow<StaffEntity?> = _current.asStateFlow()

    fun login(staff: StaffEntity) { _current.value = staff }
    fun logout() { _current.value = null }

    fun requireStaff(): StaffEntity = _current.value ?: throw AppException.NotLoggedIn()

    /** Returns the logged-in staff member, or throws if the role does not have the permission. */
    suspend fun require(permission: String): StaffEntity {
        val staff = requireStaff()
        val allowed = rolePermissionDao.isAllowed(staff.role, permission) == true
        if (!allowed) throw AppException.PermissionDenied(permission)
        return staff
    }

    /** For hiding buttons in the UI. The repositories still enforce the rule. */
    suspend fun can(permission: String): Boolean {
        val staff = _current.value ?: return false
        return rolePermissionDao.isAllowed(staff.role, permission) == true
    }
}
