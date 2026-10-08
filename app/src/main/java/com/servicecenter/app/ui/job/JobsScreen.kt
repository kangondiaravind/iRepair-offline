package com.servicecenter.app.ui.job

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.servicecenter.app.data.local.dao.JobSummary
import com.servicecenter.app.data.local.entity.StatusEntity
import com.servicecenter.app.data.repository.ConfigRepository
import com.servicecenter.app.data.repository.JobRepository
import com.servicecenter.app.ui.common.AppTopBar
import com.servicecenter.app.ui.common.JobSummaryCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class JobsViewModel @Inject constructor(
    jobs: JobRepository,
    config: ConfigRepository
) : ViewModel() {
    val statuses: StateFlow<List<StatusEntity>> = config.statuses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** null means "All". */
    var selected by mutableStateOf<String?>(null)
        private set
    private val selectedFlow = MutableStateFlow<String?>(null)

    fun select(statusId: String?) {
        selected = statusId
        selectedFlow.value = statusId
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val list: StateFlow<List<JobSummary>> = selectedFlow
        .flatMapLatest { id -> if (id == null) jobs.observeRecent(200) else jobs.observeByStatus(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(onOpenJob: (String) -> Unit, vm: JobsViewModel = hiltViewModel()) {
    val statuses by vm.statuses.collectAsStateWithLifecycle()
    val jobs by vm.list.collectAsStateWithLifecycle()

    Scaffold(topBar = { AppTopBar("Jobs") }) { padding ->
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(selected = vm.selected == null, onClick = { vm.select(null) }, label = { Text("All") })
                }
                items(statuses, key = { it.id }) { status ->
                    FilterChip(
                        selected = vm.selected == status.id,
                        onClick = { vm.select(status.id) },
                        label = { Text(status.name) }
                    )
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (jobs.isEmpty()) {
                    item { Text("No jobs here.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(jobs, key = { it.job.id }) { summary ->
                    JobSummaryCard(summary, onClick = { onOpenJob(summary.job.id) })
                }
            }
        }
    }
}
