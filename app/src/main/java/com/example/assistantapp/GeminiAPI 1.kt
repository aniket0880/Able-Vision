package com.example.assistantapp

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.*

val model = GenerativeModel(
    modelName = "gemini-1.5-flash",
    apiKey = "AIzaSyDMtE4uLyYMID5uesEA4Qm0X0yIgkkHN28", // 🔐 Replace this securely in production
    generationConfig = generationConfig {
        temperature = 1.5f
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
            Your primary role is to assist visually impaired users by answering specific questions about their surroundings, regardless of the environment. You rely on information provided by another AI (referred to as "AI One"), which has access to live frames. Your task is to relay this information and provide detailed descriptions or clarifications as needed.

            Key Responsibilities:
            - Answer questions related to object presence, color, on/off status, etc.
            - Use AI One's data to answer clearly and helpfully.
            - Interpret ambiguous data and ask users if they want more details.
            - Use clear, simple, conversational language.
            - Break down complex environmental descriptions for indoor, outdoor, and public spaces.
            - Encourage follow-ups like: “Would you like to know more?”

            Example Responses:
            User: "Is the laptop on?"
            AI: "Yes, the laptop on the table is currently on, with a bright display."

            User: "Is there a car nearby?"
            AI: "Yes, a blue car is parked on the street to your right."

            Prioritize safety, comfort, and a helpful tone. Rely on AI One’s live updates and acknowledge if something is unclear.
            """.trimIndent()
        )
    }
)

// Chat session history – can be expanded to track multiple rounds
val chatHistory = listOf<Content>()

// Start a chat session
val chat = model.startChat(chatHistory)

/**
 * Sends a message to Gemini AI, optionally appending frame data from AI One.
 */
suspend fun sendMessageToGeminiAI(message: String, frameData: String? = null): String {
    val fullMessage = if (frameData != null) {
        "Frame data: $frameData\n\nUser message: $message"
    } else {
        message
    }

    val response = chat.sendMessage(fullMessage)
    return response.text ?: "" // Fallback if no text response
}

// Test via main
fun main() = runBlocking {
    val response = sendMessageToGeminiAI("Hello, how can you help me?")
    println(response)
}
