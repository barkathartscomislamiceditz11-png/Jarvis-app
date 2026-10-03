package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.ui.JarvisViewModel
import com.example.ui.theme.JarvisAqua
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisNavyDark
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JarvisTutorScreen(
  viewModel: JarvisViewModel,
  modifier: Modifier = Modifier
) {
  val messages by viewModel.messages.collectAsStateWithLifecycle()
  val isProcessing by viewModel.isProcessingQuery.collectAsStateWithLifecycle()
  val isListening by viewModel.voiceManager.isListening.collectAsStateWithLifecycle()
  val isStrictOffline by viewModel.isStrictOfflineMode.collectAsStateWithLifecycle()

  var inputText by remember { mutableStateOf("") }
  var isTutorModeActive by remember { mutableStateOf(true) }
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(JarvisNavyDark)
  ) {
    // Header Control Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(JarvisSurfaceDark)
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Calculate,
          contentDescription = "Math Tutor Mode",
          tint = JarvisCyan,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isTutorModeActive) "MATH TUTOR PROTOCOL" else "DIRECT CHAT CONSOLE",
          color = JarvisCyanBright,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = if (isTutorModeActive) "STEP-BY-STEP" else "DIRECT",
          color = JarvisTextSecondary,
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(4.dp))
        Switch(
          checked = isTutorModeActive,
          onCheckedChange = { isTutorModeActive = it },
          colors = SwitchDefaults.colors(
            checkedThumbColor = JarvisCyanBright,
            checkedTrackColor = JarvisSurfaceVariant,
            uncheckedThumbColor = JarvisTextSecondary,
            uncheckedTrackColor = JarvisSurfaceDark
          ),
          modifier = Modifier.testTag("tutor_mode_switch")
        )
        Spacer(modifier = Modifier.width(6.dp))
        IconButton(
          onClick = { viewModel.clearChat() },
          modifier = Modifier.size(32.dp).testTag("clear_chat_button")
        ) {
          Icon(
            imageVector = Icons.Default.DeleteSweep,
            contentDescription = "Clear Chat History",
            tint = JarvisTextSecondary,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    // Message List
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      items(messages, key = { it.id }) { msg ->
        ChatMessageBubble(message = msg)
      }

      if (isProcessing) {
        item {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(JarvisSurfaceDark)
              .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
              .padding(12.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = JarvisCyan
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "JARVIS computing response...",
                color = JarvisTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }
    }

    // Bottom Input Bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(JarvisSurfaceDark)
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          placeholder = {
            Text(
              text = if (isTutorModeActive) "Ask math problem or school topic..." else "Command Jarvis...",
              color = JarvisTextSecondary.copy(alpha = 0.6f),
              fontSize = 12.sp,
              fontFamily = FontFamily.Monospace
            )
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JarvisCyan,
            unfocusedBorderColor = JarvisSurfaceVariant,
            focusedTextColor = JarvisTextPrimary,
            unfocusedTextColor = JarvisTextPrimary,
            cursorColor = JarvisCyanBright,
            focusedContainerColor = JarvisNavyDark,
            unfocusedContainerColor = JarvisNavyDark
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("chat_input_field"),
          maxLines = 3
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Mic input
        IconButton(
          onClick = {
            if (isListening) viewModel.voiceManager.stopListening()
            else viewModel.voiceManager.startListening()
          },
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (isListening) JarvisCyan else JarvisNavyDark)
            .border(1.dp, JarvisCyan, CircleShape)
            .testTag("tutor_mic_btn")
        ) {
          Icon(
            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = "Voice Input",
            tint = if (isListening) JarvisNavyDark else JarvisCyanBright,
            modifier = Modifier.size(18.dp)
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Send button
        IconButton(
          onClick = {
            if (inputText.isNotBlank()) {
              viewModel.sendUserMessage(inputText, isMathTutor = isTutorModeActive)
              inputText = ""
            }
          },
          enabled = inputText.isNotBlank(),
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (inputText.isNotBlank()) JarvisCyan else JarvisSurfaceVariant)
            .testTag("send_message_button")
        ) {
          Icon(
            imageVector = Icons.Default.Send,
            contentDescription = "Send Message",
            tint = if (inputText.isNotBlank()) JarvisNavyDark else JarvisTextSecondary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
  val isJarvis = message.sender == "jarvis"
  val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
  val timeStr = timeFormat.format(Date(message.timestamp))

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isJarvis) Alignment.Start else Alignment.End
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(bottom = 2.dp)
    ) {
      Text(
        text = if (isJarvis) "JARVIS AI // RESP" else "USER // COMMAND",
        color = if (isJarvis) JarvisCyan else JarvisAqua,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = timeStr,
        color = JarvisTextSecondary.copy(alpha = 0.6f),
        fontSize = 8.sp,
        fontFamily = FontFamily.Monospace
      )
      if (isJarvis && message.isOfflineProcessed) {
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "• OFFLINE",
          color = JarvisAqua,
          fontSize = 8.sp,
          fontFamily = FontFamily.Monospace
        )
      }
    }

    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(if (isJarvis) JarvisSurfaceDark else JarvisSurfaceVariant)
        .border(
          width = 1.dp,
          color = if (isJarvis) JarvisCyan.copy(alpha = 0.4f) else JarvisAqua.copy(alpha = 0.25f),
          shape = RoundedCornerShape(8.dp)
        )
        .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
      Text(
        text = message.message,
        color = JarvisTextPrimary,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontFamily = if (isJarvis && message.message.contains("=")) FontFamily.Monospace else FontFamily.Default
      )
    }
  }
}
