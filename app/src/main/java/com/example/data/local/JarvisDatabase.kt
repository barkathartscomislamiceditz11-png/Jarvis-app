package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Assignment
import com.example.data.model.ChatMessage
import com.example.data.model.LocalVaultFile
import com.example.data.model.StudySession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [ChatMessage::class, Assignment::class, LocalVaultFile::class, StudySession::class],
  version = 2,
  exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
  abstract fun jarvisDao(): JarvisDao

  companion object {
    @Volatile
    private var INSTANCE: JarvisDatabase? = null

    fun getDatabase(context: Context): JarvisDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          JarvisDatabase::class.java,
          "jarvis_core_db"
        )
        .addCallback(object : Callback() {
          override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
              populateInitialData(getDatabase(context).jarvisDao())
            }
          }
        })
        .fallbackToDestructiveMigration()
        .build()
        INSTANCE = instance
        instance
      }
    }

    private suspend fun populateInitialData(dao: JarvisDao) {
      val now = System.currentTimeMillis()
      // Initial greeting
      dao.insertMessage(
        ChatMessage(
          sender = "jarvis",
          message = "System online. Good day, sir. All local systems operational, telemetry encrypted, and waiting for your command. You can speak to me anytime or say 'Jarvis' to activate.",
          timestamp = now - 60000,
          isOfflineProcessed = true
        )
      )

      // Sample School Assignments
      dao.insertAssignment(
        Assignment(
          title = "Calculus Problem Set #4",
          subject = "Mathematics",
          dueDateTimestamp = now + (3600 * 1000 * 6), // 6 hours
          priority = "HIGH",
          notes = VaultSecurityManager.encrypt("Derivative rules & tangent slopes: problems 12 to 28.")
        )
      )
      dao.insertAssignment(
        Assignment(
          title = "Physics Lab: Quantum Mechanics & Waveforms",
          subject = "Physics",
          dueDateTimestamp = now + (3600 * 1000 * 24), // tomorrow
          priority = "MEDIUM",
          notes = VaultSecurityManager.encrypt("Complete photon frequency analysis graphs and lab notes.")
        )
      )
      dao.insertAssignment(
        Assignment(
          title = "Linear Algebra Quiz Preparation",
          subject = "Mathematics",
          dueDateTimestamp = now + (3600 * 1000 * 48),
          priority = "HIGH",
          notes = VaultSecurityManager.encrypt("Matrix eigenvalues, dot products, and transformations.")
        )
      )

      // Initial Vault Files & Study Notes
      dao.insertVaultFile(
        LocalVaultFile(
          fileName = "Math_Formulas_CheatSheet.md",
          category = "Study Notes",
          subject = "Mathematics",
          contentSnippet = VaultSecurityManager.encrypt("Quadratic: x = (-b ± √(b² - 4ac)) / 2a. Derivative: d/dx(x^n) = n*x^(n-1). Euler: e^(i*pi) + 1 = 0"),
          sizeKb = 2,
          tags = "algebra,calculus,formulas"
        )
      )
      dao.insertVaultFile(
        LocalVaultFile(
          fileName = "Physics_Electromagnetism_Summary.md",
          category = "Study Notes",
          subject = "Physics",
          contentSnippet = VaultSecurityManager.encrypt("Maxwell's equations: Gauss's Law, Faraday's Law of Induction, Ampere-Maxwell Law. c = 1/√(ε₀μ₀)."),
          sizeKb = 3,
          tags = "physics,electromagnetism,maxwell"
        )
      )
      dao.insertVaultFile(
        LocalVaultFile(
          fileName = "School_Schedule_Semester_A.dat",
          category = "Personal Memo",
          subject = "School",
          contentSnippet = VaultSecurityManager.encrypt("Period 1: AP Calculus (8:30 AM). Period 2: Physics Lab (10:15 AM). Period 3: Computer Science (1:00 PM)."),
          sizeKb = 3,
          tags = "schedule,classes"
        )
      )
      dao.insertVaultFile(
        LocalVaultFile(
          fileName = "Arc_Core_Diagnostics_Log.enc",
          category = "System Log",
          subject = "Security",
          contentSnippet = VaultSecurityManager.encrypt("AES-128 CBC local encrypted block. Zero cloud egress. Offline integrity verified: 100%."),
          sizeKb = 5,
          tags = "diagnostics,security"
        )
      )

      // Study Session
      dao.insertStudySession(
        StudySession(
          subject = "Mathematics",
          minutes = 45,
          score = 95,
          timestamp = now - (3600 * 1000 * 12)
        )
      )
    }
  }
}
