package com.example.ui.rapidfire

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RapidFireCard
import com.example.data.questionbank.CuratedQuestionBank
import com.example.data.repository.StudyRepository
import com.example.data.syllabus.IcaiSyllabusData
import com.example.ui.components.PaperTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RapidFireScreen(
  userId: Int,
  studyRepository: StudyRepository,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var tts by remember { mutableStateOf<TextToSpeech?>(null) }
  var isTtsReady by remember { mutableStateOf(false) }

  DisposableEffect(Unit) {
    var speechEngine: TextToSpeech? = null
    speechEngine = TextToSpeech(context) { status ->
      if (status == TextToSpeech.SUCCESS) {
        speechEngine?.language = Locale.ENGLISH
        isTtsReady = true
      }
    }
    tts = speechEngine
    onDispose {
      speechEngine?.stop()
      speechEngine?.shutdown()
    }
  }

  val allCards = remember { CuratedQuestionBank.getRapidFireCards() }
  var selectedPaperFilter by remember { mutableStateOf<Int?>(null) }
  var recallCountdownSeconds by remember { mutableStateOf(5) } // pause duration between question and answer
  var speechSpeed by remember { mutableStateOf(1.0f) }

  val filteredCards = remember(allCards, selectedPaperFilter) {
    if (selectedPaperFilter == null) allCards
    else allCards.filter { it.paperId == selectedPaperFilter }
  }

  var currentIndex by remember { mutableStateOf(0) }
  var isAudioPlaying by remember { mutableStateOf(false) }
  var phase by remember { mutableStateOf("IDLE") } // "QUESTION", "COUNTDOWN", "ANSWER", "IDLE"
  var countdownRemaining by remember { mutableStateOf(5) }
  var autoAdvance by remember { mutableStateOf(true) }
  var savedToVault by remember { mutableStateOf(false) }

  val activeCard = remember(filteredCards, currentIndex) {
    if (filteredCards.isNotEmpty()) filteredCards[currentIndex.coerceIn(0, filteredCards.size - 1)] else null
  }

  // Play drill cycle
  fun playCardCycle(card: RapidFireCard) {
    scope.launch {
      if (tts == null || !isTtsReady) return@launch
      tts!!.setSpeechRate(speechSpeed)

      // 1. Speak question
      phase = "QUESTION"
      isAudioPlaying = true
      savedToVault = false
      tts!!.speak("Question. ${card.question}", TextToSpeech.QUEUE_FLUSH, null, "qid")

      while (tts?.isSpeaking == true) {
        delay(100)
      }

      // 2. Countdown pause for mental recall
      phase = "COUNTDOWN"
      countdownRemaining = recallCountdownSeconds
      while (countdownRemaining > 0 && isAudioPlaying) {
        delay(1000)
        countdownRemaining--
      }

      if (!isAudioPlaying) {
        phase = "IDLE"
        return@launch
      }

      // 3. Speak answer
      phase = "ANSWER"
      tts!!.speak("Official Answer. ${card.answer}", TextToSpeech.QUEUE_FLUSH, null, "aid")

      while (tts?.isSpeaking == true) {
        delay(100)
      }

      phase = "IDLE"

      // 4. Auto-advance if enabled
      if (autoAdvance && isAudioPlaying) {
        delay(2000)
        if (currentIndex < filteredCards.size - 1) {
          currentIndex++
          playCardCycle(filteredCards[currentIndex])
        } else {
          isAudioPlaying = false
        }
      } else {
        isAudioPlaying = false
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Commute Voice Rapid-Fire", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Hands-Free Audio Recall Drills", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
          }
        }
      )
    },
    modifier = modifier
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Paper Filter Chips
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        item {
          FilterChip(
            selected = selectedPaperFilter == null,
            onClick = {
              selectedPaperFilter = null
              currentIndex = 0
            },
            label = { Text("All Subjects") }
          )
        }
        items(IcaiSyllabusData.papers) { paper ->
          FilterChip(
            selected = selectedPaperFilter == paper.id,
            onClick = {
              selectedPaperFilter = paper.id
              currentIndex = 0
            },
            label = { Text("P${paper.id}") }
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (activeCard == null) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
          Text("No cards available in this filter.")
        }
      } else {
        // Flashcard display
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  PaperTag(paperId = activeCard.paperId)
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = activeCard.subjectName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                  )
                }
                Text(
                  text = "${currentIndex + 1} of ${filteredCards.size}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Question Block
              Text(
                text = "Rapid-Fire Question:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = activeCard.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )

              Spacer(modifier = Modifier.height(20.dp))

              // Countdown / Recall state
              when (phase) {
                "QUESTION" -> {
                  Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(12.dp)
                    ) {
                      Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("Speaking question aloud...", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                  }
                }
                "COUNTDOWN" -> {
                  Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.Center,
                      modifier = Modifier.padding(12.dp)
                    ) {
                      Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = "Recall window: $countdownRemaining seconds...",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                      )
                    }
                  }
                }
                "ANSWER" -> {
                  Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                      Text("Official ICAI Answer:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(text = activeCard.answer, style = MaterialTheme.typography.bodyMedium)
                      Spacer(modifier = Modifier.height(4.dp))
                      Text("Ref: ${activeCard.keyProvision}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  }
                }
                else -> {
                  // Reveal button when stopped
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                      Text("Answer (Tap Play for Audio):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(text = activeCard.answer, style = MaterialTheme.typography.bodySmall)
                      Spacer(modifier = Modifier.height(4.dp))
                      Text("Ref: ${activeCard.keyProvision}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                  }
                }
              }
            }

            // Quick add to mistake vault
            TextButton(
              onClick = {
                scope.launch {
                  studyRepository.addMistake(
                    userId = userId,
                    paperId = activeCard.paperId,
                    subjectName = activeCard.subjectName,
                    topicOrStandard = activeCard.keyProvision,
                    question = activeCard.question,
                    studentAttempt = "Flagged during rapid-fire commute revision",
                    marksLost = 2.0f,
                    totalMarks = 2.0f,
                    missingKeywords = listOf("Rapid-Fire Drill"),
                    toppersSolution = activeCard.answer
                  )
                  savedToVault = true
                }
              },
              enabled = !savedToVault,
              modifier = Modifier.align(Alignment.End)
            ) {
              Icon(if (savedToVault) Icons.Default.Check else Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (savedToVault) "Saved to Vault" else "Add to Mistake Vault", fontSize = 12.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transport Controls
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Previous
          IconButton(
            onClick = {
              if (currentIndex > 0) {
                currentIndex--
                tts?.stop()
                if (isAudioPlaying) playCardCycle(filteredCards[currentIndex])
              }
            },
            enabled = currentIndex > 0,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant)
          ) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
          }

          // Big Play / Pause Button
          Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            shadowElevation = 4.dp,
            modifier = Modifier
              .size(68.dp)
              .clip(CircleShape)
              .clickable {
                if (isAudioPlaying) {
                  isAudioPlaying = false
                  tts?.stop()
                  phase = "IDLE"
                } else {
                  playCardCycle(activeCard)
                }
              }
              .testTag("rapid_fire_play_button")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isAudioPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
              )
            }
          }

          // Next
          IconButton(
            onClick = {
              if (currentIndex < filteredCards.size - 1) {
                currentIndex++
                tts?.stop()
                if (isAudioPlaying) playCardCycle(filteredCards[currentIndex])
              }
            },
            enabled = currentIndex < filteredCards.size - 1,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant)
          ) {
            Icon(Icons.Default.SkipNext, contentDescription = "Next")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Configuration toggles (Speed & Auto-advance)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pause: ${recallCountdownSeconds}s", style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.width(6.dp))
            listOf(3, 5, 10).forEach { sec ->
              FilterChip(
                selected = recallCountdownSeconds == sec,
                onClick = { recallCountdownSeconds = sec },
                label = { Text("${sec}s", fontSize = 10.sp) },
                modifier = Modifier.padding(horizontal = 2.dp)
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Auto-Advance", style = MaterialTheme.typography.labelSmall)
            Switch(
              checked = autoAdvance,
              onCheckedChange = { autoAdvance = it },
              modifier = Modifier.padding(start = 6.dp)
            )
          }
        }
      }
    }
  }
}
