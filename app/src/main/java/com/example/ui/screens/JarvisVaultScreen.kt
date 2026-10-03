package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Assignment
import com.example.data.model.LocalVaultFile
import com.example.ui.JarvisViewModel
import com.example.ui.theme.JarvisAlert
import com.example.ui.theme.JarvisAqua
import com.example.ui.theme.JarvisBorderSubtle
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisNavyDark
import com.example.ui.theme.JarvisSuccess
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import com.example.ui.theme.JarvisWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JarvisVaultScreen(
  viewModel: JarvisViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val vaultFiles by viewModel.vaultFiles.collectAsStateWithLifecycle()
  val assignments by viewModel.assignments.collectAsStateWithLifecycle()
  val isStrictOffline by viewModel.isStrictOfflineMode.collectAsStateWithLifecycle()
  val searchQuery by viewModel.vaultSearchQuery.collectAsStateWithLifecycle()

  var selectedTab by remember { mutableIntStateOf(0) } // 0: Study Notes, 1: Assignment Vault
  var selectedCategoryFilter by remember { mutableStateOf("ALL") }
  var showAddFileDialog by remember { mutableStateOf(false) }
  var viewingFile by remember { mutableStateOf<LocalVaultFile?>(null) }
  var viewingAssignment by remember { mutableStateOf<Assignment?>(null) }
  var showPurgeConfirm by remember { mutableStateOf(false) }

  // Filter notes
  val filteredFiles = vaultFiles.filter { file ->
    val matchesCategory = selectedCategoryFilter == "ALL" || file.category.equals(selectedCategoryFilter, ignoreCase = true)
    val matchesSearch = searchQuery.isBlank() ||
      file.fileName.contains(searchQuery, ignoreCase = true) ||
      file.subject.contains(searchQuery, ignoreCase = true) ||
      file.tags.contains(searchQuery, ignoreCase = true)
    matchesCategory && matchesSearch
  }

  // Filter assignments
  val filteredAssignments = assignments.filter { assign ->
    searchQuery.isBlank() ||
      assign.title.contains(searchQuery, ignoreCase = true) ||
      assign.subject.contains(searchQuery, ignoreCase = true)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(JarvisNavyDark)
  ) {
    // 1. Vault Top Tabs: Study Notes vs Assignment Vault
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = JarvisSurfaceDark,
      contentColor = JarvisCyanBright,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = JarvisCyanBright
        )
      }
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("STUDY NOTES (${vaultFiles.size})", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }
        },
        modifier = Modifier.testTag("vault_tab_notes")
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ASSIGNMENTS (${assignments.size})", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }
        },
        modifier = Modifier.testTag("vault_tab_assignments")
      )
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Privacy & Encryption Status Card
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisSurfaceDark)
            .border(1.dp, JarvisCyan, RoundedCornerShape(8.dp))
            .padding(14.dp)
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Shield,
                  contentDescription = null,
                  tint = if (isStrictOffline) JarvisSuccess else JarvisCyan,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "LOCAL ON-DEVICE DATA VAULT",
                    color = JarvisCyanBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  )
                  Text(
                    text = if (isStrictOffline) "100% Offline Air-Gapped • AES Encrypted" else "Hardware Encrypted • Zero Data Leaks",
                    color = JarvisTextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                  )
                }
              }

              Switch(
                checked = isStrictOffline,
                onCheckedChange = { viewModel.toggleStrictOfflineMode() },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = JarvisSuccess,
                  checkedTrackColor = JarvisSurfaceVariant,
                  uncheckedThumbColor = JarvisCyan,
                  uncheckedTrackColor = JarvisSurfaceDark
                ),
                modifier = Modifier.testTag("strict_offline_toggle")
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Telemetry mini row
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(JarvisNavyDark)
                .padding(8.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              TelemetryMiniText(label = "STORAGE ENGINE", value = "ROOM / SQLITE")
              TelemetryMiniText(label = "CIPHER SUITE", value = "AES-CBC PKCS5")
              TelemetryMiniText(label = "INTEGRITY", value = "100% SECURE")
            }
          }
        }
      }

      // Search and Filter Bar
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { viewModel.setVaultSearchQuery(it) },
          placeholder = {
            Text(
              text = if (selectedTab == 0) "Search study notes, formulas, tags..." else "Search assignments, subjects...",
              color = JarvisTextSecondary.copy(alpha = 0.6f),
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace
            )
          },
          leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = JarvisCyan, modifier = Modifier.size(18.dp))
          },
          trailingIcon = {
            if (searchQuery.isNotBlank()) {
              IconButton(onClick = { viewModel.setVaultSearchQuery("") }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear Search", tint = JarvisTextSecondary, modifier = Modifier.size(16.dp))
              }
            }
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JarvisCyan,
            unfocusedBorderColor = JarvisSurfaceVariant,
            focusedTextColor = JarvisTextPrimary,
            unfocusedTextColor = JarvisTextPrimary,
            cursorColor = JarvisCyanBright,
            focusedContainerColor = JarvisSurfaceDark,
            unfocusedContainerColor = JarvisSurfaceDark
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_search_field")
        )
      }

      // Category Filter Chips (For Study Notes)
      if (selectedTab == 0) {
        item {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf("ALL", "Study Notes", "Math Formula", "Personal Memo", "System Log").forEach { cat ->
              val isSelected = selectedCategoryFilter == cat
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (isSelected) JarvisCyan else JarvisSurfaceDark)
                  .border(1.dp, if (isSelected) JarvisCyanBright else JarvisBorderSubtle, RoundedCornerShape(4.dp))
                  .clickable { selectedCategoryFilter = cat }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = cat,
                  color = if (isSelected) JarvisNavyDark else JarvisTextSecondary,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }
        }
      }

      // Action Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (selectedTab == 0) "// ENCRYPTED STUDY NOTES (${filteredFiles.size})" else "// SECURE ASSIGNMENTS (${filteredAssignments.size})",
            color = JarvisCyanBright,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )

          Row {
            if (selectedTab == 0) {
              Button(
                onClick = { showAddFileDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = JarvisNavyDark),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.testTag("add_vault_file_btn")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "NEW NOTE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
              }
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
              onClick = { showPurgeConfirm = true },
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(JarvisSurfaceDark)
                .border(1.dp, JarvisAlert.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .testTag("purge_vault_btn")
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Purge Vault", tint = JarvisAlert, modifier = Modifier.size(16.dp))
            }
          }
        }
      }

      // Content List based on selected tab
      if (selectedTab == 0) {
        if (filteredFiles.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "No study notes found in local vault",
                color = JarvisTextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        } else {
          items(filteredFiles, key = { it.id }) { file ->
            VaultFileCard(
              file = file,
              onView = { viewingFile = file },
              onDelete = { viewModel.deleteVaultFile(file.id) }
            )
          }
        }
      } else {
        if (filteredAssignments.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "No assignment records found",
                color = JarvisTextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        } else {
          items(filteredAssignments, key = { it.id }) { assignment ->
            VaultAssignmentCard(
              assignment = assignment,
              onView = { viewingAssignment = assignment },
              onDelete = { viewModel.deleteAssignment(assignment.id) }
            )
          }
        }
      }
    }
  }

  // Dialog to Add New Study Note / File
  if (showAddFileDialog) {
    AddVaultStudyNoteDialog(
      onDismiss = { showAddFileDialog = false },
      onConfirm = { name, cat, subject, content, tags ->
        viewModel.addVaultFile(name, cat, subject, content, tags)
        showAddFileDialog = false
        Toast.makeText(context, "Note encrypted & stored in Room database", Toast.LENGTH_SHORT).show()
      }
    )
  }

  // Dialog to View Decrypted File Content
  if (viewingFile != null) {
    val decryptedContent = viewModel.decryptText(viewingFile!!.contentSnippet)
    ViewVaultFileDialog(
      file = viewingFile!!,
      decryptedContent = decryptedContent,
      onCopy = {
        clipboardManager.setText(AnnotatedString(decryptedContent))
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
      },
      onDismiss = { viewingFile = null }
    )
  }

  // Dialog to View Decrypted Assignment Details
  if (viewingAssignment != null) {
    val decryptedNotes = viewModel.decryptText(viewingAssignment!!.notes)
    ViewAssignmentVaultDialog(
      assignment = viewingAssignment!!,
      decryptedNotes = decryptedNotes,
      onDismiss = { viewingAssignment = null }
    )
  }

  // Dialog to confirm purge
  if (showPurgeConfirm) {
    AlertDialog(
      onDismissRequest = { showPurgeConfirm = false },
      containerColor = JarvisSurfaceDark,
      title = {
        Text("SHRED ALL VAULT DATA?", color = JarvisAlert, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
      },
      text = {
        Text(
          "This will irrevocably wipe all encrypted study notes and local records from this device's Room database.",
          color = JarvisTextPrimary
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearVault()
            showPurgeConfirm = false
            Toast.makeText(context, "Local Vault shredded cleanly", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = JarvisAlert, contentColor = Color.White)
        ) {
          Text("CONFIRM SHRED", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showPurgeConfirm = false }) {
          Text("ABORT", color = JarvisTextSecondary, fontFamily = FontFamily.Monospace)
        }
      }
    )
  }
}

@Composable
fun VaultAssignmentCard(
  assignment: Assignment,
  onView: () -> Unit,
  onDelete: () -> Unit
) {
  val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(assignment.dueDateTimestamp))
  val priorityColor = when (assignment.priority) {
    "HIGH" -> JarvisAlert
    "MEDIUM" -> JarvisWarning
    else -> JarvisAqua
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(JarvisSurfaceDark)
      .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(8.dp))
      .clickable { onView() }
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(JarvisSurfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Assignment, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(priorityColor.copy(alpha = 0.2f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
              Text(assignment.priority, color = priorityColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text("[${assignment.subject}]", color = JarvisAqua, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
          }

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = assignment.title,
            color = JarvisTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )

          Text(
            text = "Due: $dateStr • Encrypted in Room DB",
            color = JarvisTextSecondary.copy(alpha = 0.7f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Row {
        IconButton(onClick = onView, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Visibility, contentDescription = "View", tint = JarvisCyan, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Delete, contentDescription = "Delete", tint = JarvisAlert.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
        }
      }
    }
  }
}

@Composable
fun AddVaultStudyNoteDialog(
  onDismiss: () -> Unit,
  onConfirm: (name: String, cat: String, subject: String, content: String, tags: String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var cat by remember { mutableStateOf("Study Notes") }
  var subject by remember { mutableStateOf("Mathematics") }
  var content by remember { mutableStateOf("") }
  var tags by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = JarvisSurfaceDark,
    title = {
      Text("NEW ENCRYPTED STUDY NOTE", color = JarvisCyanBright, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Note Title (e.g. Calculus_Derivatives)", color = JarvisTextSecondary) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JarvisCyan,
            unfocusedBorderColor = JarvisSurfaceVariant,
            focusedTextColor = JarvisTextPrimary,
            unfocusedTextColor = JarvisTextPrimary
          ),
          modifier = Modifier.fillMaxWidth().testTag("vault_filename_input")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject", color = JarvisTextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = JarvisCyan,
              unfocusedBorderColor = JarvisSurfaceVariant,
              focusedTextColor = JarvisTextPrimary,
              unfocusedTextColor = JarvisTextPrimary
            ),
            modifier = Modifier.weight(1f)
          )

          OutlinedTextField(
            value = cat,
            onValueChange = { cat = it },
            label = { Text("Category", color = JarvisTextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = JarvisCyan,
              unfocusedBorderColor = JarvisSurfaceVariant,
              focusedTextColor = JarvisTextPrimary,
              unfocusedTextColor = JarvisTextPrimary
            ),
            modifier = Modifier.weight(1f)
          )
        }

        OutlinedTextField(
          value = tags,
          onValueChange = { tags = it },
          label = { Text("Tags (comma-separated)", color = JarvisTextSecondary) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JarvisCyan,
            unfocusedBorderColor = JarvisSurfaceVariant,
            focusedTextColor = JarvisTextPrimary,
            unfocusedTextColor = JarvisTextPrimary
          ),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = content,
          onValueChange = { content = it },
          label = { Text("Study Content / Formulas / Solutions", color = JarvisTextSecondary) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JarvisCyan,
            unfocusedBorderColor = JarvisSurfaceVariant,
            focusedTextColor = JarvisTextPrimary,
            unfocusedTextColor = JarvisTextPrimary
          ),
          modifier = Modifier.fillMaxWidth().height(120.dp).testTag("vault_content_input"),
          maxLines = 6
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(name.ifBlank { "Untitled_Note" }, cat, subject, content, tags) },
        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = JarvisNavyDark),
        modifier = Modifier.testTag("save_vault_file_btn")
      ) {
        Text("ENCRYPT & SAVE TO ROOM", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("ABORT", color = JarvisTextSecondary, fontFamily = FontFamily.Monospace)
      }
    }
  )
}

@Composable
fun ViewVaultFileDialog(
  file: LocalVaultFile,
  decryptedContent: String,
  onCopy: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = JarvisSurfaceDark,
    title = {
      Column {
        Text(file.fileName, color = JarvisCyanBright, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text("[${file.subject} • ${file.category}]", color = JarvisAqua, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
      }
    },
    text = {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "DECRYPTED ON-DEVICE PAYLOAD:", color = JarvisAqua, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
          IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Content", tint = JarvisCyan, modifier = Modifier.size(16.dp))
          }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(JarvisNavyDark)
            .padding(12.dp)
        ) {
          Text(
            text = decryptedContent.ifBlank { "(Empty content)" },
            color = JarvisTextPrimary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = JarvisNavyDark)
      ) {
        Text("RE-LOCK VAULT", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      }
    }
  )
}

@Composable
fun ViewAssignmentVaultDialog(
  assignment: Assignment,
  decryptedNotes: String,
  onDismiss: () -> Unit
) {
  val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(assignment.dueDateTimestamp))

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = JarvisSurfaceDark,
    title = {
      Column {
        Text(assignment.title, color = JarvisCyanBright, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text("Subject: ${assignment.subject} • Priority: ${assignment.priority}", color = JarvisAqua, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
      }
    },
    text = {
      Column {
        Text(text = "DUE DATE: $dateStr", color = JarvisCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "SECURE STUDY NOTES & GUIDELINES:", color = JarvisTextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(JarvisNavyDark)
            .padding(12.dp)
        ) {
          Text(
            text = decryptedNotes.ifBlank { "No additional notes provided." },
            color = JarvisTextPrimary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = JarvisNavyDark)
      ) {
        Text("CLOSE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      }
    }
  )
}

@Composable
fun VaultFileCard(
  file: LocalVaultFile,
  onView: () -> Unit,
  onDelete: () -> Unit
) {
  val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(file.dateModified))

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(JarvisSurfaceDark)
      .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(8.dp))
      .clickable { onView() }
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(JarvisSurfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Lock, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
          Text(
            text = file.fileName,
            color = JarvisTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row {
            Text(
              text = "[${file.subject} • ${file.category}]",
              color = JarvisAqua,
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "$dateStr • ${file.sizeKb} KB",
              color = JarvisTextSecondary.copy(alpha = 0.7f),
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      Row {
        IconButton(onClick = onView, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Visibility, contentDescription = "View", tint = JarvisCyan, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Delete, contentDescription = "Delete", tint = JarvisAlert.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
        }
      }
    }
  }
}

@Composable
fun TelemetryMiniText(label: String, value: String) {
  Column {
    Text(text = label, color = JarvisTextSecondary.copy(alpha = 0.6f), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
    Text(text = value, color = JarvisCyanBright, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
  }
}
