package com.servicecenter.app.data.repository

import androidx.room.withTransaction
import com.servicecenter.app.data.auth.SessionManager
import com.servicecenter.app.data.local.AppDatabase
import com.servicecenter.app.data.local.Permissions
import com.servicecenter.app.data.local.dao.ModeTotal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class PeriodReport(
    val jobsReceived: Int,
    val jobsDelivered: Int,
    val collectedByMode: List<ModeTotal>,
    val totalCollected: Long,
    val totalExpenses: Long,
    val totalDiscounts: Long
) {
    /** Per-job expenses only, so this is the repair margin, not net profit after rent and salary. */
    val profit: Long get() = totalCollected - totalExpenses
}

data class DayRow(val day: String, val collected: Long, val expenses: Long) {
    val profit: Long get() = collected - expenses
}

data class MonthlyReport(val summary: PeriodReport, val days: List<DayRow>)

/** Owner only (permission view_reports). Money counts on the day it was received. */
@Singleton
class ReportRepository @Inject constructor(
    private val db: AppDatabase,
    private val session: SessionManager
) {
    suspend fun daily(date: LocalDate): PeriodReport {
        session.require(Permissions.VIEW_REPORTS)
        return db.withTransaction { period(startOf(date), startOf(date.plusDays(1))) }
    }

    suspend fun monthly(month: YearMonth): MonthlyReport {
        session.require(Permissions.VIEW_REPORTS)
        val start = startOf(month.atDay(1))
        val end = startOf(month.plusMonths(1).atDay(1))
        return db.withTransaction {
            val report = db.reportDao()
            val collected = report.collectedPerDay(start, end).associate { it.day to it.amount }
            val spent = report.expensesPerDay(start, end).associate { it.day to it.amount }
            val days = (collected.keys + spent.keys).sorted().map { day ->
                DayRow(day, collected[day] ?: 0L, spent[day] ?: 0L)
            }
            MonthlyReport(period(start, end), days)
        }
    }

    private suspend fun period(start: Long, end: Long): PeriodReport {
        val report = db.reportDao()
        return PeriodReport(
            jobsReceived = report.jobsReceived(start, end),
            jobsDelivered = report.jobsDelivered(start, end),
            collectedByMode = report.collectedByMode(start, end),
            totalCollected = report.totalCollected(start, end),
            totalExpenses = report.totalExpenses(start, end),
            totalDiscounts = report.totalDiscounts(start, end)
        )
    }

    private fun startOf(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
