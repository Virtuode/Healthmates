package com.corps.healthmate.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.corps.healthmate.R
import com.corps.healthmate.adapters.ChatAiAdapter
import com.corps.healthmate.adapters.ChatMessageAi
import com.corps.healthmate.adapters.SymptomAdapter
import com.corps.healthmate.interfaces.ResponseCallback
import com.corps.healthmate.utils.GeminiResp
import com.google.ai.client.generativeai.java.ChatFutures
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

class AiChatActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private var userInputEditText: EditText? = null
    private var sendButton: MaterialButton? = null
    private var micButton: ImageButton? = null
    private var chatRecyclerView: RecyclerView? = null
    private var chatAiAdapter: ChatAiAdapter? = null
    private var chatMessageAis: MutableList<ChatMessageAi>? = null
    private var chatModel: ChatFutures? = null
    private var loadingAnimationView: LottieAnimationView? = null
    private var handler: Handler? = null

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListening = false
    private var isSpeaking = false
    private var currentLanguage = Locale.ENGLISH
    private var awaitingSymptomSelection = false // Track if we're waiting for symptom input
    private var lastHealthInput = "" // Store the last health-related input

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_chat)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        initViews()
        setupChat()
        setupTextToSpeech()
        setupSpeechRecognizer()
        checkPermissions()

        sendButton!!.setOnClickListener { handleUserInput() }
        micButton!!.setOnClickListener { startVoiceInput() }
    }

    private fun initViews() {
        userInputEditText = findViewById(R.id.user_input_edittext)
        sendButton = findViewById(R.id.send_button)
        micButton = findViewById(R.id.mic_button)
        chatRecyclerView = findViewById(R.id.chat_recyclerview)
        loadingAnimationView = findViewById(R.id.loading_animation)

        loadingAnimationView?.setAnimation(R.raw.chat_loading)
        loadingAnimationView?.playAnimation()

        handler = Handler(Looper.getMainLooper())
    }

    private fun setupChat() {
        chatMessageAis = mutableListOf()
        chatAiAdapter = ChatAiAdapter(chatMessageAis!!)
        chatRecyclerView!!.layoutManager = LinearLayoutManager(this)
        chatRecyclerView!!.adapter = chatAiAdapter
        chatModel = GeminiResp.getChatModel()
    }

    private fun setupTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setPitch(1.0f)
                textToSpeech.setSpeechRate(0.9f)
                setTTSLanguage(Locale.ENGLISH)
            } else {
                Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show()
            }
        }

        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                runOnUiThread { micButton?.setImageResource(R.drawable.ic_mic_active) }
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                runOnUiThread { micButton?.setImageResource(R.drawable.ic_mic) }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                runOnUiThread { micButton?.setImageResource(R.drawable.ic_mic) }
            }
        })
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                micButton?.setImageResource(R.drawable.ic_mic_active)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                micButton?.setImageResource(R.drawable.ic_mic)
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    userInputEditText?.setText(text)
                    detectAndSetLanguage(text)
                    handleUserInput()
                }
            }

            override fun onError(error: Int) {
                isListening = false
                micButton?.setImageResource(R.drawable.ic_mic)
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    else -> "Error occurred in speech recognition"
                }
                Toast.makeText(this@AiChatActivity, errorMessage, Toast.LENGTH_SHORT).show()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startVoiceInput() {
        if (isSpeaking) {
            textToSpeech.stop()
            isSpeaking = false
        }

        if (!isListening) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US,hi-IN")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            }
            try {
                speechRecognizer.startListening(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error starting voice input", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun detectAndSetLanguage(text: String) {
        val isHindi = text.any { it in '\u0900'..'\u097F' }
        currentLanguage = if (isHindi) Locale("hi", "IN") else Locale.ENGLISH
        setTTSLanguage(currentLanguage)
    }

    private fun setTTSLanguage(locale: Locale) {
        val result = textToSpeech.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "${locale.displayLanguage} TTS not supported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleUserInput() {
        if (isSpeaking) {
            textToSpeech.stop()
            isSpeaking = false
        }

        val userInput = userInputEditText!!.text.toString().trim().lowercase()
        if (TextUtils.isEmpty(userInput)) return

        addUserMessage(userInput)
        userInputEditText!!.setText("")
        loadingAnimationView!!.visibility = View.VISIBLE

        val healthKeywords = listOf(
            "fever", "cough", "headache", "sore throat", "runny nose", "nausea",
            "sick", "pain", "hurt", "tired", "vomiting", "diarrhea", "chills", "rash"
        )

        if (awaitingSymptomSelection) {
            processSymptomSelection(userInput)
        } else if (healthKeywords.any { userInput.contains(it) }) {
            lastHealthInput = userInput
            if (userInput.contains("sick") || userInput.contains("not feeling well")) {
                showSymptomDialog(getDynamicSymptoms("general"))
            } else {
                showSymptomDialog(getDynamicSymptoms(userInput))
            }
        } else {
            GeminiResp.getResponse(chatModel, userInput, object : ResponseCallback {
                override fun onResponse(response: String) {
                    handler!!.postDelayed({
                        loadingAnimationView!!.visibility = View.GONE
                        addAiMessage(response)
                        speakText(response)
                    }, 1000)
                }

                override fun onError(throwable: Throwable) {
                    loadingAnimationView!!.visibility = View.GONE
                    addAiMessage("Sorry, I couldn’t process that. Try again?")
                    throwable.printStackTrace()
                }
            })
        }
    }

    private fun getDynamicSymptoms(userInput: String): List<String> {
        val baseSymptoms = listOf("Coughing", "Runny nose", "Headache", "Sore throat")
        return when {
            userInput.contains("fever") -> baseSymptoms + listOf("Chills", "Fatigue")
            userInput.contains("nausea") -> baseSymptoms + listOf("Vomiting", "Diarrhea")
            userInput.contains("cough") -> baseSymptoms + listOf("Chest pain", "Fever")
            userInput.contains("headache") -> baseSymptoms + listOf("Dizziness", "Nausea")
            else -> baseSymptoms + listOf("Fatigue", "Fever") // Default for "sick" or general
        }
    }



    private fun processSymptomSelection(userInput: String) {
        val selectedNumber = userInput.toIntOrNull()
        if (selectedNumber == null || selectedNumber !in 1..4) {
            val errorMsg = "Please enter a valid number (1-4) from the symptom list."
            handler!!.postDelayed({
                loadingAnimationView!!.visibility = View.GONE
                addAiMessage(errorMsg)
                speakText(errorMsg)
                awaitingSymptomSelection = false
            }, 1000)
        } else {
            // For voice/text input fallback (if not using dialog)
            val symptoms = getDynamicSymptoms(lastHealthInput)
            val selectedSymptom = symptoms[selectedNumber - 1]
            handleSymptomResponse(listOf(selectedSymptom))
        }
    }

    private fun handleSymptomResponse(symptoms: List<String>) {
        val symptomText = symptoms.joinToString(" and ")
        val prompt = """
            The user reports: '$lastHealthInput' with additional symptoms: $symptomText. 
            Provide basic health advice, clarify you’re not a doctor, and suggest next steps if needed.
        """.trimIndent()
        GeminiResp.getResponse(chatModel, prompt, object : ResponseCallback {
            override fun onResponse(response: String) {
                handler!!.postDelayed({
                    loadingAnimationView!!.visibility = View.GONE
                    addAiMessage(response)
                    speakText(response)
                    awaitingSymptomSelection = false
                }, 1000)
            }

            override fun onError(throwable: Throwable) {
                loadingAnimationView!!.visibility = View.GONE
                addAiMessage("Sorry, I couldn’t process that. Can you tell me more?")
                speakText("Sorry, I couldn’t process that. Can you tell me more?")
                awaitingSymptomSelection = false
                throwable.printStackTrace()
            }
        })
    }

    private fun speakText(text: String) {
        if (!isSpeaking) {
            val utteranceId = "utterance_${System.currentTimeMillis()}"
            detectAndSetLanguage(text)
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            micButton?.isEnabled = false
        }
    }

    private fun addUserMessage(userInput: String) {
        val userMessage = ChatMessageAi(userMessage = userInput)
        chatMessageAis!!.add(userMessage)
        updateChat()
    }

    private fun showSymptomDialog(symptoms: List<String>) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_symptom_selection, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.symptom_recycler_view)
        val submitButton = dialogView.findViewById<MaterialButton>(R.id.submit_button)

        val adapter = SymptomAdapter(symptoms) { selectedSymptoms ->
            submitButton.isEnabled = selectedSymptoms.isNotEmpty()
        }
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Select Your Symptoms")
            .setView(dialogView)
            .setNegativeButton("Cancel") { _, _ ->
                loadingAnimationView!!.visibility = View.GONE
                awaitingSymptomSelection = false
            }
            .setOnDismissListener { if (awaitingSymptomSelection) loadingAnimationView!!.visibility = View.GONE }
            .create()

        submitButton.setOnClickListener {
            val selectedSymptoms = adapter.getSelectedSymptoms()
            if (selectedSymptoms.isEmpty()) {
                Toast.makeText(this, "Please select at least one symptom", Toast.LENGTH_SHORT).show()
            } else {
                dialog.dismiss()
                handleSymptomResponse(selectedSymptoms)
            }
        }

        // Set dialog size
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.8).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val prompt = "I’m sorry you’re not feeling well. Please select any symptoms you’re experiencing."
        addAiMessage(prompt)
        speakText(prompt)
        handler!!.postDelayed({
            loadingAnimationView!!.visibility = View.GONE
            awaitingSymptomSelection = true
            dialog.show()
        }, 1000)
    }

    private fun addAiMessage(aiResponse: String?) {
        val aiMessage = ChatMessageAi(aiResponse = aiResponse)
        chatMessageAis!!.add(aiMessage)
        updateChat()
    }

    private fun updateChat() {
        chatAiAdapter!!.notifyItemInserted(chatMessageAis!!.size - 1)
        chatRecyclerView!!.scrollToPosition(chatMessageAis!!.size - 1)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setTTSLanguage(currentLanguage)
        } else {
            Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (isSpeaking) {
            textToSpeech.stop()
            isSpeaking = false
        }
        super.onBackPressedDispatcher.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        if (isSpeaking) {
            textToSpeech.stop()
            isSpeaking = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler?.removeCallbacksAndMessages(null)
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }
}