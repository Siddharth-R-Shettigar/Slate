package com.example.ui.workspace

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.calendar.CalendarSyncRepository
import com.example.data.calendar.DeviceCalendarEvent
import com.example.data.local.SlateTaskEntity
import com.example.ui.CalendarViewMode
import com.example.ui.components.ScheduleDateTimeDialog
import com.example.ui.components.SlateIcons
import com.example.ui.components.containedTapGlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarSyncPage(
  hasCalendarPermissions: Boolean,
  syncedEvents: List<DeviceCalendarEvent>,
  scheduledTasks: List<SlateTaskEntity>,
  selectedDateMillis: Long,
  viewMode: CalendarViewMode,
  isSyncing: Boolean,
  lastSyncedMillis: Long?,
  onPermissionsResult: () -> Unit,
  onSelectDate: (Long) -> Unit,
  onChangeViewMode: (CalendarViewMode) -> Unit,
  onSyncNow: () -> Unit,
  onAddCalendarTask: (title: String, startMillis: Long) -> Unit,
  onImportEventToActiveList: (DeviceCalendarEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
    onResult = { onPermissionsResult() },
  )
  val lazyListState = rememberLazyListState()
  val scope = rememberCoroutineScope()

  var isAddingInline by remember { mutableStateOf(false) }

  val selectedDayBounds = remember(selectedDateMillis) {
    CalendarSyncRepository.dayBoundsMillis(selectedDateMillis)
  }

  val dayTasks = remember(scheduledTasks, selectedDayBounds) {
    scheduledTasks.filter { task ->
      val start = task.scheduledStartMillis ?: return@filter false
      start in selectedDayBounds.first..selectedDayBounds.second
    }
  }

  val dayEvents = remember(syncedEvents, selectedDayBounds, dayTasks) {
    val linkedIds = dayTasks.mapNotNull { it.calendarEventId }.toSet()
    syncedEvents.filter { event ->
      event.eventId !in linkedIds &&
        event.endMillis >= selectedDayBounds.first &&
        event.startMillis <= selectedDayBounds.second
    }
  }

  val populatedDayKeys = remember(syncedEvents, scheduledTasks) {
    val cal = Calendar.getInstance()
    val keys = mutableSetOf<Int>()
    syncedEvents.forEach { ev ->
      cal.timeInMillis = ev.startMillis
      keys += cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
    }
    scheduledTasks.forEach { task ->
      task.scheduledStartMillis?.let { start ->
        cal.timeInMillis = start
        keys += cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
      }
    }
    keys
  }

  val itemSurfaceModifier = remember {
    Modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 64.dp)
  }
  val itemRowModifier = remember {
    Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 14.dp)
  }
  val eventAccentBarModifier = remember {
    Modifier
      .width(4.dp)
      .height(34.dp)
      .clip(CircleShape)
  }
  val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      state = lazyListState,
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 640.dp)
        .imePadding(),
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 148.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Permission Prompt Banner (shown ONLY if permissions are not yet granted)
      if (!hasCalendarPermissions) {
        item(key = "calendar_permission_prompt", contentType = "permission_banner") {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = SlateIcons.CalendarMonthRounded,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                  text = "Connect Samsung & Device Calendar",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
              Text(
                text = "Grant calendar access for two-way sync. Choose your target calendar account in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
              )
              Button(
                onClick = {
                  permissionLauncher.launch(
                    arrayOf(
                      Manifest.permission.READ_CALENDAR,
                      Manifest.permission.WRITE_CALENDAR,
                    )
                  )
                },
                modifier = Modifier.testTag("grant_calendar_permission_button"),
              ) {
                Text("Enable Two-Way Sync")
              }
            }
          }
        }
      }

      // Interactive Monthly / Weekly Calendar Card (no text wrapping!)
      item(key = "interactive_calendar_card", contentType = "calendar_grid") {
        InteractiveCalendarCard(
          selectedDateMillis = selectedDateMillis,
          viewMode = viewMode,
          populatedDayKeys = populatedDayKeys,
          hasCalendarPermissions = hasCalendarPermissions,
          isSyncing = isSyncing,
          onSelectDate = onSelectDate,
          onChangeViewMode = onChangeViewMode,
          onSyncNow = onSyncNow,
        )
      }

      // Selected Day Header
      item(key = "selected_day_header", contentType = "day_header") {
        val dayLabel = remember(selectedDateMillis) {
          SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(selectedDateMillis))
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text(
              text = dayLabel,
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            lastSyncedMillis?.let { syncedAt ->
              val timeStr = timeFormat.format(Date(syncedAt))
              Text(
                text = "Synced $timeStr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      if (dayTasks.isEmpty() && dayEvents.isEmpty() && !isAddingInline) {
        item(key = "day_empty", contentType = "empty_state") {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
          ) {
            Text(
              text = "Nothing scheduled for this day yet.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(20.dp),
            )
          }
        }
      }

      items(
        items = dayTasks,
        key = { it.id },
        contentType = { "calendar_task_card" },
      ) { task ->
        val timeText = remember(task.scheduledStartMillis) {
          task.scheduledStartMillis?.let { timeFormat.format(Date(it)) } ?: ""
        }
        Surface(
          modifier = itemSurfaceModifier,
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
          Row(
            modifier = itemRowModifier,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = if (task.calendarEventId != null) SlateIcons.EventAvailable else SlateIcons.Schedule,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = if (task.calendarEventId != null) "$timeText • Synced task" else "$timeText • Slate task",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      items(
        items = dayEvents,
        key = { "cal_ev_${it.eventId}_${it.startMillis}" },
        contentType = { "calendar_event_card" },
      ) { event ->
        val timeRange = remember(event) {
          if (event.allDay) "All day"
          else "${timeFormat.format(Date(event.startMillis))} – ${timeFormat.format(Date(event.endMillis))}"
        }
        Surface(
          onClick = { onImportEventToActiveList(event) },
          modifier = itemSurfaceModifier,
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
          Row(
            modifier = itemRowModifier,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = eventAccentBarModifier
                .background(if (event.color != 0) Color(event.color) else MaterialTheme.colorScheme.tertiary)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "$timeRange • Tap to pin to active list",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Inline Add Scheduled Task Box right where the final element appears (no popup!)
      item(key = "inline_calendar_add_box", contentType = "inline_add_card") {
        InlineCalendarAddCard(
          isExpanded = isAddingInline,
          selectedDateMillis = selectedDateMillis,
          onExpand = {
            isAddingInline = true
            scope.launch {
              delay(120)
              lazyListState.animateScrollToItem(
                (dayTasks.size + dayEvents.size + 2).coerceAtLeast(0)
              )
            }
          },
          onCancel = { isAddingInline = false },
          onAdd = { title, startMillis ->
            onAddCalendarTask(title, startMillis)
            isAddingInline = false
          },
        )
      }
    }
  }
}

@Composable
private fun InlineCalendarAddCard(
  isExpanded: Boolean,
  selectedDateMillis: Long,
  onExpand: () -> Unit,
  onCancel: () -> Unit,
  onAdd: (title: String, startMillis: Long) -> Unit,
) {
  var title by remember(isExpanded) { mutableStateOf("") }
  var startMillis by remember(selectedDateMillis, isExpanded) {
    val cal = Calendar.getInstance().apply {
      timeInMillis = selectedDateMillis
      set(Calendar.HOUR_OF_DAY, 10)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    mutableLongStateOf(cal.timeInMillis)
  }
  var showClockPicker by remember { mutableStateOf(false) }
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(isExpanded) {
    if (isExpanded) {
      delay(80)
      focusRequester.requestFocus()
      keyboardController?.show()
    }
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 64.dp)
      .clip(RoundedCornerShape(20.dp))
      .animateContentSize(
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMediumLow,
        )
      )
      .then(if (!isExpanded) Modifier.clickable(onClick = onExpand) else Modifier)
      .testTag("add_calendar_task_button"),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    tonalElevation = if (isExpanded) 4.dp else 1.dp,
  ) {
    AnimatedContent(
      targetState = isExpanded,
      transitionSpec = { fadeIn() togetherWith fadeOut() },
      label = "inlineCalAddAnim",
    ) { expanded ->
      if (!expanded) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
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
            text = "Schedule item for this day",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      } else {
        val timeLabel = remember(startMillis) {
          SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()).format(Date(startMillis))
        }
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "Schedule & Sync",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            IconButton(
              onClick = onCancel,
              modifier = Modifier.size(40.dp),
            ) {
              Icon(Icons.Rounded.Close, contentDescription = "Cancel")
            }
          }

          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Task or event title") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
              capitalization = KeyboardCapitalization.Sentences,
              imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
              onDone = {
                if (title.isNotBlank()) {
                  onAdd(title.trim(), startMillis)
                }
              },
            ),
            modifier = Modifier
              .fillMaxWidth()
              .focusRequester(focusRequester)
              .testTag("calendar_task_title_input"),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            AssistChip(
              onClick = { showClockPicker = true },
              label = { Text(timeLabel, maxLines = 1, softWrap = false) },
              leadingIcon = {
                Icon(
                  imageVector = SlateIcons.Event,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              shape = CircleShape,
            )

            Row {
              TextButton(onClick = onCancel) {
                Text("Cancel")
              }
              Spacer(modifier = Modifier.width(4.dp))
              FilledTonalButton(
                onClick = {
                  if (title.isNotBlank()) {
                    onAdd(title.trim(), startMillis)
                  }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_calendar_task_button"),
              ) {
                Text("Add & Sync")
              }
            }
          }
        }
      }
    }
  }

  if (showClockPicker) {
    ScheduleDateTimeDialog(
      initialMillis = startMillis,
      title = "Schedule & Sync",
      onDismiss = { showClockPicker = false },
      onConfirm = { picked ->
        startMillis = picked
        showClockPicker = false
      },
    )
  }
}

@Composable
private fun InteractiveCalendarCard(
  selectedDateMillis: Long,
  viewMode: CalendarViewMode,
  populatedDayKeys: Set<Int>,
  hasCalendarPermissions: Boolean,
  isSyncing: Boolean,
  onSelectDate: (Long) -> Unit,
  onChangeViewMode: (CalendarViewMode) -> Unit,
  onSyncNow: () -> Unit,
) {
  val selectedDayKey = remember(selectedDateMillis) {
    val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
  }
  val todayDayKey = remember {
    val cal = Calendar.getInstance()
    cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
  }
  val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
  val monthTitle = remember(selectedDateMillis) {
    monthFormatter.format(Date(selectedDateMillis))
  }
  val periodKey = remember(selectedDateMillis, viewMode) {
    val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    if (viewMode == CalendarViewMode.MONTH) {
      cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
    } else {
      cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.WEEK_OF_YEAR)
    }
  }
  var navDirection by remember { mutableStateOf(1) }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    tonalElevation = 2.dp,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Row 1: Month Year + Navigation Arrows + Sync Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              navDirection = -1
              val next = Calendar.getInstance().apply {
                timeInMillis = selectedDateMillis
                add(if (viewMode == CalendarViewMode.MONTH) Calendar.MONTH else Calendar.WEEK_OF_YEAR, -1)
              }
              onSelectDate(next.timeInMillis)
            },
            modifier = Modifier.size(44.dp),
          ) {
            Icon(SlateIcons.KeyboardArrowLeft, contentDescription = "Previous")
          }
          AnimatedContent(
            targetState = monthTitle,
            transitionSpec = {
              (androidx.compose.animation.slideInHorizontally(
                animationSpec = tween(220, easing = FastOutSlowInEasing)
              ) { (it / 4) * navDirection } + fadeIn(tween(180))) togetherWith
                (androidx.compose.animation.slideOutHorizontally(
                  animationSpec = tween(180, easing = FastOutSlowInEasing)
                ) { -(it / 4) * navDirection } + fadeOut(tween(140)))
            },
            label = "monthTitleAnim",
          ) { title ->
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false,
            )
          }
          IconButton(
            onClick = {
              navDirection = 1
              val next = Calendar.getInstance().apply {
                timeInMillis = selectedDateMillis
                add(if (viewMode == CalendarViewMode.MONTH) Calendar.MONTH else Calendar.WEEK_OF_YEAR, 1)
              }
              onSelectDate(next.timeInMillis)
            },
            modifier = Modifier.size(44.dp),
          ) {
            Icon(SlateIcons.KeyboardArrowRight, contentDescription = "Next")
          }
        }

        if (hasCalendarPermissions) {
          IconButton(
            onClick = onSyncNow,
            enabled = !isSyncing,
            modifier = Modifier
              .size(44.dp)
              .testTag("sync_calendar_now_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = "Sync calendar now",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      // Row 2: Dedicated Full-Width Pill Switcher (Month | Week never wraps!)
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          CalendarViewMode.entries.forEach { mode ->
            val selected = viewMode == mode
            val bgColorState = animateColorAsState(
              targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
              animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
              label = "calModePillBg",
            )
            val textColorState = animateColorAsState(
              targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
              label = "calModePillText",
            )
            Box(
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 40.dp)
                .clip(CircleShape)
                .drawBehind { drawRect(bgColorState.value) }
                .containedTapGlow(MaterialTheme.colorScheme.primary)
                .clickable { onChangeViewMode(mode) }
                .testTag("calendar_mode_${mode.name.lowercase()}"),
              contentAlignment = Alignment.Center,
            ) {
              BasicText(
                text = mode.label,
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = { textColorState.value },
                maxLines = 1,
                softWrap = false,
              )
            }
          }
        }
      }

      // Weekday header row
      val weekdays = remember { listOf("S", "M", "T", "W", "T", "F", "S") }
      Row(modifier = Modifier.fillMaxWidth()) {
        weekdays.forEach { day ->
          Text(
            text = day,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
          )
        }
      }

      // Animated Calendar Grid (precomputed cells -> zero Calendar allocations inside grid!)
      AnimatedContent(
        targetState = periodKey to viewMode,
        transitionSpec = {
          (androidx.compose.animation.slideInHorizontally(
            animationSpec = tween(220, easing = FastOutSlowInEasing)
          ) { w -> (w / 3) * navDirection } + fadeIn(tween(180))) togetherWith
            (androidx.compose.animation.slideOutHorizontally(
              animationSpec = tween(180, easing = FastOutSlowInEasing)
            ) { w -> -(w / 3) * navDirection } + fadeOut(tween(140)))
        },
        label = "calendarGridSlideAnim",
      ) { (animPeriod, animatedMode) ->
        val daysGrid = remember(animPeriod, animatedMode) {
          buildCalendarDayCells(selectedDateMillis, animatedMode)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          daysGrid.chunked(7).forEach { week ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              week.forEach { cell ->
                if (cell == null) {
                  Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                } else {
                  val isSelected = cell.dayKey == selectedDayKey
                  val isToday = cell.dayKey == todayDayKey
                  val hasItems = cell.dayKey in populatedDayKeys

                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .aspectRatio(1f)
                      .clip(CircleShape)
                      .background(
                        when {
                          isSelected -> MaterialTheme.colorScheme.primary
                          isToday -> MaterialTheme.colorScheme.primaryContainer
                          else -> Color.Transparent
                        }
                      )
                      .clickable { onSelectDate(cell.epochMillis) },
                    contentAlignment = Alignment.Center,
                  ) {
                    Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.Center,
                    ) {
                      Text(
                        text = cell.dayOfMonthText,
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                          isSelected -> MaterialTheme.colorScheme.onPrimary
                          isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                          else -> MaterialTheme.colorScheme.onSurface
                        },
                      )
                      if (hasItems) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                          modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                              if (isSelected) MaterialTheme.colorScheme.onPrimary
                              else MaterialTheme.colorScheme.primary
                            )
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Immutable
private data class CalendarDayCell(
  val epochMillis: Long,
  val dayOfMonthText: String,
  val dayKey: Int,
)

private fun buildCalendarDayCells(selectedMillis: Long, mode: CalendarViewMode): List<CalendarDayCell?> {
  val cal = Calendar.getInstance().apply { timeInMillis = selectedMillis }
  return if (mode == CalendarViewMode.WEEK) {
    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    (0 until 7).map {
      val cell = CalendarDayCell(
        epochMillis = cal.timeInMillis,
        dayOfMonthText = cal.get(Calendar.DAY_OF_MONTH).toString(),
        dayKey = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR),
      )
      cal.add(Calendar.DAY_OF_MONTH, 1)
      cell
    }
  } else {
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDow = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val year = cal.get(Calendar.YEAR)
    val cells = ArrayList<CalendarDayCell?>(42)
    repeat(firstDow) { cells += null }
    for (day in 1..daysInMonth) {
      cal.set(Calendar.DAY_OF_MONTH, day)
      cells += CalendarDayCell(
        epochMillis = cal.timeInMillis,
        dayOfMonthText = day.toString(),
        dayKey = year * 1000 + cal.get(Calendar.DAY_OF_YEAR),
      )
    }
    while (cells.size % 7 != 0) {
      cells += null
    }
    cells
  }
}
