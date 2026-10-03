package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
  primary = JarvisCyan,
  onPrimary = Color(0xFF002028),
  primaryContainer = Color(0xFF004D59),
  onPrimaryContainer = JarvisCyanBright,
  secondary = JarvisAqua,
  onSecondary = Color(0xFF002018),
  secondaryContainer = Color(0xFF004D38),
  onSecondaryContainer = JarvisAqua,
  tertiary = JarvisBlue,
  onTertiary = Color.White,
  background = JarvisNavyDark,
  onBackground = JarvisTextPrimary,
  surface = JarvisSurfaceDark,
  onSurface = JarvisTextPrimary,
  surfaceVariant = JarvisSurfaceVariant,
  onSurfaceVariant = JarvisTextSecondary,
  outline = JarvisBorderSubtle,
  error = JarvisAlert,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Force Jarvis sci-fi theme for authentic HUD look
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = JarvisColorScheme,
    typography = Typography,
    content = content
  )
}
