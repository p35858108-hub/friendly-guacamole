package com.siri.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siri.assistant.data.GeminiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Phase { Orb, Input }

sealed interface ResponseState {
    data object Hidden : ResponseState
    data class Loading(val query: String) : ResponseState
    data class Success(val query: String, val text: String) : ResponseState
}

data class AssistantUiState(
    val phase: Phase = Phase.Orb,
    val query: String = "",
    val response: ResponseState = ResponseState.Hidden,
)

sealed interface AssistantEvent {
    data class OpenWebSearch(val query: String) : AssistantEvent
    data object StartVoiceInput : AssistantEvent
    data object Close : AssistantEvent
}

class AssistantViewModel(
    private val repository: GeminiRepository = GeminiRepository(BuildConfig.GEMINI_API_KEY),
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private val _events = Channel<AssistantEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var requestJob: Job? = null

    fun expand() = _uiState.update { it.copy(phase = Phase.Input) }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun onVoiceClick() {
        _events.trySend(AssistantEvent.StartVoiceInput)
    }

    fun onVoiceResult(text: String?) {
        if (text.isNullOrBlank()) return
        _uiState.update { it.copy(phase = Phase.Input, query = text) }
        submit()
    }

    fun submit() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return

        requestJob?.cancel()
        _uiState.update { it.copy(response = ResponseState.Loading(query)) }

        requestJob = viewModelScope.launch {
            repository.ask(query)
                .onSuccess { text ->
                    _uiState.update { it.copy(response = ResponseState.Success(query, text)) }
                }
                .onFailure {
                    // Never leave the user stuck: hand the exact text to Google in the browser.
                    _uiState.update { it.copy(response = ResponseState.Hidden) }
                    _events.send(AssistantEvent.OpenWebSearch(query))
                }
        }
    }

    fun dismissResponse() {
        requestJob?.cancel()
        _uiState.update { it.copy(response = ResponseState.Hidden) }
    }

    fun close() {
        requestJob?.cancel()
        _events.trySend(AssistantEvent.Close)
    }

    fun reset() {
        requestJob?.cancel()
        _uiState.value = AssistantUiState()
    }
}
