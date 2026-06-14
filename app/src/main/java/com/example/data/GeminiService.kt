package com.example.data

import android.util.Log
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

/**
 * Service companion that connects with the Google Generative AI (Gemini) REST API
 * to generate environment recommendations and personalized eco insights based on
 * the user's daily carbon footprint metrics and logged reduction habits.
 */
public object GeminiService {
    private const val TAG: String = "GeminiService"
    private const val MODEL_NAME: String = "gemini-3.5-flash"
    private const val BASE_URL: String = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Synthesizes day metrics and reductions list into an LLM prompt and queries Gemini API
     * asynchronously. Returns a formatted markdown guide with assessments and tips.
     *
     * @param totalCo2 Sum total of computed emission metrics in kg CO2.
     * @param fields Key-value map representing the decomposed footprints from transit, utilities, diet, and waste.
     * @param loggedActions List of green habit actions registered by the user.
     * @return Markdown-formatted narrative insights or error directions response.
     */
    public suspend fun getPersonalizedEcoInsights(
        totalCo2: Float,
        fields: Map<String, Float>,
        loggedActions: List<LoggedAction>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured or using default placeholder.")
            return@withContext "API key not configured. Please add your GEMINI_API_KEY to the secrets panel in AI Studio so EcoPrint AI can generate customized recommendations! 🌱"
        }

        // Prepare context summary for candidate
        val calculatorSummary = fields.entries.joinToString(", ") { "${it.key}: ${String.format("%.1f", it.value)} kg CO₂" }
        val actionsSummary = if (loggedActions.isEmpty()) {
            "No green habits logged today yet."
        } else {
            loggedActions.joinToString(", ") { "${it.actionName} (-${it.co2Saved} kg)" }
        }

        val prompt = """
            You are EcoPrint AI, a friendly, professional environmental expert.
            The user wants custom feedback on their carbon footprint.
            
            USER PROFILE FOR TODAY:
            - Calculated Carbon Footprint: $totalCo2 kg CO2 equivalents.
            - Breakdown of sources: $calculatorSummary
            - Actions taken today to offset/reduce emissions: $actionsSummary
            
            Please analyze this and provide:
            1. Evaluation: Briefly assess their footprint of $totalCo2 kg. (Mention daily average target is around 5 to 10 kg CO₂ to prevent climate change, global average is 12 kg, so they can compare).
            2. Top Reduction Tip: Provide 2 highly personalized, realistic green tips based on their highest emission source.
            3. Gratitude & Hope: Praise any green actions they logged today, and leave them with a short energetic 1-sentence eco-quote.
            
            Style Guidelines:
            - Use friendly, professional, clear tone.
            - Keep it concise, under 250 words total.
            - Use beautiful bullet points.
            - Do not mention technical prompt instructions.
        """.trimIndent()

        try {
            // Construct request JSON dynamically using org.json
            val root = JSONObject()
            
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            
            root.put("contents", contentsArray)

            // Optional generation configuration
            val config = JSONObject()
            config.put("temperature", 0.7)
            root.put("generationConfig", config)

            val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            
            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request failed: code=${response.code}, body=$errorBody")
                    return@withContext "Oops, could not connect to Gemini. Code ${response.code}. Please ensure your API key is correctly entered and active."
                }

                val responseBody = response.body?.string() ?: return@withContext "Empty response from Gemini."
                val jsonResponse = JSONObject(responseBody)
                
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "No text generated.")
                        }
                    }
                }
                
                return@withContext "Gemini processed your data but returned no readable insights. Try again in a moment!"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini API call", e)
            return@withContext "Error connecting to AI advisor: ${e.localizedMessage}. Check your internet connection and try again."
        }
    }
}
