package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

object AgroVoiceHelper {
    private const val TAG = "AgroVoiceHelper"
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isSpeakingNow = false

    fun init(context: Context, onReady: ((Boolean) -> Unit)? = null) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("hi", "IN"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.ENGLISH)
                    }
                    tts?.setPitch(1.0f)
                    tts?.setSpeechRate(0.95f)
                    isInitialized = true
                    onReady?.invoke(true)
                } else {
                    Log.e(TAG, "Failed to initialize TextToSpeech: status=$status")
                    isInitialized = false
                    onReady?.invoke(false)
                }
            }
        } else {
            onReady?.invoke(isInitialized)
        }
    }

    fun speak(
        context: Context,
        text: String,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        init(context) { ready ->
            if (!ready || tts == null) {
                onError("Voice engine initialization failed on this device")
                return@init
            }

            val utteranceId = "voice_reminder_${System.currentTimeMillis()}"
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {
                    isSpeakingNow = true
                    onStart()
                }

                override fun onDone(id: String?) {
                    isSpeakingNow = false
                    onDone()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) {
                    isSpeakingNow = false
                    onError("Error during voice playback")
                }

                override fun onError(id: String?, errorCode: Int) {
                    isSpeakingNow = false
                    onError("Voice playback error (code $errorCode)")
                }
            })

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
    }

    fun stop() {
        try {
            tts?.stop()
            isSpeakingNow = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS: ${e.message}")
        }
    }

    fun isSpeaking(): Boolean = isSpeakingNow

    fun playPromptChime() {
        try {
            val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 85)
            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_PROMPT, 180)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    toneGen.release()
                } catch (e: Exception) {
                    // Ignore release errors
                }
            }, 300)
        } catch (e: Exception) {
            Log.e(TAG, "Tone error: ${e.message}")
        }
    }

    fun playHelloReminder(
        context: Context,
        retailerName: String,
        amount: Double,
        dueDate: Long,
        billNumber: String = "",
        isHindi: Boolean = true,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        playPromptChime()
        val text = if (isHindi) {
            buildHindiReminderMessage(retailerName, amount, dueDate, billNumber)
        } else {
            buildEnglishReminderMessage(retailerName, amount, dueDate, billNumber)
        }
        // Small delay so prompt chime rings clearly before speech begins
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            speak(context, text, onStart, onDone, onError)
        }, 220)
    }

    fun synthesizeAndShareVoice(
        context: Context,
        text: String,
        retailerName: String
    ) {
        init(context) { ready ->
            if (!ready || tts == null) {
                Toast.makeText(context, "Voice engine not ready", Toast.LENGTH_SHORT).show()
                return@init
            }

            try {
                val sanitizedName = retailerName.replace("[^a-zA-Z0-9]".toRegex(), "_").take(20)
                val audioFile = File(context.cacheDir, "PaymentReminder_${sanitizedName}_${System.currentTimeMillis()}.wav")
                val utteranceId = "synth_file_${System.currentTimeMillis()}"

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}

                    override fun onDone(id: String?) {
                        if (id == utteranceId && audioFile.exists() && audioFile.length() > 0) {
                            shareAudioFile(context, audioFile, retailerName)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) {
                        Log.e(TAG, "Synthesis failed for $id")
                    }
                })

                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                }

                val result = tts?.synthesizeToFile(text, params, audioFile, utteranceId)
                if (result == TextToSpeech.SUCCESS) {
                    Toast.makeText(context, "Generating voice audio for $retailerName...", Toast.LENGTH_SHORT).show()
                } else {
                    // Fallback to text share if audio synthesis is not supported on this engine
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Payment Reminder Voice Script"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error synthesizing audio: ${e.message}", e)
                Toast.makeText(context, "Unable to generate voice audio file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareAudioFile(context: Context, audioFile: File, retailerName: String) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, audioFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Payment Reminder - $retailerName")
                putExtra(Intent.EXTRA_TEXT, "Audio Payment Reminder from Siddhi Vinayak Krishi Vikas Kendra")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Voice Reminder via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing audio file: ${e.message}", e)
        }
    }

    fun buildHindiReminderMessage(
        retailerName: String,
        amount: Double,
        dueDate: Long,
        billNumber: String = "",
        companyName: String = "SV AGRO SHOPE"
    ): String {
        val amountInt = amount.toLong()
        val billPart = if (billNumber.isNotBlank()) " bill number $billNumber" else ""
        val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH).format(java.util.Date(dueDate))
        return "Hello! Namaskar $retailerName ji, $companyName se aapke payment ke sambandh mein ek zaroori reminder hai. Aapka bakaya rashi $amountInt rupaye$billPart, $dateStr tak jama karvayein. Kripya shighra bhugtan karein. Dhanyawad."
    }

    fun buildEnglishReminderMessage(
        retailerName: String,
        amount: Double,
        dueDate: Long,
        billNumber: String = "",
        companyName: String = "SV AGRO SHOPE"
    ): String {
        val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH).format(java.util.Date(dueDate))
        val billPart = if (billNumber.isNotBlank()) " against bill $billNumber" else ""
        return "Hello $retailerName! This is a reminder from $companyName regarding your pending payment of ₹${"%,.2f".format(amount)}$billPart due on $dateStr. Kindly arrange the payment at your earliest convenience. Thank you."
    }

    fun synthesizeAndShareHelloVoice(
        context: Context,
        retailerName: String,
        amount: Double,
        dueDate: Long,
        billNumber: String = "",
        isHindi: Boolean = true
    ) {
        val text = if (isHindi) {
            buildHindiReminderMessage(retailerName, amount, dueDate, billNumber)
        } else {
            buildEnglishReminderMessage(retailerName, amount, dueDate, billNumber)
        }
        synthesizeAndShareVoice(context, text, retailerName)
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            isSpeakingNow = false
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing TTS: ${e.message}")
        }
    }
}
