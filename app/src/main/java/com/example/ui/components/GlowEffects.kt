package com.example.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Radiates a bright, localized glow centered at the exact tap coordinates,
 * strictly clipped inside the host container's bounds. Uses a single Animatable
 * and a single coroutine per tap for minimal overhead on mid-range hardware.
 */
fun Modifier.containedTapGlow(
  glowColor: Color,
  maxAlpha: Float = 0.60f,
): Modifier = composed {
  val scope = rememberCoroutineScope()
  val anim = remember { Animatable(1f) }
  var tapCenter by remember { mutableStateOf(Offset.Zero) }

  this
    .pointerInput(Unit) {
      awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        tapCenter = down.position
        scope.launch {
          anim.snapTo(0f)
          anim.animateTo(1f, tween(durationMillis = 380, easing = FastOutSlowInEasing))
        }
      }
    }
    .drawWithContent {
      drawContent()
      val t = anim.value
      if (t < 0.99f) {
        val alpha = maxAlpha * (1f - t)
        val maxRadius = hypot(size.width, size.height) * 0.78f
        val radius = (24f + maxRadius * t).coerceAtLeast(1f)
        drawCircle(
          brush = Brush.radialGradient(
            colorStops = arrayOf(
              0.0f to Color.White.copy(alpha = (alpha * 0.55f).coerceIn(0f, 1f)),
              0.25f to glowColor.copy(alpha = alpha.coerceIn(0f, 1f)),
              0.62f to glowColor.copy(alpha = (alpha * 0.45f).coerceIn(0f, 1f)),
              1.0f to Color.Transparent,
            ),
            center = tapCenter,
            radius = radius,
          ),
          radius = radius,
          center = tapCenter,
        )
      }
    }
}

/**
 * Google AI Mode-inspired border light effect: performs ONE luminous multi-color sweep
 * around the stationary pill border when toggled ON, then smoothly fades out and stops
 * completely so zero GPU/CPU work runs while idle.
 */
@Composable
fun Modifier.aiModeGlowPillBorder(
  colors: List<Color>,
  cornerRadius: Dp = 32.dp,
  strokeWidth: Dp = 2.6.dp,
): Modifier {
  val sweepProgress = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    sweepProgress.snapTo(0f)
    sweepProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 1150, easing = FastOutSlowInEasing),
    )
  }

  val matrix = remember { Matrix() }
  val haloPaint = remember {
    android.graphics.Paint().apply {
      isAntiAlias = true
      style = android.graphics.Paint.Style.STROKE
    }
  }
  val shaderStops = remember { floatArrayOf(0f, 0.12f, 0.24f, 0.30f, 0.36f, 0.48f, 0.62f, 1f) }
  val baseColorInts = remember(colors) {
    val c0 = colors[0]
    val c1 = colors.getOrElse(1) { c0 }
    val c2 = colors.getOrElse(2) { c0 }
    val c3 = colors.getOrElse(3) { c0 }
    intArrayOf(
      c0.copy(alpha = 0.12f).toArgb(),
      c0.copy(alpha = 0.90f).toArgb(),
      c1.copy(alpha = 1.0f).toArgb(),
      Color.White.copy(alpha = 0.95f).toArgb(),
      c2.copy(alpha = 1.0f).toArgb(),
      c3.copy(alpha = 0.90f).toArgb(),
      c0.copy(alpha = 0.12f).toArgb(),
      c0.copy(alpha = 0.12f).toArgb(),
    )
  }

  // Cache SweepGradient + ShaderBrush per pill size
  var cachedW by remember { mutableStateOf(-1f) }
  var cachedH by remember { mutableStateOf(-1f) }
  var cachedShader by remember { mutableStateOf<SweepGradient?>(null) }
  var cachedBrush by remember { mutableStateOf<ShaderBrush?>(null) }

  return this.drawWithContent {
    drawContent()

    val p = sweepProgress.value
    if (p >= 0.999f) return@drawWithContent

    // Smooth fade-in at 0..0.12 and smooth fade-out at 0.72..1.0
    val envelope = when {
      p < 0.12f -> (p / 0.12f)
      p > 0.72f -> ((1f - p) / 0.28f).coerceIn(0f, 1f)
      else -> 1f
    }
    if (envelope <= 0.005f) return@drawWithContent

    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@drawWithContent

    val cx = w / 2f
    val cy = h / 2f
    val cornerPx = cornerRadius.toPx()
    val strokePx = strokeWidth.toPx()

    if (cachedShader == null || cachedW != w || cachedH != h) {
      val s = SweepGradient(cx, cy, baseColorInts, shaderStops)
      cachedShader = s
      cachedBrush = ShaderBrush(s)
      cachedW = w
      cachedH = h
      haloPaint.strokeWidth = strokePx * 3.2f
      haloPaint.maskFilter = BlurMaskFilter(8.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
    }

    val sweepShader = cachedShader ?: return@drawWithContent
    val brush = cachedBrush ?: return@drawWithContent

    val angle = -90f + 380f * p
    matrix.reset()
    matrix.postRotate(angle, cx, cy)
    sweepShader.setLocalMatrix(matrix)

    // 1. Soft blurred luminous halo
    drawIntoCanvas { canvas ->
      haloPaint.shader = sweepShader
      haloPaint.alpha = (205f * envelope).toInt().coerceIn(0, 255)
      val inset = strokePx * 0.5f
      canvas.nativeCanvas.drawRoundRect(
        inset,
        inset,
        w - inset,
        h - inset,
        cornerPx,
        cornerPx,
        haloPaint,
      )
    }

    // 2. Crisp luminous border light sweep
    val inset = strokePx / 2f
    drawRoundRect(
      brush = brush,
      topLeft = Offset(inset, inset),
      size = Size(w - strokePx, h - strokePx),
      cornerRadius = CornerRadius(cornerPx, cornerPx),
      alpha = envelope,
      style = Stroke(width = strokePx),
    )
  }
}
