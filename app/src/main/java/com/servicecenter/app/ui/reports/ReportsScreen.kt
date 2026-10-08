package com.servicecenter.app.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.servicecenter.app.core.Money
import com.servicecenter.app.data.repository.MonthlyReport
import com.servicecenter.app.data.repository.PeriodReport
import com.servicecenter.app.data.repository.ReportRepository
import com.servicecenter.app.ui.common.AppTopBar
import com.servicecenter.app.ui.common.BaseViewModel
import com.servicecenter.app.ui.common.ErrorBanner
import com.servicecenter.app.ui.common.KeyValueRow
import com.servicecenter.app.ui.common.Panel
import com.servicecenter.app.ui.common.SectionLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reports: ReportRepository
) : BaseViewModel() {
    var monthlyMode by mutableStateOf(false)
        private set
    var date by mutableStateOf(LocalDate.now())
        private set
    var month by mutableStateOf(YearMonth.now())
        private set
    var daily by mutableStateOf<PeriodReport?>(null)
        private set
    var monthly by mutableStateOf<MonthlyReport?>(null)
        private set

    fun selectMonthly(value: Boolean) {
        monthlyMode = value
        load()
    }

    fun step(delta: Long) {
        if (monthlyMode) month = month.plusMonths(delta) else date = date.plusDays(delta)
        load()
    }

    fun load() {
        perform {
            if (monthlyMode) monthly = reports.monthly(month) else daily = reports.daily(date)
        }
    }
}

private val dayFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())
private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

@Composable
fun ReportsScreen(vm: ReportsViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.load() }

    Scaffold(topBar = { AppTopBar("Reports") }) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex = if (vm.monthlyMode) 1 else 0) {
                Tab(selected = !vm.monthlyMode, onClick = { vm.selectMonthly(false) }, text = { Text("Daily") })
                Tab(selected = vm.monthlyMode, onClick = { vm.selectMonthly(true) }, text = { Text("Monthly") })
            }
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { vm.step(-1) }) { Text("‹ Previous") }
                    Text(
                        if (vm.monthlyMode) vm.month.format(monthFormat) else vm.date.format(dayFormat),
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(onClick = { vm.step(1) }) { Text("Next ›") }
                }
                ErrorBanner(vm.error)
                val report = if (vm.monthlyMode) vm.monthly?.summary else vm.daily
                if (report != null) {
                    ReportBody(report)
                    if (vm.monthlyMode) {
                        vm.monthly?.let { m ->
                            SectionLabel("Day by day")
                            Row(Modifier.fillMaxWidth()) {
                                Text("Day", Modifier.weight(1.2f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("In", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Out", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Profit", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (m.days.isEmpty()) Text("No activity this month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            m.days.forEach { row ->
                                Row(Modifier.fillMaxWidth()) {
                                    Text(row.day.takeLast(5), Modifier.weight(1.2f))
                                    Text(Money.format(row.collected), Modifier.weight(1f))
                                    Text(Money.format(row.expenses), Modifier.weight(1f))
                                    Text(Money.format(row.profit), Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportBody(report: PeriodReport) {
    Panel {
        KeyValueRow("Jobs received", report.jobsReceived.toString())
        KeyValueRow("Jobs delivered", report.jobsDelivered.toString())
    }
    Panel {
        KeyValueRow("Collected", Money.format(report.totalCollected), bold = true)
        report.collectedByMode.forEach { KeyValueRow("  ${it.modeName}", Money.format(it.total)) }
        KeyValueRow("Expenses", Money.format(report.totalExpenses))
        KeyValueRow("Discounts given", Money.format(report.totalDiscounts))
    }
    Panel {
        KeyValueRow("Profit", Money.format(report.profit), bold = true)
        Text(
            "Collected minus job expenses. Rent and salary are not included.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
