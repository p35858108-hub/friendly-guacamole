package com.siri.assistant

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siri.assistant.ui.SiriOverlay
import com.siri.assistant.ui.SiriTheme
import com.siri.assistant.util.WebSearchFallback
import java.util.Locale

class AssistantActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        viewModel.onVoiceResult(text)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        applyBlurBehind()

        setContent {
            SiriTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is AssistantEvent.OpenWebSearch -> {
                                WebSearchFallback.open(this@AssistantActivity, event.query)
                                finish()
                            }
                            AssistantEvent.StartVoiceInput -> launchSpeechRecognizer()
                            AssistantEvent.Close -> finish()
                        }
                    }
                }

                BackHandler { viewModel.close() }

                SiriOverlay(
                    state = state,
                    onOrbClick = viewModel::expand,
                    onQueryChange = viewModel::onQueryChange,
                    onSubmit = viewModel::submit,
                    onVoiceClick = viewModel::onVoiceClick,
                    onDismissResponse = viewModel::dismissResponse,
                    onDismissAll = viewModel::close,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.reset()
    }

    private fun launchSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Search or Ask")
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            viewModel.expand()
        }
    }

    /** Real frosted-glass of whatever app is behind us (Android 12+). */
    private fun applyBlurBehind() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = 48 }
        }
    }
}
