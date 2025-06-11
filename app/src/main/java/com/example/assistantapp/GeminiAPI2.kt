package com.example.assistantapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

// 🔐 Replace with your actual Gemini API key securely in production
val ReadModel = GenerativeModel(
    modelName = "gemini-1.5-flash",
    apiKey = "AIzaSyDMtE4uLyYMID5uesEA4Qm0X0yIgkkHN28", // <- Replace this securely in real apps
    generationConfig = generationConfig {
        temperature = 0.2f
        topK = 64
        topP = 0.95f
        maxOutputTokens = 8192
        responseMimeType = "text/plain"
    },
    safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE),
    ),
    systemInstruction = content {
        text(
            """
            Your user is a blind person.
            From the input image, help the user by identifying what they’re looking at and read aloud any visible text, including from books, signboards, labels, or posters.
            """.trimIndent()
        )
    }
)

/**
 * Sends an image to Gemini AI for OCR-style reading and streams the extracted text.
 */
suspend fun sendFrameToGemini2AI(
    bitmap: Bitmap,
    onPartialResult: (String) -> Unit,
    onError: (String) -> Unit
) {
    try {
        withContext(Dispatchers.IO) {
            val inputContent = content {
                image(bitmap)
                text("Read the text from this image and provide the content.")
            }

            var fullResponse = ""
            ReadModel.generateContentStream(inputContent).collect { chunk ->
                chunk.text?.let {
                    fullResponse += it
                    onPartialResult(it)
                }
            }
        }
    } catch (e: IOException) {
        Log.e("GeminiAI", "Network error: ${e.message}")
        onError("Network error: ${e.message}")
    } catch (e: Exception) {
        Log.e("GeminiAI", "Unexpected error: ${e.message}")
        onError("Unexpected error: ${e.message}")
    }
}
