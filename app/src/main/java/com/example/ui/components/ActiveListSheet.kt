package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.calendar.CalendarSyncRepository
import com.example.data.calendar.DeviceCalendarEvent
import com.example.data.local.SlateListWithCounts
import com.example.data.local.SlateTaskEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActiveListSheet(
  activeList: SlateListWithCounts?,
  tasks: List<SlateTaskEntity>,
  allTasks: List<SlateTaskEntity>,
  todaysEvents: List<DeviceCalendarEvent>,
  isTodaysCalendarMode: Boolean,
  hasCalendarPermissions: Boolean,
  highlightedTaskId: Long? = null,
  recentlyAddedTaskId: Long? = null,
  pendingDeleteTaskId: Long? = null,
  dismissSignal: Int = 0,
  onHasActiveSubStateChanged: (Boolean) -> Unit = {},
  onSheetDragDelta: (Float) -> Unit,
  onSheetDragStopped: (velocityY: Float) -> Unit,
  onCollapseSheet: () -> Unit,
  onToggleTaskComplete: (SlateTaskEntity) -> Unit,
  onDeleteTask: (SlateTaskEntity) -> Unit,
  onOpenTaskContextMenu: (SlateTaskEntity, Float) -> Unit,
  onPersistTaskOrder: (List<Long>) -> Unit,
  onAddTask: (title: String, scheduledStartMillis: Long?, syncToCalendar: Boolean) -> Unit,
  onImportCalendarEvent: (DeviceCalendarEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val lazyListState = rememberLazyListState()

  val accentColor = remember(activeList?.colorTagHex) {
    activeList?.colorTagHex?.let { Color(it) }
  } ?: MaterialTheme.colorScheme.primary

  // Filter today's scheduled tasks if Today's Calendar Events Mode is active
  val displayedTasksBase = remember(tasks, allTasks, isTodaysCalendarMode) {
    if (isTodaysCalendarMode) {
      val (todayStart, todayEnd) = CalendarSyncRepository.dayBoundsMillis(System.currentTimeMillis())
      allTasks.filter { task ->
        val start = task.scheduledStartMillis
        start != null && start in todayStart..todayEnd
      }
    } else {
      tasks
    }
  }

  // Local mutable ordering list for responsive drag-and-drop without flicker
  var orderedTasks by remember { mutableStateOf(displayedTasksBase) }
  var draggingTaskId by remember { mutableStateOf<Long?>(null) }
  var dragOffsetY by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(displayedTasksBase, draggingTaskId) {
    if (draggingTaskId == null) {
      orderedTasks = displayedTasksBase
    }
  }

  LaunchedEffect(recentlyAddedTaskId, orderedTasks.size) {
    val targetId = recentlyAddedTaskId ?: return@LaunchedEffect
    val idx = orderedTasks.indexOfFirst { it.id == targetId }
    if (idx >= 0) {
      lazyListState.animateScrollToItem(idx)
    }
  }

  // Unlinked Samsung/device calendar events for Today's Mode
  val unlinkedTodaysEvents = remember(todaysEvents, allTasks, isTodaysCalendarMode) {
    if (!isTodaysCalendarMode) {
      emptyList()
    } else {
      val linkedIds = allTasks.mapNotNull { it.calendarEventId }.toSet()
      todaysEvents.filterNot { it.eventId in linkedIds }
    }
  }

  // Inline Add Item & Expanded Today's Event state
  var isAddingInline by remember { mutableStateOf(false) }
  var inlineText by remember { mutableStateOf("") }
  var inlineScheduledMillis by remember { mutableStateOf<Long?>(null) }
  var showInlineSchedulePicker by remember { mutableStateOf(false) }
  var expandedEventKey by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(isAddingInline, expandedEventKey) {
    onHasActiveSubStateChanged(isAddingInline || expandedEventKey != null)
  }

  LaunchedEffect(dismissSignal) {
    if (dismissSignal > 0) {
      isAddingInline = false
      inlineText = ""
      inlineScheduledMillis = null
      expandedEventKey = null
    }
  }

  // Dynamic density: subtly compact item card height & spacing when list grows
  val totalVisibleItems = orderedTasks.size + unlinkedTodaysEvents.size
  val minCardHeight = when {
    totalVisibleItems >= 10 -> 48.dp
    totalVisibleItems >= 6 -> 56.dp
    else -> 64.dp
  }
  val cardVerticalPadding = when {
    totalVisibleItems >= 10 -> 8.dp
    totalVisibleItems >= 6 -> 11.dp
    else -> 14.dp
  }
  val itemSpacing = when {
    totalVisibleItems >= 10 -> 6.dp
    totalVisibleItems >= 6 -> 8.dp
    else -> 10.dp
  }

  val sheetTitle = remember(isTodaysCalendarMode, activeList?.name) {
    if (isTodaysCalendarMode) {
      SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
    } else {
      activeList?.name ?: "List"
    }
  }

  val sheetDraggableState = rememberDraggableState { delta ->
    onSheetDragDelta(delta)
  }

  Surface(
    modifier = modifier
      .fillMaxSize()
      .testTag("layer1_active_list_sheet"),
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    tonalElevation = 3.dp,
    shadowElevation = 18.dp,
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Draggable Top Handle + Header Zone
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .draggable(
              state = sheetDraggableState,
              orientation = Orientation.Vertical,
              onDragStopped = { velocity -> onSheetDragStopped(velocity) },
            )
            .padding(horizontal = 20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          // Tactile 40dp x 4dp Drag Handle inside 32dp touch area
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(32.dp)
              .clickable(onClick = onCollapseSheet)
              .testTag("sheet_drag_handle"),
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
            )
          }

          // Bold Expressive Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .widthIn(max = 640.dp)
              .padding(top = 4.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                if (!isTodaysCalendarMode) {
                  Box(
                    modifier = Modifier
                      .size(12.dp)
                      .clip(CircleShape)
                      .background(accentColor)
                  )
                }
                Text(
                  text = sheetTitle,
                  style = MaterialTheme.typography.displaySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.testTag("sheet_header_title"),
                )
              }
              if (isTodaysCalendarMode) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = SlateIcons.EventAvailable,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp),
                  )
                  Text(
                    text = stringResource(R.string.todays_events_mode),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }

            // Count badge + Quick Collapse button (48dp touch target)
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              val activeCount = orderedTasks.count { !it.isCompleted } + unlinkedTodaysEvents.size
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
              ) {
                Text(
                  text = activeCount.toString(),
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
              }
              IconButton(
                onClick = onCollapseSheet,
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .containedTapGlow(MaterialTheme.colorScheme.primary)
                  .testTag("collapse_sheet_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.KeyboardArrowDown,
                  contentDescription = stringResource(R.string.cd_drag_handle),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        // Main N x 1 LazyColumn of Item Cards (scrolls cleanly behind floating Add Item bar)
        LazyColumn(
          state = lazyListState,
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .widthIn(max = 640.dp),
          contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 108.dp),
          verticalArrangement = Arrangement.spacedBy(itemSpacing),
        ) {
          if (orderedTasks.isEmpty() && unlinkedTodaysEvents.isEmpty() && !isAddingInline) {
            item(key = "empty_hint", contentType = "empty_hint") {
              EmptyListHintCard(isTodaysCalendarMode = isTodaysCalendarMode)
            }
          }

          items(
            items = orderedTasks,
            key = { it.id },
            contentType = { "task_card" },
          ) { task ->
            val isDraggingThis = draggingTaskId == task.id
            InteractiveTaskCard(
              task = task,
              accentColor = accentColor,
              isDragging = isDraggingThis,
              isHighlighted = highlightedTaskId == task.id,
              isNewlyAdded = recentlyAddedTaskId == task.id,
              isPendingDelete = pendingDeleteTaskId == task.id,
              minCardHeight = minCardHeight,
              cardVerticalPadding = cardVerticalPadding,
              dragOffsetYProvider = { if (draggingTaskId == task.id) dragOffsetY else 0f },
              onToggleComplete = { onToggleTaskComplete(task) },
              onDelete = { onDeleteTask(task) },
              onOpenContextMenu = { cardTopY ->
                onOpenTaskContextMenu(task, cardTopY)
              },
              onStartDragReorder = {
                onOpenTaskContextMenu(task, -1f)
                draggingTaskId = task.id
                dragOffsetY = 0f
              },
              onDragReorderDelta = { deltaY ->
                dragOffsetY += deltaY
                val currentIndex = orderedTasks.indexOfFirst { it.id == task.id }
                if (currentIndex >= 0) {
                  val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                  val currentItemInfo = visibleItems.firstOrNull { it.key == task.id }
                  val stepHeight = (currentItemInfo?.size ?: 180) + 20

                  if (dragOffsetY > stepHeight * 0.65f && currentIndex < orderedTasks.lastIndex) {
                    val mutable = orderedTasks.toMutableList()
                    val moved = mutable.removeAt(currentIndex)
                    mutable.add(currentIndex + 1, moved)
                    orderedTasks = mutable
                    dragOffsetY -= stepHeight
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  } else if (dragOffsetY < -stepHeight * 0.65f && currentIndex > 0) {
                    val mutable = orderedTasks.toMutableList()
                    val moved = mutable.removeAt(currentIndex)
                    mutable.add(currentIndex - 1, moved)
                    orderedTasks = mutable
                    dragOffsetY += stepHeight
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  }
                }
              },
              onEndDragReorder = {
                draggingTaskId = null
                dragOffsetY = 0f
                onPersistTaskOrder(orderedTasks.map { it.id })
              },
            )
          }

          if (unlinkedTodaysEvents.isNotEmpty()) {
            item(key = "todays_calendar_header", contentType = "section_header") {
              Text(
                text = "Calendar Events",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
              )
            }
            items(
              items = unlinkedTodaysEvents,
              key = { "cal_event_${it.eventId}_${it.startMillis}" },
              contentType = { "calendar_event_card" },
            ) { event ->
              val eventKey = "today_ev_${event.eventId}_${event.startMillis}"
              ExpandableCalendarEventCard(
                event = event,
                isExpanded = expandedEventKey == eventKey,
                minHeight = minCardHeight,
                verticalPadding = cardVerticalPadding,
                onToggleExpand = {
                  expandedEventKey = if (expandedEventKey == eventKey) null else eventKey
                },
                onAddToSlate = {
                  onImportCalendarEvent(event)
                  expandedEventKey = null
                },
              )
            }
          }
        }
      }

      // Floating "+ Add item" Pill / Expandable Input Card docked cleanly at the bottom
      FloatingAddItemBar(
        isExpanded = isAddingInline,
        text = inlineText,
        scheduledMillis = inlineScheduledMillis,
        hasCalendarPermissions = hasCalendarPermissions,
        onExpand = { isAddingInline = true },
        onTextChange = { inlineText = it },
        onOpenTimePicker = { showInlineSchedulePicker = true },
        onClearScheduledTime = { inlineScheduledMillis = null },
        onCancel = {
          isAddingInline = false
          inlineText = ""
          inlineScheduledMillis = null
        },
        onSubmit = {
          if (inlineText.isNotBlank()) {
            onAddTask(
              inlineText.trim(),
              inlineScheduledMillis,
              inlineScheduledMillis != null,
            )
            inlineText = ""
            inlineScheduledMillis = null
            isAddingInline = false
          }
        },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .widthIn(max = 640.dp)
          .navigationBarsPadding()
          .imePadding()
          .padding(horizontal = 20.dp, vertical = 14.dp),
      )
    }
  }

  if (showInlineSchedulePicker) {
    ScheduleDateTimeDialog(
      initialMillis = inlineScheduledMillis,
      title = "Time & Calendar Sync",
      onDismiss = { showInlineSchedulePicker = false },
      onConfirm = { millis ->
        inlineScheduledMillis = millis
        showInlineSchedulePicker = false
      },
    )
  }
}

@Composable
private fun FloatingAddItemBar(
  isExpanded: Boolean,
  text: String,
  scheduledMillis: Long?,
  hasCalendarPermissions: Boolean,
  onExpand: () -> Unit,
  onTextChange: (String) -> Unit,
  onOpenTimePicker: () -> Unit,
  onClearScheduledTime: () -> Unit,
  onCancel: () -> Unit,
  onSubmit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(isExpanded) {
    if (isExpanded) {
      delay(60)
      focusRequester.requestFocus()
      keyboardController?.show()
    }
  }

  AnimatedContent(
    targetState = isExpanded,
    transitionSpec = {
      fadeIn(spring(stiffness = Spring.StiffnessMedium)) togetherWith
        fadeOut(spring(stiffness = Spring.StiffnessMedium))
    },
    label = "inlineAddCardAnim",
    modifier = modifier.animateContentSize(
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
      )
    ),
  ) { expanded ->
    if (!expanded) {
      Surface(
        onClick = onExpand,
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 58.dp)
          .clip(RoundedCornerShape(28.dp))
          .stickyPressScale(pressedScale = 0.975f)
          .containedTapGlow(Color.White, maxAlpha = 0.35f)
          .testTag("inline_add_item_card"),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.add_item_label),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    } else {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 64.dp)
          .testTag("inline_add_input_card"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 44.dp)
                .padding(horizontal = 8.dp),
              contentAlignment = Alignment.CenterStart,
            ) {
              if (text.isEmpty()) {
                Text(
                  text = stringResource(R.string.add_item_placeholder),
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                maxLines = 1,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                  capitalization = KeyboardCapitalization.Sentences,
                  imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                  onDone = { onSubmit() },
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .focusRequester(focusRequester)
                  .testTag("inline_add_textfield"),
              )
            }

            IconButton(
              onClick = onCancel,
              modifier = Modifier
                .size(48.dp)
                .testTag("inline_add_cancel_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Cancel",
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
              )
            }

            FilledIconButton(
              onClick = onSubmit,
              enabled = text.isNotBlank(),
              colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
              modifier = Modifier
                .size(48.dp)
                .testTag("inline_add_submit_button"),
            ) {
              Icon(
                imageVector = SlateIcons.ArrowUpward,
                contentDescription = "Save item",
              )
            }
          }

          // Optional "Add Time / Sync to Calendar" Chip
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            val chipLabel = if (scheduledMillis != null) {
              formatScheduledBadge(scheduledMillis)
            } else {
              stringResource(R.string.add_time_sync_calendar)
            }
            AssistChip(
              onClick = onOpenTimePicker,
              label = {
                Text(
                  text = chipLabel,
                  style = MaterialTheme.typography.labelMedium,
                )
              },
              leadingIcon = {
                Icon(
                  imageVector = if (hasCalendarPermissions) SlateIcons.Event else SlateIcons.Schedule,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                labelColor = MaterialTheme.colorScheme.primary,
                leadingIconContentColor = MaterialTheme.colorScheme.primary,
              ),
              border = AssistChipDefaults.assistChipBorder(
                enabled = true,
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
              ),
              shape = CircleShape,
              modifier = Modifier.testTag("inline_add_time_chip"),
            )
            if (scheduledMillis != null) {
              IconButton(
                onClick = onClearScheduledTime,
                modifier = Modifier.size(36.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = "Clear scheduled time",
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(16.dp),
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
private fun EmptyListHintCard(isTodaysCalendarMode: Boolean) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = if (isTodaysCalendarMode) SlateIcons.Event else Icons.Rounded.Add,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        modifier = Modifier.size(32.dp),
      )
      Text(
        text = if (isTodaysCalendarMode) "No events scheduled for today" else "Your slate is clear",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = if (isTodaysCalendarMode) {
          "Tap Add item below to schedule a task and sync it to your calendar."
        } else {
          "Tap Add item below, or pull the handle down to browse Lists, Calendar & Search."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
