package com.servicecenter.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.app.data.auth.AuthRepository
import com.servicecenter.app.ui.common.BaseViewModel
import com.servicecenter.app.ui.common.ErrorBanner
import com.servicecenter.app.ui.common.Field
import com.servicecenter.app.ui.common.PrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(private val auth: AuthRepository) : BaseViewModel() {
    var pin by mutableStateOf("")
        private set
    var length by mutableStateOf(4)
        private set
    var wrongPin by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch { length = auth.pinLength() }
    }

    fun onDigit(digit: Char) {
        if (pin.length >= length) return
        wrongPin = false
        pin += digit
        if (pin.length == length) submit()
    }

    fun onBackspace() {
        pin = pin.dropLast(1)
    }

    /** On success the session changes and the app moves on by itself. */
    private fun submit() {
        viewModelScope.launch {
            val staff = auth.login(pin)
            wrongPin = staff == null
            pin = ""
        }
    }
}

@Composable
fun LoginScreen(vm: LoginViewModel = hiltViewModel()) {
    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(horizontal = 24.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "IR",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Service center", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text("Enter your PIN to continue", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(vm.length) { index ->
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index < vm.pin.length) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (vm.wrongPin) "Wrong PIN. Try again." else " ",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    keys.chunked(3).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { key ->
                                if (key.isEmpty()) {
                                    Spacer(Modifier.weight(1f).height(56.dp))
                                } else {
                                    val onClick = {
                                        if (key == "⌫") vm.onBackspace() else vm.onDigit(key[0])
                                    }
                                    if (key == "⌫") {
                                        OutlinedButton(
                                            onClick = onClick,
                                            modifier = Modifier.weight(1f).height(56.dp),
                                            shape = MaterialTheme.shapes.medium
                                        ) { Text(key, fontSize = 20.sp) }
                                    } else {
                                        FilledTonalButton(
                                            onClick = onClick,
                                            modifier = Modifier.weight(1f).height(56.dp),
                                            shape = MaterialTheme.shapes.medium
                                        ) {
                                            Text(key, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@HiltViewModel
class OwnerSetupViewModel @Inject constructor(private val auth: AuthRepository) : BaseViewModel() {
    fun create(name: String, pin: String, confirm: String, onDone: () -> Unit) {
        if (pin != confirm) {
            showError("The two PINs do not match")
            return
        }
        perform(onDone) { auth.createFirstOwner(name, pin) }
    }
}

@Composable
fun OwnerSetupScreen(onDone: () -> Unit, vm: OwnerSetupViewModel = hiltViewModel()) {
    var name by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Set up your shop", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "You are the owner. Choose a 4-digit PIN to open the app.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Field("Your name", name, { name = it })
                Field("PIN", pin, { pin = it.filter(Char::isDigit).take(4) }, keyboard = KeyboardType.NumberPassword, password = true)
                Field("Confirm PIN", confirm, { confirm = it.filter(Char::isDigit).take(4) }, keyboard = KeyboardType.NumberPassword, password = true)
                ErrorBanner(vm.error)
                PrimaryButton("Create owner", onClick = { vm.create(name, pin, confirm, onDone) }, enabled = !vm.busy)
            }
        }
    }
}
