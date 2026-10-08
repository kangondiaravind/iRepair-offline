package com.servicecenter.app.ui.job

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.servicecenter.app.core.Money
import com.servicecenter.app.data.local.DefaultIds
import com.servicecenter.app.data.local.Permissions
import com.servicecenter.app.data.local.dao.JobSummary
import com.servicecenter.app.data.local.dao.PaymentWithMode
import com.servicecenter.app.data.local.entity.ExpenseEntity
import com.servicecenter.app.data.local.entity.PaymentModeEntity
import com.servicecenter.app.data.local.entity.StatusEntity
import com.servicecenter.app.data.repository.ConfigRepository
import com.servicecenter.app.data.repository.ExpenseRepository
import com.servicecenter.app.data.repository.JobRepository
import com.servicecenter.app.data.repository.PaymentRepository
import com.servicecenter.app.ui.common.AppTopBar
import com.servicecenter.app.ui.common.BaseViewModel
import com.servicecenter.app.ui.common.ErrorBanner
import com.servicecenter.app.ui.common.Field
import com.servicecenter.app.ui.common.KeyValueRow
import com.servicecenter.app.ui.common.MoneyField
import com.servicecenter.app.ui.common.Panel
import com.servicecenter.app.ui.common.Pill
import com.servicecenter.app.ui.common.PrimaryButton
import com.servicecenter.app.ui.common.SecondaryButton
import com.servicecenter.app.ui.common.SectionLabel
import com.servicecenter.app.ui.common.Sheet
import com.servicecenter.app.ui.common.can
import com.servicecenter.app.ui.common.formatDateTime
import com.servicecenter.app.ui.common.paiseToInput
import com.servicecenter.app.ui.common.parseAmount
import com.servicecenter.app.ui.common.parsePaymentParts
import com.servicecenter.app.ui.common.statusTone
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val jobRepository: JobRepository,
    private val payments: PaymentRepository,
    private val expenses: ExpenseRepository,
    config: ConfigRepository
) : BaseViewModel() {
    private val jobId: String = checkNotNull(savedState["jobId"])

    val summary: StateFlow<JobSummary?> = jobRepository.observe(jobId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val paymentList: StateFlow<List<PaymentWithMode>> = payments.observeForJob(jobId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val expenseList: StateFlow<List<ExpenseEntity>> = expenses.observeForJob(jobId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val statuses: StateFlow<List<StatusEntity>> = config.statuses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val paymentModes: StateFlow<List<PaymentModeEntity>> = config.paymentModes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun changeStatus(statusId: String) = perform { jobRepository.updateStatus(jobId, statusId) }

    fun recordPayment(amounts: Map<String, String>, onDone: () -> Unit) =
        perform(onDone) { payments.record(jobId, parsePaymentParts(amounts)) }

    fun refund(modeId: String, amountText: String, onDone: () -> Unit) = perform(onDone) {
        payments.refund(jobId, modeId, parseAmount(amountText, "the refund amount"))
    }

    fun addExpense(description: String, amountText: String, onDone: () -> Unit) = perform(onDone) {
        expenses.add(jobId, description, parseAmount(amountText, "the expense amount"))
    }

    fun editFinalAmount(amountText: String, note: String, onDone: () -> Unit) = perform(onDone) {
        jobRepository.updateFinalAmount(jobId, parseAmount(amountText, "the final amount", allowZero = true), note)
    }

    fun voidPayment(id: String) = perform { payments.void(id) }
    fun voidExpense(id: String) = perform { expenses.void(id) }
}

private enum class DetailSheet { Payment, Expense, FinalAmount, Refund }

private data class VoidTarget(val id: String, val isPayment: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(onBack: () -> Unit, vm: JobDetailViewModel = hiltViewModel()) {
    val summary by vm.summary.collectAsStateWithLifecycle()
    val payments by vm.paymentList.collectAsStateWithLifecycle()
    val expenses by vm.expenseList.collectAsStateWithLifecycle()
    val statuses by vm.statuses.collectAsStateWithLifecycle()
    val modes by vm.paymentModes.collectAsStateWithLifecycle()

    var sheet by rememberSaveable { mutableStateOf<DetailSheet?>(null) }
    var statusMenu by remember { mutableStateOf(false) }
    var voidTarget by remember { mutableStateOf<VoidTarget?>(null) }

    fun open(which: DetailSheet) {
        vm.clearError()
        sheet = which
    }

    Scaffold(topBar = { AppTopBar(summary?.job?.jobNo ?: "Job", onBack) }) { padding ->
        val s = summary
        if (s == null) {
            Box(Modifier.padding(padding).padding(16.dp)) { Text("Loading…") }
            return@Scaffold
        }
        val job = s.job
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("${s.customerName} · ${s.deviceTypeName} · ${job.model}", fontWeight = FontWeight.Medium)
            Text(job.problem)
            if (job.accessories.isNotBlank()) {
                Text("Accessories: ${job.accessories}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "Received ${formatDateTime(job.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Pill(s.statusName, statusTone(job.statusId))
                if (can(Permissions.UPDATE_STATUS)) {
                    Box {
                        TextButton(onClick = { statusMenu = true }) { Text("Change status") }
                        DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                            statuses.forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status.name) },
                                    onClick = {
                                        statusMenu = false
                                        vm.changeStatus(status.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            ErrorBanner(if (sheet == null) vm.error else null)

            Panel {
                KeyValueRow("Estimate", Money.format(job.estimate))
                KeyValueRow("Final amount", Money.format(job.finalAmount))
                if (s.discount > 0) KeyValueRow("Discount", Money.format(s.discount))
                if (s.extraCharge > 0) KeyValueRow("Extra charge", Money.format(s.extraCharge))
                job.discountNote?.let { KeyValueRow("Note", it) }
                KeyValueRow("Paid", Money.format(s.totalPaid))
                KeyValueRow("Balance", Money.format(s.balance), bold = true)
                if (can(Permissions.VIEW_REPORTS)) {
                    KeyValueRow("Expenses", Money.format(s.totalExpenses))
                    KeyValueRow("Profit", Money.format(s.profit), bold = true)
                }
            }

            SectionLabel("Payments")
            if (payments.isEmpty()) Text("No payments yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            payments.forEach { p ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        val amount = if (p.payment.amount < 0) "Refund ${Money.format(-p.payment.amount)}" else Money.format(p.payment.amount)
                        Text(amount)
                        Text(
                            "${p.modeName} · ${formatDateTime(p.payment.paidAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (can(Permissions.CORRECT_PAYMENT)) {
                        IconButton(onClick = { voidTarget = VoidTarget(p.payment.id, true) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Void payment")
                        }
                    }
                }
            }

            SectionLabel("Expenses")
            if (expenses.isEmpty()) Text("No expenses yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            expenses.forEach { e ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(e.description)
                        Text(
                            Money.format(e.amount) + " · " + formatDateTime(e.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (can(Permissions.CORRECT_EXPENSE)) {
                        IconButton(onClick = { voidTarget = VoidTarget(e.id, false) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Void expense")
                        }
                    }
                }
            }

            if (can(Permissions.RECORD_PAYMENT) && s.balance > 0 && job.statusId != DefaultIds.STATUS_CANCELLED) {
                PrimaryButton("Record payment", onClick = { open(DetailSheet.Payment) })
            }
            if (can(Permissions.ADD_EXPENSE)) {
                SecondaryButton("Add expense", onClick = { open(DetailSheet.Expense) })
            }
            if (can(Permissions.EDIT_FINAL_AMOUNT)) {
                SecondaryButton("Edit final amount", onClick = { open(DetailSheet.FinalAmount) })
            }
            if (can(Permissions.CORRECT_PAYMENT) && s.totalPaid > 0) {
                SecondaryButton("Refund", onClick = { open(DetailSheet.Refund) })
            }
        }

        when (sheet) {
            DetailSheet.Payment -> PaymentSheet(vm, s, modes) { sheet = null }
            DetailSheet.Expense -> ExpenseSheet(vm) { sheet = null }
            DetailSheet.FinalAmount -> FinalAmountSheet(vm, s) { sheet = null }
            DetailSheet.Refund -> RefundSheet(vm, s, modes) { sheet = null }
            null -> Unit
        }

        voidTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { voidTarget = null },
                title = { Text(if (target.isPayment) "Void this payment?" else "Void this expense?") },
                text = { Text("It stops counting in totals and reports. The entry stays in the history.") },
                confirmButton = {
                    TextButton(onClick = {
                        if (target.isPayment) vm.voidPayment(target.id) else vm.voidExpense(target.id)
                        voidTarget = null
                    }) { Text("Void") }
                },
                dismissButton = { TextButton(onClick = { voidTarget = null }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun PaymentSheet(vm: JobDetailViewModel, s: JobSummary, modes: List<PaymentModeEntity>, onClose: () -> Unit) {
    val amounts = remember { mutableStateMapOf<String, String>() }
    val total = amounts.values.sumOf { text -> com.servicecenter.app.core.Money.parseRupeesToPaise(text) ?: 0L }
    Sheet("Record payment", onClose) {
        KeyValueRow("Balance", Money.format(s.balance))
        modes.forEach { mode ->
            MoneyField(mode.name, amounts[mode.id] ?: "", { text -> amounts[mode.id] = text })
        }
        KeyValueRow("Total", Money.format(total), bold = true)
        KeyValueRow("Left to pay", Money.format(s.balance - total))
        ErrorBanner(vm.error)
        PrimaryButton("Save payment", onClick = { vm.recordPayment(amounts.toMap(), onClose) }, enabled = !vm.busy)
    }
}

@Composable
private fun ExpenseSheet(vm: JobDetailViewModel, onClose: () -> Unit) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    Sheet("Add expense", onClose) {
        Field("What was it for", description, { description = it })
        MoneyField("Amount", amount, { amount = it })
        ErrorBanner(vm.error)
        PrimaryButton("Save expense", onClick = { vm.addExpense(description, amount, onClose) }, enabled = !vm.busy)
    }
}

@Composable
private fun FinalAmountSheet(vm: JobDetailViewModel, s: JobSummary, onClose: () -> Unit) {
    var amount by remember { mutableStateOf(paiseToInput(s.job.finalAmount)) }
    var note by remember { mutableStateOf(s.job.discountNote ?: "") }
    val entered = com.servicecenter.app.core.Money.parseRupeesToPaise(amount)
    Sheet("Edit final amount", onClose) {
        KeyValueRow("Estimate", Money.format(s.job.estimate))
        MoneyField("Final amount", amount, { amount = it })
        if (entered != null) {
            val diff = s.job.estimate - entered
            if (diff > 0) KeyValueRow("Discount", Money.format(diff))
            if (diff < 0) KeyValueRow("Extra charge", Money.format(-diff))
            KeyValueRow("Paid so far", Money.format(s.totalPaid))
            KeyValueRow("New balance", Money.format(entered - s.totalPaid), bold = true)
        }
        Field("Note (optional)", note, { note = it })
        ErrorBanner(vm.error)
        PrimaryButton("Save", onClick = { vm.editFinalAmount(amount, note, onClose) }, enabled = !vm.busy)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefundSheet(vm: JobDetailViewModel, s: JobSummary, modes: List<PaymentModeEntity>, onClose: () -> Unit) {
    var modeId by remember { mutableStateOf(modes.firstOrNull()?.id ?: "") }
    var amount by remember { mutableStateOf("") }
    Sheet("Refund", onClose) {
        KeyValueRow("Received so far", Money.format(s.totalPaid))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.forEach { mode ->
                FilterChip(selected = modeId == mode.id, onClick = { modeId = mode.id }, label = { Text(mode.name) })
            }
        }
        MoneyField("Refund amount", amount, { amount = it })
        ErrorBanner(vm.error)
        PrimaryButton("Save refund", onClick = { vm.refund(modeId, amount, onClose) }, enabled = !vm.busy)
    }
}
