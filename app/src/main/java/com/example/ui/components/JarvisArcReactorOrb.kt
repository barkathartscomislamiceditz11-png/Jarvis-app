package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisAqua
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisArcReactorOrb(
  isListening: Boolean,
  isSpeaking: Boolean,
  audioLevel: Float, // 0.0 to 1.0
  modifier: Modifier = Modifier,
  size: Dp = 260.dp,
  onClick: () -> Unit = {}
) {
  val infiniteTransition = rememberInfiniteTransition(label = "jarvis_orb")

  // Smooth continuous rotation for outer HUD ring
  val outerRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 18000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "outer_rot"
  )

  // Counter-rotation for intermediate gear ring
  val innerRotation by infiniteTransition.animateFloat(
    initialValue = 360f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 10000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "inner_rot"
  )

  // Pulsing scale for the glowing orb
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.92f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (isSpeaking) 600 else if (isListening) 900 else 2400,
        easing = FastOutSlowInEasing
      ),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  // Expanding radiation ring
  val waveRadius by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "wave_radius"
  )

  val interactionSource = remember { MutableInteractionSource() }

  Box(
    modifier = modifier
      .size(size)
      .testTag("jarvis_arc_reactor_orb")
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      ),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
      val radius = (size.toPx() / 2f) * 0.92f

      // 1. Outermost subtle glowing halo
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            JarvisCyan.copy(alpha = if (isSpeaking || isListening) 0.35f else 0.15f),
            JarvisAqua.copy(alpha = 0.05f),
            Color.Transparent
          ),
          center = center,
          radius = radius * 1.15f
        ),
        radius = radius * 1.15f,
        center = center
      )

      // 2. Animated acoustic radiation wave ring when active
      if (isListening || isSpeaking || audioLevel > 0.05f) {
        val waveAlpha = (1f - waveRadius).coerceIn(0f, 1f) * 0.6f
        drawCircle(
          color = JarvisCyanBright.copy(alpha = waveAlpha),
          radius = radius * waveRadius,
          center = center,
          style = Stroke(width = 2.dp.toPx())
        )
      }

      // 3. Outermost segmented tick ring (Outer HUD bracket)
      rotate(outerRotation, pivot = center) {
        drawCircle(
          color = JarvisCyan.copy(alpha = 0.4f),
          radius = radius,
          center = center,
          style = Stroke(
            width = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 18f), 0f)
          )
        )

        // 4 Segmented Accent Arcs on outer ring
        val arcStroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        for (i in 0 until 4) {
          val startAngle = i * 90f + 15f
          drawArc(
            color = JarvisCyanBright,
            startAngle = startAngle,
            sweepAngle = 45f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = arcStroke
          )
        }
      }

      // 4. Intermediate Technical HUD Ring (Rotating opposite)
      val midRadius = radius * 0.78f
      rotate(innerRotation, pivot = center) {
        drawCircle(
          color = JarvisAqua.copy(alpha = 0.6f),
          radius = midRadius,
          center = center,
          style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f), 0f)
          )
        )

        // Triple heavy telemetry brackets
        val bracketStroke = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Square)
        for (i in 0 until 3) {
          val startAngle = i * 120f + 20f
          drawArc(
            color = JarvisCyan,
            startAngle = startAngle,
            sweepAngle = 35f,
            useCenter = false,
            topLeft = Offset(center.x - midRadius, center.y - midRadius),
            size = Size(midRadius * 2, midRadius * 2),
            style = bracketStroke
          )
        }
      }

      // 5. Radial Audio Ticks radiating from the inner core
      val coreRadius = radius * 0.48f * pulseScale
      val tickCount = 28
      val dynamicGain = (audioLevel * 16f).coerceAtLeast(0f)
      for (i in 0 until tickCount) {
        val angleRad = Math.toRadians((i * (360f / tickCount)).toDouble())
        val baseStart = coreRadius + 4.dp.toPx()
        val tickLength = 7.dp.toPx() + (if (i % 2 == 0) dynamicGain else dynamicGain * 0.5f)
        val p1 = Offset(
          center.x + (baseStart * cos(angleRad)).toFloat(),
          center.y + (baseStart * sin(angleRad)).toFloat()
        )
        val p2 = Offset(
          center.x + ((baseStart + tickLength) * cos(angleRad)).toFloat(),
          center.y + ((baseStart + tickLength) * sin(angleRad)).toFloat()
        )
        val tickAlpha = if (isSpeaking || isListening) 0.85f else 0.45f
        drawLine(
          color = if (i % 4 == 0) JarvisCyanBright else JarvisAqua.copy(alpha = tickAlpha),
          start = p1,
          end = p2,
          strokeWidth = 2.dp.toPx(),
          cap = StrokeCap.Round
        )
      }

      // 6. Glowing Arc Reactor Center Core (Pulsing Orb)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            JarvisCyanBright.copy(alpha = if (isSpeaking) 0.95f else 0.85f),
            JarvisCyan.copy(alpha = 0.7f),
            JarvisAqua.copy(alpha = 0.35f),
            Color.Transparent
          ),
          center = center,
          radius = coreRadius
        ),
        radius = coreRadius,
        center = center
      )

      // 7. Sharp Inner Core Ring
      drawCircle(
        color = JarvisCyanBright,
        radius = coreRadius * 0.72f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
      )

      // Center Bright Spark
      drawCircle(
        color = Color.White,
        radius = (coreRadius * 0.28f) * (if (isSpeaking) 1.2f else 1.0f),
        center = center
      )
    }

    // Overlay Status Text in center
    val statusText = when {
      isSpeaking -> "TRANSMITTING"
      isListening -> "LISTENING"
      audioLevel > 0.1f -> "ACTIVE"
      else -> "ONLINE"
    }

    Text(
      text = statusText,
      color = JarvisCyanBright,
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      letterSpacing = 1.5.sp,
      modifier = Modifier.align(Alignment.BottomCenter)
    )
  }
}
