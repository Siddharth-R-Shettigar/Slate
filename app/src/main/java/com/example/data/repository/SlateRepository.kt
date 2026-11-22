package com.example.data.repository

import com.example.data.calendar.CalendarSyncRepository
import com.example.data.calendar.DeviceCalendar
import com.example.data.calendar.DeviceCalendarEvent
import com.example.data.local.ListDao
import com.example.data.local.SlateListEntity
import com.example.data.local.SlateListWithCounts
import com.example.data.local.SlateTaskEntity
import com.example.data.local.TaskDao
import com.example.data.preferences.SettingsRepository
import com.example.data.preferences.SlateSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class SlateRepository(
  private val listDao: ListDao,
  private val taskDao: TaskDao,
  val settingsRepository: SettingsRepository,
  val calendarSyncRepository: CalendarSyncRepository,
) {
  val listsWithCountsFlow: Flow<List<SlateListWithCounts>> =
    listDao.observeListsWithCounts().flowOn(Dispatchers.IO)
  val allTasksFlow: Flow<List<SlateTaskEntity>> =
    taskDao.observeAllTasks().flowOn(Dispatchers.IO)
  val scheduledTasksFlow: Flow<List<SlateTaskEntity>> =
    taskDao.observeScheduledTasks().flowOn(Dispatchers.IO)
  val settingsFlow: Flow<SlateSettings> =
    settingsRepository.settingsFlow.flowOn(Dispatchers.IO)

  fun observeTasksForList(listId: Long): Flow<List<SlateTaskEntity>> =
    taskDao.observeTasksForList(listId).flowOn(Dispatchers.IO)

  suspend fun ensureDefaultListExists(): Long = withContext(Dispatchers.IO) {
    val existing = listDao.getAllListsOnce()
    if (existing.isNotEmpty()) {
      return@withContext existing.first().id
    }
    listDao.insertList(SlateListEntity(id = 1L, name = "List", sortOrder = 0))
  }

  suspend fun createList(name: String, colorTagHex: Long): Long = withContext(Dispatchers.IO) {
    val cleanName = name.trim().ifEmpty { "Untitled List" }
    val nextOrder = listDao.getNextSortOrder()
    listDao.insertList(
      SlateListEntity(name = cleanName, colorTagHex = colorTagHex, sortOrder = nextOrder)
    )
  }

  suspend fun updateList(listId: Long, name: String, colorTagHex: Long) = withContext(Dispatchers.IO) {
    val existing = listDao.getListById(listId) ?: return@withContext
    val cleanName = name.trim().ifEmpty { existing.name }
    listDao.updateList(existing.copy(name = cleanName, colorTagHex = colorTagHex))
  }

  suspend fun deleteList(listId: Long) = withContext(Dispatchers.IO) {
    val lists = listDao.getAllListsOnce()
    if (lists.size <= 1) return@withContext
    listDao.deleteListById(listId)
    val settings = settingsRepository.settingsFlow.first()
    if (settings.defaultLaunchListId == listId) {
      val fallback = lists.firstOrNull { it.id != listId }?.id ?: return@withContext
      settingsRepository.setDefaultLaunchListId(fallback)
    }
  }

  suspend fun persistListOrder(orderedIds: List<Long>) = withContext(Dispatchers.IO) {
    listDao.persistListOrder(orderedIds)
  }

  suspend fun addTask(
    listId: Long,
    title: String,
    scheduledStartMillis: Long? = null,
    syncToCalendarId: Long? = null,
  ): Long = withContext(Dispatchers.IO) {
    val cleanTitle = title.trim()
    if (cleanTitle.isEmpty()) return@withContext -1L
    val nextOrder = taskDao.getNextSortOrderForList(listId)
    var calendarEventId: Long? = null
    var actualCalendarId: Long? = null
    val endMillis = scheduledStartMillis?.let { it + 3_600_000L }

    if (scheduledStartMillis != null && syncToCalendarId != null && calendarSyncRepository.hasWritePermission()) {
      calendarEventId = calendarSyncRepository.insertEvent(
        calendarId = syncToCalendarId,
        title = cleanTitle,
        startMillis = scheduledStartMillis,
        endMillis = endMillis ?: (scheduledStartMillis + 3_600_000L),
      )
      if (calendarEventId != null) {
        actualCalendarId = syncToCalendarId
      }
    }

    taskDao.insertTask(
      SlateTaskEntity(
        listId = listId,
        title = cleanTitle,
        scheduledStartMillis = scheduledStartMillis,
        scheduledEndMillis = endMillis,
        calendarEventId = calendarEventId,
        calendarId = actualCalendarId,
        sortOrder = nextOrder,
      )
    )
  }

  suspend fun toggleTaskCompleted(task: SlateTaskEntity) = withContext(Dispatchers.IO) {
    taskDao.updateTask(
      task.copy(
        isCompleted = !task.isCompleted,
        updatedAt = System.currentTimeMillis(),
      )
    )
  }

  suspend fun toggleTaskStarred(task: SlateTaskEntity) = withContext(Dispatchers.IO) {
    taskDao.updateTask(
      task.copy(
        isStarred = !task.isStarred,
        updatedAt = System.currentTimeMillis(),
      )
    )
  }

  suspend fun updateTaskTitle(task: SlateTaskEntity, newTitle: String) = withContext(Dispatchers.IO) {
    val cleanTitle = newTitle.trim()
    if (cleanTitle.isEmpty() || cleanTitle == task.title) return@withContext
    val updated = task.copy(
      title = cleanTitle,
      updatedAt = System.currentTimeMillis(),
    )
    taskDao.updateTask(updated)
    if (task.calendarEventId != null && task.scheduledStartMillis != null && calendarSyncRepository.hasWritePermission()) {
      calendarSyncRepository.updateEvent(
        eventId = task.calendarEventId,
        title = cleanTitle,
        startMillis = task.scheduledStartMillis,
        endMillis = task.scheduledEndMillis ?: (task.scheduledStartMillis + 3_600_000L),
      )
    }
  }

  suspend fun scheduleTaskAndSync(
    task: SlateTaskEntity,
    startMillis: Long,
    targetCalendarId: Long?,
  ) = withContext(Dispatchers.IO) {
    val endMillis = startMillis + 3_600_000L
    var eventId = task.calendarEventId
    val calId = task.calendarId ?: targetCalendarId

    if (calendarSyncRepository.hasWritePermission() && calId != null) {
      if (eventId != null) {
        val updated = calendarSyncRepository.updateEvent(eventId, task.title, startMillis, endMillis)
        if (!updated) {
          eventId = calendarSyncRepository.insertEvent(calId, task.title, startMillis, endMillis)
        }
      } else {
        eventId = calendarSyncRepository.insertEvent(calId, task.title, startMillis, endMillis)
      }
    }

    taskDao.updateTask(
      task.copy(
        scheduledStartMillis = startMillis,
        scheduledEndMillis = endMillis,
        calendarEventId = eventId,
        calendarId = if (eventId != null) calId else task.calendarId,
        updatedAt = System.currentTimeMillis(),
      )
    )
  }

  suspend fun moveTaskToList(task: SlateTaskEntity, targetListId: Long) = withContext(Dispatchers.IO) {
    if (task.listId == targetListId) return@withContext
    val nextOrder = taskDao.getNextSortOrderForList(targetListId)
    taskDao.updateTask(
      task.copy(
        listId = targetListId,
        sortOrder = nextOrder,
        updatedAt = System.currentTimeMillis(),
      )
    )
  }

  suspend fun deleteTask(task: SlateTaskEntity, deleteLinkedCalendarEvent: Boolean = true) = withContext(Dispatchers.IO) {
    taskDao.deleteTaskById(task.id)
    if (deleteLinkedCalendarEvent && task.calendarEventId != null) {
      calendarSyncRepository.deleteEvent(task.calendarEventId)
    }
  }

  suspend fun restoreTask(task: SlateTaskEntity): Long = withContext(Dispatchers.IO) {
    var restoredEventId = task.calendarEventId
    if (task.scheduledStartMillis != null && task.calendarId != null && task.calendarEventId != null) {
      restoredEventId = calendarSyncRepository.insertEvent(
        calendarId = task.calendarId,
        title = task.title,
        startMillis = task.scheduledStartMillis,
        endMillis = task.scheduledEndMillis ?: (task.scheduledStartMillis + 3_600_000L),
      ) ?: task.calendarEventId
    }
    taskDao.insertTask(task.copy(calendarEventId = restoredEventId))
  }

  suspend fun persistTaskOrder(orderedIds: List<Long>) = withContext(Dispatchers.IO) {
    taskDao.persistOrder(orderedIds)
  }

  suspend fun importCalendarEventAsTask(event: DeviceCalendarEvent, listId: Long): Long = withContext(Dispatchers.IO) {
    val existing = taskDao.getLinkedCalendarTasks().firstOrNull { it.calendarEventId == event.eventId }
    if (existing != null) return@withContext existing.id
    val nextOrder = taskDao.getNextSortOrderForList(listId)
    taskDao.insertTask(
      SlateTaskEntity(
        listId = listId,
        title = event.title,
        scheduledStartMillis = event.startMillis,
        scheduledEndMillis = event.endMillis,
        calendarEventId = event.eventId,
        calendarId = event.calendarId,
        sortOrder = nextOrder,
      )
    )
  }

  suspend fun performTwoWayCalendarSync(
    selectedCalendarId: Long?,
    rangeStartMillis: Long,
    rangeEndMillis: Long,
  ): TwoWaySyncResult = withContext(Dispatchers.IO) {
    if (!calendarSyncRepository.hasReadPermission()) {
      return@withContext TwoWaySyncResult(emptyList(), emptyList(), null)
    }
    val calendars = calendarSyncRepository.getWritableCalendars()
    val effectiveCalendarId = selectedCalendarId
      ?.takeIf { id -> calendars.any { it.id == id } }
      ?: calendars.firstOrNull()?.id

    if (effectiveCalendarId != null && calendarSyncRepository.hasWritePermission()) {
      val unlinked = taskDao.getUnlinkedScheduledTasks()
      for (task in unlinked) {
        val start = task.scheduledStartMillis ?: continue
        val end = task.scheduledEndMillis ?: (start + 3_600_000L)
        val eventId = calendarSyncRepository.insertEvent(effectiveCalendarId, task.title, start, end)
        if (eventId != null) {
          taskDao.updateTask(
            task.copy(
              calendarEventId = eventId,
              calendarId = effectiveCalendarId,
              updatedAt = System.currentTimeMillis(),
            )
          )
        }
      }
    }

    val linkedTasks = taskDao.getLinkedCalendarTasks()
    for (task in linkedTasks) {
      val eventId = task.calendarEventId ?: continue
      val remote = calendarSyncRepository.getEventById(eventId)
      if (remote == null) {
        taskDao.updateTask(
          task.copy(
            calendarEventId = null,
            calendarId = null,
            updatedAt = System.currentTimeMillis(),
          )
        )
      } else if (remote.title != task.title || remote.startMillis != task.scheduledStartMillis) {
        taskDao.updateTask(
          task.copy(
            title = remote.title,
            scheduledStartMillis = remote.startMillis,
            scheduledEndMillis = remote.endMillis,
            updatedAt = System.currentTimeMillis(),
          )
        )
      }
    }

    val rangeEvents = calendarSyncRepository.queryEventsInRange(
      startMillis = rangeStartMillis,
      endMillis = rangeEndMillis,
      calendarId = null,
    )
    TwoWaySyncResult(
      calendars = calendars,
      events = rangeEvents,
      resolvedCalendarId = effectiveCalendarId,
    )
  }
}

data class TwoWaySyncResult(
  val calendars: List<DeviceCalendar>,
  val events: List<DeviceCalendarEvent>,
  val resolvedCalendarId: Long?,
)
