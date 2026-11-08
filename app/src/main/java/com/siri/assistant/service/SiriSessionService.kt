package com.siri.assistant.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.siri.assistant.AssistantActivity

class SiriSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = SiriSession(this)
}

/**
 * The session window cannot easily host Compose (it has no Lifecycle/SavedState owners),
 * so on every invocation (long-press home, swipe from corner, power button…) we launch a
 * transparent assistant activity that renders the whole UI, then hide the empty session.
 */
class SiriSession(context: Context) : VoiceInteractionSession(context) {

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        val intent = Intent(context, AssistantActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startAssistantActivity(intent)
        hide()
    }
}
