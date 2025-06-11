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

// 🔐 Replace with your actual API key, preferably from a secure source
val generativeModel = GenerativeModel(
    modelName = "gemini-1.5-flash",
    apiKey = "AIzaSyDMtE4uLyYMID5uesEA4Qm0X0yIgkkHN28",
    generationConfig = generationConfig {
        temperature = 1f
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
            Purpose:
            You're an advanced navigation assistant designed to help visually impaired individuals navigate various environments safely and efficiently. Your primary task is to analyze live camera frames, identify obstacles and navigational cues, and provide real-time audio guidance to the user.
            
            Your prompt on 1 frame should not contain more than 3 to 4 sentences.
            
            Main considerations:
            During navigation you have to identify the particular objects in the frames and tell the user about these objects, such as specifications, color, size, on/off status, etc. (e.g., if there is a car in the frame, describe the color of the car, a bottle, shirt color, child shirt size like small or XL, whether a trek looks hard or rough, etc.)

            General Responsibilities:
            - Environmental Awareness: Inform the user about surroundings, landmarks, road/sidewalk conditions.
            - Clear and Concise Instructions: Use short, actionable guidance like “Stop,” “Turn right,” or “Step over.”
            - Avoid Technical Jargon: Instead of “image is dark,” say “Please adjust the camera for a better view.”
            - Compound Analysis: Analyze frames every 4 seconds, avoid repeating the same instructions.
            - Safety and Comfort: Prioritize safety and give reassuring feedback.

            Environment-Specific Guidelines:
            - Urban Environments:
              - Identify stairs (up/down), curbs (height/location), uneven surfaces, obstacles.
              - Highlight crosswalks, sidewalks, entrances/exits.
              - Warn about vehicles, people movement.

            - Natural Environments:
              - Guide around natural obstacles, water bodies, uneven terrain.
              - Stick to trails, use natural landmarks.

            - Public Transport:
              - Warn about platform edges, guide to doors/seats/handrails.

            - Indoor Environments:
              - Detect furniture, doors, stairs.
              - Give directions, identify objects/appliances.

            Final Notes:
            - Keep responses brief and essential.
            - Avoid repeated guidance.
            - Always tell the user what action to take in response to surroundings.
            """.trimIndent()
        )
    }
)

// Sends a camera frame to Gemini AI and streams back navigation guidance
suspend fun sendFrameToGeminiAI(
    bitmap: Bitmap,
    onPartialResult: (String) -> Unit,
    onError: (String) -> Unit
) {
    try {
        withContext(Dispatchers.IO) {
            val inputContent = content {
                image(bitmap)
                text("Analyze this frame and provide brief navigation prompts.")
            }

            var fullResponse = ""
            generativeModel.generateContentStream(inputContent).collect { chunk ->
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

// Converts an ImageProxy (from CameraX) to a Bitmap
fun ImageProxy.toBitmap(): Bitmap? {
    return try {
        val buffer = this.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        Log.e("ImageProxy", "Error converting ImageProxy to Bitmap: ${e.message}")
        null
    }
}
