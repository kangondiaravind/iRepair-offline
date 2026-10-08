package com.servicecenter.app.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.servicecenter.app.data.auth.AuthRepository
import com.servicecenter.app.data.local.Roles
import com.servicecenter.app.data.local.entity.StaffEntity
import com.servicecenter.app.ui.common.AppTopBar
import com.servicecenter.app.ui.common.BaseViewModel
import com.servicecenter.app.ui.common.ErrorBanner
import com.servicecenter.app.ui.common.Field
import com.servicecenter.app.ui.common.Pill
import com.servicecenter.app.ui.common.PrimaryButton
import com.servicecenter.app.ui.common.SecondaryButton
import com.servicecenter.app.ui.common.Tone
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StaffViewModel @Inject constructor(
    private val auth: AuthRepository
) : BaseViewModel() {
    val staff: StateFlow<List<StaffEntity>> = auth.observeStaff()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String, pin: String, onDone: () -> Unit) = perform(onDone) { auth.addStaff(name, pin) }
    fun changePin(staffId: String, pin: String, onDone: () -> Unit) = perform(onDone) { auth.changePin(staffId, pin) }
    fun deactivate(staffId: String) = perform { auth.deactivate(staffId) }
}

private sealed interface StaffDialog {
    data object Add : StaffDialog
    data class ChangePin(val member: StaffEntity) : StaffDialog
    data class Deactivate(val member: StaffEntity) : StaffDialog
}

@Composable
fun StaffScreen(onBack: () -> Unit, vm: StaffViewModel = hiltViewModel()) {
    val staff by vm.staff.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<StaffDialog?>(null) }

    Scaffold(topBar = { AppTopBar("Staff", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { PrimaryButton("Add staff", onClick = { vm.clearError(); dialog = StaffDialog.Add }) }
            item { ErrorBanner(if (dialog == null) vm.error else null) }
            items(staff, key = { it.id }) { member ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(member.name, fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Pill(if (member.role == Roles.OWNER) "Owner" else "Staff", Tone.Info)
                                if (!member.active) Pill("Inactive", Tone.Neutral)
                            }
                        }
                        if (member.active) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SecondaryButton("Change PIN", { vm.clearError(); dialog = StaffDialog.ChangePin(member) }, Modifier.weight(1f))
                                SecondaryButton("Deactivate", { vm.clearError(); dialog = StaffDialog.Deactivate(member) }, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Permissions and settings will move to the web Admin Panel later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    when (val d = dialog) {
        StaffDialog.Add -> PinDialog(
            title = "Add staff",
            askName = true,
            vm = vm,
            onDismiss = { dialog = null }
        ) { name, pin -> vm.add(name, pin) { dialog = null } }

        is StaffDialog.ChangePin -> PinDialog(
            title = "New PIN for ${d.member.name}",
            askName = false,
            vm = vm,
            onDismiss = { dialog = null }
        ) { _, pin -> vm.changePin(d.member.id, pin) { dialog = null } }

        is StaffDialog.Deactivate -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Deactivate ${d.member.name}?") },
            text = { Text("They will no longer be able to log in. Their past entries stay in the records.") },
            confirmButton = {
                TextButton(onClick = { vm.deactivate(d.member.id); dialog = null }) { Text("Deactivate") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } }
        )

        null -> Unit
    }
}

@Composable
private fun PinDialog(
    title: String,
    askName: Boolean,
    vm: StaffViewModel,
    onDismiss: () -> Unit,
    onSave: (name: String, pin: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (askName) Field("Name", name, { name = it })
                Field("PIN", pin, { pin = it.filter(Char::isDigit).take(4) }, keyboard = KeyboardType.NumberPassword, password = true)
                ErrorBanner(vm.error)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, pin) }, enabled = !vm.busy) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
