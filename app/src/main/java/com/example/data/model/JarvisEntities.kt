package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val sender: String, // "user" or "jarvis"
  val message: String,
  val timestamp: Long = System.currentTimeMillis(),
  val isOfflineProcessed: Boolean = true,
  val mathExplanation: String? = null
)

@Entity(tableName = "assignments")
data class Assignment(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val title: String,
  val subject: String,
  val dueDateTimestamp: Long,
  val priority: String = "HIGH", // "HIGH", "MEDIUM", "LOW"
  val isCompleted: Boolean = false,
  val notes: String = ""
)

@Entity(tableName = "local_vault_files")
data class LocalVaultFile(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val fileName: String,
  val category: String = "Study Notes", // "Study Notes", "Math Formula", "School Assignment", "Exam Prep"
  val subject: String = "General",
  val contentSnippet: String,
  val isEncrypted: Boolean = true,
  val dateModified: Long = System.currentTimeMillis(),
  val sizeKb: Int = 4,
  val tags: String = ""
)

@Entity(tableName = "study_sessions")
data class StudySession(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val subject: String,
  val minutes: Int,
  val score: Int = 100,
  val timestamp: Long = System.currentTimeMillis()
)
