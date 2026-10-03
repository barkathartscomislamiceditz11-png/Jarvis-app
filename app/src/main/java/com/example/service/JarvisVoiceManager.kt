package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoicePreset(val displayName: String, val gender: String, val pitch: Float, val speed: Float) {
  JARVIS("JARVIS Classic", "Male", 0.88f, 1.02f),
  FRIDAY("FRIDAY Core", "Female", 1.22f, 1.08f),
  EDITH("EDITH Tactical", "Female", 1.05f, 1.0f),
  STARK("STARK Prime", "Male", 0.98f, 1.15f)
}

class JarvisVoiceManager(
  private val context: Context,
  private val onWakeWordDetected: (String) -> Unit
) {
  private val TAG = "JarvisVoiceManager"

  private var speechRecognizer: SpeechRecognizer? = null
  private var textToSpeech: TextToSpeech? = null
  private var isTtsReady = false

  private val _isListening = MutableStateFlow(false)
  val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

  private val _isSpeaking = MutableStateFlow(false)
  val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

  private val _audioLevel = MutableStateFlow(0f) // 0.0 to 1.0
  val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

  private val _isContinuousHotwordActive = MutableStateFlow(true)
  val isContinuousHotwordActive: StateFlow<Boolean> = _isContinuousHotwordActive.asStateFlow()

  private val _currentPreset = MutableStateFlow(VoicePreset.JARVIS)
  val currentPreset: StateFlow<VoicePreset> = _currentPreset.asStateFlow()

  private val _isSpeechMuted = MutableStateFlow(false)
  val isSpeechMuted: StateFlow<Boolean> = _isSpeechMuted.asStateFlow()

  private val handler = Handler(Looper.getMainLooper())

  init {
    initTts()
    initRecognizer()
  }

  private fun initTts() {
    textToSpeech = TextToSpeech(context) { status ->
      if (status == TextToSpeech.SUCCESS) {
        val result = textToSpeech?.setLanguage(Locale.US)
        if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
          isTtsReady = true
          applyCurrentPreset()
        }
      }
    }

    textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
      override fun onStart(utteranceId: String?) {
        _isSpeaking.value = true
      }

      override fun onDone(utteranceId: String?) {
        _isSpeaking.value = false
        // Resume hotword listening if continuous listening is enabled
        if (_isContinuousHotwordActive.value) {
          handler.postDelayed({ startListening() }, 500)
        }
      }

      override fun onError(utteranceId: String?) {
        _isSpeaking.value = false
        if (_isContinuousHotwordActive.value) {
          handler.postDelayed({ startListening() }, 500)
        }
      }
    })
  }

  fun setVoicePreset(preset: VoicePreset) {
    _currentPreset.value = preset
    applyCurrentPreset()
  }

  fun toggleMute() {
    _isSpeechMuted.value = !_isSpeechMuted.value
    if (_isSpeechMuted.value) {
      stopSpeaking()
    }
  }

  fun setContinuousHotword(enabled: Boolean) {
    _isContinuousHotwordActive.value = enabled
    if (enabled) {
      startListening()
    } else {
      stopListening()
    }
  }

  private fun applyCurrentPreset() {
    if (!isTtsReady) return
    val preset = _currentPreset.value
    textToSpeech?.setPitch(preset.pitch)
    textToSpeech?.setSpeechRate(preset.speed)

    // Try finding gender matched voice if available
    try {
      val voices = textToSpeech?.voices
      val targetVoice = voices?.firstOrNull { voice ->
        if (preset.gender == "Female") {
          voice.name.lowercase().contains("female") || voice.name.lowercase().contains("en-us-x-sfg")
        } else {
          voice.name.lowercase().contains("male") || voice.name.lowercase().contains("en-us-x-iol")
        }
      }
      if (targetVoice != null) {
        textToSpeech?.voice = targetVoice
      }
    } catch (e: Exception) {
      Log.d(TAG, "Voice selection defaulted: ${e.message}")
    }
  }

  private fun initRecognizer() {
    if (SpeechRecognizer.isRecognitionAvailable(context)) {
      speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
        setRecognitionListener(object : RecognitionListener {
          override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
          }

          override fun onBeginningOfSpeech() {}

          override fun onRmsChanged(rmsdB: Float) {
            // Normalize -2dB..10dB to 0..1 range
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _audioLevel.value = normalized
          }

          override fun onBufferReceived(buffer: ByteArray?) {}

          override fun onEndOfSpeech() {
            _isListening.value = false
            _audioLevel.value = 0f
          }

          override fun onError(error: Int) {
            _isListening.value = false
            _audioLevel.value = 0f
            // If continuous hotword listening is active, restart safely after brief delay
            if (_isContinuousHotwordActive.value && !_isSpeaking.value) {
              handler.postDelayed({ startListening() }, 1000)
            }
          }

          override fun onResults(results: Bundle?) {
            _isListening.value = false
            _audioLevel.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val spokenText = matches?.firstOrNull() ?: ""

            if (spokenText.isNotBlank()) {
              onWakeWordDetected(spokenText)
            }

            // Resume listening if continuous mode is on and not currently speaking
            if (_isContinuousHotwordActive.value && !_isSpeaking.value) {
              handler.postDelayed({ startListening() }, 1000)
            }
          }

          override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()?.lowercase() ?: ""
            if (text.contains("jarvis")) {
              // Early hotword wake detection!
              _audioLevel.value = 0.95f
            }
          }

          override fun onEvent(eventType: Int, params: Bundle?) {}
        })
      }
    }
  }

  fun startListening() {
    if (_isSpeaking.value) return // Don't listen to self
    try {
      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
      }
      speechRecognizer?.startListening(intent)
      _isListening.value = true
    } catch (e: Exception) {
      Log.e(TAG, "Error starting speech recognizer: ${e.message}")
    }
  }

  fun stopListening() {
    try {
      speechRecognizer?.stopListening()
      _isListening.value = false
      _audioLevel.value = 0f
    } catch (e: Exception) {
      Log.e(TAG, "Error stopping recognizer: ${e.message}")
    }
  }

  fun speak(text: String) {
    if (_isSpeechMuted.value || text.isBlank()) return
    // Pause speech recognizer while speaking so Jarvis doesn't trigger on its own voice
    stopListening()
    applyCurrentPreset()

    val params = Bundle().apply {
      putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "JARVIS_${System.currentTimeMillis()}")
    }
    textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "JARVIS_MSG")
  }

  fun stopSpeaking() {
    textToSpeech?.stop()
    _isSpeaking.value = false
  }

  fun shutdown() {
    stopListening()
    speechRecognizer?.destroy()
    textToSpeech?.stop()
    textToSpeech?.shutdown()
  }
}
