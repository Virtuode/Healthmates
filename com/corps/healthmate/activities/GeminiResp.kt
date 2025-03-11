import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.Content.Builder
import com.google.ai.client.generativeai.type.GenerationConfig.Builder

object GeminiResp {
    fun getResponse(chatModel: ChatFutures?, query: String?, callback: ResponseCallback) {
        val userMessageBuilder: Content.Builder = Content.Builder()
        userMessageBuilder.role = "user"
        userMessageBuilder.text(query)
        val userMessage: Content = userMessageBuilder.build()

        // ...
    }

    fun getChatModel(): ChatFutures {
        val apiKey = BuildConfig.apiKey

        // Setup safety settings to avoid harmful content
        val harassmentSafety = SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH)

        // Setup generation configuration for the model
        val configBuilder: GenerationConfig.Builder = GenerationConfig.Builder()
        configBuilder.temperature = 0.9f
        configBuilder.topK = 16
        configBuilder.topP = 0.1f
        val generationConfig: GenerationConfig = configBuilder.build()

        // Create the generative model instance
        val generativeModel = GenerativeModel(
            "gemini-1.5-flash", // Model identifier
            apiKey,             // API key
            generationConfig,   // Configuration
            listOf(harassmentSafety) // Safety settings
        )

        // Correctly return a ChatFutures instance
        return GenerativeModelFutures.from(generativeModel).startChat()
    }
} 