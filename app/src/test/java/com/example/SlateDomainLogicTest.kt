package com.example

import com.example.data.calendar.CalendarSyncRepository
import com.example.data.local.SlateTaskEntity
import com.example.ui.SearchFilter
import com.example.ui.components.extractMeetingLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SlateDomainLogicTest {

  @Test
  fun dayBoundsCoversExact24HourSpanInLocalCalendar() {
    val cal = Calendar.getInstance().apply {
      set(2026, Calendar.NOVEMBER, 21, 14, 35, 12)
    }
    val (start, end) = CalendarSyncRepository.dayBoundsMillis(cal.timeInMillis)
    assertTrue(end > start)
    assertEquals(86_400_000L - 1L, end - start)

    val startCal = Calendar.getInstance().apply { timeInMillis = start }
    assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
    assertEquals(0, startCal.get(Calendar.MINUTE))
    assertEquals(0, startCal.get(Calendar.SECOND))
  }

  @Test
  fun searchFiltersMatchStarredScheduledAndCompletedProperly() {
    val tasks = listOf(
      SlateTaskEntity(id = 1L, listId = 1L, title = "Buy groceries", isStarred = true),
      SlateTaskEntity(
        id = 2L,
        listId = 1L,
        title = "Team standup",
        scheduledStartMillis = 1_700_000_000_000L,
      ),
      SlateTaskEntity(id = 3L, listId = 1L, title = "Archive receipts", isCompleted = true),
    )

    val starred = tasks.filter { it.isStarred }
    val scheduled = tasks.filter { it.scheduledStartMillis != null }
    val completed = tasks.filter { it.isCompleted }

    assertEquals(listOf(1L), starred.map { it.id })
    assertEquals(listOf(2L), scheduled.map { it.id })
    assertEquals(listOf(3L), completed.map { it.id })
    assertEquals(4, SearchFilter.entries.size)
  }

  @Test
  fun extractMeetingLinkPrioritizesVideoProvidersAndHandlesPlainUrls() {
    val meet = extractMeetingLink(
      description = "Agenda: https://docs.example.com/notes\nJoin at https://meet.google.com/abc-defg-hij.",
      location = "",
    )
    assertNotNull(meet)
    assertEquals("https://meet.google.com/abc-defg-hij", meet!!.url)
    assertEquals("Join Google Meet", meet.providerLabel)
    assertTrue(meet.isVideoMeeting)

    val zoom = extractMeetingLink(
      description = "",
      location = "https://us02web.zoom.us/j/123456789",
    )
    assertNotNull(zoom)
    assertEquals("Join Zoom", zoom!!.providerLabel)
    assertTrue(zoom.isVideoMeeting)

    val teams = extractMeetingLink(
      description = "Join here: https://teams.microsoft.com/l/meetup-join/19%3ameeting",
      location = "Conference Room B",
    )
    assertNotNull(teams)
    assertEquals("Join Teams", teams!!.providerLabel)
    assertTrue(teams.isVideoMeeting)

    val generic = extractMeetingLink(
      description = "See details at https://slate.example.org/events/42)",
      location = "",
    )
    assertNotNull(generic)
    assertEquals("https://slate.example.org/events/42", generic!!.url)
    assertEquals("Open Link", generic.providerLabel)
    assertFalse(generic.isVideoMeeting)

    assertNull(extractMeetingLink(description = "Offline catchup", location = "Lobby"))
  }
}
