package com.example.ui.mistakevault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MistakeVaultEntity
import com.example.data.repository.StudyRepository
import com.example.ui.components.PaperTag
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakeVaultScreen(
  userId: Int,
  studyRepository: StudyRepository,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  val mistakes by studyRepository.getUserMistakes(userId).collectAsState(initial = emptyList())

  var filterDueTodayOnly by remember { mutableStateOf(false) }
  var selectedPaperFilter by remember { mutableStateOf<Int?>(null) }

  val now = System.currentTimeMillis()

  val filteredMistakes = remember(mistakes, filterDueTodayOnly, selectedPaperFilter) {
    mistakes.filter { item ->
      val matchesPaper = if (selectedPaperFilter != null) item.paperId == selectedPaperFilter else true
      val matchesDue = if (filterDueTodayOnly) item.nextReviewDate <= now && !item.isMastered else true
      matchesPaper && matchesDue
    }
  }

  val dueTodayCount = remember(mistakes) {
    mistakes.count { it.nextReviewDate <= now && !it.isMastered }
  }

  val context = androidx.compose.ui.platform.LocalContext.current
  val allTopics by studyRepository.getTopicsForUser(userId).collectAsState(initial = emptyList())
  var isExportingPdf by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Mistake Vault & Spaced Repetition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("1, 3, 7, 14, 30-Day Retention Schedule", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
          }
        },
        actions = {
          IconButton(
            onClick = {
              scope.launch {
                isExportingPdf = true
                try {
                  val file = com.example.data.export.PdfReportGenerator.generateStudyReport(
                    context = context,
                    user = null,
                    allTopics = allTopics,
                    mistakes = mistakes
                  )
                  com.example.data.export.PdfReportGenerator.shareOrViewPdf(context, file)
                } catch (e: Exception) {
                  e.printStackTrace()
                } finally {
                  isExportingPdf = false
                }
              }
            },
            modifier = Modifier.testTag("export_mistake_vault_pdf_btn")
          ) {
            if (isExportingPdf) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF Report", tint = MaterialTheme.colorScheme.primary)
            }
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
    ) {
      // Due Today Banner
      if (dueTodayCount > 0) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { filterDueTodayOnly = !filterDueTodayOnly }
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "$dueTodayCount questions due for spaced review today!",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
              )
            }
            Text(
              text = if (filterDueTodayOnly) "Show All" else "Review Now",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      // Filter Row
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        item {
          FilterChip(
            selected = !filterDueTodayOnly && selectedPaperFilter == null,
            onClick = {
              filterDueTodayOnly = false
              selectedPaperFilter = null
            },
            label = { Text("All (${mistakes.size})") }
          )
        }
        item {
          FilterChip(
            selected = filterDueTodayOnly,
            onClick = { filterDueTodayOnly = !filterDueTodayOnly },
            label = { Text("Due Today ($dueTodayCount)") }
          )
        }
        (1..6).forEach { pId ->
          item {
            FilterChip(
              selected = selectedPaperFilter == pId,
              onClick = {
                selectedPaperFilter = if (selectedPaperFilter == pId) null else pId
              },
              label = { Text("Paper $pId") }
            )
          }
        }
      }

      // Mistake Items List
      if (filteredMistakes.isEmpty()) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.VerifiedUser,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = if (filterDueTodayOnly) "No reviews due today! Great work." else "Mistake Vault is empty.",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Questions evaluated in Examiner Checker or Test Generator can be saved here for spaced retention drills.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 16.dp)
            )
          }
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(filteredMistakes, key = { it.id }) { item ->
            MistakeCard(
              item = item,
              onAdvance = { remembered ->
                scope.launch {
                  studyRepository.advanceSpacedRepetition(item.id, item.reviewIntervalDays, remembered)
                }
              },
              onDelete = {
                scope.launch {
                  studyRepository.deleteMistake(item.id)
                }
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MistakeCard(
  item: MistakeVaultEntity,
  onAdvance: (Boolean) -> Unit,
  onDelete: () -> Unit
) {
  var isExpanded by remember { mutableStateOf(false) }
  val now = System.currentTimeMillis()
  val isDue = item.nextReviewDate <= now && !item.isMastered

  val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
  val nextReviewDateStr = remember(item.nextReviewDate) { sdf.format(Date(item.nextReviewDate)) }

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          PaperTag(paperId = item.paperId)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = item.topicOrStandard,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = if (item.isMastered) Color(0xFF10B981).copy(alpha = 0.15f)
            else if (isDue) Color(0xFFEF4444).copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = if (item.isMastered) "Mastered (30d)" else if (isDue) "Review Due!" else "Next: $nextReviewDateStr (${item.reviewIntervalDays}d)",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (item.isMastered) Color(0xFF10B981) else if (isDue) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(16.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(text = item.question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

      if (item.missingKeywords.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Deficiency: ${item.missingKeywords}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          fontSize = 11.sp
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Toggle Solution
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(onClick = { isExpanded = !isExpanded }) {
          Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (isExpanded) "Hide Model Solution" else "Reveal Topper Solution", fontSize = 12.sp)
        }

        // Spaced Repetition Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          IconButton(
            onClick = { onAdvance(false) },
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(Color(0xFFEF4444).copy(alpha = 0.12f))
          ) {
            Icon(Icons.Default.Replay, contentDescription = "Forgot (Reset to 1d)", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
          }
          IconButton(
            onClick = { onAdvance(true) },
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(Color(0xFF10B981).copy(alpha = 0.15f))
          ) {
            Icon(Icons.Default.Check, contentDescription = "Remembered (Advance)", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
          }
        }
      }

      AnimatedVisibility(visible = isExpanded) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("Official Topper Rewrite:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = item.toppersSolution,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
              )
            }
          }
        }
      }
    }
  }
}
