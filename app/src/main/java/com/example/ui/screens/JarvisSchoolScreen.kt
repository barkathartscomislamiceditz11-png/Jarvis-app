package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Assignment
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

@Composable
fun JarvisSchoolScreen(
  viewModel: JarvisViewModel,
  modifier: Modifier = Modifier
) {
  var selectedSubTab by remember { mutableIntStateOf(0) }
  val subTabs = listOf("ASSIGNMENTS", "MATH PRACTICE", "FOCUS TIMER")

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(JarvisNavyDark)
  ) {
    // Secondary Tab Bar
    TabRow(
      selectedTabIndex = selectedSubTab,
      containerColor = JarvisSurfaceDark,
      contentColor = JarvisCyanBright,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
          color = JarvisCyanBright
        )
      }
    ) {
      subTabs.forEachIndexed { index, title ->
        Tab(
          selected = selectedSubTab == index,
          onClick = { selectedSubTab = index },
          text = {
            Text(
              text = title,
              fontFamily = FontFamily.Monospace,
              fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Normal,
              fontSize = 11.sp
            )
          },
          modifier = Modifier.testTag("school_tab_$index")
        )
      }
    }

    when (selectedSubTab) {
      0 -> AssignmentsTabContent(viewModel)
      1 -> MathPracticeTabContent(viewModel)
      2 -> FocusTimerTabContent(viewModel)
    }
  }
}

@Composable
fun AssignmentsTabContent(viewModel: JarvisViewModel) {
  val assignments by viewModel.assignments.collectAsStateWithLifecycle()
  var showAddDialog by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "// ACADEMIC TELEMETRY",
              color = JarvisCyanBright,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
            val pending = assignments.count { !it.isCompleted }
            Text(
              text = "$pending pending assignments • Local encryption active",
              color = JarvisTextSecondary,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace
            )
          }

          Button(
            onClick = { showAddDialog = true },
            colors = ButtonDefaults.buttonColors(
              containerColor = JarvisCyan,
              contentColor = JarvisNavyDark
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.testTag("add_assignment_btn")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "ADD", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
          }
        }
      }

      items(assignments, key = { it.id }) { assignment ->
        AssignmentCard(
          assignment = assignment,
          decryptedNotes = viewModel.decryptText(assignment.notes),
          onToggleComplete = { viewModel.toggleAssignmentCompleted(assignment) },
          onDelete = { viewModel.deleteAssignment(assignment.id) }
        )
      }
    }

    if (showAddDialog) {
      AddAssignmentDialog(
        onDismiss = { showAddDialog = false },
        onConfirm = { title, subject, priority, notes, hours ->
          viewModel.addAssignment(title, subject, priority, notes, hours)
          showAddDialog = false
        }
      )
    }
  }
}

@Composable
fun AssignmentCard(
  assignment: Assignment,
  decryptedNotes: String,
  onToggleComplete: () -> Unit,
  onDelete: () -> Unit
) {
  val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
  val dueStr = dateFormat.format(Date(assignment.dueDateTimestamp))
  val isOverdue = assignment.dueDateTimestamp < System.currentTimeMillis() && !assignment.isCompleted

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
      .border(
        1.dp,
        if (assignment.isCompleted) JarvisBorderSubtle else if (isOverdue) JarvisAlert.copy(alpha = 0.5f) else JarvisCyan.copy(alpha = 0.35f),
        RoundedCornerShape(8.dp)
      )
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Checkbox(
        checked = assignment.isCompleted,
        onCheckedChange = { onToggleComplete() },
        colors = CheckboxDefaults.colors(
          checkedColor = JarvisSuccess,
          uncheckedColor = JarvisCyan,
          checkmarkColor = JarvisNavyDark
        ),
        modifier = Modifier.testTag("assignment_checkbox_${assignment.id}")
      )

      Spacer(modifier = Modifier.width(8.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Priority Pill
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(3.dp))
              .background(priorityColor.copy(alpha = 0.2f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = assignment.priority,
              color = priorityColor,
              fontSize = 8.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          Text(
            text = "[${assignment.subject}]",
            color = JarvisTextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = assignment.title,
          color = if (assignment.isCompleted) JarvisTextSecondary else JarvisTextPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          textDecoration = if (assignment.isCompleted) TextDecoration.LineThrough else TextDecoration.None
        )

        if (decryptedNotes.isNotBlank()) {
          Text(
            text = decryptedNotes,
            color = JarvisTextSecondary.copy(alpha = 0.7f),
            fontSize = 11.sp,
            maxLines = 2
          )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
          text = if (isOverdue) "DUE: $dueStr (OVERDUE)" else "DUE: $dueStr",
          color = if (isOverdue) JarvisAlert else JarvisAqua,
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace
        )
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier.size(32.dp).testTag("delete_assignment_${assignment.id}")
      ) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete Assignment",
          tint = JarvisTextSecondary.copy(alpha = 0.5f),
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}

@Composable
fun AddAssignmentDialog(
  onDismiss: () -> Unit,
  onConfirm: (title: String, subject: String, priority: String, notes: String, hours: Long) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var subject by remember { mutableStateOf("Mathematics") }
  var priority by remember { mutableStateOf("HIGH") }
  var notes by remember { mutableStateOf("") }
  var hoursStr by remember { mutableStateOf("24") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = JarvisSurfaceDark,
    title = {
      Text(
        text = "NEW ACADEMIC DIRECTIVE",
        color = JarvisCyanBright,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Assignment Title", color = JarvisTextSecondary) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JarvisCyan,
            unfocusedBorderColor = JarvisSurfaceVariant,
            focusedTextColor = JarvisTextPrimary,
            unfocusedTextColor = JarvisTextPrimary
          ),
          modifier = Modifier.fillMaxWidth().testTag("add_title_field")
        )

        OutlinedTextField(
          value = subject,
          onValueChange = { subject = it },
          label = { Text("Subject (e.g. Maths, Physics, CS)", color = JarvisTextSecondary) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JarvisCyan,
            unfocusedBorderColor = JarvisSurfaceVariant,
            focusedTextColor = JarvisTextPrimary,
            unfocusedTextColor = JarvisTextPrimary
          ),
          modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("HIGH", "MEDIUM", "LOW").forEach { p ->
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (priority == p) JarvisCyan else JarvisSurfaceVariant)
                .clickable { priority = p }
                .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text(
                text = p,
                color = if (priority == p) JarvisNavyDark else JarvisTextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes / Formula References", color = JarvisTextSecondary) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JarvisCyan,
            unfocusedBorderColor = JarvisSurfaceVariant,
            focusedTextColor = JarvisTextPrimary,
            unfocusedTextColor = JarvisTextPrimary
          ),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = hoursStr,
          onValueChange = { hoursStr = it },
          label = { Text("Due in hours (e.g. 6, 24, 48)", color = JarvisTextSecondary) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JarvisCyan,
            unfocusedBorderColor = JarvisSurfaceVariant,
            focusedTextColor = JarvisTextPrimary,
            unfocusedTextColor = JarvisTextPrimary
          ),
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val hours = hoursStr.toLongOrNull() ?: 24L
          onConfirm(title, subject, priority, notes, hours)
        },
        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = JarvisNavyDark),
        modifier = Modifier.testTag("confirm_add_assignment_btn")
      ) {
        Text("RECORD", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("CANCEL", color = JarvisTextSecondary, fontFamily = FontFamily.Monospace)
      }
    }
  )
}

@Composable
fun MathPracticeTabContent(viewModel: JarvisViewModel) {
  val question by viewModel.currentQuizQuestion.collectAsStateWithLifecycle()
  val score by viewModel.quizScore.collectAsStateWithLifecycle()
  val feedback by viewModel.quizFeedback.collectAsStateWithLifecycle()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Score Bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(JarvisSurfaceDark)
        .border(1.dp, JarvisCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "// MATH MASTERY INDEX",
            color = JarvisCyanBright,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "Personalized intelligent tutor evaluation",
            color = JarvisTextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
          )
        }
        Text(
          text = "$score PTS",
          color = JarvisAqua,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (question != null) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(JarvisSurfaceDark)
          .border(1.dp, JarvisCyan, RoundedCornerShape(10.dp))
          .padding(16.dp)
      ) {
        Column {
          Text(
            text = "QUESTION:",
            color = JarvisCyanBright,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = question!!.question,
            color = JarvisTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(14.dp))

          question!!.options.forEachIndexed { index, option ->
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(JarvisSurfaceVariant)
                .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(6.dp))
                .clickable { viewModel.answerQuizQuestion(index) }
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("quiz_option_$index")
            ) {
              Text(
                text = "${('A' + index)}. $option",
                color = JarvisTextPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      AnimatedVisibility(visible = feedback != null) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisSurfaceDark)
            .border(1.dp, JarvisAqua, RoundedCornerShape(8.dp))
            .padding(12.dp)
        ) {
          Text(
            text = feedback ?: "",
            color = JarvisCyanBright,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Button(
        onClick = { viewModel.generateNewQuizQuestion() },
        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = JarvisNavyDark),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().testTag("next_quiz_btn")
      ) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "NEXT CHALLENGE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      }
    }
  }
}

@Composable
fun FocusTimerTabContent(viewModel: JarvisViewModel) {
  val secondsLeft by viewModel.studyTimerSeconds.collectAsStateWithLifecycle()
  val isRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()

  val minutes = secondsLeft / 60
  val seconds = secondsLeft % 60
  val timeDisplay = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = "// QUANTUM FOCUS MATRIX",
      color = JarvisCyanBright,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 1.sp
    )
    Text(
      text = "25-Minute Cognitive Deep Work Protocol",
      color = JarvisTextSecondary,
      fontSize = 10.sp,
      fontFamily = FontFamily.Monospace
    )

    Spacer(modifier = Modifier.height(28.dp))

    // Circular Countdown Dial
    Box(
      modifier = Modifier
        .size(200.dp)
        .clip(CircleShape)
        .background(JarvisSurfaceDark)
        .border(3.dp, if (isRunning) JarvisCyanBright else JarvisBorderSubtle, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = timeDisplay,
          color = JarvisCyanBright,
          fontSize = 38.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
        Text(
          text = if (isRunning) "FOCUS ACTIVE" else "STANDBY",
          color = if (isRunning) JarvisSuccess else JarvisTextSecondary,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(28.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Button(
        onClick = {
          if (isRunning) viewModel.pauseStudyTimer()
          else viewModel.startStudyTimer()
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isRunning) JarvisWarning else JarvisCyan,
          contentColor = JarvisNavyDark
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.testTag("timer_play_pause_btn")
      ) {
        Icon(
          imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
          contentDescription = null
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isRunning) "PAUSE" else "INITIATE",
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace
        )
      }

      Button(
        onClick = { viewModel.resetStudyTimer() },
        colors = ButtonDefaults.buttonColors(
          containerColor = JarvisSurfaceDark,
          contentColor = JarvisTextPrimary
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.border(1.dp, JarvisBorderSubtle, RoundedCornerShape(6.dp)).testTag("timer_reset_btn")
      ) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "RESET", fontFamily = FontFamily.Monospace)
      }
    }
  }
}
