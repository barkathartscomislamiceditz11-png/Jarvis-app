package com.example.ui

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiClient
import com.example.data.ai.JarvisOfflineEngine
import com.example.data.local.JarvisDatabase
import com.example.data.local.VaultSecurityManager
import com.example.data.model.Assignment
import com.example.data.model.ChatMessage
import com.example.data.model.LocalVaultFile
import com.example.data.model.StudySession
import com.example.data.repository.JarvisRepository
import com.example.service.JarvisVoiceManager
import com.example.service.JarvisWakeWordBackgroundService
import com.example.service.VoicePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class JarvisTab {
  CORE_HUD,
  TUTOR_CHAT,
  SCHOOL_STUDY,
  PRIVACY_VAULT,
  VOICE_CONFIG
}

data class MathPracticeQuestion(
  val question: String,
  val options: List<String>,
  val correctIndex: Int,
  val explanation: String
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: JarvisRepository

  val messages: StateFlow<List<ChatMessage>>
  val assignments: StateFlow<List<Assignment>>
  val vaultFiles: StateFlow<List<LocalVaultFile>>
  val studySessions: StateFlow<List<StudySession>>

  private val _currentTab = MutableStateFlow(JarvisTab.CORE_HUD)
  val currentTab: StateFlow<JarvisTab> = _currentTab.asStateFlow()

  private val _isStrictOfflineMode = MutableStateFlow(false)
  val isStrictOfflineMode: StateFlow<Boolean> = _isStrictOfflineMode.asStateFlow()

  private val _isProcessingQuery = MutableStateFlow(false)
  val isProcessingQuery: StateFlow<Boolean> = _isProcessingQuery.asStateFlow()

  private val _lastWakeQuery = MutableStateFlow<String?>(null)
  val lastWakeQuery: StateFlow<String?> = _lastWakeQuery.asStateFlow()

  // Study Focus Timer
  private val _studyTimerSeconds = MutableStateFlow(25 * 60)
  val studyTimerSeconds: StateFlow<Int> = _studyTimerSeconds.asStateFlow()

  private val _isTimerRunning = MutableStateFlow(false)
  val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

  private var countDownTimer: CountDownTimer? = null

  // Math Practice Quiz State
  private val _currentQuizQuestion = MutableStateFlow<MathPracticeQuestion?>(null)
  val currentQuizQuestion: StateFlow<MathPracticeQuestion?> = _currentQuizQuestion.asStateFlow()

  private val _quizScore = MutableStateFlow(0)
  val quizScore: StateFlow<Int> = _quizScore.asStateFlow()

  private val _quizFeedback = MutableStateFlow<String?>(null)
  val quizFeedback: StateFlow<String?> = _quizFeedback.asStateFlow()

  // Voice Engine
  val voiceManager: JarvisVoiceManager
  val isBackgroundWakeRunning: StateFlow<Boolean> = JarvisWakeWordBackgroundService.isServiceRunning

  init {
    val db = JarvisDatabase.getDatabase(application)
    repository = JarvisRepository(db.jarvisDao())

    messages = repository.allMessages.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    assignments = repository.allAssignments.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    vaultFiles = repository.allVaultFiles.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    studySessions = repository.allStudySessions.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    voiceManager = JarvisVoiceManager(application) { spokenText ->
      handleSpokenVoiceInput(spokenText)
    }

    // Observe background wake events
    viewModelScope.launch {
      JarvisWakeWordBackgroundService.wakeEvents.collect { event ->
        _lastWakeQuery.value = event.spokenText
      }
    }

    generateNewQuizQuestion()
  }

  fun toggleBackgroundWakeService(context: android.content.Context) {
    if (JarvisWakeWordBackgroundService.isServiceRunning.value) {
      JarvisWakeWordBackgroundService.stopService(context)
      voiceManager.setContinuousHotword(false)
    } else {
      JarvisWakeWordBackgroundService.startService(context)
      voiceManager.setContinuousHotword(true)
    }
  }

  fun setTab(tab: JarvisTab) {
    _currentTab.value = tab
  }

  fun toggleStrictOfflineMode() {
    _isStrictOfflineMode.value = !_isStrictOfflineMode.value
  }

  private fun handleSpokenVoiceInput(spokenText: String) {
    _lastWakeQuery.value = spokenText
    // If it's a hotword wake or normal command:
    val lower = spokenText.lowercase().trim()
    if (lower == "jarvis" || lower == "hey jarvis") {
      val greeting = "Yes, sir? Systems online and listening."
      viewModelScope.launch {
        repository.insertMessage(
          ChatMessage(sender = "jarvis", message = greeting, isOfflineProcessed = true)
        )
      }
      voiceManager.speak(greeting)
    } else {
      // Process full command
      sendUserMessage(spokenText)
    }
  }

  fun sendUserMessage(text: String, isMathTutor: Boolean = true) {
    val clean = text.trim()
    if (clean.isBlank()) return

    viewModelScope.launch {
      // 1. Record User Message
      repository.insertMessage(
        ChatMessage(sender = "user", message = clean)
      )

      _isProcessingQuery.value = true

      // 2. Decide Offline vs Gemini API
      val responseText: String
      val isOffline: Boolean

      if (_isStrictOfflineMode.value) {
        val result = JarvisOfflineEngine.processQuery(clean)
        responseText = result.responseText
        isOffline = true
      } else {
        // Try Gemini AI first, fallback to offline
        val offlineResult = JarvisOfflineEngine.processQuery(clean)
        if (offlineResult.mathResult != null) {
          // Instant sub-second math response!
          responseText = offlineResult.responseText
          isOffline = true
        } else {
          val aiResponse = GeminiClient.askJarvis(clean, isMathTutorMode = isMathTutor)
          responseText = aiResponse
          isOffline = false
        }
      }

      _isProcessingQuery.value = false

      // 3. Save Jarvis Response
      repository.insertMessage(
        ChatMessage(
          sender = "jarvis",
          message = responseText,
          isOfflineProcessed = isOffline
        )
      )

      // 4. Voice Speech readout
      // For spoken TTS, extract first sentence or clean summary
      val spokenSummary = responseText.split("\n\n").firstOrNull() ?: responseText
      voiceManager.speak(spokenSummary)
    }
  }

  // --- Assignments ---
  fun addAssignment(title: String, subject: String, priority: String, notes: String, hoursFromNow: Long = 24) {
    viewModelScope.launch {
      val dueTime = System.currentTimeMillis() + (hoursFromNow * 3600 * 1000)
      val encryptedNotes = VaultSecurityManager.encrypt(notes)
      repository.insertAssignment(
        Assignment(
          title = title.ifBlank { "Untitled Assignment" },
          subject = subject.ifBlank { "General" },
          dueDateTimestamp = dueTime,
          priority = priority,
          notes = encryptedNotes
        )
      )
    }
  }

  fun toggleAssignmentCompleted(assignment: Assignment) {
    viewModelScope.launch {
      repository.updateAssignment(assignment.copy(isCompleted = !assignment.isCompleted))
    }
  }

  fun deleteAssignment(id: Int) {
    viewModelScope.launch {
      repository.deleteAssignment(id)
    }
  }

  // --- Vault Files & Encrypted Study Notes ---
  private val _vaultSearchQuery = MutableStateFlow("")
  val vaultSearchQuery: StateFlow<String> = _vaultSearchQuery.asStateFlow()

  fun setVaultSearchQuery(query: String) {
    _vaultSearchQuery.value = query
  }

  fun addVaultFile(fileName: String, category: String, subject: String = "General", content: String, tags: String = "") {
    viewModelScope.launch {
      val encryptedContent = VaultSecurityManager.encrypt(content)
      repository.insertVaultFile(
        LocalVaultFile(
          fileName = if (fileName.endsWith(".enc")) fileName else "$fileName.enc",
          category = category,
          subject = subject.ifBlank { "General" },
          contentSnippet = encryptedContent,
          sizeKb = (content.length / 100).coerceAtLeast(1),
          tags = tags
        )
      )
    }
  }

  fun decryptText(cipherText: String): String {
    return VaultSecurityManager.decrypt(cipherText)
  }

  fun deleteVaultFile(id: Int) {
    viewModelScope.launch {
      repository.deleteVaultFile(id)
    }
  }

  fun clearVault() {
    viewModelScope.launch {
      repository.clearVault()
    }
  }

  fun clearChat() {
    viewModelScope.launch {
      repository.clearMessages()
    }
  }

  // --- Study Timer ---
  fun startStudyTimer() {
    if (_isTimerRunning.value) return
    _isTimerRunning.value = true
    countDownTimer = object : CountDownTimer(_studyTimerSeconds.value * 1000L, 1000L) {
      override fun onTick(millisUntilFinished: Long) {
        _studyTimerSeconds.value = (millisUntilFinished / 1000).toInt()
      }

      override fun onFinish() {
        _isTimerRunning.value = false
        _studyTimerSeconds.value = 25 * 60
        viewModelScope.launch {
          repository.insertStudySession(
            StudySession(subject = "Focus Session", minutes = 25, score = 100)
          )
        }
        voiceManager.speak("Study session completed, sir. Outstanding focus logged to local records.")
      }
    }.start()
  }

  fun pauseStudyTimer() {
    countDownTimer?.cancel()
    _isTimerRunning.value = false
  }

  fun resetStudyTimer() {
    countDownTimer?.cancel()
    _isTimerRunning.value = false
    _studyTimerSeconds.value = 25 * 60
  }

  // --- Math Practice Quiz ---
  fun generateNewQuizQuestion() {
    _quizFeedback.value = null
    val quizPool = listOf(
      MathPracticeQuestion(
        question = "Solve for x: 3x + 9 = 30",
        options = listOf("x = 5", "x = 7", "x = 9", "x = 6"),
        correctIndex = 1,
        explanation = "Subtract 9 from both sides: 3x = 21. Divide by 3: x = 7."
      ),
      MathPracticeQuestion(
        question = "What is the area of a circle with radius r = 7? (Use π ≈ 22/7)",
        options = listOf("154", "44", "98", "147"),
        correctIndex = 0,
        explanation = "Area = π·r² = (22/7)·(49) = 22·7 = 154."
      ),
      MathPracticeQuestion(
        question = "Evaluate: 15% of 240",
        options = listOf("32", "36", "40", "28"),
        correctIndex = 1,
        explanation = "10% of 240 is 24, 5% is 12. Total = 24 + 12 = 36."
      ),
      MathPracticeQuestion(
        question = "Find the roots of: x² - 9 = 0",
        options = listOf("x = ±3", "x = 9", "x = ±9", "x = 0"),
        correctIndex = 0,
        explanation = "Difference of squares: (x - 3)(x + 3) = 0 ⟹ x = 3 or x = -3."
      ),
      MathPracticeQuestion(
        question = "What is the derivative of f(x) = 5x³?",
        options = listOf("15x²", "5x²", "15x³", "3x²"),
        correctIndex = 0,
        explanation = "Power rule: d/dx [a·x^n] = a·n·x^(n-1). 5·3·x^(3-1) = 15x²."
      )
    )
    _currentQuizQuestion.value = quizPool.random()
  }

  fun answerQuizQuestion(selectedIndex: Int) {
    val current = _currentQuizQuestion.value ?: return
    if (selectedIndex == current.correctIndex) {
      _quizScore.value += 10
      _quizFeedback.value = "Correct, sir! +10 Mastery Points. ${current.explanation}"
      voiceManager.speak("Correct, sir! Mastery points added.")
    } else {
      _quizFeedback.value = "Incorrect. Correct answer was ${current.options[current.correctIndex]}. ${current.explanation}"
    }
  }

  override fun onCleared() {
    super.onCleared()
    countDownTimer?.cancel()
    voiceManager.shutdown()
  }
}
