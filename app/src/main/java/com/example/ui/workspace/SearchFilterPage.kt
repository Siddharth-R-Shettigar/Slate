package com.example.ui.workspace

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.calendar.DeviceCalendarEvent
import com.example.data.local.SlateListWithCounts
import com.example.data.local.SlateTaskEntity
import com.example.ui.SearchFilter
import com.example.ui.components.ExpandableCalendarEventCard
import com.example.ui.components.SlateIcons
import com.example.ui.components.containedTapGlow
import com.example.ui.components.formatScheduledBadge

@Composable
fun SearchFilterPage(
  searchQuery: String,
  selectedFilter: SearchFilter,
  allTasks: List<SlateTaskEntity>,
  lists: List<SlateListWithCounts>,
  syncedEvents: List<DeviceCalendarEvent>,
  dismissSignal: Int = 0,
  onHasActiveSubStateChanged: (Boolean) -> Unit = {},
  onSelectFilter: (SearchFilter) -> Unit,
  onToggleTaskComplete: (SlateTaskEntity) -> Unit,
  onToggleTaskStar: (SlateTaskEntity) -> Unit,
  onJumpToList: (Long) -> Unit,
  onImportEvent: (DeviceCalendarEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  val listById = remember(lists) { lists.associateBy { it.id } }
  var expandedEventKey by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(expandedEventKey) {
    onHasActiveSubStateChanged(expandedEventKey != null)
  }

  LaunchedEffect(dismissSignal) {
    if (dismissSignal > 0) {
      expandedEventKey = null
    }
  }

  val filteredTasks = remember(allTasks, searchQuery, selectedFilter) {
    val q = searchQuery.trim().lowercase()
    allTasks.filter { task ->
      val matchesQuery = q.isEmpty() || task.title.lowercase().contains(q)
      val matchesFilter = when (selectedFilter) {
        SearchFilter.ALL -> true
        SearchFilter.STARRED -> task.isStarred
        SearchFilter.SCHEDULED -> task.scheduledStartMillis != null
        SearchFilter.COMPLETED -> task.isCompleted
      }
      matchesQuery && matchesFilter
    }
  }

  val filteredEvents = remember(syncedEvents, allTasks, searchQuery, selectedFilter) {
    if (selectedFilter == SearchFilter.STARRED || selectedFilter == SearchFilter.COMPLETED) {
      emptyList()
    } else {
      val q = searchQuery.trim().lowercase()
      val linkedIds = allTasks.mapNotNull { it.calendarEventId }.toSet()
      syncedEvents.filter { ev ->
        ev.eventId !in linkedIds && (q.isEmpty() || ev.title.lowercase().contains(q))
      }
    }
  }

  val primaryColor = MaterialTheme.colorScheme.primary
  val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
  val cardBorder = remember(outlineColor) { BorderStroke(1.dp, outlineColor) }

  val chipBaseModifier = remember(primaryColor) {
    Modifier
      .clip(CircleShape)
      .containedTapGlow(primaryColor)
  }
  val resultCardModifier = remember {
    Modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 64.dp)
  }
  val taskRowModifier = remember {
    Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 10.dp)
  }
  val iconButtonModifier = remember { Modifier.size(48.dp) }
  val dotBaseModifier = remember {
    Modifier
      .size(8.dp)
      .clip(CircleShape)
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 640.dp)
        .imePadding(),
    ) {
      // Filter Chips Row (search input itself is in the morphed glowing bottom pill!)
      LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(
          items = SearchFilter.entries,
          key = { it.name },
          contentType = { "filter_chip" },
        ) { filter ->
          FilterChip(
            selected = selectedFilter == filter,
            onClick = { onSelectFilter(filter) },
            label = {
              Text(
                text = filter.label,
                maxLines = 1,
                softWrap = false,
              )
            },
            border = FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = selectedFilter == filter,
              borderColor = outlineColor,
              selectedBorderColor = primaryColor.copy(alpha = 0.65f),
            ),
            shape = CircleShape,
            modifier = chipBaseModifier.testTag("search_filter_${filter.name.lowercase()}"),
          )
        }
      }

      // Live Results
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (filteredTasks.isEmpty() && filteredEvents.isEmpty()) {
          item(key = "no_search_results", contentType = "empty_state") {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(20.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLow,
              border = cardBorder,
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Search,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(30.dp),
                )
                Text(
                  text = "No matching items",
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = "Type in the search pill below or switch filter chips.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        items(
          items = filteredTasks,
          key = { it.id },
          contentType = { "search_task_card" },
        ) { task ->
          val parentList = listById[task.listId]
          val listColor = parentList?.colorTagHex?.let { Color(it) } ?: primaryColor

          Surface(
            onClick = { onJumpToList(task.listId) },
            modifier = resultCardModifier,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = cardBorder,
          ) {
            Row(
              modifier = taskRowModifier,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              IconButton(
                onClick = { onToggleTaskComplete(task) },
                modifier = iconButtonModifier,
              ) {
                Icon(
                  imageVector = if (task.isCompleted) {
                    SlateIcons.CheckCircle
                  } else {
                    SlateIcons.RadioButtonUnchecked
                  },
                  contentDescription = "Toggle completion",
                  tint = if (task.isCompleted) listColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = task.title,
                  style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                  ),
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Box(modifier = dotBaseModifier.background(listColor))
                  Text(
                    text = parentList?.name ?: "List",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  task.scheduledStartMillis?.let { millis ->
                    Text(
                      text = "• ${formatScheduledBadge(millis)}",
                      style = MaterialTheme.typography.labelMedium,
                      color = primaryColor,
                    )
                  }
                }
              }

              IconButton(
                onClick = { onToggleTaskStar(task) },
                modifier = iconButtonModifier,
              ) {
                Icon(
                  imageVector = if (task.isStarred) Icons.Rounded.Star else SlateIcons.StarOutline,
                  contentDescription = "Toggle star",
                  tint = if (task.isStarred) {
                    MaterialTheme.colorScheme.tertiary
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                )
              }
            }
          }
        }

        items(
          items = filteredEvents,
          key = { "search_ev_${it.eventId}_${it.startMillis}" },
          contentType = { "search_event_card" },
        ) { event ->
          val eventKey = "search_ev_${event.eventId}_${event.startMillis}"
          ExpandableCalendarEventCard(
            event = event,
            isExpanded = expandedEventKey == eventKey,
            onToggleExpand = {
              expandedEventKey = if (expandedEventKey == eventKey) null else eventKey
            },
            onAddToSlate = {
              onImportEvent(event)
              expandedEventKey = null
            },
          )
        }
      }
    }
  }
}
