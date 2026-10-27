package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    UserEntity::class,
    TopicProgressEntity::class,
    MistakeVaultEntity::class,
    TestHistoryEntity::class,
    StudyScheduleEntity::class
  ],
  version = 2,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun userDao(): UserDao
  abstract fun studyDao(): StudyDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "cainter_study_desk_pro.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
