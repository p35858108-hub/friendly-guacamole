package com.siri.assistant.util

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object WebSearchFallback {

    /**
     * Opens the default browser on a Google search for the user's exact text.
     * Falls back to ACTION_WEB_SEARCH (any search app) if no browser handles https.
     */
    fun open(context: Context, query: String): Boolean {
        val uri = Uri.parse("https://www.google.com/search")
            .buildUpon()
            .appendQueryParameter("q", query)
            .build()

        val browserIntent = Intent(Intent.ACTION_VIEW, uri)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(browserIntent)
            true
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_WEB_SEARCH)
                        .putExtra(SearchManager.QUERY, query)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
    }
}
