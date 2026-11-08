package com.siri.assistant.service

import android.service.voice.VoiceInteractionService
import android.util.Log

/**
 * Bound by the system once the user selects this app under
 * Settings > Apps > Default apps > Digital assistant app.
 */
class SiriVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        Log.d("SiriVIS", "Assistant service ready")
    }
}
