package com.example.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SlateTaskEntity
import com.example.ui.components.ActiveListSheet
import com.example.ui.components.BlurredContextMenuOverlay
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
  var schedulingTask by remember { mutableStateOf<SlateTaskEntity?>(null) }
  var movingTask by remember { mutableStateOf<SlateTaskEntity?>(null) }

  LaunchedEffect(viewModel) {
    viewModel.snackbarEvents.collect { event ->
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

    PredictiveBackHandler(enabled = contextMenuTarget != null) { progress ->
      try {
        progress.collect { }
        contextMenuTarget = null
      } catch (e: CancellationException) {
        throw e
      }
    }

    PredictiveBackHandler(
      enabled = uiState.isSheetOpen && !uiState.isSettingsOpen && contextMenuTarget == null
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

    // Derived booleans so animations never trigger unnecessary root recompositions
    val isSheetVisible by remember(collapsedOffsetPx) {
      derivedStateOf {
        val currentOffset = sheetOffsetAnim.value + collapsedOffsetPx * 0.45f * predictiveBackProgress
        currentOffset < collapsedOffsetPx * 0.995f
      }
    }

    // LAYER 2: Main Workspace (pure GPU depth scale without blur RenderEffect overhead)
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
        },
    ) {
      WorkspaceScreen(
        uiState = uiState,
        isLayer2Visible = !uiState.isSheetOpen,
        onWorkspacePageChanged = viewModel::setWorkspacePage,
        onOpenSettings = viewModel::openSettings,
        onPullUpActiveSheet = viewModel::openSheet,
        onSelectAndOpenList = { listId ->
          viewModel.selectActiveList(listId, openSheetImmediately = true)
        },
        onSetDefaultLaunchList = viewModel::setDefaultLaunchList,
        onCreateList = viewModel::createList,
        onUpdateList = viewModel::updateList,
        onDeleteList = viewModel::deleteList,
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
            translationY = rawOffset.coerceAtLeast(-24f)
          },
      ) {
        ActiveListSheet(
          activeList = uiState.activeList,
          tasks = uiState.activeListTasks,
          allTasks = uiState.allTasks,
          todaysEvents = uiState.todaysEvents,
          isTodaysCalendarMode = uiState.settings.useTodaysCalendarAsLaunchList,
          hasCalendarPermissions = uiState.hasCalendarPermissions,
          onSheetDragDelta = { delta ->
            scope.launch {
              val next = (sheetOffsetAnim.value + delta).coerceIn(0f, collapsedOffsetPx)
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
                    dampingRatio = 0.86f,
                    stiffness = 420f,
                  ),
                )
              }
            }
          },
          onCollapseSheet = viewModel::collapseSheet,
          onToggleTaskComplete = viewModel::toggleTaskCompleted,
          onDeleteTask = viewModel::deleteTaskWithUndo,
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

      contextMenuTarget?.let { (selectedTask, cardTopY) ->
        val freshTask = uiState.allTasks.firstOrNull { it.id == selectedTask.id } ?: selectedTask
        BlurredContextMenuOverlay(
          task = freshTask,
          accentColor = listAccentColor,
          cardTopInRootY = cardTopY,
          onDismiss = { contextMenuTarget = null },
          onToggleStar = { viewModel.toggleTaskStarred(freshTask) },
          onAssignDateTime = {
            contextMenuTarget = null
            schedulingTask = freshTask
          },
          onMoveToList = {
            contextMenuTarget = null
            movingTask = freshTask
          },
          onDelete = { viewModel.deleteTaskWithUndo(freshTask) },
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
        onToggleOledDarkMode = viewModel::setOledDarkModeEnabled,
        onSelectColorTheme = viewModel::setAppColorTheme,
        onCalendarPermissionsResult = viewModel::refreshCalendarPermissionsAndSync,
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

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
    )
  }
}
