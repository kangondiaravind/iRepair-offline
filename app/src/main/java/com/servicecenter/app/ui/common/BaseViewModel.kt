package com.servicecenter.app.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servicecenter.app.core.AppException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

fun Throwable.userMessage(): String =
    if (this is AppException) message ?: "Something went wrong. Try again."
    else "Something went wrong. Try again."

/** Runs a suspend action, shows its error message, and exposes a busy flag to the screen. */
abstract class BaseViewModel : ViewModel() {
    var error by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    fun showError(message: String) { error = message }
    fun clearError() { error = null }

    protected fun perform(onDone: () -> Unit = {}, block: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true
            error = null
            try {
                block()
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.userMessage()
            } finally {
                busy = false
            }
        }
    }
}
