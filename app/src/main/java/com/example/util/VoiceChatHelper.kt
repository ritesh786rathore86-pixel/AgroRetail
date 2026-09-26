package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object VoiceChatHelper {
    private const val TAG = "VoiceChatHelper"

    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var recordingStartTime: Long = 0L

    private var mediaPlayer: MediaPlayer? = null
    private var activePlayingUri: String? = null
    private var progressJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId: StateFlow<String?> = _playingMessageId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var recordTimerJob: Job? = null

    fun startRecording(context: Context): Boolean {
        return try {
            stopPlaying()
            val outputDir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
            val file = File(outputDir, "VoiceNote_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordingStartTime = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDurationSec.value = 0

            recordTimerJob?.cancel()
            recordTimerJob = scope.launch {
                while (_isRecording.value) {
                    delay(500)
                    _recordingDurationSec.value = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}", e)
            cleanupRecording()
            false
        }
    }

    data class RecordingResult(val file: File, val durationMs: Long)

    fun stopRecording(): RecordingResult? {
        recordTimerJob?.cancel()
        _isRecording.value = false
        val durationMs = System.currentTimeMillis() - recordingStartTime

        return try {
            mediaRecorder?.apply {
                try { stop() } catch (_: Exception) {}
                release()
            }
            mediaRecorder = null
            val file = currentRecordingFile
            currentRecordingFile = null
            if (file != null && file.exists() && file.length() > 0) {
                RecordingResult(file, durationMs.coerceAtLeast(1000L))
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder: ${e.message}", e)
            cleanupRecording()
            null
        }
    }

    fun cancelRecording() {
        recordTimerJob?.cancel()
        _isRecording.value = false
        cleanupRecording()
    }

    private fun cleanupRecording() {
        try {
            mediaRecorder?.apply {
                try { stop() } catch (_: Exception) {}
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
        try {
            currentRecordingFile?.delete()
        } catch (_: Exception) {}
        currentRecordingFile = null
        _isRecording.value = false
        _recordingDurationSec.value = 0
    }

    fun playAudio(messageId: String, uriString: String, onFinished: () -> Unit = {}) {
        if (_playingMessageId.value == messageId) {
            stopPlaying()
            return
        }

        stopPlaying()

        try {
            val player = MediaPlayer()
            player.setDataSource(uriString)
            player.prepare()
            player.start()
            mediaPlayer = player
            activePlayingUri = uriString
            _playingMessageId.value = messageId
            _playbackProgress.value = 0f

            progressJob = scope.launch {
                while (player.isPlaying) {
                    val current = player.currentPosition
                    val total = player.duration
                    if (total > 0) {
                        _playbackProgress.value = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    }
                    delay(100)
                }
            }

            player.setOnCompletionListener {
                stopPlaying()
                onFinished()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                stopPlaying()
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio: ${e.message}", e)
            stopPlaying()
        }
    }

    fun stopPlaying() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        activePlayingUri = null
        _playingMessageId.value = null
        _playbackProgress.value = 0f
    }
}
