package com.servicecenter.app.core

import android.util.Base64
import java.math.RoundingMode
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Errors the screens can show to the user as-is. */
sealed class AppException(message: String) : Exception(message) {
    class NotLoggedIn : AppException("Please enter your PIN to continue")
    class PermissionDenied(val permission: String) : AppException("You do not have permission for this action")
    class Validation(message: String) : AppException(message)
    class NotFound(what: String) : AppException("$what not found")
}

interface TimeProvider { fun now(): Long }

object SystemTimeProvider : TimeProvider {
    override fun now(): Long = System.currentTimeMillis()
}

fun newId(): String = UUID.randomUUID().toString()

/** All amounts are stored as Long paise. The UI works in rupees. */
object Money {
    /** "1500" or "1500.50" -> paise. Returns null for invalid text. */
    fun parseRupeesToPaise(text: String): Long? {
        val value = text.trim().toBigDecimalOrNull() ?: return null
        return try {
            value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (e: ArithmeticException) {
            null
        }
    }

    fun format(paise: Long): String {
        val sign = if (paise < 0) "-" else ""
        val abs = Math.abs(paise)
        val rupees = abs / 100
        val rem = abs % 100
        return if (rem == 0L) "$sign₹$rupees" else "$sign₹$rupees.${rem.toString().padStart(2, '0')}"
    }
}

/**
 * Salted PBKDF2 hash. The PIN itself is never stored.
 * Needs API 26+ for PBKDF2WithHmacSHA256. A 4-digit PIN is a convenience lock,
 * not strong security, because there are only 10,000 possible values.
 */
object PinHasher {
    private const val ITERATIONS = 100_000
    private const val KEY_BITS = 256

    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), Base64.decode(salt, Base64.NO_WRAP), ITERATIONS, KEY_BITS)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun verify(pin: String, salt: String, expectedHash: String): Boolean =
        MessageDigest.isEqual(hash(pin, salt).toByteArray(), expectedHash.toByteArray())
}
