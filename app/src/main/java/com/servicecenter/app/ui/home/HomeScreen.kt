package com.servicecenter.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.servicecenter.app.data.auth.SessionManager
import com.servicecenter.app.data.local.Permissions
import com.servicecenter.app.data.local.dao.JobSummary
import com.servicecenter.app.data.local.entity.CustomerEntity
import com.servicecenter.app.data.repository.CustomerRepository
import com.servicecenter.app.data.repository.JobRepository
import com.servicecenter.app.ui.common.AppTopBar
import com.servicecenter.app.ui.common.JobSummaryCard
import com.servicecenter.app.ui.common.PrimaryButton
import com.servicecenter.app.ui.common.SecondaryButton
import com.servicecenter.app.ui.common.SectionLabel
import com.servicecenter.app.ui.common.can
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    customers: CustomerRepository,
    jobs: JobRepository,
    session: SessionManager
) : ViewModel() {
    val staffName: String = session.current.value?.name ?: ""

    var query by mutableStateOf("")
        private set
    private val queryFlow = MutableStateFlow("")

    fun onQuery(text: String) {
        query = text
        queryFlow.value = text
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<CustomerEntity>> = queryFlow
        .debounce(150)
        .flatMapLatest { customers.search(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val openJobs: StateFlow<List<JobSummary>> = jobs.observeOpen()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCustomer: (String) -> Unit,
    onNewCustomer: () -> Unit,
    onNewEnquiry: () -> Unit,
    onOpenJob: (String) -> Unit,
    onStaff: () -> Unit,
    onLogout: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val results by vm.results.collectAsStateWithLifecycle()
    val openJobs by vm.openJobs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar("Home", actions = {
                if (can(Permissions.MANAGE_STAFF)) {
                    IconButton(onClick = onStaff) { Icon(Icons.Default.Settings, contentDescription = "Staff") }
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Log out")
                }
            })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Signed in as ${vm.staffName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                OutlinedTextField(
                    value = vm.query,
                    onValueChange = vm::onQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search by phone or name") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
            }
            if (vm.query.isNotBlank()) {
                if (results.isEmpty()) {
                    item { Text("No customer found. Add them as a new customer.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(results, key = { it.id }) { customer ->
                        Card(onClick = { onOpenCustomer(customer.id) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(customer.name, fontWeight = FontWeight.Medium)
                                    Text(customer.customerCode, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(customer.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (can(Permissions.CREATE_CUSTOMER)) PrimaryButton("New customer", onNewCustomer, Modifier.weight(1f))
                    if (can(Permissions.LOG_ENQUIRY)) SecondaryButton("Walk-in enquiry", onNewEnquiry, Modifier.weight(1f))
                }
            }
            item { SectionLabel("Open jobs") }
            if (openJobs.isEmpty()) {
                item { Text("No open jobs.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(openJobs.take(15), key = { it.job.id }) { summary ->
                    JobSummaryCard(summary, onClick = { onOpenJob(summary.job.id) })
                }
            }
        }
    }
}
