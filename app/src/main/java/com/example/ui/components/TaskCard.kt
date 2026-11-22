package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.local.SlateTaskEntity
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun InteractiveTaskCard(
  task: SlateTaskEntity,
  accentColor: Color,
  isDragging: Boolean,
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

  val swipeOffset = remember { Animatable(0f) }
  var cardTopInRoot by remember { mutableFloatStateOf(0f) }

  val swipeThresholdPx = with(density) { 88.dp.toPx() }
  val reorderSlopPx = with(density) { 12.dp.toPx() }

  val dragScaleState = animateFloatAsState(
    targetValue = if (isDragging) 1.03f else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium,
    ),
    label = "dragScale",
  )

  val isSwipeVisible by remember(isDragging) {
    derivedStateOf { !isDragging && abs(swipeOffset.value) > 2f }
  }
  val isSwipingRight by remember {
    derivedStateOf { swipeOffset.value >= 0f }
  }
  val isSwipeThresholdMet by remember(swipeThresholdPx) {
    derivedStateOf { abs(swipeOffset.value) >= swipeThresholdPx }
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .zIndex(if (isDragging) 10f else 0f)
      .onGloballyPositioned { coords ->
        cardTopInRoot = coords.positionInRoot().y
      }
      .graphicsLayer {
        translationY = dragOffsetYProvider()
        val s = dragScaleState.value
        scaleX = s
        scaleY = s
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
      tonalElevation = if (isDragging) 10.dp else 1.dp,
      onToggleComplete = onToggleComplete,
      modifier = Modifier
        .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }
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
                    // Vertical scroll -> let LazyColumn take over
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
              // Long-press fired!
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
                  swipeOffset.snapTo((swipeOffset.value + dx).coerceIn(-420f, 420f))
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
                scope.launch {
                  swipeOffset.animateTo(
                    targetValue = -1000f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                  )
                  onDelete()
                }
              } else {
                scope.launch {
                  swipeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
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
  tonalElevation: androidx.compose.ui.unit.Dp = 1.dp,
  showDragGrip: Boolean = false,
  onToggleComplete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val containerColorState = animateColorAsState(
    targetValue = when {
      task.isCompleted -> MaterialTheme.colorScheme.surfaceContainerLow
      task.isStarred -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
      else -> MaterialTheme.colorScheme.surfaceContainer
    },
    animationSpec = tween(durationMillis = 160),
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

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 64.dp)
      .clip(RoundedCornerShape(20.dp))
      .drawBehind { drawRect(containerColorState.value) }
      .testTag("task_card_${task.id}"),
    shape = RoundedCornerShape(20.dp),
    color = Color.Transparent,
    tonalElevation = tonalElevation,
    shadowElevation = if (tonalElevation > 2.dp) 10.dp else 0.dp,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
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
          .alpha(if (task.isCompleted) 0.52f else 1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = task.title,
          style = MaterialTheme.typography.bodyLarge.copy(
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
          ),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
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
