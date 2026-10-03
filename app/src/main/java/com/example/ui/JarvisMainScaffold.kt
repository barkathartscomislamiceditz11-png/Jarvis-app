package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.JarvisTopBar
import com.example.ui.screens.JarvisCoreScreen
import com.example.ui.screens.JarvisSchoolScreen
import com.example.ui.screens.JarvisTutorScreen
import com.example.ui.screens.JarvisVaultScreen
import com.example.ui.screens.JarvisVoiceSettingsScreen
import com.example.ui.theme.JarvisBorderSubtle
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisNavyDark
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun JarvisMainScaffold(
  viewModel: JarvisViewModel = viewModel()
) {
  val context = LocalContext.current
  val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
  val isStrictOffline by viewModel.isStrictOfflineMode.collectAsStateWithLifecycle()
  val isContinuousHotword by viewModel.voiceManager.isContinuousHotwordActive.collectAsStateWithLifecycle()
  val isBackgroundServiceRunning by viewModel.isBackgroundWakeRunning.collectAsStateWithLifecycle()
  val isMuted by viewModel.voiceManager.isSpeechMuted.collectAsStateWithLifecycle()

  // Permissions launcher for audio and notifications
  val permissionsLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { perms ->
    val audioGranted = perms[Manifest.permission.RECORD_AUDIO] == true
    if (audioGranted) {
      if (isContinuousHotword || isBackgroundServiceRunning) {
        viewModel.toggleBackgroundWakeService(context)
      }
    }
  }

  LaunchedEffect(Unit) {
    val neededPermissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
    if (android.os.Build.VERSION.SDK_INT >= 33) {
      neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    val missing = neededPermissions.filter {
      ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

    if (missing.isNotEmpty()) {
      permissionsLauncher.launch(missing.toTypedArray())
    } else {
      // Start background wake radar
      if (isContinuousHotword && !isBackgroundServiceRunning) {
        viewModel.toggleBackgroundWakeService(context)
      }
    }
  }

  // Handle back button: return to Core HUD if in subscreen
  BackHandler(enabled = currentTab != JarvisTab.CORE_HUD) {
    viewModel.setTab(JarvisTab.CORE_HUD)
  }

  Scaffold(
    topBar = {
      JarvisTopBar(
        isStrictOfflineMode = isStrictOffline,
        isContinuousHotword = isContinuousHotword || isBackgroundServiceRunning,
        isMuted = isMuted,
        onToggleMute = { viewModel.voiceManager.toggleMute() },
        onToggleHotword = { viewModel.toggleBackgroundWakeService(context) }
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = JarvisSurfaceDark,
        modifier = Modifier.border(1.dp, JarvisBorderSubtle)
      ) {
        NavigationBarItem(
          selected = currentTab == JarvisTab.CORE_HUD,
          onClick = { viewModel.setTab(JarvisTab.CORE_HUD) },
          icon = { Icon(Icons.Default.Circle, contentDescription = "Core HUD", modifier = Modifier.size(20.dp)) },
          label = { Text("CORE", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = JarvisNavyDark,
            selectedTextColor = JarvisCyanBright,
            indicatorColor = JarvisCyan,
            unselectedIconColor = JarvisTextSecondary,
            unselectedTextColor = JarvisTextSecondary
          ),
          modifier = Modifier.testTag("nav_core_hud")
        )

        NavigationBarItem(
          selected = currentTab == JarvisTab.TUTOR_CHAT,
          onClick = { viewModel.setTab(JarvisTab.TUTOR_CHAT) },
          icon = { Icon(Icons.Default.Chat, contentDescription = "AI Tutor & Chat", modifier = Modifier.size(20.dp)) },
          label = { Text("TUTOR", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = JarvisNavyDark,
            selectedTextColor = JarvisCyanBright,
            indicatorColor = JarvisCyan,
            unselectedIconColor = JarvisTextSecondary,
            unselectedTextColor = JarvisTextSecondary
          ),
          modifier = Modifier.testTag("nav_tutor_chat")
        )

        NavigationBarItem(
          selected = currentTab == JarvisTab.SCHOOL_STUDY,
          onClick = { viewModel.setTab(JarvisTab.SCHOOL_STUDY) },
          icon = { Icon(Icons.Default.School, contentDescription = "School & Math", modifier = Modifier.size(20.dp)) },
          label = { Text("SCHOOL", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = JarvisNavyDark,
            selectedTextColor = JarvisCyanBright,
            indicatorColor = JarvisCyan,
            unselectedIconColor = JarvisTextSecondary,
            unselectedTextColor = JarvisTextSecondary
          ),
          modifier = Modifier.testTag("nav_school_study")
        )

        NavigationBarItem(
          selected = currentTab == JarvisTab.PRIVACY_VAULT,
          onClick = { viewModel.setTab(JarvisTab.PRIVACY_VAULT) },
          icon = { Icon(Icons.Default.Security, contentDescription = "Privacy Vault", modifier = Modifier.size(20.dp)) },
          label = { Text("VAULT", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = JarvisNavyDark,
            selectedTextColor = JarvisCyanBright,
            indicatorColor = JarvisCyan,
            unselectedIconColor = JarvisTextSecondary,
            unselectedTextColor = JarvisTextSecondary
          ),
          modifier = Modifier.testTag("nav_privacy_vault")
        )

        NavigationBarItem(
          selected = currentTab == JarvisTab.VOICE_CONFIG,
          onClick = { viewModel.setTab(JarvisTab.VOICE_CONFIG) },
          icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = "Voice Matrix", modifier = Modifier.size(20.dp)) },
          label = { Text("VOICE", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = JarvisNavyDark,
            selectedTextColor = JarvisCyanBright,
            indicatorColor = JarvisCyan,
            unselectedIconColor = JarvisTextSecondary,
            unselectedTextColor = JarvisTextSecondary
          ),
          modifier = Modifier.testTag("nav_voice_config")
        )
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(JarvisNavyDark)
        .padding(paddingValues)
    ) {
      when (currentTab) {
        JarvisTab.CORE_HUD -> JarvisCoreScreen(viewModel = viewModel)
        JarvisTab.TUTOR_CHAT -> JarvisTutorScreen(viewModel = viewModel)
        JarvisTab.SCHOOL_STUDY -> JarvisSchoolScreen(viewModel = viewModel)
        JarvisTab.PRIVACY_VAULT -> JarvisVaultScreen(viewModel = viewModel)
        JarvisTab.VOICE_CONFIG -> JarvisVoiceSettingsScreen(viewModel = viewModel)
      }
    }
  }
}
