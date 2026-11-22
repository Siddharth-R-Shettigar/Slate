package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.ui.theme.ListColorTags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch

@Database(
  entities = [SlateListEntity::class, SlateTaskEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class SlateDatabase : RoomDatabase() {
  abstract fun listDao(): ListDao
  abstract fun taskDao(): TaskDao

  companion object {
    @Volatile
    private var INSTANCE: SlateDatabase? = null

    fun getInstance(context: Context): SlateDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
      }
    }

    private fun buildDatabase(appContext: Context): SlateDatabase {
      return Room.databaseBuilder(
        appContext,
        SlateDatabase::class.java,
        "slate_database.db",
      )
        .setQueryExecutor(Dispatchers.IO.asExecutor())
        .setTransactionExecutor(Dispatchers.IO.asExecutor())
        .addCallback(
          object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
              super.onCreate(db)
              CoroutineScope(Dispatchers.IO).launch {
                val instance = getInstance(appContext)
                if (instance.listDao().getAllListsOnce().isEmpty()) {
                  instance.listDao().insertList(
                    SlateListEntity(
                      id = 1L,
                      name = "List",
                      colorTagHex = ListColorTags.Sky,
                      sortOrder = 0,
                    )
                  )
                }
              }
            }
          }
        )
        .build()
    }
  }
}
