package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
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
import com.example.ui.theme.JarvisAqua
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisNavyDark
import com.example.ui.theme.JarvisSuccess
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import kotlin.random.Random

@Composable
fun JarvisTopBar(
  isStrictOfflineMode: Boolean,
  isContinuousHotword: Boolean,
  isMuted: Boolean,
  onToggleMute: () -> Unit,
  onToggleHotword: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(JarvisNavyDark)
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (isContinuousHotword) JarvisSuccess else JarvisAqua)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "J.A.R.V.I.S. // CORE MARK VII",
          color = JarvisCyanBright,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.sp
        )
      }
      Text(
        text = if (isStrictOfflineMode) "AIR-GAPPED // LOCAL ENCRYPTION" else "HYBRID NEURAL LINK // ACTIVE",
        color = JarvisTextSecondary.copy(alpha = 0.7f),
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.5.sp
      )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      // Hotword Status Pill
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .background(JarvisSurfaceVariant)
          .border(1.dp, if (isContinuousHotword) JarvisCyan else Color.Transparent, RoundedCornerShape(4.dp))
          .clickable { onToggleHotword() }
          .padding(horizontal = 8.dp, vertical = 4.dp)
          .testTag("hotword_toggle_chip")
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (isContinuousHotword) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = "Hotword Detection",
            tint = if (isContinuousHotword) JarvisCyan else JarvisTextSecondary,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isContinuousHotword) "WAKE: ON" else "WAKE: OFF",
            color = if (isContinuousHotword) JarvisCyanBright else JarvisTextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Mute audio button
      IconButton(
        onClick = onToggleMute,
        modifier = Modifier
          .size(36.dp)
          .testTag("mute_toggle_btn")
      ) {
        Icon(
          imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
          contentDescription = if (isMuted) "Unmute Jarvis Audio" else "Mute Jarvis Audio",
          tint = if (isMuted) JarvisTextSecondary else JarvisCyan
        )
      }
    }
  }
}

@Composable
fun JarvisAudioWaveform(
  isSpeaking: Boolean,
  isListening: Boolean,
  audioLevel: Float,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "waveform")
  val wavePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(400),
      repeatMode = RepeatMode.Reverse
    ),
    label = "wave_phase"
  )

  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(32.dp)
      .padding(horizontal = 24.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
  ) {
    val barCount = 20
    for (i in 0 until barCount) {
      val baseHeight = 4f
      val dynamicFactor = when {
        isSpeaking -> (Math.sin((i.toDouble() / barCount) * Math.PI * 2 + wavePhase * 3) + 1.2).toFloat() * 10f
        isListening -> ((audioLevel * 22f) * (0.4f + (i % 3) * 0.3f)).coerceAtLeast(3f)
        else -> 3f + (i % 4)
      }
      val barHeight = (baseHeight + dynamicFactor).coerceIn(4f, 28f)

      Box(
        modifier = Modifier
          .width(3.dp)
          .height(barHeight.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(
            if (isSpeaking) JarvisCyanBright
            else if (isListening) JarvisAqua
            else JarvisCyan.copy(alpha = 0.3f)
          )
      )
    }
  }
}

@Composable
fun JarvisTelemetryDashboard(
  storageUsageMb: Float = 14.8f,
  encryptionStatus: String = "AES-256 GCM",
  neuralLatencyMs: Int = 8,
  pendingAssignmentsCount: Int = 3,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // 1. Core Power / Status Gauge
    TelemetryCard(
      title = "CORE STATUS",
      value = "100%",
      subValue = "ARC REACTOR STABLE",
      accentColor = JarvisCyan,
      modifier = Modifier.weight(1f)
    )

    // 2. Privacy Vault Telemetry
    TelemetryCard(
      title = "DATA VAULT",
      value = "${storageUsageMb}MB",
      subValue = encryptionStatus,
      accentColor = JarvisAqua,
      modifier = Modifier.weight(1f)
    )

    // 3. Academic Status
    TelemetryCard(
      title = "STUDY QUEUE",
      value = "$pendingAssignmentsCount DUE",
      subValue = "${neuralLatencyMs}ms RESP.",
      accentColor = JarvisCyanBright,
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun TelemetryCard(
  title: String,
  value: String,
  subValue: String,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(JarvisSurfaceDark)
      .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
      .padding(horizontal = 10.dp, vertical = 8.dp)
  ) {
    Column {
      Text(
        text = title,
        color = JarvisTextSecondary.copy(alpha = 0.7f),
        fontSize = 8.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        color = accentColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace
      )
      Spacer(modifier = Modifier.height(1.dp))
      Text(
        text = subValue,
        color = JarvisTextPrimary.copy(alpha = 0.65f),
        fontSize = 7.5.sp,
        fontFamily = FontFamily.Monospace,
        maxLines = 1
      )
    }
  }
}
