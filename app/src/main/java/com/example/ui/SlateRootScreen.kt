package com.example.ui

import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SlateListWithCounts
import com.example.data.local.SlateTaskEntity
import com.example.ui.components.ActiveListSheet
import com.example.ui.components.BlurredContextMenuOverlay
import com.example.ui.components.DeleteListConfirmationDialog
import com.example.ui.components.EditTaskDialog
import com.example.ui.components.MoveToListDialog
import com.example.ui.components.ScheduleDateTimeDialog
import com.example.ui.settings.SettingsScreen
import com.example.ui.workspace.WorkspaceScreen
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun SlateRootScreen(
  viewModel: SlateViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current

  var contextMenuTarget by remember { mutableStateOf<Pair<SlateTaskEntity, Float>?>(null) }
  var editingTask by remember { mutableStateOf<SlateTaskEntity?>(null) }
  var schedulingTask by remember { mutableStateOf<SlateTaskEntity?>(null) }
  var movingTask by remember { mutableStateOf<SlateTaskEntity?>(null) }
  var deleteListTarget by remember { mutableStateOf<SlateListWithCounts?>(null) }
  var pendingDeleteTaskId by remember { mutableStateOf<Long?>(null) }

  var hasLayer1ActiveSubState by remember { mutableStateOf(false) }
  var dismissLayer1Signal by remember { mutableIntStateOf(0) }
  var hasLayer2ActiveSubState by remember { mutableStateOf(false) }
  var dismissLayer2Signal by remember { mutableIntStateOf(0) }

  LaunchedEffect(uiState.isSheetOpen) {
    if (!uiState.isSheetOpen) {
      contextMenuTarget = null
      pendingDeleteTaskId = null
    }
  }

  LaunchedEffect(viewModel) {
    viewModel.snackbarEvents.collect { event ->
      snackbarHostState.currentSnackbarData?.dismiss()
      when (event) {
        is SlateSnackbarEvent.TaskDeleted -> {
          val res = snackbarHostState.showSnackbar(
            message = "Deleted \"${event.task.title}\"",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short,
          )
          if (res == SnackbarResult.ActionPerformed) {
            viewModel.undoDeleteTask(event.task)
          }
        }
        is SlateSnackbarEvent.Message -> {
          snackbarHostState.showSnackbar(
            message = event.text,
            duration = SnackbarDuration.Short,
          )
        }
      }
    }
  }

  if (!uiState.isReady) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    )
    return
  }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    val totalHeightPx = with(density) { maxHeight.toPx() }
    val statusTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Leave continuous ~18dp peek (or ~4% height) at top edge revealing Layer 2
    val peekDp = maxOf(18.dp, maxHeight * 0.04f)
    val sheetRestingTopDp = statusTopDp + peekDp
    val sheetRestingTopPx = with(density) { sheetRestingTopDp.toPx() }
    val collapsedOffsetPx = (totalHeightPx - sheetRestingTopPx).coerceAtLeast(1f)

    // Sheet vertical translation from resting top (0f = open, collapsedOffsetPx = collapsed)
    val sheetOffsetAnim = remember {
      Animatable(if (uiState.isSheetOpen) 0f else collapsedOffsetPx)
    }
    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(uiState.isSheetOpen, collapsedOffsetPx) {
      val target = if (uiState.isSheetOpen) 0f else collapsedOffsetPx
      if (uiState.isSheetOpen) {
        sheetOffsetAnim.animateTo(
          targetValue = target,
          animationSpec = spring(
            dampingRatio = 0.86f,
            stiffness = 420f,
          ),
        )
      } else {
        sheetOffsetAnim.animateTo(
          targetValue = target,
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 480f,
          ),
        )
      }
    }

    val hasAnyModalDialog = editingTask != null ||
      schedulingTask != null ||
      movingTask != null ||
      deleteListTarget != null

    // Strict Predictive Back Priority Chain (Settings handles itself inside AnimatedVisibility below)
    // 7. Search tab Back -> returns to previous non-search tab (Lists or Calendar)
    PredictiveBackHandler(
      enabled = !uiState.isSettingsOpen &&
        !uiState.isSheetOpen &&
        !hasAnyModalDialog &&
        !hasLayer2ActiveSubState &&
        uiState.workspacePage == 2,
    ) { progress ->
      try {
        progress.collect { }
        viewModel.navigateBackFromSearch()
      } catch (e: CancellationException) {
        throw e
      }
    }

    // 6. Layer 2 active inline state (edit list card, inline add calendar card, expanded event)
    PredictiveBackHandler(
      enabled = !uiState.isSettingsOpen &&
        !uiState.isSheetOpen &&
        !hasAnyModalDialog &&
        hasLayer2ActiveSubState,
    ) { progress ->
      try {
        progress.collect { }
        dismissLayer2Signal++
      } catch (e: CancellationException) {
        throw e
      }
    }

    // 5. Layer 1 foreground sheet predictive collapse
    PredictiveBackHandler(
      enabled = !uiState.isSettingsOpen &&
        uiState.isSheetOpen &&
        contextMenuTarget == null &&
        !hasAnyModalDialog &&
        !hasLayer1ActiveSubState,
    ) { progress ->
      try {
        progress.collect { backEvent ->
          predictiveBackProgress = backEvent.progress
        }
        predictiveBackProgress = 0f
        viewModel.collapseSheet()
      } catch (e: CancellationException) {
        predictiveBackProgress = 0f
        throw e
      }
    }

    // 4. Layer 1 active inline state (inline task input or expanded calendar event card)
    PredictiveBackHandler(
      enabled = !uiState.isSettingsOpen &&
        uiState.isSheetOpen &&
        contextMenuTarget == null &&
        !hasAnyModalDialog &&
        hasLayer1ActiveSubState,
    ) { progress ->
      try {
        progress.collect { }
        dismissLayer1Signal++
      } catch (e: CancellationException) {
        throw e
      }
    }

    // 3. Modal dialogs (EditTask, ScheduleDateTime, MoveToList, DeleteListConfirmation)
    PredictiveBackHandler(
      enabled = !uiState.isSettingsOpen && contextMenuTarget == null && hasAnyModalDialog,
    ) { progress ->
      try {
        progress.collect { }
        editingTask = null
        schedulingTask = null
        movingTask = null
        deleteListTarget = null
      } catch (e: CancellationException) {
        throw e
      }
    }

    // 2. Long-press blurred context menu
    PredictiveBackHandler(
      enabled = !uiState.isSettingsOpen && contextMenuTarget != null,
    ) { progress ->
      try {
        progress.collect { }
        contextMenuTarget = null
      } catch (e: CancellationException) {
        throw e
      }
    }

    // Derived booleans so animations never trigger unnecessary root recompositions
    val isSheetVisible by remember(collapsedOffsetPx) {
      derivedStateOf {
        val currentOffset = sheetOffsetAnim.value + collapsedOffsetPx * 0.45f * predictiveBackProgress
        currentOffset < collapsedOffsetPx * 0.995f
      }
    }

    val isSheetVisiblyOpen by remember(collapsedOffsetPx) {
      derivedStateOf {
        val currentOffset = sheetOffsetAnim.value + collapsedOffsetPx * 0.45f * predictiveBackProgress
        val openFrac = 1f - (currentOffset.coerceIn(0f, collapsedOffsetPx) / collapsedOffsetPx)
        openFrac > 0.14f
      }
    }

    val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val blurEnabled = uiState.settings.backdropBlurEnabled && blurSupported
    val shouldBlurLayer2 = blurEnabled && (
      isSheetVisiblyOpen ||
        contextMenuTarget != null ||
        hasAnyModalDialog
      )
    val shouldBlurLayer1 = blurEnabled && (
      contextMenuTarget != null ||
        editingTask != null ||
        schedulingTask != null ||
        movingTask != null
      )

    // LAYER 2: Main Workspace (GPU depth scale + optional fixed-radius RenderEffect blur)
    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          val eff = (sheetOffsetAnim.value + collapsedOffsetPx * 0.45f * predictiveBackProgress)
            .coerceIn(0f, collapsedOffsetPx)
          val openFrac = 1f - (eff / collapsedOffsetPx)
          val depthScale = 0.96f + 0.04f * (1f - openFrac)
          scaleX = depthScale
          scaleY = depthScale
        }
        .then(
          if (shouldBlurLayer2) {
            Modifier.blur(12.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
          } else {
            Modifier
          },
        ),
    ) {
      WorkspaceScreen(
        uiState = uiState,
        isLayer2Visible = !uiState.isSheetOpen,
        dismissLayer2Signal = dismissLayer2Signal,
        onHasLayer2SubStateChanged = { hasLayer2ActiveSubState = it },
        onWorkspacePageChanged = viewModel::setWorkspacePage,
        onNavigateBackFromSearch = viewModel::navigateBackFromSearch,
        onOpenSettings = viewModel::openSettings,
        onPullUpActiveSheet = viewModel::openSheet,
        onSelectAndOpenList = { listId ->
          viewModel.selectActiveList(listId, openSheetImmediately = true)
        },
        onSetDefaultLaunchList = viewModel::setDefaultLaunchList,
        onCreateList = viewModel::createList,
        onUpdateList = viewModel::updateList,
        onRequestDeleteList = { list -> deleteListTarget = list },
        onPersistListOrder = viewModel::persistReorderedLists,
        onCalendarPermissionsResult = viewModel::refreshCalendarPermissionsAndSync,
        onSelectDate = viewModel::selectDate,
        onChangeCalendarViewMode = viewModel::setCalendarViewMode,
        onSyncCalendarNow = viewModel::syncCalendarNow,
        onAddCalendarTask = viewModel::addCalendarScheduledTask,
        onImportCalendarEvent = viewModel::importEventToActiveList,
        onSearchQueryChange = viewModel::setSearchQuery,
        onSelectSearchFilter = viewModel::setSearchFilter,
        onToggleTaskComplete = viewModel::toggleTaskCompleted,
        onToggleTaskStar = viewModel::toggleTaskStarred,
      )

      // Smooth GPU-alpha scrim over Layer 2 when Layer 1 is open (zero recompositions during drag!)
      Box(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            val eff = (sheetOffsetAnim.value + collapsedOffsetPx * 0.45f * predictiveBackProgress)
              .coerceIn(0f, collapsedOffsetPx)
            val openFrac = 1f - (eff / collapsedOffsetPx)
            alpha = 0.34f * openFrac
          }
          .background(Color.Black),
      )
    }

    // LAYER 1: Foreground Interactive Floating Sheet (~96% height, continuous top peek)
    // Positioned via fixed top padding + GPU graphicsLayer translationY (zero layout passes per frame!)
    if (isSheetVisible || uiState.isSheetOpen) {
      val listAccentColor = uiState.activeList?.colorTagHex?.let { Color(it) }
        ?: MaterialTheme.colorScheme.primary

      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = sheetRestingTopDp)
          .graphicsLayer {
            val rawOffset = sheetOffsetAnim.value + collapsedOffsetPx * 0.45f * predictiveBackProgress
            translationY = rawOffset.coerceAtLeast(-44f)
            if (rawOffset < 0f) {
              val stretch = (-rawOffset / 44f).coerceIn(0f, 1f)
              scaleX = 1f - 0.012f * stretch
              scaleY = 1f + 0.014f * stretch
              transformOrigin = TransformOrigin(0.5f, 0f)
            } else {
              scaleX = 1f
              scaleY = 1f
            }
          }
          .then(
            if (shouldBlurLayer1) {
              Modifier.blur(12.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            } else {
              Modifier
            },
          ),
      ) {
        ActiveListSheet(
          activeList = uiState.activeList,
          tasks = uiState.activeListTasks,
          allTasks = uiState.allTasks,
          todaysEvents = uiState.todaysEvents,
          isTodaysCalendarMode = uiState.settings.useTodaysCalendarAsLaunchList,
          hasCalendarPermissions = uiState.hasCalendarPermissions,
          highlightedTaskId = contextMenuTarget?.first?.id,
          recentlyAddedTaskId = uiState.recentlyAddedTaskId,
          pendingDeleteTaskId = pendingDeleteTaskId,
          dismissSignal = dismissLayer1Signal,
          onHasActiveSubStateChanged = { hasLayer1ActiveSubState = it },
          onSheetDragDelta = { delta ->
            scope.launch {
              val cur = sheetOffsetAnim.value
              val dampedDelta = if (cur < 0f && delta < 0f) delta * 0.25f else delta
              val next = (cur + dampedDelta).coerceIn(-44f, collapsedOffsetPx)
              sheetOffsetAnim.snapTo(next)
            }
          },
          onSheetDragStopped = { velocityY ->
            val current = sheetOffsetAnim.value
            val shouldCollapse = velocityY > 850f || current > collapsedOffsetPx * 0.22f
            scope.launch {
              if (shouldCollapse) {
                viewModel.collapseSheet()
              } else {
                sheetOffsetAnim.animateTo(
                  targetValue = 0f,
                  animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = 420f,
                  ),
                )
              }
            }
          },
          onCollapseSheet = viewModel::collapseSheet,
          onToggleTaskComplete = viewModel::toggleTaskCompleted,
          onDeleteTask = { task ->
            if (pendingDeleteTaskId == task.id) {
              pendingDeleteTaskId = null
            }
            viewModel.deleteTaskWithUndo(task)
          },
          onOpenTaskContextMenu = { task, cardTopY ->
            if (cardTopY < 0f) {
              contextMenuTarget = null
            } else {
              contextMenuTarget = task to cardTopY
            }
          },
          onPersistTaskOrder = viewModel::persistReorderedTasks,
          onAddTask = viewModel::addTask,
          onImportCalendarEvent = viewModel::importEventToActiveList,
        )
      }

      // Elevated Context Menu Overlay with sharp highlighted card copy above blurred layers
      contextMenuTarget?.let { (selectedTask, cardTopY) ->
        val freshTask = uiState.allTasks.firstOrNull { it.id == selectedTask.id } ?: selectedTask
        BlurredContextMenuOverlay(
          task = freshTask,
          accentColor = listAccentColor,
          cardTopInRootY = cardTopY,
          onDismiss = { contextMenuTarget = null },
          onEditTitle = {
            contextMenuTarget = null
            editingTask = freshTask
          },
          onToggleStar = { viewModel.toggleTaskStarred(freshTask) },
          onAssignDateTime = {
            contextMenuTarget = null
            schedulingTask = freshTask
          },
          onMoveToList = {
            contextMenuTarget = null
            movingTask = freshTask
          },
          onDelete = {
            pendingDeleteTaskId = freshTask.id
          },
        )
      }
    }

    // SETTINGS SCREEN OVERLAY (Opaque slide right/left with no fade transparency)
    AnimatedVisibility(
      visible = uiState.isSettingsOpen,
      enter = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
      ),
      exit = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
      ),
    ) {
      SettingsScreen(
        settings = uiState.settings,
        hasCalendarPermissions = uiState.hasCalendarPermissions,
        writableCalendars = uiState.writableCalendars,
        effectiveCalendarId = uiState.effectiveCalendarId,
        onClose = viewModel::closeSettings,
        onToggleOpenDirectly = viewModel::setOpenDirectlyIntoActiveList,
        onToggleTodaysEventsMode = viewModel::setUseTodaysCalendarAsLaunchList,
        onSelectTargetCalendar = viewModel::setTargetSyncCalendar,
        onToggleBackdropBlur = viewModel::setBackdropBlurEnabled,
        onToggleOledDarkMode = viewModel::setOledDarkModeEnabled,
        onSelectColorTheme = viewModel::setAppColorTheme,
        onCalendarPermissionsResult = viewModel::refreshCalendarPermissionsAndSync,
      )
    }

    editingTask?.let { target ->
      EditTaskDialog(
        initialTitle = target.title,
        onDismiss = { editingTask = null },
        onConfirm = { newTitle ->
          viewModel.updateTaskTitle(target, newTitle)
          editingTask = null
        },
      )
    }

    schedulingTask?.let { target ->
      ScheduleDateTimeDialog(
        initialMillis = target.scheduledStartMillis,
        title = "Assign Date & Time",
        onDismiss = { schedulingTask = null },
        onConfirm = { newStartMillis ->
          viewModel.scheduleTaskAndSync(target, newStartMillis)
          schedulingTask = null
        },
      )
    }

    movingTask?.let { target ->
      MoveToListDialog(
        lists = uiState.lists,
        currentListId = target.listId,
        onDismiss = { movingTask = null },
        onSelectList = { destListId ->
          viewModel.moveTaskToList(target, destListId)
          movingTask = null
        },
      )
    }

    deleteListTarget?.let { target ->
      DeleteListConfirmationDialog(
        list = target,
        onDismiss = { deleteListTarget = null },
        onConfirmDelete = {
          viewModel.deleteList(target.id)
          deleteListTarget = null
        },
      )
    }

    // Undo / Feedback Snackbar cleanly elevated above the Floating Bottom Nav Pill & Add Item Bar
    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .imePadding()
        .padding(start = 20.dp, end = 20.dp, bottom = 96.dp)
        .widthIn(max = 560.dp)
        .testTag("slate_snackbar_host"),
    ) { snackbarData ->
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        shadowElevation = 10.dp,
        tonalElevation = 4.dp,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("slate_snackbar"),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = snackbarData.visuals.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          snackbarData.visuals.actionLabel?.let { actionLabel ->
            TextButton(
              onClick = { snackbarData.performAction() },
              colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.inversePrimary,
              ),
              modifier = Modifier.testTag("snackbar_action_button"),
            ) {
              Text(
                text = actionLabel,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }
  }
}
