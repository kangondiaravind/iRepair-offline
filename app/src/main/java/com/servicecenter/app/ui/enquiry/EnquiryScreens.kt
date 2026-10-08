package com.servicecenter.app.ui.enquiry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.servicecenter.app.data.local.Permissions
import com.servicecenter.app.data.local.entity.EnquiryEntity
import com.servicecenter.app.data.repository.CustomerRepository
import com.servicecenter.app.data.repository.EnquiryRepository
import com.servicecenter.app.ui.common.AppTopBar
import com.servicecenter.app.ui.common.BaseViewModel
import com.servicecenter.app.ui.common.ErrorBanner
import com.servicecenter.app.ui.common.Field
import com.servicecenter.app.ui.common.Pill
import com.servicecenter.app.ui.common.PrimaryButton
import com.servicecenter.app.ui.common.SecondaryButton
import com.servicecenter.app.ui.common.Tone
import com.servicecenter.app.ui.common.can
import com.servicecenter.app.ui.common.formatDateTime
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class EnquiriesViewModel @Inject constructor(
    enquiries: EnquiryRepository,
    private val customers: CustomerRepository
) : BaseViewModel() {
    val list: StateFlow<List<EnquiryEntity>> = enquiries.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** If the phone number already belongs to a customer, go straight to a new job for them. */
    fun convert(enquiry: EnquiryEntity, onExisting: (customerId: String) -> Unit, onNew: () -> Unit) {
        perform {
            val existing = if (enquiry.phone.isBlank()) null else customers.findByPhone(enquiry.phone)
            if (existing != null) onExisting(existing.id) else onNew()
        }
    }
}

@Composable
fun EnquiriesScreen(
    onNew: () -> Unit,
    onConvertExisting: (customerId: String, enquiryId: String) -> Unit,
    onConvertNew: (enquiryId: String) -> Unit,
    vm: EnquiriesViewModel = hiltViewModel()
) {
    val list by vm.list.collectAsStateWithLifecycle()
    val canLogEnquiry = can(Permissions.LOG_ENQUIRY)

    Scaffold(topBar = { AppTopBar("Enquiries") }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (canLogEnquiry) {
                item { PrimaryButton("New enquiry", onClick = onNew) }
            }
            item { ErrorBanner(vm.error) }
            if (list.isEmpty()) {
                item { Text("No enquiries yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(list, key = { it.id }) { enquiry ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                listOf(enquiry.name, enquiry.phone).filter { it.isNotBlank() }.joinToString(" · "),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                formatDateTime(enquiry.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            listOf(enquiry.model, enquiry.notes).filter { it.isNotBlank() }.joinToString(" · "),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (enquiry.jobId != null) {
                            Pill("Converted to job", Tone.Good)
                        } else if (can(Permissions.CREATE_JOB)) {
                            SecondaryButton(
                                "Convert to job",
                                onClick = {
                                    vm.convert(
                                        enquiry,
                                        onExisting = { customerId -> onConvertExisting(customerId, enquiry.id) },
                                        onNew = { onConvertNew(enquiry.id) }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@HiltViewModel
class NewEnquiryViewModel @Inject constructor(
    private val enquiries: EnquiryRepository
) : BaseViewModel() {
    fun save(name: String, phone: String, model: String, notes: String, onDone: () -> Unit) {
        perform(onDone) { enquiries.log(name, phone, model, notes) }
    }
}

@Composable
fun NewEnquiryScreen(onBack: () -> Unit, vm: NewEnquiryViewModel = hiltViewModel()) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    Scaffold(topBar = { AppTopBar("Walk-in enquiry", onBack) }) { padding ->
        Column(
            Modifier.padding(padding).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Field("Name", name, { name = it })
            Field("Phone number", phone, { phone = it }, keyboard = KeyboardType.Phone)
            Field("Model", model, { model = it })
            Field("Notes", notes, { notes = it }, minLines = 3)
            ErrorBanner(vm.error)
            PrimaryButton("Save enquiry", onClick = { vm.save(name, phone, model, notes, onBack) }, enabled = !vm.busy)
            Text(
                "No job is created. Convert it later if the customer decides to repair.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
