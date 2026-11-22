package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.calendar.DeviceCalendarEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class MeetingLinkInfo(
  val url: String,
  val providerLabel: String,
  val isVideoMeeting: Boolean,
)

private val urlRegex = Regex("""https?://[^\s<>"')\]]+""", RegexOption.IGNORE_CASE)

fun extractMeetingLink(description: String, location: String): MeetingLinkInfo? {
  val candidates = (urlRegex.findAll(location).map { it.value } +
    urlRegex.findAll(description).map { it.value })
    .map { it.trimEnd('.', ',', ';', ')') }
    .distinct()
    .toList()
  if (candidates.isEmpty()) return null

  val prioritized = candidates.firstOrNull { url ->
    val lower = url.lowercase(Locale.ROOT)
    lower.contains("meet.google.com") ||
      lower.contains("zoom.us") ||
      lower.contains("teams.microsoft.com") ||
      lower.contains("teams.live.com") ||
      lower.contains("webex.com")
  } ?: candidates.first()

  val lower = prioritized.lowercase(Locale.ROOT)
  val (label, isVideo) = when {
    lower.contains("meet.google.com") -> "Join Google Meet" to true
    lower.contains("zoom.us") -> "Join Zoom" to true
    lower.contains("teams.microsoft.com") || lower.contains("teams.live.com") -> "Join Teams" to true
    lower.contains("webex.com") -> "Join Webex" to true
    else -> "Open Link" to false
  }
  return MeetingLinkInfo(url = prioritized, providerLabel = label, isVideoMeeting = isVideo)
}

private val fullDateFormatterTL = ThreadLocal.withInitial {
  SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
}
private val timeOnlyFormatterTL = ThreadLocal.withInitial {
  SimpleDateFormat("h:mm a", Locale.getDefault())
}

@Composable
fun ExpandableCalendarEventCard(
  event: DeviceCalendarEvent,
  isExpanded: Boolean,
  isAlreadyInSlate: Boolean = false,
  minHeight: Dp = 64.dp,
  verticalPadding: Dp = 14.dp,
  onToggleExpand: () -> Unit,
  onAddToSlate: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uriHandler = LocalUriHandler.current
  val accentColor = remember(event.color) {
    if (event.color != 0) Color(event.color) else null
  } ?: MaterialTheme.colorScheme.tertiary

  val dateText = remember(event.startMillis) {
    fullDateFormatterTL.get()?.format(Date(event.startMillis)).orEmpty()
  }
  val timeText = remember(event.startMillis, event.endMillis, event.allDay) {
    if (event.allDay) {
      "All day"
    } else {
      val fmt = timeOnlyFormatterTL.get()
      "${fmt?.format(Date(event.startMillis)).orEmpty()} – ${fmt?.format(Date(event.endMillis)).orEmpty()}"
    }
  }
  val meetingLink = remember(event.description, event.location) {
    extractMeetingLink(event.description, event.location)
  }

  val chevronRotation = animateFloatAsState(
    targetValue = if (isExpanded) 180f else 0f,
    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
    label = "calEventChevron",
  )

  val borderColor = if (isExpanded) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
  } else {
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = minHeight)
      .clip(RoundedCornerShape(20.dp))
      .stickyPressScale(pressedScale = 0.985f)
      .containedTapGlow(accentColor, maxAlpha = 0.40f)
      .animateContentSize(
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioNoBouncy,
          stiffness = Spring.StiffnessMedium,
        )
      )
      .clickable(onClick = onToggleExpand)
      .testTag("calendar_event_card_${event.eventId}_${event.startMillis}"),
    shape = RoundedCornerShape(20.dp),
    color = if (isExpanded) {
      MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
      MaterialTheme.colorScheme.surfaceContainerHigh
    },
    border = BorderStroke(if (isExpanded) 1.5.dp else 1.dp, borderColor),
    tonalElevation = if (isExpanded) 4.dp else 1.dp,
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .width(4.dp)
            .height(34.dp)
            .clip(CircleShape)
            .background(accentColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = event.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isExpanded) FontWeight.SemiBold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (isExpanded) 4 else 2,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(2.dp))
          val summarySubtitle = remember(timeText, event.calendarName) {
            if (event.calendarName.isNotBlank()) "$timeText • ${event.calendarName}" else timeText
          }
          Text(
            text = summarySubtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.Rounded.KeyboardArrowDown,
          contentDescription = if (isExpanded) "Collapse event details" else "Expand event details",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier
            .size(22.dp)
            .graphicsLayer { rotationZ = chevronRotation.value },
        )
      }

      AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn(tween(180)) + expandVertically(tween(220, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(140)) + shrinkVertically(tween(180, easing = FastOutSlowInEasing)),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

          EventDetailRow(
            icon = SlateIcons.Event,
            text = dateText,
            tint = MaterialTheme.colorScheme.primary,
          )

          EventDetailRow(
            icon = SlateIcons.Schedule,
            text = timeText,
            tint = MaterialTheme.colorScheme.primary,
          )

          if (event.calendarName.isNotBlank()) {
            EventDetailRow(
              icon = SlateIcons.CalendarMonthOutlined,
              text = event.calendarName,
              tint = accentColor,
            )
          }

          if (event.location.isNotBlank()) {
            EventDetailRow(
              icon = SlateIcons.LocationOn,
              text = event.location,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          if (event.description.isNotBlank()) {
            EventDetailRow(
              icon = SlateIcons.Notes,
              text = event.description.trim(),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 6,
            )
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (meetingLink != null) {
              FilledTonalButton(
                onClick = {
                  runCatching { uriHandler.openUri(meetingLink.url) }
                },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier
                  .defaultMinSize(minHeight = 48.dp)
                  .testTag("calendar_event_join_meeting_${event.eventId}"),
              ) {
                Icon(
                  imageVector = if (meetingLink.isVideoMeeting) SlateIcons.VideoCall else SlateIcons.OpenInNew,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = meetingLink.providerLabel,
                  maxLines = 1,
                  softWrap = false,
                )
              }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isAlreadyInSlate) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = "Added",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                }
              }
            } else {
              Button(
                onClick = onAddToSlate,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier
                  .defaultMinSize(minHeight = 48.dp)
                  .testTag("calendar_event_add_to_slate_${event.eventId}"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Add,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Add to Slate",
                  maxLines = 1,
                  softWrap = false,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EventDetailRow(
  icon: ImageVector,
  text: String,
  tint: Color,
  maxLines: Int = 2,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = tint,
      modifier = Modifier
        .padding(top = 2.dp)
        .size(18.dp),
    )
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = maxLines,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
  }
}
