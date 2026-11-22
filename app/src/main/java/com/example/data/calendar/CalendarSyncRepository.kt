package com.example.data.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

@Immutable
data class DeviceCalendar(
  val id: Long,
  val displayName: String,
  val accountName: String,
  val accountType: String,
  val ownerAccount: String,
  val color: Int,
  val isPrimary: Boolean,
  val isSamsungCalendar: Boolean,
)

@Immutable
data class DeviceCalendarEvent(
  val eventId: Long,
  val calendarId: Long,
  val title: String,
  val description: String,
  val startMillis: Long,
  val endMillis: Long,
  val allDay: Boolean,
  val color: Int,
)

class CalendarSyncRepository(private val context: Context) {

  fun hasReadPermission(): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
      PackageManager.PERMISSION_GRANTED

  fun hasWritePermission(): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
      PackageManager.PERMISSION_GRANTED

  fun hasCalendarPermissions(): Boolean = hasReadPermission() && hasWritePermission()

  fun observeCalendarChanges(): Flow<Unit> = callbackFlow {
    if (!hasReadPermission()) {
      close()
      return@callbackFlow
    }
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
      override fun onChange(selfChange: Boolean) {
        trySend(Unit)
      }
    }
    try {
      context.contentResolver.registerContentObserver(
        CalendarContract.Events.CONTENT_URI,
        true,
        observer,
      )
      context.contentResolver.registerContentObserver(
        CalendarContract.Instances.CONTENT_URI,
        true,
        observer,
      )
    } catch (_: SecurityException) {
      close()
      return@callbackFlow
    }
    awaitClose {
      runCatching { context.contentResolver.unregisterContentObserver(observer) }
    }
  }.flowOn(Dispatchers.IO)

  suspend fun getWritableCalendars(): List<DeviceCalendar> = withContext(Dispatchers.IO) {
    if (!hasReadPermission()) return@withContext emptyList()
    val projection = arrayOf(
      CalendarContract.Calendars._ID,
      CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
      CalendarContract.Calendars.ACCOUNT_NAME,
      CalendarContract.Calendars.ACCOUNT_TYPE,
      CalendarContract.Calendars.OWNER_ACCOUNT,
      CalendarContract.Calendars.CALENDAR_COLOR,
      CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
      CalendarContract.Calendars.IS_PRIMARY,
      CalendarContract.Calendars.VISIBLE,
    )
    val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
    val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())

    val result = mutableListOf<DeviceCalendar>()
    try {
      context.contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC",
      )?.use { cursor ->
        val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
        val nameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
        val accNameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
        val accTypeIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
        val ownerIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.OWNER_ACCOUNT)
        val colorIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
        val primaryIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)

        while (cursor.moveToNext()) {
          val id = cursor.getLong(idIdx)
          val displayName = cursor.getString(nameIdx)?.takeIf { it.isNotBlank() } ?: "Calendar #$id"
          val accountName = cursor.getString(accNameIdx) ?: ""
          val accountType = cursor.getString(accTypeIdx) ?: ""
          val ownerAccount = cursor.getString(ownerIdx) ?: ""
          val color = cursor.getInt(colorIdx)
          val isPrimary = cursor.getInt(primaryIdx) == 1
          val isSamsung = isSamsungAccountOrCalendar(displayName, accountName, accountType, ownerAccount)
          result += DeviceCalendar(
            id = id,
            displayName = displayName,
            accountName = accountName,
            accountType = accountType,
            ownerAccount = ownerAccount,
            color = color,
            isPrimary = isPrimary,
            isSamsungCalendar = isSamsung,
          )
        }
      }
    } catch (_: SecurityException) {
      return@withContext emptyList()
    }
    result.sortedWith(
      compareByDescending<DeviceCalendar> { it.isSamsungCalendar }
        .thenByDescending { it.isPrimary }
        .thenBy { it.displayName.lowercase() }
    )
  }

  private fun isSamsungAccountOrCalendar(
    displayName: String,
    accountName: String,
    accountType: String,
    ownerAccount: String,
  ): Boolean {
    val combined = "$displayName $accountName $accountType $ownerAccount".lowercase()
    return combined.contains("samsung") ||
      combined.contains("sec.") ||
      combined.contains("my calendar") ||
      accountType.equals(CalendarContract.ACCOUNT_TYPE_LOCAL, ignoreCase = true)
  }

  /**
   * Queries events across ALL visible device calendars (or a specific calendar if provided)
   * combining CalendarContract.Instances (for recurring & expanded events) and direct
   * CalendarContract.Events rows (in case a newly added event hasn't expanded into Instances yet).
   */
  suspend fun queryEventsInRange(
    startMillis: Long,
    endMillis: Long,
    calendarId: Long? = null,
  ): List<DeviceCalendarEvent> = withContext(Dispatchers.IO) {
    if (!hasReadPermission()) return@withContext emptyList()

    val eventsByKey = linkedMapOf<Pair<Long, Long>, DeviceCalendarEvent>()

    // 1. Query CalendarContract.Instances (handles recurring events + standard events)
    val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(builder, startMillis)
    ContentUris.appendId(builder, endMillis)

    val instanceProjection = arrayOf(
      CalendarContract.Instances.EVENT_ID,
      CalendarContract.Instances.CALENDAR_ID,
      CalendarContract.Instances.TITLE,
      CalendarContract.Instances.DESCRIPTION,
      CalendarContract.Instances.BEGIN,
      CalendarContract.Instances.END,
      CalendarContract.Instances.ALL_DAY,
      CalendarContract.Instances.DISPLAY_COLOR,
    )
    val instanceSelection = buildString {
      append("${CalendarContract.Instances.VISIBLE} = 1")
      if (calendarId != null) {
        append(" AND ${CalendarContract.Instances.CALENDAR_ID} = ?")
      }
    }
    val instanceArgs = calendarId?.let { arrayOf(it.toString()) }

    try {
      context.contentResolver.query(
        builder.build(),
        instanceProjection,
        instanceSelection,
        instanceArgs,
        "${CalendarContract.Instances.BEGIN} ASC",
      )?.use { cursor ->
        val eventIdIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
        val calIdIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
        val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
        val descIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
        val beginIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
        val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
        val allDayIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
        val colorIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.DISPLAY_COLOR)

        while (cursor.moveToNext()) {
          val title = cursor.getString(titleIdx)?.trim().orEmpty()
          if (title.isEmpty()) continue
          val eventId = cursor.getLong(eventIdIdx)
          val begin = cursor.getLong(beginIdx)
          val rawEnd = cursor.getLong(endIdx)
          eventsByKey[eventId to begin] = DeviceCalendarEvent(
            eventId = eventId,
            calendarId = cursor.getLong(calIdIdx),
            title = title,
            description = cursor.getString(descIdx) ?: "",
            startMillis = begin,
            endMillis = if (rawEnd > begin) rawEnd else begin + 3_600_000L,
            allDay = cursor.getInt(allDayIdx) == 1,
            color = cursor.getInt(colorIdx),
          )
        }
      }
    } catch (_: SecurityException) {
      return@withContext emptyList()
    }

    // 2. Also query CalendarContract.Events directly for non-recurring events not yet in Instances
    val eventProjection = arrayOf(
      CalendarContract.Events._ID,
      CalendarContract.Events.CALENDAR_ID,
      CalendarContract.Events.TITLE,
      CalendarContract.Events.DESCRIPTION,
      CalendarContract.Events.DTSTART,
      CalendarContract.Events.DTEND,
      CalendarContract.Events.ALL_DAY,
      CalendarContract.Events.DISPLAY_COLOR,
      CalendarContract.Events.RRULE,
    )
    val eventSelection = buildString {
      append("${CalendarContract.Events.DELETED} = 0")
      append(" AND ${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?")
      if (calendarId != null) {
        append(" AND ${CalendarContract.Events.CALENDAR_ID} = ?")
      }
    }
    val eventArgs = if (calendarId != null) {
      arrayOf(startMillis.toString(), endMillis.toString(), calendarId.toString())
    } else {
      arrayOf(startMillis.toString(), endMillis.toString())
    }

    try {
      context.contentResolver.query(
        CalendarContract.Events.CONTENT_URI,
        eventProjection,
        eventSelection,
        eventArgs,
        "${CalendarContract.Events.DTSTART} ASC",
      )?.use { cursor ->
        val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
        val calIdIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID)
        val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
        val descIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
        val startIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
        val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
        val allDayIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
        val colorIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DISPLAY_COLOR)
        val rruleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.RRULE)

        while (cursor.moveToNext()) {
          val rrule = cursor.getString(rruleIdx)
          if (!rrule.isNullOrBlank()) continue // Recurring events are handled by Instances
          val title = cursor.getString(titleIdx)?.trim().orEmpty()
          if (title.isEmpty()) continue
          val eventId = cursor.getLong(idIdx)
          val start = cursor.getLong(startIdx)
          if (eventsByKey.keys.any { it.first == eventId }) continue
          val rawEnd = cursor.getLong(endIdx)
          eventsByKey[eventId to start] = DeviceCalendarEvent(
            eventId = eventId,
            calendarId = cursor.getLong(calIdIdx),
            title = title,
            description = cursor.getString(descIdx) ?: "",
            startMillis = start,
            endMillis = if (rawEnd > start) rawEnd else start + 3_600_000L,
            allDay = cursor.getInt(allDayIdx) == 1,
            color = cursor.getInt(colorIdx),
          )
        }
      }
    } catch (_: SecurityException) {
      // Ignore if denied mid-flight
    }

    eventsByKey.values.sortedBy { it.startMillis }
  }

  suspend fun queryTodaysEvents(calendarId: Long? = null): List<DeviceCalendarEvent> =
    withContext(Dispatchers.IO) {
      val (start, end) = dayBoundsMillis(System.currentTimeMillis())
      queryEventsInRange(start, end, calendarId)
    }

  suspend fun getEventById(eventId: Long): DeviceCalendarEvent? = withContext(Dispatchers.IO) {
    if (!hasReadPermission()) return@withContext null
    val projection = arrayOf(
      CalendarContract.Events._ID,
      CalendarContract.Events.CALENDAR_ID,
      CalendarContract.Events.TITLE,
      CalendarContract.Events.DESCRIPTION,
      CalendarContract.Events.DTSTART,
      CalendarContract.Events.DTEND,
      CalendarContract.Events.ALL_DAY,
      CalendarContract.Events.DISPLAY_COLOR,
    )
    try {
      context.contentResolver.query(
        ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
        projection,
        "${CalendarContract.Events.DELETED} = 0",
        null,
        null,
      )?.use { cursor ->
        if (cursor.moveToFirst()) {
          val calId = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID))
          val title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)) ?: ""
          val desc = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)) ?: ""
          val start = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
          val end = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
          val allDay = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)) == 1
          val color = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Events.DISPLAY_COLOR))
          return@withContext DeviceCalendarEvent(
            eventId = eventId,
            calendarId = calId,
            title = title,
            description = desc,
            startMillis = start,
            endMillis = if (end > start) end else start + 3_600_000L,
            allDay = allDay,
            color = color,
          )
        }
      }
    } catch (_: SecurityException) {
      return@withContext null
    }
    null
  }

  suspend fun insertEvent(
    calendarId: Long,
    title: String,
    startMillis: Long,
    endMillis: Long = startMillis + 3_600_000L,
  ): Long? = withContext(Dispatchers.IO) {
    if (!hasWritePermission()) return@withContext null
    val safeEnd = if (endMillis > startMillis) endMillis else startMillis + 3_600_000L
    val values = ContentValues().apply {
      put(CalendarContract.Events.CALENDAR_ID, calendarId)
      put(CalendarContract.Events.TITLE, title)
      put(CalendarContract.Events.DESCRIPTION, "Synced from Slate")
      put(CalendarContract.Events.DTSTART, startMillis)
      put(CalendarContract.Events.DTEND, safeEnd)
      put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
    }
    try {
      val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
      uri?.let { ContentUris.parseId(it) }
    } catch (_: SecurityException) {
      null
    } catch (_: IllegalArgumentException) {
      null
    }
  }

  suspend fun updateEvent(
    eventId: Long,
    title: String,
    startMillis: Long,
    endMillis: Long = startMillis + 3_600_000L,
  ): Boolean = withContext(Dispatchers.IO) {
    if (!hasWritePermission()) return@withContext false
    val safeEnd = if (endMillis > startMillis) endMillis else startMillis + 3_600_000L
    val values = ContentValues().apply {
      put(CalendarContract.Events.TITLE, title)
      put(CalendarContract.Events.DTSTART, startMillis)
      put(CalendarContract.Events.DTEND, safeEnd)
      put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
    }
    try {
      val updated = context.contentResolver.update(
        ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
        values,
        null,
        null,
      )
      updated > 0
    } catch (_: SecurityException) {
      false
    }
  }

  suspend fun deleteEvent(eventId: Long): Boolean = withContext(Dispatchers.IO) {
    if (!hasWritePermission()) return@withContext false
    try {
      val deleted = context.contentResolver.delete(
        ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
        null,
        null,
      )
      deleted > 0
    } catch (_: SecurityException) {
      false
    }
  }

  companion object {
    fun dayBoundsMillis(epochMillis: Long): Pair<Long, Long> {
      val cal = Calendar.getInstance().apply {
        timeInMillis = epochMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
      val start = cal.timeInMillis
      cal.add(Calendar.DAY_OF_YEAR, 1)
      val end = cal.timeInMillis - 1L
      return start to end
    }
  }
}
