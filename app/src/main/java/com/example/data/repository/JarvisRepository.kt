package com.example.data.repository

import com.example.data.local.JarvisDao
import com.example.data.model.Assignment
import com.example.data.model.ChatMessage
import com.example.data.model.LocalVaultFile
import com.example.data.model.StudySession
import kotlinx.coroutines.flow.Flow

class JarvisRepository(private val dao: JarvisDao) {
  val allMessages: Flow<List<ChatMessage>> = dao.getAllMessages()
  val allAssignments: Flow<List<Assignment>> = dao.getAllAssignments()
  val pendingAssignments: Flow<List<Assignment>> = dao.getPendingAssignments()
  val allVaultFiles: Flow<List<LocalVaultFile>> = dao.getAllVaultFiles()
  val allStudySessions: Flow<List<StudySession>> = dao.getAllStudySessions()

  suspend fun insertMessage(message: ChatMessage): Long = dao.insertMessage(message)
  suspend fun clearMessages() = dao.clearAllMessages()

  suspend fun insertAssignment(assignment: Assignment): Long = dao.insertAssignment(assignment)
  suspend fun updateAssignment(assignment: Assignment) = dao.updateAssignment(assignment)
  suspend fun deleteAssignment(id: Int) = dao.deleteAssignmentById(id)

  fun searchVaultFiles(query: String): Flow<List<LocalVaultFile>> = dao.searchVaultFiles(query)
  fun getVaultFilesByCategory(cat: String): Flow<List<LocalVaultFile>> = dao.getVaultFilesByCategory(cat)
  suspend fun insertVaultFile(file: LocalVaultFile): Long = dao.insertVaultFile(file)
  suspend fun updateVaultFile(file: LocalVaultFile) = dao.updateVaultFile(file)
  suspend fun deleteVaultFile(id: Int) = dao.deleteVaultFileById(id)
  suspend fun clearVault() = dao.clearVault()

  suspend fun insertStudySession(session: StudySession): Long = dao.insertStudySession(session)
}
