package com.servicecenter.app.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.servicecenter.app.data.local.Permissions
import com.servicecenter.app.data.local.dao.JobSummary
import com.servicecenter.app.data.local.entity.CustomerEntity
import com.servicecenter.app.data.repository.CustomerRepository
import com.servicecenter.app.data.repository.EnquiryRepository
import com.servicecenter.app.data.repository.JobRepository
import com.servicecenter.app.ui.common.AppTopBar
import com.servicecenter.app.ui.common.BaseViewModel
import com.servicecenter.app.ui.common.ErrorBanner
import com.servicecenter.app.ui.common.Field
import com.servicecenter.app.ui.common.JobSummaryCard
import com.servicecenter.app.ui.common.PrimaryButton
import com.servicecenter.app.ui.common.SectionLabel
import com.servicecenter.app.ui.common.can
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CustomerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    customers: CustomerRepository,
    jobRepository: JobRepository
) : ViewModel() {
    private val customerId: String = checkNotNull(savedState["customerId"])
    val customer: StateFlow<CustomerEntity?> = customers.observe(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val jobs: StateFlow<List<JobSummary>> = jobRepository.observeForCustomer(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun CustomerScreen(
    onBack: () -> Unit,
    onNewJob: (customerId: String) -> Unit,
    onOpenJob: (String) -> Unit,
    vm: CustomerViewModel = hiltViewModel()
) {
    val customer by vm.customer.collectAsStateWithLifecycle()
    val jobs by vm.jobs.collectAsStateWithLifecycle()
    val canCreateJob = can(Permissions.CREATE_JOB)
    Scaffold(topBar = { AppTopBar(customer?.name ?: "Customer", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            customer?.let { c ->
                item {
                    Text("${c.phone} · ${c.customerCode}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (canCreateJob) {
                    item { PrimaryButton("New job", onClick = { onNewJob(c.id) }) }
                }
            }
            item { SectionLabel("Past jobs") }
            if (jobs.isEmpty()) {
                item { Text("No jobs yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(jobs, key = { it.job.id }) { summary ->
                    JobSummaryCard(summary, onClick = { onOpenJob(summary.job.id) })
                }
            }
        }
    }
}

@HiltViewModel
class NewCustomerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val customers: CustomerRepository,
    private val enquiries: EnquiryRepository
) : BaseViewModel() {
    /** Set when this customer is being created from a walk-in enquiry. */
    val enquiryId: String? = savedState["enquiryId"]

    var name by mutableStateOf("")
        private set
    var phone by mutableStateOf("")
        private set
    var duplicate by mutableStateOf<CustomerEntity?>(null)
        private set

    init {
        enquiryId?.let { id ->
            viewModelScope.launch {
                enquiries.get(id)?.let { name = it.name; phone = it.phone }
            }
        }
    }

    fun onName(text: String) { name = text }
    fun onPhone(text: String) { phone = text }
    fun clearDuplicate() { duplicate = null }

    fun save(allowDuplicate: Boolean, onCreated: (String) -> Unit) {
        duplicate = null
        perform {
            if (!allowDuplicate) {
                val existing = customers.findByPhone(phone)
                if (existing != null) {
                    duplicate = existing
                    return@perform
                }
            }
            val customer = customers.create(name, phone, allowDuplicatePhone = allowDuplicate)
            onCreated(customer.id)
        }
    }
}

@Composable
fun NewCustomerScreen(
    onBack: () -> Unit,
    onCreated: (customerId: String, enquiryId: String?) -> Unit,
    vm: NewCustomerViewModel = hiltViewModel()
) {
    Scaffold(topBar = { AppTopBar("New customer", onBack) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Field("Name", vm.name, vm::onName)
            Field("Phone number", vm.phone, vm::onPhone, keyboard = KeyboardType.Phone)
            ErrorBanner(vm.error)
            PrimaryButton(
                "Save and continue",
                onClick = { vm.save(false) { id -> onCreated(id, vm.enquiryId) } },
                enabled = !vm.busy
            )
        }
    }

    vm.duplicate?.let { existing ->
        AlertDialog(
            onDismissRequest = vm::clearDuplicate,
            title = { Text("This number is already saved") },
            text = { Text("${existing.name} (${existing.customerCode}) uses this phone number.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearDuplicate()
                    onCreated(existing.id, vm.enquiryId)
                }) { Text("Use existing") }
            },
            dismissButton = {
                TextButton(onClick = { vm.save(true) { id -> onCreated(id, vm.enquiryId) } }) { Text("Add anyway") }
            }
        )
    }
}
