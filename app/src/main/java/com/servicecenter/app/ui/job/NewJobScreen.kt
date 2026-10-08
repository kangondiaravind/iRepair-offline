package com.servicecenter.app.ui.job

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.servicecenter.app.core.Money
import com.servicecenter.app.data.local.Permissions
import com.servicecenter.app.data.local.entity.CustomerEntity
import com.servicecenter.app.data.local.entity.DeviceTypeEntity
import com.servicecenter.app.data.local.entity.PaymentModeEntity
import com.servicecenter.app.data.repository.ConfigRepository
import com.servicecenter.app.data.repository.CustomerRepository
import com.servicecenter.app.data.repository.EnquiryRepository
import com.servicecenter.app.data.repository.JobRepository
import com.servicecenter.app.data.repository.NewJob
import com.servicecenter.app.ui.common.AppTopBar
import com.servicecenter.app.ui.common.BaseViewModel
import com.servicecenter.app.ui.common.ErrorBanner
import com.servicecenter.app.ui.common.Field
import com.servicecenter.app.ui.common.MoneyField
import com.servicecenter.app.ui.common.PrimaryButton
import com.servicecenter.app.ui.common.SectionLabel
import com.servicecenter.app.ui.common.can
import com.servicecenter.app.ui.common.parseAmount
import com.servicecenter.app.ui.common.parsePaymentParts
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NewJobViewModel @Inject constructor(
    savedState: SavedStateHandle,
    customers: CustomerRepository,
    private val jobs: JobRepository,
    private val enquiries: EnquiryRepository,
    config: ConfigRepository
) : BaseViewModel() {
    private val customerId: String = checkNotNull(savedState["customerId"])

    /** Set when the job is created from a walk-in enquiry: the enquiry is linked to the new job. */
    private val enquiryId: String? = savedState["enquiryId"]

    val customer: StateFlow<CustomerEntity?> = customers.observe(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val deviceTypes: StateFlow<List<DeviceTypeEntity>> = config.deviceTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val paymentModes: StateFlow<List<PaymentModeEntity>> = config.paymentModes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var deviceTypeId by mutableStateOf("")
    var model by mutableStateOf("")
    var problem by mutableStateOf("")
    var accessories by mutableStateOf("")
    var estimate by mutableStateOf("")

    /** payment mode id -> amount text. */
    val advance = mutableStateMapOf<String, String>()

    init {
        viewModelScope.launch {
            val first = config.deviceTypes().first { it.isNotEmpty() }.first()
            if (deviceTypeId.isEmpty()) deviceTypeId = first.id
        }
        enquiryId?.let { id ->
            viewModelScope.launch {
                enquiries.get(id)?.let {
                    model = it.model
                    problem = it.notes
                }
            }
        }
    }

    fun save(onCreated: (String) -> Unit) {
        perform {
            val estimatePaise = parseAmount(estimate, "the estimation", allowZero = true)
            val parts = parsePaymentParts(advance)
            val input = NewJob(customerId, deviceTypeId, model, problem, accessories, estimatePaise)
            val job = if (enquiryId != null) {
                enquiries.convertToJob(enquiryId, input, parts)
            } else {
                jobs.createJob(input, parts)
            }
            onCreated(job.id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewJobScreen(
    onBack: () -> Unit,
    onCreated: (jobId: String) -> Unit,
    vm: NewJobViewModel = hiltViewModel()
) {
    val customer by vm.customer.collectAsStateWithLifecycle()
    val deviceTypes by vm.deviceTypes.collectAsStateWithLifecycle()
    val modes by vm.paymentModes.collectAsStateWithLifecycle()

    Scaffold(topBar = { AppTopBar("New job", onBack) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            customer?.let {
                Text("${it.name} · ${it.customerCode}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                deviceTypes.forEach { type ->
                    FilterChip(
                        selected = vm.deviceTypeId == type.id,
                        onClick = { vm.deviceTypeId = type.id },
                        label = { Text(type.name) }
                    )
                }
            }
            Field("Model", vm.model, { vm.model = it })
            Field("Problem description", vm.problem, { vm.problem = it }, minLines = 2)
            Field("Accessories given", vm.accessories, { vm.accessories = it })
            MoneyField("Estimation", vm.estimate, { vm.estimate = it })

            if (can(Permissions.RECORD_PAYMENT) && modes.isNotEmpty()) {
                SectionLabel("Advance payment (optional)")
                modes.forEach { mode ->
                    MoneyField(mode.name, vm.advance[mode.id] ?: "", { text -> vm.advance[mode.id] = text })
                }
            }
            ErrorBanner(vm.error)
            PrimaryButton("Save job", onClick = { vm.save(onCreated) }, enabled = !vm.busy)
            Text(
                "Date, time and job number are filled in automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
