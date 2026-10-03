package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Assignment
import com.example.data.model.ChatMessage
import com.example.data.model.LocalVaultFile
import com.example.data.model.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface JarvisDao {
  // Chat History
  @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
  fun getAllMessages(): Flow<List<ChatMessage>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: ChatMessage): Long

  @Query("DELETE FROM chat_messages")
  suspend fun clearAllMessages()

  // Assignments
  @Query("SELECT * FROM assignments ORDER BY isCompleted ASC, dueDateTimestamp ASC")
  fun getAllAssignments(): Flow<List<Assignment>>

  @Query("SELECT * FROM assignments WHERE isCompleted = 0 ORDER BY dueDateTimestamp ASC")
  fun getPendingAssignments(): Flow<List<Assignment>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAssignment(assignment: Assignment): Long

  @Update
  suspend fun updateAssignment(assignment: Assignment)

  @Query("DELETE FROM assignments WHERE id = :id")
  suspend fun deleteAssignmentById(id: Int)

  // Local Vault Files (Study Notes & Secure Documents)
  @Query("SELECT * FROM local_vault_files ORDER BY dateModified DESC")
  fun getAllVaultFiles(): Flow<List<LocalVaultFile>>

  @Query("SELECT * FROM local_vault_files WHERE category = :category ORDER BY dateModified DESC")
  fun getVaultFilesByCategory(category: String): Flow<List<LocalVaultFile>>

  @Query("SELECT * FROM local_vault_files WHERE fileName LIKE '%' || :query || '%' OR subject LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY dateModified DESC")
  fun searchVaultFiles(query: String): Flow<List<LocalVaultFile>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertVaultFile(file: LocalVaultFile): Long

  @Update
  suspend fun updateVaultFile(file: LocalVaultFile)

  @Query("DELETE FROM local_vault_files WHERE id = :id")
  suspend fun deleteVaultFileById(id: Int)

  @Query("DELETE FROM local_vault_files")
  suspend fun clearVault()

  // Study Sessions
  @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
  fun getAllStudySessions(): Flow<List<StudySession>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStudySession(session: StudySession): Long
}
