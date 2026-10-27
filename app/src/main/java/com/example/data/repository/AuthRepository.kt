package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.UserEntity
import com.example.data.syllabus.IcaiSyllabusData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

class AuthRepository(context: Context) {

  private val db = AppDatabase.getDatabase(context)
  private val userDao = db.userDao()
  private val studyDao = db.studyDao()
  private val prefs = context.getSharedPreferences("ca_auth_session", Context.MODE_PRIVATE)

  private val _currentUser = MutableStateFlow<UserEntity?>(null)
  val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

  suspend fun checkExistingSession() = withContext(Dispatchers.IO) {
    val savedUserId = prefs.getInt("logged_in_user_id", -1)
    if (savedUserId != -1) {
      val user = userDao.getUserById(savedUserId)
      _currentUser.value = user
    }
    // If no user exists in database, seed a default demo student account
    if (_currentUser.value == null) {
      val demoEmail = "student@cainter.pro"
      val existing = userDao.getUserByEmail(demoEmail)
      if (existing != null) {
        setSession(existing)
      } else {
        val result = register(
          fullName = "CA Aspirant (Jan 2027)",
          email = demoEmail,
          password = "Password@123",
          attempt = "Jan 2027",
          targetGroup = "Both Groups"
        )
        if (result.isSuccess) {
          _currentUser.value = result.getOrNull()
        }
      }
    }
  }

  suspend fun loginWithGoogle(
    email: String,
    fullName: String,
    uid: String
  ): Result<UserEntity> = withContext(Dispatchers.IO) {
    try {
      val cleanEmail = email.trim().lowercase()
      val existing = userDao.getUserByEmail(cleanEmail)
      if (existing != null) {
        setSession(existing)
        Result.success(existing)
      } else {
        val newUser = UserEntity(
          email = cleanEmail,
          passwordHash = "google_auth_${uid}",
          salt = "google_salt",
          fullName = fullName.ifBlank { "CA Aspirant (Jan 2027)" },
          attempt = "Jan 2027",
          targetGroup = "Both Groups"
        )
        val newId = userDao.insertUser(newUser).toInt()
        val created = newUser.copy(id = newId)
        seedUserSyllabus(newId)
        setSession(created)
        Result.success(created)
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun register(
    fullName: String,
    email: String,
    password: String,
    attempt: String = "Jan 2027",
    targetGroup: String = "Both Groups"
  ): Result<UserEntity> = withContext(Dispatchers.IO) {
    try {
      val cleanEmail = email.trim().lowercase()
      if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
        return@withContext Result.failure(Exception("Please enter a valid email address."))
      }
      if (password.length < 6) {
        return@withContext Result.failure(Exception("Password must be at least 6 characters."))
      }

      val existing = userDao.getUserByEmail(cleanEmail)
      if (existing != null) {
        return@withContext Result.failure(Exception("An account with this email already exists."))
      }

      val salt = generateSalt()
      val hash = hashPassword(password, salt)

      val newUser = UserEntity(
        email = cleanEmail,
        passwordHash = hash,
        salt = salt,
        fullName = fullName.ifBlank { "CA Student" },
        attempt = attempt,
        targetGroup = targetGroup
      )

      val newId = userDao.insertUser(newUser).toInt()
      val createdUser = newUser.copy(id = newId)

      // Seed comprehensive ICAI syllabus for this user into Room
      seedUserSyllabus(newId)

      setSession(createdUser)
      Result.success(createdUser)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun login(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
    try {
      val cleanEmail = email.trim().lowercase()
      val user = userDao.getUserByEmail(cleanEmail)
        ?: return@withContext Result.failure(Exception("No account found with this email."))

      val testHash = hashPassword(password, user.salt)
      if (testHash != user.passwordHash) {
        return@withContext Result.failure(Exception("Invalid password. Please try again."))
      }

      setSession(user)
      Result.success(user)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun logout() = withContext(Dispatchers.IO) {
    prefs.edit().remove("logged_in_user_id").apply()
    _currentUser.value = null
  }

  private fun setSession(user: UserEntity) {
    prefs.edit().putInt("logged_in_user_id", user.id).apply()
    _currentUser.value = user
  }

  private suspend fun seedUserSyllabus(userId: Int) {
    val initialTopics = IcaiSyllabusData.getInitialTopics()
    val entities = initialTopics.map { topic ->
      com.example.data.local.TopicProgressEntity(
        uid = "${userId}_${topic.id}",
        userId = userId,
        topicId = topic.id,
        paperId = topic.paperId,
        chapterName = topic.chapterName,
        topicTitle = topic.topicTitle,
        icaiRef = topic.icaiRef,
        isCustom = false,
        status = topic.status.name,
        notes = ""
      )
    }
    studyDao.insertAllTopics(entities)
  }

  private fun generateSalt(): String {
    val random = SecureRandom()
    val saltBytes = ByteArray(16)
    random.nextBytes(saltBytes)
    return saltBytes.joinToString("") { "%02x".format(it) }
  }

  private fun hashPassword(password: String, salt: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val bytes = md.digest((password + salt).toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }
}
