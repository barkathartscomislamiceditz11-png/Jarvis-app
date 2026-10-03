package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.JarvisTab
import com.example.ui.JarvisViewModel
import com.example.ui.components.JarvisArcReactorOrb
import com.example.ui.components.JarvisAudioWaveform
import com.example.ui.components.JarvisTelemetryDashboard
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JarvisCoreScreen(
  viewModel: JarvisViewModel,
  modifier: Modifier = Modifier
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val isListening by viewModel.voiceManager.isListening.collectAsStateWithLifecycle()
  val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsStateWithLifecycle()
  val audioLevel by viewModel.voiceManager.audioLevel.collectAsStateWithLifecycle()
  val isContinuousHotword by viewModel.voiceManager.isContinuousHotwordActive.collectAsStateWithLifecycle()
  val isBackgroundServiceRunning by viewModel.isBackgroundWakeRunning.collectAsStateWithLifecycle()
  val bgAudioLevel by com.example.service.JarvisWakeWordBackgroundService.audioLevelFlow.collectAsStateWithLifecycle()
  val isStrictOffline by viewModel.isStrictOfflineMode.collectAsStateWithLifecycle()
  val isProcessing by viewModel.isProcessingQuery.collectAsStateWithLifecycle()
  val messages by viewModel.messages.collectAsStateWithLifecycle()
  val assignments by viewModel.assignments.collectAsStateWithLifecycle()

  val latestJarvisMessage = messages.lastOrNull { it.sender == "jarvis" }
  val pendingCount = assignments.count { !it.isCompleted }

  val isRadarActive = isContinuousHotword || isBackgroundServiceRunning
  val effectiveAudioLevel = if (isBackgroundServiceRunning) bgAudioLevel.coerceAtLeast(audioLevel) else audioLevel

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(JarvisNavyDark)
      .verticalScroll(rememberScrollState())
      .padding(bottom = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // 1. Hotword "Call Jarvis" Banner
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(
          Brush.horizontalGradient(
            listOf(JarvisSurfaceDark, JarvisSurfaceVariant)
          )
        )
        .border(1.dp, JarvisCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(if (isRadarActive) JarvisSuccess else JarvisAqua)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = if (isRadarActive) "VOICE RADAR ACTIVE: SAY \"JARVIS\"" else "VOICE RADAR PAUSED (TAP TO TALK)",
              color = JarvisCyanBright,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.8.sp
            )
            Text(
              text = if (isBackgroundServiceRunning) "Foreground Service listening in background" else "Always listening for your command • 0ms Wake Latency",
              color = JarvisTextSecondary.copy(alpha = 0.8f),
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        IconButton(
          onClick = {
            viewModel.toggleBackgroundWakeService(context)
          },
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isRadarActive) JarvisCyan else JarvisSurfaceDark)
            .border(1.dp, JarvisCyan, CircleShape)
            .testTag("core_mic_button")
        ) {
          Icon(
            imageVector = if (isRadarActive) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = "Voice Input Toggle",
            tint = if (isRadarActive) JarvisNavyDark else JarvisCyanBright,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Centerpiece: Pulsing Arc Reactor Orb
    JarvisArcReactorOrb(
      isListening = isListening || isBackgroundServiceRunning,
      isSpeaking = isSpeaking,
      audioLevel = effectiveAudioLevel,
      size = 250.dp,
      onClick = {
        viewModel.toggleBackgroundWakeService(context)
      }
    )

    Spacer(modifier = Modifier.height(8.dp))

    // 3. Audio Frequency Waveform
    JarvisAudioWaveform(
      isSpeaking = isSpeaking,
      isListening = isListening || isBackgroundServiceRunning,
      audioLevel = effectiveAudioLevel
    )

    Spacer(modifier = Modifier.height(14.dp))

    // 4. Telemetry Dashboard (matching images)
    JarvisTelemetryDashboard(
      storageUsageMb = 16.4f,
      encryptionStatus = if (isStrictOffline) "OFFLINE LOCK" else "AES-256 LOCAL",
      neuralLatencyMs = if (isStrictOffline) 5 else 12,
      pendingAssignmentsCount = pendingCount
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 5. Latest Voice / Jarvis Output Card
    AnimatedVisibility(
      visible = latestJarvisMessage != null || isProcessing,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(JarvisSurfaceDark)
          .border(1.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
          .padding(14.dp)
          .testTag("latest_response_card")
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "// JARVIS RESPONSE FEED",
              color = JarvisCyanBright,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 1.sp
            )
            if (latestJarvisMessage?.isOfflineProcessed == true) {
              Text(
                text = "[OFFLINE FAST ENGINE]",
                color = JarvisAqua,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (isProcessing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = JarvisCyan
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Synthesizing neural telemetry...",
                color = JarvisTextSecondary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
              )
            }
          } else {
            Text(
              text = latestJarvisMessage?.message ?: "",
              color = JarvisTextPrimary,
              fontSize = 13.sp,
              lineHeight = 18.sp
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 6. Fast Command Quick Action Chips
    Text(
      text = "DIRECTIVE PROTOCOLS",
      color = JarvisCyanBright,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 1.2.sp,
      modifier = Modifier
        .align(Alignment.Start)
        .padding(horizontal = 16.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))

    FlowRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      QuickCommandChip(
        label = "Solve 4x + 16 = 48",
        icon = Icons.Default.Calculate,
        onClick = { viewModel.sendUserMessage("solve 4x + 16 = 48") }
      )
      QuickCommandChip(
        label = "Homework & School",
        icon = Icons.Default.School,
        onClick = { viewModel.setTab(JarvisTab.SCHOOL_STUDY) }
      )
      QuickCommandChip(
        label = "Math Practice",
        icon = Icons.Default.Calculate,
        onClick = { viewModel.setTab(JarvisTab.SCHOOL_STUDY) }
      )
      QuickCommandChip(
        label = "25m Study Timer",
        icon = Icons.Default.Timer,
        onClick = {
          viewModel.startStudyTimer()
          viewModel.setTab(JarvisTab.SCHOOL_STUDY)
        }
      )
      QuickCommandChip(
        label = "Local Data Vault",
        icon = Icons.Default.Security,
        onClick = { viewModel.setTab(JarvisTab.PRIVACY_VAULT) }
      )
      QuickCommandChip(
        label = "What is 15% of 320?",
        icon = Icons.Default.Calculate,
        onClick = { viewModel.sendUserMessage("what is 15% of 320") }
      )
    }
  }
}

@Composable
fun QuickCommandChip(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(JarvisSurfaceDark)
      .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(6.dp))
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 7.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = JarvisCyan,
        modifier = Modifier.size(13.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        color = JarvisTextPrimary,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}
