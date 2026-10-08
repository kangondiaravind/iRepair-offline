package com.servicecenter.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query

data class ModeTotal(val modeName: String, val total: Long)

/** day is yyyy-MM-dd in the phone's local time zone. */
data class DayAmount(val day: String, val amount: Long)

/**
 * All ranges are [start, end) in epoch milliseconds. Pass local midnight to local midnight.
 * Money counts on the day it was received (advance on Day 1, balance on the delivery day).
 * Refunds are negative payments, so they reduce the totals automatically.
 * Profit = totalCollected - totalExpenses (per-job expenses only).
 */
@Dao
interface ReportDao {

    @Query(
        "SELECT m.name AS modeName, SUM(p.amount) AS total FROM payments p " +
            "JOIN payment_modes m ON m.id = p.paymentModeId " +
            "WHERE p.isDeleted = 0 AND p.paidAt >= :start AND p.paidAt < :end " +
            "GROUP BY m.id ORDER BY m.name"
    )
    suspend fun collectedByMode(start: Long, end: Long): List<ModeTotal>

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM payments " +
            "WHERE isDeleted = 0 AND paidAt >= :start AND paidAt < :end"
    )
    suspend fun totalCollected(start: Long, end: Long): Long

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM expenses " +
            "WHERE isDeleted = 0 AND createdAt >= :start AND createdAt < :end"
    )
    suspend fun totalExpenses(start: Long, end: Long): Long

    @Query(
        "SELECT COUNT(*) FROM jobs WHERE isDeleted = 0 AND createdAt >= :start AND createdAt < :end"
    )
    suspend fun jobsReceived(start: Long, end: Long): Int

    @Query(
        "SELECT COUNT(*) FROM jobs WHERE isDeleted = 0 AND deliveredAt >= :start AND deliveredAt < :end"
    )
    suspend fun jobsDelivered(start: Long, end: Long): Int

    /** Total discount given on jobs delivered in the range (estimate minus final amount, when positive). */
    @Query(
        "SELECT COALESCE(SUM(MAX(estimate - finalAmount, 0)), 0) FROM jobs " +
            "WHERE isDeleted = 0 AND deliveredAt >= :start AND deliveredAt < :end"
    )
    suspend fun totalDiscounts(start: Long, end: Long): Long

    @Query(
        "SELECT strftime('%Y-%m-%d', paidAt / 1000, 'unixepoch', 'localtime') AS day, " +
            "SUM(amount) AS amount FROM payments " +
            "WHERE isDeleted = 0 AND paidAt >= :start AND paidAt < :end " +
            "GROUP BY day ORDER BY day"
    )
    suspend fun collectedPerDay(start: Long, end: Long): List<DayAmount>

    @Query(
        "SELECT strftime('%Y-%m-%d', createdAt / 1000, 'unixepoch', 'localtime') AS day, " +
            "SUM(amount) AS amount FROM expenses " +
            "WHERE isDeleted = 0 AND createdAt >= :start AND createdAt < :end " +
            "GROUP BY day ORDER BY day"
    )
    suspend fun expensesPerDay(start: Long, end: Long): List<DayAmount>
}
