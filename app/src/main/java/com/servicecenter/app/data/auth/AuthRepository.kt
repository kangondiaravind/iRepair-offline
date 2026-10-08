package com.servicecenter.app.data.auth

import com.servicecenter.app.core.AppException
import com.servicecenter.app.core.PinHasher
import com.servicecenter.app.core.TimeProvider
import com.servicecenter.app.core.newId
import com.servicecenter.app.data.local.Permissions
import com.servicecenter.app.data.local.Roles
import com.servicecenter.app.data.local.SettingKeys
import com.servicecenter.app.data.local.SyncState
import com.servicecenter.app.data.local.dao.SettingDao
import com.servicecenter.app.data.local.dao.StaffDao
import com.servicecenter.app.data.local.entity.StaffEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

/**
 * PIN-only login: the PIN identifies the person, so two active people can never share a PIN.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val staffDao: StaffDao,
    private val settingDao: SettingDao,
    private val session: SessionManager,
    private val time: TimeProvider
) {
    fun observeStaff(): Flow<List<StaffEntity>> = staffDao.observeAll()

    suspend fun pinLength(): Int = settingDao.getValue(SettingKeys.PIN_LENGTH)?.toIntOrNull() ?: 4

    /** False on the very first launch: show the "create Owner PIN" screen. */
    suspend fun hasOwner(): Boolean = staffDao.countActiveByRole(Roles.OWNER) > 0

    suspend fun createFirstOwner(name: String, pin: String): StaffEntity {
        if (hasOwner()) throw AppException.Validation("An owner already exists")
        val owner = buildStaff(name, pin, Roles.OWNER)
        staffDao.upsert(owner)
        session.login(owner)
        return owner
    }

    /** Returns the matching person, or null for a wrong PIN. */
    suspend fun login(pin: String): StaffEntity? {
        val match = staffDao.getActive().firstOrNull { PinHasher.verify(pin, it.pinSalt, it.pinHash) }
        if (match != null) session.login(match)
        return match
    }

    fun logout() = session.logout()

    suspend fun addStaff(name: String, pin: String): StaffEntity {
        session.require(Permissions.MANAGE_STAFF)
        val staff = buildStaff(name, pin, Roles.STAFF)
        staffDao.upsert(staff)
        return staff
    }

    suspend fun changePin(staffId: String, newPin: String) {
        session.require(Permissions.MANAGE_STAFF)
        val staff = staffDao.getById(staffId) ?: throw AppException.NotFound("Staff member")
        validatePin(newPin, excludeId = staffId)
        val salt = PinHasher.newSalt()
        staffDao.upsert(
            staff.copy(
                pinSalt = salt,
                pinHash = PinHasher.hash(newPin, salt),
                updatedAt = time.now(),
                syncState = SyncState.PENDING
            )
        )
    }

    suspend fun deactivate(staffId: String) {
        session.require(Permissions.MANAGE_STAFF)
        val staff = staffDao.getById(staffId) ?: throw AppException.NotFound("Staff member")
        if (staff.role == Roles.OWNER && staffDao.countActiveByRole(Roles.OWNER) <= 1) {
            throw AppException.Validation("The only owner cannot be deactivated")
        }
        staffDao.upsert(staff.copy(active = false, updatedAt = time.now(), syncState = SyncState.PENDING))
    }

    private suspend fun buildStaff(name: String, pin: String, role: String): StaffEntity {
        if (name.isBlank()) throw AppException.Validation("Enter a name")
        validatePin(pin)
        val salt = PinHasher.newSalt()
        return StaffEntity(
            id = newId(),
            name = name.trim(),
            pinHash = PinHasher.hash(pin, salt),
            pinSalt = salt,
            role = role,
            updatedAt = time.now()
        )
    }

    private suspend fun validatePin(pin: String, excludeId: String? = null) {
        val length = pinLength()
        if (pin.length != length || !pin.all { it.isDigit() }) {
            throw AppException.Validation("PIN must be $length digits")
        }
        val clash = staffDao.getActive().any {
            it.id != excludeId && PinHasher.verify(pin, it.pinSalt, it.pinHash)
        }
        if (clash) throw AppException.Validation("This PIN is already used by someone else")
    }
}
