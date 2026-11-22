package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {
  @Query(
    """
    SELECT l.id, l.name, l.color_tag_hex, l.sort_order, l.created_at,
      SUM(CASE WHEN t.id IS NOT NULL AND t.is_completed = 0 THEN 1 ELSE 0 END) AS active_count,
      COUNT(t.id) AS total_count
    FROM slate_lists l
    LEFT JOIN slate_tasks t ON t.list_id = l.id
    GROUP BY l.id
    ORDER BY l.sort_order ASC, l.id ASC
    """
  )
  fun observeListsWithCounts(): Flow<List<SlateListWithCounts>>

  @Query("SELECT * FROM slate_lists ORDER BY sort_order ASC, id ASC")
  suspend fun getAllListsOnce(): List<SlateListEntity>

  @Query("SELECT * FROM slate_lists WHERE id = :listId LIMIT 1")
  suspend fun getListById(listId: Long): SlateListEntity?

  @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM slate_lists")
  suspend fun getNextSortOrder(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertList(list: SlateListEntity): Long

  @Update
  suspend fun updateList(list: SlateListEntity)

  @Query("DELETE FROM slate_lists WHERE id = :listId")
  suspend fun deleteListById(listId: Long)

  @Transaction
  suspend fun persistListOrder(orderedListIds: List<Long>) {
    orderedListIds.forEachIndexed { index, id ->
      updateListSortOrder(id, index)
    }
  }

  @Query("UPDATE slate_lists SET sort_order = :sortOrder WHERE id = :listId")
  suspend fun updateListSortOrder(listId: Long, sortOrder: Int)
}

@Dao
interface TaskDao {
  @Query("SELECT * FROM slate_tasks WHERE list_id = :listId ORDER BY sort_order ASC, id ASC")
  fun observeTasksForList(listId: Long): Flow<List<SlateTaskEntity>>

  @Query("SELECT * FROM slate_tasks ORDER BY is_completed ASC, is_starred DESC, sort_order ASC, updated_at DESC")
  fun observeAllTasks(): Flow<List<SlateTaskEntity>>

  @Query("SELECT * FROM slate_tasks WHERE scheduled_start_millis IS NOT NULL ORDER BY scheduled_start_millis ASC")
  fun observeScheduledTasks(): Flow<List<SlateTaskEntity>>

  @Query("SELECT * FROM slate_tasks WHERE id = :taskId LIMIT 1")
  suspend fun getTaskById(taskId: Long): SlateTaskEntity?

  @Query("SELECT * FROM slate_tasks WHERE calendar_event_id IS NOT NULL")
  suspend fun getLinkedCalendarTasks(): List<SlateTaskEntity>

  @Query("SELECT * FROM slate_tasks WHERE scheduled_start_millis IS NOT NULL AND calendar_event_id IS NULL")
  suspend fun getUnlinkedScheduledTasks(): List<SlateTaskEntity>

  @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM slate_tasks WHERE list_id = :listId")
  suspend fun getNextSortOrderForList(listId: Long): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTask(task: SlateTaskEntity): Long

  @Update
  suspend fun updateTask(task: SlateTaskEntity)

  @Update
  suspend fun updateTasks(tasks: List<SlateTaskEntity>)

  @Query("DELETE FROM slate_tasks WHERE id = :taskId")
  suspend fun deleteTaskById(taskId: Long)

  @Transaction
  suspend fun persistOrder(orderedTaskIds: List<Long>) {
    val now = System.currentTimeMillis()
    orderedTaskIds.forEachIndexed { index, id ->
      updateSortOrder(id, index, now)
    }
  }

  @Query("UPDATE slate_tasks SET sort_order = :sortOrder, updated_at = :updatedAt WHERE id = :taskId")
  suspend fun updateSortOrder(taskId: Long, sortOrder: Int, updatedAt: Long)
}
