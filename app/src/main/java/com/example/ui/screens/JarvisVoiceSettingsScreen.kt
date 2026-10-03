package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.VoicePreset
import com.example.ui.JarvisViewModel
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

@Composable
fun JarvisVoiceSettingsScreen(
  viewModel: JarvisViewModel,
  modifier: Modifier = Modifier
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val currentPreset by viewModel.voiceManager.currentPreset.collectAsStateWithLifecycle()
  val isContinuousHotword by viewModel.voiceManager.isContinuousHotwordActive.collectAsStateWithLifecycle()
  val isBackgroundServiceRunning by viewModel.isBackgroundWakeRunning.collectAsStateWithLifecycle()
  val isMuted by viewModel.voiceManager.isSpeechMuted.collectAsStateWithLifecycle()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(JarvisNavyDark)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Hotword Wake Engine Card
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
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(if (isBackgroundServiceRunning || isContinuousHotword) JarvisSuccess else JarvisAqua)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "BACKGROUND \"JARVIS\" WAKE SERVICE",
                  color = JarvisCyanBright,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
                Text(
                  text = if (isBackgroundServiceRunning) "Foreground Service Active • Mic Online" else "Never tap — just call \"Jarvis\"",
                  color = JarvisTextSecondary,
                  fontSize = 9.sp,
                  fontFamily = FontFamily.Monospace
                )
              }
            }

            Switch(
              checked = isBackgroundServiceRunning || isContinuousHotword,
              onCheckedChange = { viewModel.toggleBackgroundWakeService(context) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = JarvisSuccess,
                checkedTrackColor = JarvisSurfaceVariant,
                uncheckedThumbColor = JarvisCyan,
                uncheckedTrackColor = JarvisSurfaceDark
              ),
              modifier = Modifier.testTag("hotword_toggle_switch")
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "When active, a dedicated Android Background Service monitors microphone input for the \"Jarvis\" wake-word continuously. It allows you to speak directives without tapping any buttons even while the app is in the background.",
            color = JarvisTextPrimary.copy(alpha = 0.8f),
            fontSize = 11.sp,
            lineHeight = 15.sp
          )

          Spacer(modifier = Modifier.height(8.dp))

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(4.dp))
              .background(JarvisNavyDark)
              .padding(horizontal = 8.dp, vertical = 6.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "SERVICE STATUS:",
                color = JarvisTextSecondary,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = if (isBackgroundServiceRunning) "[MICROPHONE FOREGROUND SERVICE ACTIVE]" else "[STANDBY]",
                color = if (isBackgroundServiceRunning) JarvisAqua else JarvisTextSecondary,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // 2. Voice Persona Presets
    item {
      Text(
        text = "// VOCAL SYNTHESIZER PRESETS",
        color = JarvisCyanBright,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp
      )
      Text(
        text = "Select custom male or female vocal acoustic matrices",
        color = JarvisTextSecondary,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace
      )
    }

    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VoicePreset.values().forEach { preset ->
          val isSelected = currentPreset == preset
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) JarvisSurfaceVariant else JarvisSurfaceDark)
              .border(
                1.dp,
                if (isSelected) JarvisCyanBright else JarvisBorderSubtle,
                RoundedCornerShape(8.dp)
              )
              .clickable { viewModel.voiceManager.setVoicePreset(preset) }
              .padding(12.dp)
              .testTag("preset_${preset.name}")
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) JarvisCyan else JarvisNavyDark),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = if (isSelected) JarvisNavyDark else JarvisCyanBright,
                    modifier = Modifier.size(18.dp)
                  )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                  Text(
                    text = preset.displayName,
                    color = if (isSelected) JarvisCyanBright else JarvisTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                  )
                  Text(
                    text = "${preset.gender} Frequency • Pitch ${preset.pitch}x • Speed ${preset.speed}x",
                    color = JarvisTextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                  )
                }
              }

              if (isSelected) {
                Text(
                  text = "[ACTIVE]",
                  color = JarvisAqua,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }
        }
      }
    }

    // 3. Audio Testing & Controls
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(JarvisSurfaceDark)
          .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(8.dp))
          .padding(14.dp)
      ) {
        Column {
          Text(
            text = "ACOUSTIC TEST MATRIX",
            color = JarvisCyanBright,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          Spacer(modifier = Modifier.height(10.dp))

          Button(
            onClick = {
              viewModel.voiceManager.speak("Testing vocal synthesizer. Systems fully functional, sir. Standing by for mathematical tutoring and school task execution.")
            },
            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = JarvisNavyDark),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth().testTag("test_voice_btn")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "TRANSMIT TEST PHRASE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Mute Voice Speech Output",
              color = JarvisTextPrimary,
              fontSize = 12.sp,
              fontFamily = FontFamily.Monospace
            )
            Switch(
              checked = isMuted,
              onCheckedChange = { viewModel.voiceManager.toggleMute() },
              colors = SwitchDefaults.colors(
                checkedThumbColor = JarvisAqua,
                checkedTrackColor = JarvisSurfaceVariant,
                uncheckedThumbColor = JarvisTextSecondary,
                uncheckedTrackColor = JarvisNavyDark
              )
            )
          }
        }
      }
    }
  }
}
