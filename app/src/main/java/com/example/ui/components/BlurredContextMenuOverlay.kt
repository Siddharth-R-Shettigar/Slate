package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SlateTaskEntity
import kotlin.math.roundToInt

@Composable
fun BlurredContextMenuOverlay(
  task: SlateTaskEntity,
  accentColor: Color,
  cardTopInRootY: Float,
  onDismiss: () -> Unit,
  onToggleStar: () -> Unit,
  onAssignDateTime: () -> Unit,
  onMoveToList: () -> Unit,
  onDelete: () -> Unit,
) {
  val density = LocalDensity.current
  val openProgress = remember(task.id) { Animatable(0f) }

  LaunchedEffect(task.id) {
    openProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
    )
  }

  val menuSlideMaxPx = with(density) { (-10).dp.toPx() }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .drawBehind {
        drawRect(Color.Black.copy(alpha = 0.42f * openProgress.value))
      }
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onDismiss,
      )
      .testTag("blurred_context_menu_backdrop"),
  ) {
    val maxHeightPx = with(density) { maxHeight.toPx() }
    val menuHeightEstimatePx = with(density) { 120.dp.toPx() }
    val cardHeightEstimatePx = with(density) { 76.dp.toPx() }
    val topSafePx = with(density) { 80.dp.toPx() }
    val bottomSafePx = maxHeightPx - cardHeightEstimatePx - menuHeightEstimatePx - with(density) { 48.dp.toPx() }

    val clampedCardTopPx = cardTopInRootY
      .coerceIn(topSafePx, maxOf(topSafePx, bottomSafePx))
      .roundToInt()

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .offset { IntOffset(0, clampedCardTopPx) },
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Selected card smoothly elevates in place from 1.00f -> 1.03f with zero double-bounce
      TaskCardSurface(
        task = task,
        accentColor = accentColor,
        tonalElevation = 12.dp,
        onToggleComplete = {},
        modifier = Modifier
          .widthIn(max = 560.dp)
          .graphicsLayer {
            val p = openProgress.value
            val s = 1f + 0.03f * p
            scaleX = s
            scaleY = s
          }
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
          ),
      )

      // Custom Material 3 Expressive Floating Pill Menu glides smoothly out from under card in one motion
      Surface(
        modifier = Modifier
          .widthIn(max = 520.dp)
          .graphicsLayer {
            val p = openProgress.value
            val s = 0.94f + 0.06f * p
            scaleX = s
            scaleY = s
            translationY = menuSlideMaxPx * (1f - p)
            alpha = p
          }
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
          )
          .testTag("expressive_context_pill_menu"),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 10.dp,
        shadowElevation = 14.dp,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          PillMenuAction(
            icon = if (task.isStarred) Icons.Rounded.Star else SlateIcons.StarOutline,
            label = if (task.isStarred) "Unstar" else "Star",
            contentDescription = stringResource(R.string.cd_star_item),
            tint = if (task.isStarred) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            containerColor = if (task.isStarred) {
              MaterialTheme.colorScheme.tertiaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceContainerHighest
            },
            testTag = "menu_action_star",
            onClick = {
              onToggleStar()
              onDismiss()
            },
          )
          PillMenuAction(
            icon = SlateIcons.Event,
            label = "Schedule",
            contentDescription = stringResource(R.string.cd_schedule_item),
            tint = MaterialTheme.colorScheme.primary,
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            testTag = "menu_action_schedule",
            onClick = onAssignDateTime,
          )
          PillMenuAction(
            icon = SlateIcons.DriveFileMove,
            label = "Move",
            contentDescription = stringResource(R.string.cd_move_item),
            tint = MaterialTheme.colorScheme.onSurface,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            testTag = "menu_action_move",
            onClick = onMoveToList,
          )
          PillMenuAction(
            icon = SlateIcons.DeleteOutline,
            label = "Delete",
            contentDescription = stringResource(R.string.cd_delete_item),
            tint = MaterialTheme.colorScheme.error,
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            testTag = "menu_action_delete",
            onClick = {
              onDelete()
              onDismiss()
            },
          )
        }
      }
    }
  }
}

@Composable
private fun PillMenuAction(
  icon: ImageVector,
  label: String,
  contentDescription: String,
  tint: Color,
  containerColor: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  Column(
    modifier = Modifier
      .defaultMinSize(minWidth = 68.dp, minHeight = 48.dp)
      .clip(RoundedCornerShape(22.dp))
      .containedTapGlow(tint)
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 6.dp)
      .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .clip(CircleShape)
        .background(containerColor),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(22.dp),
      )
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
