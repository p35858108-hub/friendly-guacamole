package com.siri.assistant.service

import android.content.Intent
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

/**
 * Android refuses to list a VoiceInteractionService as a selectable assistant unless it
 * declares a RecognitionService. Real dictation uses the system recognizer
 * (RecognizerIntent) from AssistantActivity, so this one only reports "unavailable".
 */
class StubRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        listener?.error(SpeechRecognizer.ERROR_CLIENT)
    }

    override fun onCancel(listener: Callback?) = Unit

    override fun onStopListening(listener: Callback?) = Unit
}
