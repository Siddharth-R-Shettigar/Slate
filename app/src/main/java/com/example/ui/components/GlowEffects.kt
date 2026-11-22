package com.example.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Radiates a localized glow centered at the exact active touch/drag coordinates,
 * strictly clipped inside the host container's bounds. Tracks finger movement while
 * pressed and smoothly blooms + fades on release with zero recompositions.
 */
fun Modifier.containedTapGlow(
  glowColor: Color,
  maxAlpha: Float = 0.56f,
): Modifier = composed {
  val scope = rememberCoroutineScope()
  val rippleProgress = remember { Animatable(1f) }
  val holdAlpha = remember { Animatable(0f) }
  var touchCenter by remember { mutableStateOf(Offset.Zero) }

  this
    .pointerInput(Unit) {
      awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        touchCenter = down.position
        scope.launch {
          rippleProgress.snapTo(0.18f)
          holdAlpha.snapTo(0.45f)
          holdAlpha.animateTo(1f, tween(durationMillis = 150, easing = FastOutSlowInEasing))
        }
        while (true) {
          val event = awaitPointerEvent(PointerEventPass.Initial)
          val change = event.changes.firstOrNull { it.id == down.id } ?: break
          touchCenter = change.position
          if (!change.pressed) break
        }
        scope.launch {
          holdAlpha.animateTo(0f, tween(durationMillis = 280, easing = FastOutSlowInEasing))
        }
        scope.launch {
          rippleProgress.animateTo(1f, tween(durationMillis = 360, easing = FastOutSlowInEasing))
        }
      }
    }
    .drawWithContent {
      drawContent()
      val t = rippleProgress.value
      val hAlpha = holdAlpha.value
      val combinedAlpha = maxOf(
        maxAlpha * hAlpha * 0.88f,
        if (t < 0.99f) maxAlpha * (1f - t) else 0f,
      )
      if (combinedAlpha > 0.006f) {
        val maxRadius = hypot(size.width, size.height) * 0.72f
        val radius = (38f + maxRadius * (0.28f + 0.72f * t)).coerceAtLeast(1f)
        drawCircle(
          brush = Brush.radialGradient(
            colorStops = arrayOf(
              0.0f to Color.White.copy(alpha = (combinedAlpha * 0.52f).coerceIn(0f, 1f)),
              0.24f to glowColor.copy(alpha = combinedAlpha.coerceIn(0f, 1f)),
              0.62f to glowColor.copy(alpha = (combinedAlpha * 0.40f).coerceIn(0f, 1f)),
              1.0f to Color.Transparent,
            ),
            center = touchCenter,
            radius = radius,
          ),
          radius = radius,
          center = touchCenter,
        )
      }
    }
}

/**
 * Adds a tactile sticky/squishy press scale read strictly inside graphicsLayer
 * (zero recompositions during press or release spring-back).
 */
fun Modifier.stickyPressScale(
  pressedScale: Float = 0.975f,
): Modifier = composed {
  val scope = rememberCoroutineScope()
  val scaleAnim = remember { Animatable(1f) }

  this
    .pointerInput(pressedScale) {
      awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        scope.launch {
          scaleAnim.animateTo(
            targetValue = pressedScale,
            animationSpec = spring(
              dampingRatio = Spring.DampingRatioNoBouncy,
              stiffness = Spring.StiffnessHigh,
            ),
          )
        }
        while (true) {
          val event = awaitPointerEvent(PointerEventPass.Initial)
          val change = event.changes.firstOrNull { it.id == down.id } ?: break
          if (!change.pressed) break
        }
        scope.launch {
          scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
              dampingRatio = Spring.DampingRatioMediumBouncy,
              stiffness = Spring.StiffnessMedium,
            ),
          )
        }
      }
    }
    .graphicsLayer {
      val s = scaleAnim.value
      scaleX = s
      scaleY = s
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

enum class TaskPerimeterGlowKind {
  COMPLETED,
  DELETED,
  ADDED,
}

/**
 * One-shot Material 3 Expressive perimeter glow sweep triggered strictly on discrete task events
 * (completion, deletion, creation). Runs once in drawWithContent and exits early with zero
 * work when idle so mid-range GPUs stay locked at 90Hz.
 */
@Composable
fun Modifier.taskEventPerimeterGlow(
  activeKind: TaskPerimeterGlowKind?,
  triggerToken: Int,
  glowColors: List<Color>,
  cornerRadius: Dp = 20.dp,
  strokeWidth: Dp = 2.1.dp,
): Modifier {
  val sweepProgress = remember { Animatable(1f) }

  LaunchedEffect(triggerToken) {
    if (triggerToken > 0 && activeKind != null) {
      val duration = when (activeKind) {
        TaskPerimeterGlowKind.COMPLETED -> 450
        TaskPerimeterGlowKind.DELETED -> 300
        TaskPerimeterGlowKind.ADDED -> 500
      }
      sweepProgress.snapTo(0f)
      sweepProgress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing),
      )
    }
  }

  val matrix = remember { Matrix() }
  val haloPaint = remember {
    android.graphics.Paint().apply {
      isAntiAlias = true
      style = android.graphics.Paint.Style.STROKE
    }
  }
  val shaderStops = remember { floatArrayOf(0f, 0.14f, 0.28f, 0.36f, 0.44f, 0.58f, 0.72f, 1f) }
  val c0 = glowColors.getOrElse(0) { Color.White }
  val c1 = glowColors.getOrElse(1) { c0 }
  val c2 = glowColors.getOrElse(2) { c0 }
  val baseColorInts = remember(c0, c1, c2) {
    intArrayOf(
      c0.copy(alpha = 0.08f).toArgb(),
      c0.copy(alpha = 0.85f).toArgb(),
      c1.copy(alpha = 0.98f).toArgb(),
      Color.White.copy(alpha = 0.88f).toArgb(),
      c2.copy(alpha = 0.98f).toArgb(),
      c1.copy(alpha = 0.82f).toArgb(),
      c0.copy(alpha = 0.08f).toArgb(),
      c0.copy(alpha = 0.08f).toArgb(),
    )
  }

  var cachedW by remember { mutableStateOf(-1f) }
  var cachedH by remember { mutableStateOf(-1f) }
  var cachedKind by remember { mutableStateOf<TaskPerimeterGlowKind?>(null) }
  var cachedColors by remember { mutableStateOf<IntArray?>(null) }
  var cachedShader by remember { mutableStateOf<SweepGradient?>(null) }
  var cachedBrush by remember { mutableStateOf<ShaderBrush?>(null) }

  return this.drawWithContent {
    drawContent()

    val p = sweepProgress.value
    if (p >= 0.999f || activeKind == null) return@drawWithContent

    val envelope = when {
      p < 0.14f -> (p / 0.14f)
      p > 0.66f -> ((1f - p) / 0.34f).coerceIn(0f, 1f)
      else -> 1f
    } * when (activeKind) {
      TaskPerimeterGlowKind.ADDED -> 0.85f
      TaskPerimeterGlowKind.COMPLETED -> 0.95f
      TaskPerimeterGlowKind.DELETED -> 0.92f
    }
    if (envelope <= 0.005f) return@drawWithContent

    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@drawWithContent

    val cx = w / 2f
    val cy = h / 2f
    val cornerPx = cornerRadius.toPx()
    val strokePx = strokeWidth.toPx()

    if (cachedShader == null || cachedW != w || cachedH != h || cachedKind != activeKind || cachedColors !== baseColorInts) {
      val s = SweepGradient(cx, cy, baseColorInts, shaderStops)
      cachedShader = s
      cachedBrush = ShaderBrush(s)
      cachedW = w
      cachedH = h
      cachedKind = activeKind
      cachedColors = baseColorInts
      haloPaint.strokeWidth = strokePx * 2.8f
      haloPaint.maskFilter = BlurMaskFilter(6.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
    }

    val sweepShader = cachedShader ?: return@drawWithContent
    val brush = cachedBrush ?: return@drawWithContent

    val angle = -90f + 360f * p
    matrix.reset()
    matrix.postRotate(angle, cx, cy)
    sweepShader.setLocalMatrix(matrix)

    drawIntoCanvas { canvas ->
      haloPaint.shader = sweepShader
      haloPaint.alpha = (165f * envelope).toInt().coerceIn(0, 255)
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
