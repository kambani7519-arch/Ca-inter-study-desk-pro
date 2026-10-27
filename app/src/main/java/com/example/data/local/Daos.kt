package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
  @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
  suspend fun getUserByEmail(email: String): UserEntity?

  @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
  suspend fun getUserById(userId: Int): UserEntity?

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertUser(user: UserEntity): Long

  @Update
  suspend fun updateUser(user: UserEntity)
}

@Dao
interface StudyDao {
  // Topics & Syllabus
  @Query("SELECT * FROM topics_progress WHERE userId = :userId ORDER BY paperId ASC, topicId ASC")
  fun getAllUserTopics(userId: Int): Flow<List<TopicProgressEntity>>

  @Query("SELECT * FROM topics_progress WHERE userId = :userId AND paperId = :paperId ORDER BY topicId ASC")
  fun getUserTopicsByPaper(userId: Int, paperId: Int): Flow<List<TopicProgressEntity>>

  @Query("SELECT COUNT(*) FROM topics_progress WHERE userId = :userId")
  suspend fun getUserTopicCount(userId: Int): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateTopic(topic: TopicProgressEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllTopics(topics: List<TopicProgressEntity>)

  @Query("UPDATE topics_progress SET status = :status, updatedAt = :timestamp WHERE uid = :uid")
  suspend fun updateTopicStatus(uid: String, status: String, timestamp: Long = System.currentTimeMillis())

  // Mistake Vault & Spaced Repetition
  @Query("SELECT * FROM mistake_vault WHERE userId = :userId ORDER BY nextReviewDate ASC, createdAt DESC")
  fun getUserMistakes(userId: Int): Flow<List<MistakeVaultEntity>>

  @Query("SELECT * FROM mistake_vault WHERE userId = :userId AND isMastered = 0 AND nextReviewDate <= :cutoffTime ORDER BY nextReviewDate ASC")
  fun getMistakesDueForReview(userId: Int, cutoffTime: Long): Flow<List<MistakeVaultEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMistake(item: MistakeVaultEntity): Long

  @Query("UPDATE mistake_vault SET reviewIntervalDays = :nextInterval, nextReviewDate = :nextDate, isMastered = :isMastered WHERE id = :id")
  suspend fun updateSpacedRepetition(id: Int, nextInterval: Int, nextDate: Long, isMastered: Boolean)

  @Query("DELETE FROM mistake_vault WHERE id = :id")
  suspend fun deleteMistake(id: Int)

  // Test History
  @Query("SELECT * FROM test_history WHERE userId = :userId ORDER BY dateTaken DESC")
  fun getUserTestHistory(userId: Int): Flow<List<TestHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTestHistory(record: TestHistoryEntity)

  // Weekly Study Planner Schedule
  @Query("SELECT * FROM study_schedule WHERE userId = :userId ORDER BY scheduledDate ASC, orderIndex ASC, id ASC")
  fun getAllUserSchedules(userId: Int): Flow<List<StudyScheduleEntity>>

  @Query("SELECT * FROM study_schedule WHERE userId = :userId AND scheduledDate BETWEEN :startDate AND :endDate ORDER BY scheduledDate ASC, orderIndex ASC, id ASC")
  fun getSchedulesForDateRange(userId: Int, startDate: String, endDate: String): Flow<List<StudyScheduleEntity>>

  @Query("SELECT * FROM study_schedule WHERE userId = :userId AND scheduledDate = :date ORDER BY orderIndex ASC, id ASC")
  fun getSchedulesForDate(userId: Int, date: String): Flow<List<StudyScheduleEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSchedule(item: StudyScheduleEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertScheduleList(items: List<StudyScheduleEntity>)

  @Query("UPDATE study_schedule SET scheduledDate = :newDate, timeSlot = :newSlot WHERE id = :id")
  suspend fun updateScheduleSlot(id: Long, newDate: String, newSlot: String)

  @Query("UPDATE study_schedule SET isCompleted = :isCompleted WHERE id = :id")
  suspend fun toggleScheduleCompleted(id: Long, isCompleted: Boolean)

  @Query("DELETE FROM study_schedule WHERE id = :id")
  suspend fun deleteSchedule(id: Long)

  @Query("DELETE FROM study_schedule WHERE userId = :userId AND scheduledDate BETWEEN :startDate AND :endDate")
  suspend fun clearScheduleRange(userId: Int, startDate: String, endDate: String)
}

