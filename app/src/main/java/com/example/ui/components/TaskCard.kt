package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.local.SlateTaskEntity
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val EmeraldGlow = Color(0xFF34A853)
private val CoralGlow = Color(0xFFFF7A6B)

@Composable
fun InteractiveTaskCard(
  task: SlateTaskEntity,
  accentColor: Color,
  isDragging: Boolean,
  isHighlighted: Boolean = false,
  isNewlyAdded: Boolean = false,
  isPendingDelete: Boolean = false,
  minCardHeight: Dp = 64.dp,
  cardVerticalPadding: Dp = 14.dp,
  dragOffsetYProvider: () -> Float,
  onToggleComplete: () -> Unit,
  onDelete: () -> Unit,
  onOpenContextMenu: (cardTopInRootY: Float) -> Unit,
  onStartDragReorder: () -> Unit,
  onDragReorderDelta: (Float) -> Unit,
  onEndDragReorder: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current
  val latestOnDelete by rememberUpdatedState(onDelete)

  val swipeOffset = remember { Animatable(0f) }
  var cardTopInRoot by remember { mutableFloatStateOf(0f) }
  var isLocalDeleting by remember(task.id) { mutableStateOf(false) }
  val isDeleting = isLocalDeleting || isPendingDelete

  LaunchedEffect(isDeleting) {
    if (isDeleting) {
      delay(305L)
      latestOnDelete()
    }
  }

  val swipeThresholdPx = with(density) { 88.dp.toPx() }
  val reorderSlopPx = with(density) { 12.dp.toPx() }

  val dragScaleState = animateFloatAsState(
    targetValue = when {
      isDragging -> 1.035f
      isHighlighted -> 1.025f
      else -> 1f
    },
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium,
    ),
    label = "dragScale",
  )

  val isSwipeVisible by remember(isDragging) {
    derivedStateOf { !isDragging && !isDeleting && abs(swipeOffset.value) > 2f }
  }
  val isSwipingRight by remember {
    derivedStateOf { swipeOffset.value >= 0f }
  }
  val isSwipeThresholdMet by remember(swipeThresholdPx) {
    derivedStateOf { abs(swipeOffset.value) >= swipeThresholdPx }
  }

  // Haptic tick once when swipe crosses threshold
  var wasThresholdMet by remember { mutableStateOf(false) }
  LaunchedEffect(isSwipeThresholdMet) {
    if (isSwipeThresholdMet && !wasThresholdMet) {
      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    wasThresholdMet = isSwipeThresholdMet
  }

  AnimatedVisibility(
    visible = !isDeleting,
    exit = shrinkVertically(
      animationSpec = tween(durationMillis = 290, easing = FastOutSlowInEasing),
    ) + fadeOut(
      animationSpec = tween(durationMillis = 270, easing = FastOutSlowInEasing),
    ),
    modifier = modifier
      .fillMaxWidth()
      .zIndex(if (isDragging || isHighlighted) 10f else 0f),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .onGloballyPositioned { coords ->
          cardTopInRoot = coords.positionInRoot().y
        }
        .graphicsLayer {
          translationY = dragOffsetYProvider()
          val swipeRatio = (abs(swipeOffset.value) / swipeThresholdPx).coerceIn(0f, 1.3f)
          val stretchX = 1f + 0.018f * minOf(swipeRatio, 1f)
          val squashY = 1f - 0.012f * minOf(swipeRatio, 1f)
          val s = dragScaleState.value
          scaleX = s * stretchX
          scaleY = s * squashY
        },
    ) {
      // Swipe action backdrop revealed underneath card (visibility gated via derivedStateOf!)
      if (isSwipeVisible) {
        val bgColorState = animateColorAsState(
          targetValue = if (isSwipingRight) {
            MaterialTheme.colorScheme.primaryContainer
          } else {
            MaterialTheme.colorScheme.errorContainer
          },
          label = "swipeBgColor",
        )
        val iconScaleState = animateFloatAsState(
          targetValue = if (isSwipeThresholdMet) 1.25f else 1f,
          animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
          label = "swipeIconScale",
        )

        Box(
          modifier = Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(20.dp))
            .drawBehind { drawRect(bgColorState.value) }
            .padding(horizontal = 22.dp),
          contentAlignment = if (isSwipingRight) Alignment.CenterStart else Alignment.CenterEnd,
        ) {
          Icon(
            imageVector = if (isSwipingRight) Icons.Rounded.Check else SlateIcons.DeleteOutline,
            contentDescription = if (isSwipingRight) "Complete task" else "Delete task",
            tint = if (isSwipingRight) {
              MaterialTheme.colorScheme.onPrimaryContainer
            } else {
              MaterialTheme.colorScheme.onErrorContainer
            },
            modifier = Modifier
              .size(26.dp)
              .graphicsLayer {
                val progress = (abs(swipeOffset.value) / swipeThresholdPx).coerceIn(0f, 1f)
                val s = (0.75f + 0.35f * progress) * iconScaleState.value
                scaleX = s
                scaleY = s
              },
          )
        }
      }

      // Foreground draggable/swipeable card
      TaskCardSurface(
        task = task,
        accentColor = accentColor,
        tonalElevation = if (isDragging || isHighlighted) 10.dp else 1.dp,
        isHighlighted = isDragging || isHighlighted,
        isNewlyAdded = isNewlyAdded,
        isDeleting = isDeleting,
        minHeight = minCardHeight,
        verticalPadding = cardVerticalPadding,
        onToggleComplete = onToggleComplete,
        modifier = Modifier
          .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }
          .clip(RoundedCornerShape(20.dp))
          .containedTapGlow(accentColor, maxAlpha = 0.42f)
          .pointerInput(task.id) {
            val touchSlop = viewConfiguration.touchSlop
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false)
              var mode = GestureMode.PENDING
              var totalDx = 0f
              var totalDy = 0f

              // Race 360ms hold against movement exceeding touchSlop
              try {
                withTimeout(360L) {
                  while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                      mode = GestureMode.ENDED
                      break
                    }
                    val delta = change.positionChange()
                    totalDx += delta.x
                    totalDy += delta.y

                    if (abs(totalDy) > touchSlop && abs(totalDy) > abs(totalDx)) {
                      mode = GestureMode.SCROLL_YIELD
                      break
                    }
                    if (abs(totalDx) > touchSlop && abs(totalDx) > abs(totalDy)) {
                      mode = GestureMode.SWIPE_HORIZONTAL
                      change.consume()
                      scope.launch { swipeOffset.snapTo(totalDx) }
                      break
                    }
                  }
                }
              } catch (_: TimeoutCancellationException) {
                mode = GestureMode.LONG_PRESS_HOLD
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onOpenContextMenu(cardTopInRoot)
              }

              if (mode == GestureMode.SWIPE_HORIZONTAL) {
                while (true) {
                  val event = awaitPointerEvent(PointerEventPass.Main)
                  val change = event.changes.firstOrNull { it.id == down.id } ?: break
                  if (!change.pressed) break
                  val dx = change.positionChange().x
                  change.consume()
                  scope.launch {
                    val cur = swipeOffset.value
                    val dampedDx = if (abs(cur) > swipeThresholdPx) dx * 0.45f else dx
                    swipeOffset.snapTo((cur + dampedDx).coerceIn(-460f, 460f))
                  }
                }
                val finalOffset = swipeOffset.value
                if (finalOffset >= swipeThresholdPx) {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  onToggleComplete()
                  scope.launch {
                    swipeOffset.animateTo(
                      targetValue = 0f,
                      animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                      ),
                    )
                  }
                } else if (finalOffset <= -swipeThresholdPx) {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  isLocalDeleting = true
                  scope.launch {
                    swipeOffset.animateTo(
                      targetValue = -110f,
                      animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    )
                  }
                } else {
                  scope.launch {
                    swipeOffset.animateTo(
                      targetValue = 0f,
                      animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                      ),
                    )
                  }
                }
              } else if (mode == GestureMode.LONG_PRESS_HOLD) {
                var reorderAccY = 0f
                var reordering = false
                while (true) {
                  val event = awaitPointerEvent(PointerEventPass.Main)
                  val change = event.changes.firstOrNull { it.id == down.id } ?: break
                  if (!change.pressed) {
                    if (reordering) {
                      onEndDragReorder()
                    }
                    break
                  }
                  val dy = change.positionChange().y
                  if (!reordering) {
                    reorderAccY += dy
                    if (abs(reorderAccY) > reorderSlopPx) {
                      reordering = true
                      change.consume()
                      onStartDragReorder()
                      onDragReorderDelta(reorderAccY)
                    }
                  } else {
                    change.consume()
                    onDragReorderDelta(dy)
                  }
                }
              }
            }
          },
      )
    }
  }
}

private enum class GestureMode {
  PENDING,
  SCROLL_YIELD,
  SWIPE_HORIZONTAL,
  LONG_PRESS_HOLD,
  ENDED,
}

@Composable
fun TaskCardSurface(
  task: SlateTaskEntity,
  accentColor: Color,
  tonalElevation: Dp = 1.dp,
  isHighlighted: Boolean = false,
  isNewlyAdded: Boolean = false,
  isDeleting: Boolean = false,
  minHeight: Dp = 64.dp,
  verticalPadding: Dp = 14.dp,
  showDragGrip: Boolean = false,
  onToggleComplete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var glowSeq by remember(task.id) { mutableIntStateOf(0) }
  var activeGlowKind by remember(task.id) { mutableStateOf<TaskPerimeterGlowKind?>(null) }
  var prevCompleted by remember(task.id) { mutableStateOf(task.isCompleted) }
  var prevNewlyAdded by remember(task.id) { mutableStateOf(false) }
  var prevDeleting by remember(task.id) { mutableStateOf(false) }

  LaunchedEffect(isNewlyAdded) {
    if (isNewlyAdded && !prevNewlyAdded) {
      activeGlowKind = TaskPerimeterGlowKind.ADDED
      glowSeq++
    }
    prevNewlyAdded = isNewlyAdded
  }

  LaunchedEffect(task.isCompleted) {
    if (task.isCompleted && !prevCompleted) {
      activeGlowKind = TaskPerimeterGlowKind.COMPLETED
      glowSeq++
    }
    prevCompleted = task.isCompleted
  }

  LaunchedEffect(isDeleting) {
    if (isDeleting && !prevDeleting) {
      activeGlowKind = TaskPerimeterGlowKind.DELETED
      glowSeq++
    }
    prevDeleting = isDeleting
  }

  val glowColors = when (activeGlowKind) {
    TaskPerimeterGlowKind.COMPLETED -> listOf(
      MaterialTheme.colorScheme.primary,
      EmeraldGlow,
      accentColor,
    )
    TaskPerimeterGlowKind.DELETED -> listOf(
      MaterialTheme.colorScheme.error,
      CoralGlow,
      MaterialTheme.colorScheme.errorContainer,
    )
    TaskPerimeterGlowKind.ADDED, null -> listOf(
      accentColor,
      MaterialTheme.colorScheme.primary,
      MaterialTheme.colorScheme.tertiary,
    )
  }

  val containerColorState = animateColorAsState(
    targetValue = when {
      isDeleting -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f)
      task.isCompleted -> MaterialTheme.colorScheme.surfaceContainerLow
      task.isStarred -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
      else -> MaterialTheme.colorScheme.surfaceContainer
    },
    animationSpec = tween(durationMillis = 180),
    label = "taskCardColor",
  )

  val checkScaleState = animateFloatAsState(
    targetValue = if (task.isCompleted) 1f else 0.9f,
    animationSpec = spring(
      dampingRatio = 0.72f,
      stiffness = Spring.StiffnessMedium,
    ),
    label = "checkScale",
  )

  val contentAlphaState = animateFloatAsState(
    targetValue = if (task.isCompleted) 0.54f else 1f,
    animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
    label = "taskContentAlpha",
  )

  val strikeProgress = remember(task.id) {
    Animatable(if (task.isCompleted) 1f else 0f)
  }
  LaunchedEffect(task.isCompleted) {
    strikeProgress.animateTo(
      targetValue = if (task.isCompleted) 1f else 0f,
      animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
    )
  }
  var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

  val borderStroke = when {
    isHighlighted -> BorderStroke(1.8.dp, accentColor.copy(alpha = 0.92f))
    isDeleting -> BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f))
    task.isStarred -> BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.42f))
    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = minHeight)
      .clip(RoundedCornerShape(20.dp))
      .drawBehind { drawRect(containerColorState.value) }
      .taskEventPerimeterGlow(
        activeKind = activeGlowKind,
        triggerToken = glowSeq,
        glowColors = glowColors,
        cornerRadius = 20.dp,
        strokeWidth = 2.1.dp,
      )
      .testTag("task_card_${task.id}"),
    shape = RoundedCornerShape(20.dp),
    color = Color.Transparent,
    border = borderStroke,
    tonalElevation = tonalElevation,
    shadowElevation = if (isHighlighted || tonalElevation > 2.dp) 12.dp else 0.dp,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = verticalPadding),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Tactile completion circle button (48dp touch target)
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .clickable(onClick = onToggleComplete)
          .testTag("task_check_${task.id}"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier = Modifier
            .size(24.dp)
            .graphicsLayer {
              val s = checkScaleState.value
              scaleX = s
              scaleY = s
            }
            .clip(CircleShape)
            .background(
              if (task.isCompleted) accentColor
              else MaterialTheme.colorScheme.surfaceVariant
            ),
          contentAlignment = Alignment.Center,
        ) {
          this@Row.AnimatedVisibility(
            visible = task.isCompleted,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = scaleOut(),
          ) {
            Icon(
              imageVector = Icons.Rounded.Check,
              contentDescription = "Completed",
              tint = Color.White,
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Column(
        modifier = Modifier
          .weight(1f)
          .graphicsLayer { alpha = contentAlphaState.value },
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        val strikeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
        Text(
          text = task.title,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          onTextLayout = { textLayoutResult = it },
          modifier = Modifier.drawWithContent {
            drawContent()
            val progress = strikeProgress.value
            val layout = textLayoutResult
            if (progress > 0.01f && layout != null) {
              val strokeW = 1.8.dp.toPx()
              for (line in 0 until layout.lineCount) {
                val left = layout.getLineLeft(line)
                val right = layout.getLineRight(line)
                val lineW = right - left
                if (lineW > 0f) {
                  val y = (layout.getLineTop(line) + layout.getLineBottom(line)) * 0.52f
                  drawLine(
                    color = strikeColor,
                    start = Offset(left, y),
                    end = Offset(left + lineW * progress, y),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round,
                  )
                }
              }
            }
          },
        )

        if (task.scheduledStartMillis != null) {
          val badgeLabel = remember(task.scheduledStartMillis) {
            formatScheduledBadge(task.scheduledStartMillis)
          }
          val isCalendarSynced = task.calendarEventId != null
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = if (isCalendarSynced) SlateIcons.EventAvailable else SlateIcons.Schedule,
              contentDescription = if (isCalendarSynced) "Synced to Calendar" else "Scheduled time",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = badgeLabel,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }

      if (task.isStarred) {
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.Rounded.Star,
          contentDescription = "Priority starred",
          tint = MaterialTheme.colorScheme.tertiary,
          modifier = Modifier
            .size(22.dp)
            .testTag("task_star_badge_${task.id}"),
        )
      }

      if (showDragGrip) {
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          imageVector = SlateIcons.DragIndicator,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

private val sameDayFormatTL = ThreadLocal.withInitial {
  SimpleDateFormat("h:mm a", Locale.getDefault())
}
private val fullDayFormatTL = ThreadLocal.withInitial {
  SimpleDateFormat("MMM d • h:mm a", Locale.getDefault())
}

fun formatScheduledBadge(epochMillis: Long): String {
  val cal = Calendar.getInstance()
  val nowYear = cal.get(Calendar.YEAR)
  val nowDoy = cal.get(Calendar.DAY_OF_YEAR)
  cal.timeInMillis = epochMillis
  val isSameDay = nowYear == cal.get(Calendar.YEAR) && nowDoy == cal.get(Calendar.DAY_OF_YEAR)
  val fmt = if (isSameDay) sameDayFormatTL.get() else fullDayFormatTL.get()
  return fmt?.format(Date(epochMillis)) ?: ""
}
