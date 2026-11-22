package com.example.data.local

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.ui.theme.ListColorTags

@Immutable
@Entity(tableName = "slate_lists")
data class SlateListEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  @ColumnInfo(name = "color_tag_hex") val colorTagHex: Long = ListColorTags.Sky,
  @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
  @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

@Immutable
@Entity(
  tableName = "slate_tasks",
  foreignKeys = [
    ForeignKey(
      entity = SlateListEntity::class,
      parentColumns = ["id"],
      childColumns = ["list_id"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index("list_id"), Index("calendar_event_id")],
)
data class SlateTaskEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  @ColumnInfo(name = "list_id") val listId: Long,
  val title: String,
  @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
  @ColumnInfo(name = "is_starred") val isStarred: Boolean = false,
  @ColumnInfo(name = "scheduled_start_millis") val scheduledStartMillis: Long? = null,
  @ColumnInfo(name = "scheduled_end_millis") val scheduledEndMillis: Long? = null,
  @ColumnInfo(name = "calendar_event_id") val calendarEventId: Long? = null,
  @ColumnInfo(name = "calendar_id") val calendarId: Long? = null,
  @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
  @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
  @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

@Immutable
data class SlateListWithCounts(
  val id: Long,
  val name: String,
  @ColumnInfo(name = "color_tag_hex") val colorTagHex: Long,
  @ColumnInfo(name = "sort_order") val sortOrder: Int,
  @ColumnInfo(name = "created_at") val createdAt: Long,
  @ColumnInfo(name = "active_count") val activeCount: Int,
  @ColumnInfo(name = "total_count") val totalCount: Int,
)
