package com.siri.assistant.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal Gemini REST client. Every failure mode (no key, no network, timeout, HTTP error,
 * blocked/empty answer, malformed JSON) is returned as Result.failure so the UI can fall
 * back to a Google search instead of hanging.
 */
class GeminiRepository(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val timeoutMs: Long = 15_000,
) {

    suspend fun ask(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalStateException("Missing GEMINI_API_KEY"))
        try {
            Result.success(withTimeout(timeoutMs) { request(prompt) })
        } catch (e: TimeoutCancellationException) {
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun request(prompt: String): String {
        val connection = (URL("$BASE_URL/$model:generateContent").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs.toInt()
            readTimeout = timeoutMs.toInt()
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", apiKey)
        }
        try {
            connection.outputStream.use { it.write(buildBody(prompt).toByteArray()) }
            val code = connection.responseCode
            if (code !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw IOException("Gemini HTTP $code: $error")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return parseText(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildBody(prompt: String): String = JSONObject()
        .put(
            "systemInstruction",
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT)))
        )
        .put(
            "contents",
            JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            )
        )
        .toString()

    private fun parseText(body: String): String {
        val candidates = JSONObject(body).optJSONArray("candidates")
            ?: throw IOException("No candidates (prompt may have been blocked)")
        val parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
        val text = buildString {
            for (i in 0 until parts.length()) append(parts.getJSONObject(i).optString("text"))
        }.trim()
        if (text.isEmpty()) throw IOException("Empty response")
        return text
    }

    private companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        // Swap for any current model ID from https://ai.google.dev/gemini-api/docs/models
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        const val SYSTEM_PROMPT =
            "You are a concise, friendly phone assistant. Answer in the user's language in a few short sentences."
    }
}
