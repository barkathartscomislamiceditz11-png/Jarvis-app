package com.example.data.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {

  private val client = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  private const val MODEL_NAME = "gemini-3.5-flash"
  private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

  suspend fun askJarvis(prompt: String, isMathTutorMode: Boolean = true): String = withContext(Dispatchers.IO) {
    val apiKey = try {
      BuildConfig.GEMINI_API_KEY
    } catch (e: Throwable) {
      ""
    }

    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      // Offline fallback
      return@withContext JarvisOfflineEngine.processQuery(prompt).responseText
    }

    try {
      val systemPrompt = if (isMathTutorMode) {
        "You are JARVIS, Tony Stark's personal artificial intelligence system and a brilliant mathematics tutor and school homework coach. " +
        "You speak politely, intelligently, and warmly ('sir', 'young scholar'). " +
        "When explaining maths or science, break problems down step-by-step, highlight key formulas, and explain the intuition clearly so the student masters the concept. " +
        "Keep your response concise, well-formatted, and visually structured with bullet points."
      } else {
        "You are JARVIS, the legendary artificial intelligence assistant. Speak with British elegance, efficiency, and high-tech poise ('At your command, sir'). " +
        "Assist with school assignments, personal productivity, and data organization."
      }

      val requestJson = JSONObject().apply {
        val contents = JSONArray().apply {
          val contentObj = JSONObject().apply {
            val parts = JSONArray().apply {
              put(JSONObject().apply {
                put("text", prompt)
              })
            }
            put("parts", parts)
          }
          put(contentObj)
        }
        put("contents", contents)

        val systemInstruction = JSONObject().apply {
          val parts = JSONArray().apply {
            put(JSONObject().apply {
              put("text", systemPrompt)
            })
          }
          put("parts", parts)
        }
        put("systemInstruction", systemInstruction)

        val generationConfig = JSONObject().apply {
          put("temperature", 0.6)
          put("topP", 0.95)
          put("topK", 40)
        }
        put("generationConfig", generationConfig)
      }

      val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
      val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())

      val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

      val response = client.newCall(request).execute()
      val responseString = response.body?.string()

      if (!response.isSuccessful || responseString == null) {
        // Return offline engine result if API error occurs
        return@withContext JarvisOfflineEngine.processQuery(prompt).responseText
      }

      val json = JSONObject(responseString)
      val candidates = json.optJSONArray("candidates")
      val firstCandidate = candidates?.optJSONObject(0)
      val content = firstCandidate?.optJSONObject("content")
      val parts = content?.optJSONArray("parts")
      val text = parts?.optJSONObject(0)?.optString("text")

      if (!text.isNullOrBlank()) {
        text.trim()
      } else {
        JarvisOfflineEngine.processQuery(prompt).responseText
      }
    } catch (e: Exception) {
      // Offline fallback on network timeout or connection error
      JarvisOfflineEngine.processQuery(prompt).responseText
    }
  }
}
