package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.ai.GeminiClient
import com.example.data.ai.JarvisOfflineEngine
import com.example.data.local.JarvisDatabase
import com.example.data.model.ChatMessage
import com.example.data.repository.JarvisRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class WakeEvent(
  val spokenText: String,
  val isFullCommand: Boolean,
  val timestamp: Long = System.currentTimeMillis()
)

class JarvisWakeWordBackgroundService : Service() {

  private val TAG = "JarvisWakeService"
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private var speechRecognizer: SpeechRecognizer? = null
  private var textToSpeech: TextToSpeech? = null
  private var isTtsReady = false
  private var isSpeaking = false
  private var isRecognizing = false

  private lateinit var repository: JarvisRepository
  private val mainHandler = Handler(Looper.getMainLooper())

  override fun onCreate() {
    super.onCreate()
    val db = JarvisDatabase.getDatabase(applicationContext)
    repository = JarvisRepository(db.jarvisDao())

    createNotificationChannel()
    startForegroundServiceWithNotification()

    initTts()
    initRecognizer()

    _isServiceRunning.value = true
    Log.d(TAG, "Jarvis Wake-Word Background Service started.")
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP_SERVICE) {
      stopSelf()
      return START_NOT_STICKY
    }

    startContinuousListening()
    return START_STICKY
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "JARVIS Continuous Voice Radar",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Monitors microphone for the \"Jarvis\" hotword activation"
        setShowBadge(false)
      }
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  private fun startForegroundServiceWithNotification() {
    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val openPendingIntent = PendingIntent.getActivity(
      this, 0, openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val stopIntent = Intent(this, JarvisWakeWordBackgroundService::class.java).apply {
      action = ACTION_STOP_SERVICE
    }
    val stopPendingIntent = PendingIntent.getService(
      this, 1, stopIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("J.A.R.V.I.S. Core Radar // Active")
      .setContentText("Continuous listening for \"Jarvis\" • Arc Reactor Online")
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentIntent(openPendingIntent)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Standby", stopPendingIntent)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  private fun initTts() {
    textToSpeech = TextToSpeech(applicationContext) { status ->
      if (status == TextToSpeech.SUCCESS) {
        textToSpeech?.setLanguage(Locale.US)
        textToSpeech?.setPitch(0.92f)
        textToSpeech?.setSpeechRate(1.05f)
        isTtsReady = true
      }
    }
  }

  private fun initRecognizer() {
    if (!SpeechRecognizer.isRecognitionAvailable(this)) {
      Log.w(TAG, "SpeechRecognizer not available on this device")
      return
    }

    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
      setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
          isRecognizing = true
          _isListeningFlow.value = true
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
          val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
          _audioLevelFlow.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
          isRecognizing = false
          _isListeningFlow.value = false
          _audioLevelFlow.value = 0f
        }

        override fun onError(error: Int) {
          isRecognizing = false
          _isListeningFlow.value = false
          _audioLevelFlow.value = 0f
          // Restart listening after brief delay if not currently speaking
          if (!isSpeaking && _isServiceRunning.value) {
            mainHandler.postDelayed({ startContinuousListening() }, 800)
          }
        }

        override fun onResults(results: Bundle?) {
          isRecognizing = false
          _isListeningFlow.value = false
          _audioLevelFlow.value = 0f

          val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
          val spoken = matches?.firstOrNull()?.trim() ?: ""

          if (spoken.isNotBlank()) {
            handleSpeechResult(spoken)
          }

          // Restart continuous listening
          if (!isSpeaking && _isServiceRunning.value) {
            mainHandler.postDelayed({ startContinuousListening() }, 1000)
          }
        }

        override fun onPartialResults(partialResults: Bundle?) {
          val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
          val partial = matches?.firstOrNull()?.lowercase() ?: ""
          if (partial.contains("jarvis")) {
            triggerWakeHaptic()
          }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
      })
    }
  }

  fun startContinuousListening() {
    if (isSpeaking || isRecognizing) return
    try {
      val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
      }
      speechRecognizer?.startListening(intent)
      isRecognizing = true
      _isListeningFlow.value = true
    } catch (e: Exception) {
      Log.e(TAG, "Error starting background speech recognizer: ${e.message}")
    }
  }

  fun stopContinuousListening() {
    try {
      speechRecognizer?.stopListening()
      isRecognizing = false
      _isListeningFlow.value = false
      _audioLevelFlow.value = 0f
    } catch (e: Exception) {
      Log.e(TAG, "Error stopping recognizer: ${e.message}")
    }
  }

  private fun handleSpeechResult(spokenText: String) {
    val lower = spokenText.lowercase().trim()

    // Check if wake-word "Jarvis" is in speech or if active
    if (lower.contains("jarvis") || lower.startsWith("jarvis")) {
      triggerWakeHaptic()

      val cleanCommand = lower.replace("jarvis", "").trim()
      val isFullCommand = cleanCommand.isNotBlank()

      serviceScope.launch {
        _wakeEvents.emit(WakeEvent(spokenText, isFullCommand))

        // Record user speech in Room
        repository.insertMessage(
          ChatMessage(sender = "user", message = spokenText)
        )

        val replyText: String
        val isOffline: Boolean

        if (!isFullCommand) {
          replyText = "Yes, sir? Systems online and listening for your command."
          isOffline = true
        } else {
          // Process full voice directive
          val offlineResult = JarvisOfflineEngine.processQuery(cleanCommand)
          if (offlineResult.mathResult != null || offlineResult.matchedIntent != "general") {
            replyText = offlineResult.responseText
            isOffline = true
          } else {
            replyText = try {
              GeminiClient.askJarvis(cleanCommand, isMathTutorMode = true)
            } catch (e: Throwable) {
              offlineResult.responseText
            }
            isOffline = false
          }
        }

        // Record Jarvis response in Room
        repository.insertMessage(
          ChatMessage(sender = "jarvis", message = replyText, isOfflineProcessed = isOffline)
        )

        // Speak reply out loud
        speakResponse(replyText)
      }
    }
  }

  private fun speakResponse(text: String) {
    if (!isTtsReady || text.isBlank()) return
    isSpeaking = true
    stopContinuousListening()

    val spokenSummary = text.split("\n\n").firstOrNull() ?: text
    textToSpeech?.speak(spokenSummary, TextToSpeech.QUEUE_FLUSH, null, "JARVIS_BG_RESP")

    // Resume listening after speech finishes (poll or estimate delay)
    val estimatedDuration = (spokenSummary.length * 65L).coerceIn(1200L, 8000L)
    mainHandler.postDelayed({
      isSpeaking = false
      if (_isServiceRunning.value) {
        startContinuousListening()
      }
    }, estimatedDuration)
  }

  private fun triggerWakeHaptic() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 40, 80), -1))
      } else {
        @Suppress("DEPRECATION")
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 40, 80), -1))
        } else {
          vibrator.vibrate(80)
        }
      }
    } catch (e: Exception) {
      Log.d(TAG, "Haptic unavailable: ${e.message}")
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    _isServiceRunning.value = false
    _isListeningFlow.value = false
    _audioLevelFlow.value = 0f
    stopContinuousListening()
    speechRecognizer?.destroy()
    textToSpeech?.stop()
    textToSpeech?.shutdown()
    serviceScope.cancel()
    Log.d(TAG, "Jarvis Wake-Word Background Service stopped.")
  }

  override fun onBind(intent: Intent?): IBinder? = null

  companion object {
    const val CHANNEL_ID = "jarvis_wake_channel"
    const val NOTIFICATION_ID = 1007
    const val ACTION_STOP_SERVICE = "com.example.action.STOP_WAKE_SERVICE"

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _isListeningFlow = MutableStateFlow(false)
    val isListeningFlow: StateFlow<Boolean> = _isListeningFlow.asStateFlow()

    private val _audioLevelFlow = MutableStateFlow(0f)
    val audioLevelFlow: StateFlow<Float> = _audioLevelFlow.asStateFlow()

    private val _wakeEvents = MutableSharedFlow<WakeEvent>(extraBufferCapacity = 10)
    val wakeEvents: SharedFlow<WakeEvent> = _wakeEvents.asSharedFlow()

    fun startService(context: Context) {
      val intent = Intent(context, JarvisWakeWordBackgroundService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stopService(context: Context) {
      val intent = Intent(context, JarvisWakeWordBackgroundService::class.java)
      context.stopService(intent)
    }
  }
}
