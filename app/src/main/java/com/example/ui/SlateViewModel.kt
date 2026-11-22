package com.example.ui

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.calendar.CalendarSyncRepository
import com.example.data.calendar.DeviceCalendar
import com.example.data.calendar.DeviceCalendarEvent
import com.example.data.local.SlateDatabase
import com.example.data.local.SlateListWithCounts
import com.example.data.local.SlateTaskEntity
import com.example.data.preferences.AppColorTheme
import com.example.data.preferences.SettingsRepository
import com.example.data.preferences.SlateSettings
import com.example.data.repository.SlateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class SearchFilter(val label: String) {
  ALL("All"),
  STARRED("Starred"),
  SCHEDULED("Scheduled"),
  COMPLETED("Completed"),
}

enum class CalendarViewMode(val label: String) {
  MONTH("Month"),
  WEEK("Week"),
}

@Immutable
sealed interface SlateSnackbarEvent {
  data class TaskDeleted(val task: SlateTaskEntity) : SlateSnackbarEvent
  data class Message(val text: String) : SlateSnackbarEvent
}

@Immutable
data class SlateUiState(
  val isReady: Boolean = false,
  val settings: SlateSettings = SlateSettings(),
  val isSheetOpen: Boolean = true,
  val isSettingsOpen: Boolean = false,
  val workspacePage: Int = 0,
  val previousNonSearchPage: Int = 0,
  val lists: List<SlateListWithCounts> = emptyList(),
  val activeListId: Long = 1L,
  val activeListTasks: List<SlateTaskEntity> = emptyList(),
  val allTasks: List<SlateTaskEntity> = emptyList(),
  val hasCalendarPermissions: Boolean = false,
  val writableCalendars: List<DeviceCalendar> = emptyList(),
  val syncedEvents: List<DeviceCalendarEvent> = emptyList(),
  val todaysEvents: List<DeviceCalendarEvent> = emptyList(),
  val selectedDateMillis: Long = System.currentTimeMillis(),
  val calendarViewMode: CalendarViewMode = CalendarViewMode.MONTH,
  val searchQuery: String = "",
  val searchFilter: SearchFilter = SearchFilter.ALL,
  val isSyncingCalendar: Boolean = false,
  val lastSyncedMillis: Long? = null,
  val recentlyAddedTaskId: Long? = null,
) {
  val activeList: SlateListWithCounts?
    get() = lists.firstOrNull { it.id == activeListId } ?: lists.firstOrNull()

  val effectiveCalendarId: Long?
    get() = settings.targetSyncCalendarId
      ?.takeIf { id -> writableCalendars.any { it.id == id } }
      ?: writableCalendars.firstOrNull()?.id
}

class SlateViewModel(
  application: Application,
  private val repository: SlateRepository,
) : AndroidViewModel(application) {

  private val _isReady = MutableStateFlow(false)
  private val _isSheetOpen = MutableStateFlow(true)
  private val _isSettingsOpen = MutableStateFlow(false)
  private val _workspacePage = MutableStateFlow(0)
  private val _previousNonSearchPage = MutableStateFlow(0)
  private val _activeListId = MutableStateFlow(1L)
  private val _hasCalendarPermissions = MutableStateFlow(repository.calendarSyncRepository.hasCalendarPermissions())
  private val _writableCalendars = MutableStateFlow<List<DeviceCalendar>>(emptyList())
  private val _syncedEvents = MutableStateFlow<List<DeviceCalendarEvent>>(emptyList())
  private val _todaysEvents = MutableStateFlow<List<DeviceCalendarEvent>>(emptyList())
  private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
  private val _calendarViewMode = MutableStateFlow(CalendarViewMode.MONTH)
  private val _searchQuery = MutableStateFlow("")
  private val _searchFilter = MutableStateFlow(SearchFilter.ALL)
  private val _isSyncingCalendar = MutableStateFlow(false)
  private val _lastSyncedMillis = MutableStateFlow<Long?>(null)
  private val _recentlyAddedTaskId = MutableStateFlow<Long?>(null)
  private var recentlyAddedClearJob: Job? = null

  private val _snackbarEvents = MutableSharedFlow<SlateSnackbarEvent>()
  val snackbarEvents: SharedFlow<SlateSnackbarEvent> = _snackbarEvents.asSharedFlow()

  val themePrefsFlow: StateFlow<Pair<Boolean, AppColorTheme>> = repository.settingsFlow
    .map { it.oledDarkModeEnabled to it.appColorTheme }
    .distinctUntilChanged()
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = false to AppColorTheme.WALLPAPER,
    )

  private var calendarObserverJob: Job? = null
  private var calendarSyncJob: Job? = null
  private var lastSyncCompletedAtMillis: Long = 0L

  @Suppress("OPT_IN_USAGE")
  private val activeListTasksFlow = _activeListId.flatMapLatest { id ->
    repository.observeTasksForList(id)
  }

  private val coreStateFlow = combine(
    _isReady,
    repository.settingsFlow,
    _isSheetOpen,
    _isSettingsOpen,
    _workspacePage,
    repository.listsWithCountsFlow,
    _activeListId,
    activeListTasksFlow,
    repository.allTasksFlow,
  ) { values ->
    CoreSlice(
      isReady = values[0] as Boolean,
      settings = values[1] as SlateSettings,
      isSheetOpen = values[2] as Boolean,
      isSettingsOpen = values[3] as Boolean,
      workspacePage = values[4] as Int,
      lists = @Suppress("UNCHECKED_CAST") (values[5] as List<SlateListWithCounts>),
      activeListId = values[6] as Long,
      activeListTasks = @Suppress("UNCHECKED_CAST") (values[7] as List<SlateTaskEntity>),
      allTasks = @Suppress("UNCHECKED_CAST") (values[8] as List<SlateTaskEntity>),
    )
  }

  private val calendarAndSearchSliceFlow = combine(
    _hasCalendarPermissions,
    _writableCalendars,
    _syncedEvents,
    _todaysEvents,
    _selectedDateMillis,
    _calendarViewMode,
    _searchQuery,
    _searchFilter,
    _isSyncingCalendar,
    _lastSyncedMillis,
  ) { values ->
    CalendarSearchSlice(
      hasCalendarPermissions = values[0] as Boolean,
      writableCalendars = @Suppress("UNCHECKED_CAST") (values[1] as List<DeviceCalendar>),
      syncedEvents = @Suppress("UNCHECKED_CAST") (values[2] as List<DeviceCalendarEvent>),
      todaysEvents = @Suppress("UNCHECKED_CAST") (values[3] as List<DeviceCalendarEvent>),
      selectedDateMillis = values[4] as Long,
      calendarViewMode = values[5] as CalendarViewMode,
      searchQuery = values[6] as String,
      searchFilter = values[7] as SearchFilter,
      isSyncingCalendar = values[8] as Boolean,
      lastSyncedMillis = values[9] as Long?,
    )
  }

  val uiState: StateFlow<SlateUiState> = combine(
    coreStateFlow,
    calendarAndSearchSliceFlow,
    _recentlyAddedTaskId,
  ) { core, cal, recentlyAddedTaskId ->
    val resolvedListId = if (core.lists.any { it.id == core.activeListId }) {
      core.activeListId
    } else {
      core.lists.firstOrNull()?.id ?: 1L
    }
    SlateUiState(
      isReady = core.isReady,
      settings = core.settings,
      isSheetOpen = core.isSheetOpen,
      isSettingsOpen = core.isSettingsOpen,
      workspacePage = core.workspacePage,
      previousNonSearchPage = _previousNonSearchPage.value,
      lists = core.lists,
      activeListId = resolvedListId,
      activeListTasks = core.activeListTasks,
      allTasks = core.allTasks,
      hasCalendarPermissions = cal.hasCalendarPermissions,
      writableCalendars = cal.writableCalendars,
      syncedEvents = cal.syncedEvents,
      todaysEvents = cal.todaysEvents,
      selectedDateMillis = cal.selectedDateMillis,
      calendarViewMode = cal.calendarViewMode,
      searchQuery = cal.searchQuery,
      searchFilter = cal.searchFilter,
      isSyncingCalendar = cal.isSyncingCalendar,
      lastSyncedMillis = cal.lastSyncedMillis,
      recentlyAddedTaskId = recentlyAddedTaskId,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = SlateUiState(),
  )

  init {
    viewModelScope.launch {
      val ensuredId = repository.ensureDefaultListExists()
      val initialSettings = repository.settingsFlow.first()
      val lists = repository.listsWithCountsFlow.first()
      val initialListId = if (lists.any { it.id == initialSettings.defaultLaunchListId }) {
        initialSettings.defaultLaunchListId
      } else {
        ensuredId
      }
      _activeListId.value = initialListId
      _isSheetOpen.value = initialSettings.openDirectlyIntoActiveList
      _isReady.value = true

      refreshCalendarPermissionsAndSync()
    }
  }

  fun refreshCalendarPermissionsAndSync() {
    val hasPerms = repository.calendarSyncRepository.hasCalendarPermissions()
    _hasCalendarPermissions.value = hasPerms
    if (hasPerms) {
      startObservingDeviceCalendar()
      val now = System.currentTimeMillis()
      if (calendarSyncJob?.isActive != true && now - lastSyncCompletedAtMillis > 1500L) {
        syncCalendarNow()
      }
    } else {
      calendarObserverJob?.cancel()
      calendarSyncJob?.cancel()
      _writableCalendars.value = emptyList()
      _syncedEvents.value = emptyList()
      _todaysEvents.value = emptyList()
    }
  }

  private fun startObservingDeviceCalendar() {
    if (calendarObserverJob?.isActive == true) return
    calendarObserverJob = viewModelScope.launch {
      repository.calendarSyncRepository.observeCalendarChanges().collect {
        syncCalendarNow()
      }
    }
  }

  fun syncCalendarNow() {
    if (!repository.calendarSyncRepository.hasReadPermission()) return
    if (calendarSyncJob?.isActive == true) return
    calendarSyncJob = viewModelScope.launch {
      _isSyncingCalendar.value = true
      try {
        val settings = repository.settingsFlow.first()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        val rangeStart = CalendarSyncRepository.dayBoundsMillis(cal.timeInMillis).first
        cal.add(Calendar.DAY_OF_YEAR, 120)
        val rangeEnd = CalendarSyncRepository.dayBoundsMillis(cal.timeInMillis).second

        val result = repository.performTwoWayCalendarSync(
          selectedCalendarId = settings.targetSyncCalendarId,
          rangeStartMillis = rangeStart,
          rangeEndMillis = rangeEnd,
        )
        _writableCalendars.value = result.calendars
        _syncedEvents.value = result.events
        if (settings.targetSyncCalendarId == null && result.resolvedCalendarId != null) {
          repository.settingsRepository.setTargetSyncCalendarId(result.resolvedCalendarId)
        }
        _todaysEvents.value = repository.calendarSyncRepository.queryTodaysEvents(null)
        val doneAt = System.currentTimeMillis()
        lastSyncCompletedAtMillis = doneAt
        _lastSyncedMillis.value = doneAt
      } finally {
        _isSyncingCalendar.value = false
      }
    }
  }

  // Layer Navigation
  fun openSheet() {
    _isSheetOpen.value = true
  }

  fun collapseSheet() {
    _isSheetOpen.value = false
  }

  fun openSettings() {
    _isSettingsOpen.value = true
  }

  fun closeSettings() {
    _isSettingsOpen.value = false
  }

  fun setWorkspacePage(page: Int) {
    val clamped = page.coerceIn(0, 2)
    if (clamped != 2) {
      _previousNonSearchPage.value = clamped
    }
    _workspacePage.value = clamped
  }

  fun navigateBackFromSearch() {
    _workspacePage.value = _previousNonSearchPage.value.coerceIn(0, 1)
  }

  fun selectActiveList(listId: Long, openSheetImmediately: Boolean = true) {
    _activeListId.value = listId
    if (openSheetImmediately) {
      _isSheetOpen.value = true
    }
  }

  // List CRUD
  fun createList(name: String, colorTagHex: Long) {
    viewModelScope.launch {
      val newId = repository.createList(name, colorTagHex)
      _activeListId.value = newId
    }
  }

  fun updateList(listId: Long, name: String, colorTagHex: Long) {
    viewModelScope.launch {
      repository.updateList(listId, name, colorTagHex)
    }
  }

  fun deleteList(listId: Long) {
    viewModelScope.launch {
      repository.deleteList(listId)
    }
  }

  fun setDefaultLaunchList(listId: Long) {
    viewModelScope.launch {
      repository.settingsRepository.setDefaultLaunchListId(listId)
      _activeListId.value = listId
    }
  }

  fun persistReorderedLists(orderedIds: List<Long>) {
    viewModelScope.launch {
      repository.persistListOrder(orderedIds)
    }
  }

  // Task CRUD & Gestures
  private fun markRecentlyAddedTask(taskId: Long) {
    if (taskId <= 0L) return
    recentlyAddedClearJob?.cancel()
    _recentlyAddedTaskId.value = taskId
    recentlyAddedClearJob = viewModelScope.launch {
      kotlinx.coroutines.delay(950L)
      if (_recentlyAddedTaskId.value == taskId) {
        _recentlyAddedTaskId.value = null
      }
    }
  }

  fun addTask(title: String, scheduledStartMillis: Long? = null, syncToCalendar: Boolean = false) {
    viewModelScope.launch {
      val targetListId = uiState.value.activeListId
      val calId = if (syncToCalendar || scheduledStartMillis != null) {
        uiState.value.effectiveCalendarId
      } else {
        null
      }
      val effectiveStart = if (uiState.value.settings.useTodaysCalendarAsLaunchList && scheduledStartMillis == null) {
        System.currentTimeMillis() + 3_600_000L
      } else {
        scheduledStartMillis
      }
      val newId = repository.addTask(
        listId = targetListId,
        title = title,
        scheduledStartMillis = effectiveStart,
        syncToCalendarId = if (effectiveStart != null) calId else null,
      )
      markRecentlyAddedTask(newId)
      if (effectiveStart != null && calId != null) {
        syncCalendarNow()
      }
    }
  }

  fun addCalendarScheduledTask(title: String, startMillis: Long) {
    viewModelScope.launch {
      val targetListId = uiState.value.activeListId
      val calId = uiState.value.effectiveCalendarId
      val newId = repository.addTask(
        listId = targetListId,
        title = title,
        scheduledStartMillis = startMillis,
        syncToCalendarId = calId,
      )
      markRecentlyAddedTask(newId)
      if (calId != null) {
        syncCalendarNow()
      }
    }
  }

  fun toggleTaskCompleted(task: SlateTaskEntity) {
    viewModelScope.launch {
      repository.toggleTaskCompleted(task)
    }
  }

  fun toggleTaskStarred(task: SlateTaskEntity) {
    viewModelScope.launch {
      repository.toggleTaskStarred(task)
    }
  }

  fun updateTaskTitle(task: SlateTaskEntity, newTitle: String) {
    viewModelScope.launch {
      repository.updateTaskTitle(task, newTitle)
      if (task.calendarEventId != null) {
        syncCalendarNow()
      }
    }
  }

  fun scheduleTaskAndSync(task: SlateTaskEntity, startMillis: Long) {
    viewModelScope.launch {
      val calId = uiState.value.effectiveCalendarId
      repository.scheduleTaskAndSync(task, startMillis, calId)
      if (calId != null && repository.calendarSyncRepository.hasWritePermission()) {
        syncCalendarNow()
        _snackbarEvents.emit(SlateSnackbarEvent.Message("Synced to calendar"))
      } else {
        _snackbarEvents.emit(SlateSnackbarEvent.Message("Scheduled locally"))
      }
    }
  }

  fun moveTaskToList(task: SlateTaskEntity, targetListId: Long) {
    viewModelScope.launch {
      repository.moveTaskToList(task, targetListId)
      val targetName = uiState.value.lists.firstOrNull { it.id == targetListId }?.name ?: "list"
      _snackbarEvents.emit(SlateSnackbarEvent.Message("Moved to $targetName"))
    }
  }

  fun deleteTaskWithUndo(task: SlateTaskEntity) {
    viewModelScope.launch {
      repository.deleteTask(task, deleteLinkedCalendarEvent = true)
      _snackbarEvents.emit(SlateSnackbarEvent.TaskDeleted(task))
      if (task.calendarEventId != null) {
        syncCalendarNow()
      }
    }
  }

  fun undoDeleteTask(task: SlateTaskEntity) {
    viewModelScope.launch {
      repository.restoreTask(task)
      if (task.calendarEventId != null) {
        syncCalendarNow()
      }
    }
  }

  fun persistReorderedTasks(orderedIds: List<Long>) {
    viewModelScope.launch {
      repository.persistTaskOrder(orderedIds)
    }
  }

  fun importEventToActiveList(event: DeviceCalendarEvent) {
    viewModelScope.launch {
      val newId = repository.importCalendarEventAsTask(event, uiState.value.activeListId)
      markRecentlyAddedTask(newId)
      _snackbarEvents.emit(SlateSnackbarEvent.Message("Added \"${event.title}\" to ${uiState.value.activeList?.name ?: "List"}"))
    }
  }

  // Calendar & Search state
  fun selectDate(epochMillis: Long) {
    _selectedDateMillis.value = epochMillis
  }

  fun setCalendarViewMode(mode: CalendarViewMode) {
    _calendarViewMode.value = mode
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setSearchFilter(filter: SearchFilter) {
    _searchFilter.value = filter
  }

  // Settings Toggles
  fun setOpenDirectlyIntoActiveList(enabled: Boolean) {
    viewModelScope.launch {
      repository.settingsRepository.setOpenDirectlyIntoActiveList(enabled)
    }
  }

  fun setUseTodaysCalendarAsLaunchList(enabled: Boolean) {
    viewModelScope.launch {
      repository.settingsRepository.setUseTodaysCalendarAsLaunchList(enabled)
      if (enabled && repository.calendarSyncRepository.hasReadPermission()) {
        syncCalendarNow()
      }
    }
  }

  fun setTargetSyncCalendar(calendarId: Long) {
    viewModelScope.launch {
      repository.settingsRepository.setTargetSyncCalendarId(calendarId)
      syncCalendarNow()
    }
  }

  fun setBackdropBlurEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.settingsRepository.setBackdropBlurEnabled(enabled)
    }
  }

  fun setOledDarkModeEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.settingsRepository.setOledDarkModeEnabled(enabled)
    }
  }

  fun setAppColorTheme(theme: AppColorTheme) {
    viewModelScope.launch {
      repository.settingsRepository.setAppColorTheme(theme)
    }
  }

  private data class CoreSlice(
    val isReady: Boolean,
    val settings: SlateSettings,
    val isSheetOpen: Boolean,
    val isSettingsOpen: Boolean,
    val workspacePage: Int,
    val lists: List<SlateListWithCounts>,
    val activeListId: Long,
    val activeListTasks: List<SlateTaskEntity>,
    val allTasks: List<SlateTaskEntity>,
  )

  private data class CalendarSearchSlice(
    val hasCalendarPermissions: Boolean,
    val writableCalendars: List<DeviceCalendar>,
    val syncedEvents: List<DeviceCalendarEvent>,
    val todaysEvents: List<DeviceCalendarEvent>,
    val selectedDateMillis: Long,
    val calendarViewMode: CalendarViewMode,
    val searchQuery: String,
    val searchFilter: SearchFilter,
    val isSyncingCalendar: Boolean,
    val lastSyncedMillis: Long?,
  )

  companion object {
    fun factory(application: Application): ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          val db = SlateDatabase.getInstance(application)
          val repo = SlateRepository(
            listDao = db.listDao(),
            taskDao = db.taskDao(),
            settingsRepository = SettingsRepository(application),
            calendarSyncRepository = CalendarSyncRepository(application),
          )
          return SlateViewModel(application, repo) as T
        }
      }
  }
}
