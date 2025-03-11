package com.corps.healthmate.activities

import com.corps.healthmate.BuildConfig
import com.corps.healthmate.interfaces.ResponseCallback
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.java.ChatFutures
import com.google.ai.client.generativeai.java.GenerativeModelFutures
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import java.util.concurrent.Executor

object GeminiResp {

    // Function to get the ChatFutures instance
    fun getChatModel(): ChatFutures {
        val apiKey = BuildConfig.apiKey

        // Setup safety settings to avoid harmful content
        val harassmentSafety = SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH)

        // Setup generation configuration for the model
        val configBuilder = GenerationConfig.Builder()
        configBuilder.temperature = 0.9f
        configBuilder.topK = 16
        configBuilder.topP = 0.1f
        val generationConfig = configBuilder.build()

        // Create the generative model instance
        val generativeModel = GenerativeModel(
            "gemini-1.5-flash", // Model identifier
            apiKey,             // API key
            generationConfig,   // Configuration
            listOf(harassmentSafety) // Safety settings
        )

        // Return a ChatFutures instance
        return GenerativeModelFutures.from(generativeModel).startChat()
    }

    // Function to handle response using the provided chatModel (ChatFutures)
    fun getResponse(chatModel: ChatFutures?, query: String, callback: ResponseCallback) {
        val userMessageBuilder = Content.Builder()
        userMessageBuilder.role = "user"
        userMessageBuilder.text(query)
        val userMessage: Content = userMessageBuilder.build()

        val executor = Executor { obj: Runnable -> obj.run() }

        val response = chatModel!!.sendMessage(userMessage)
        Futures.addCallback(response, object : FutureCallback<GenerateContentResponse> {
            override fun onSuccess(result: GenerateContentResponse) {
                val resultText = result.text
                val cleanedResponse = resultText!!.replace("*", " ")
                callback.onResponse(cleanedResponse)
            }

            override fun onFailure(throwable: Throwable) {
                throwable.printStackTrace()
                callback.onError(throwable)
            }
        }, executor)
    }
}
