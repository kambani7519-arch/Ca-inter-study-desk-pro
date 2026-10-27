package com.example.ui.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MistakeVaultEntity
import com.example.data.local.TestHistoryEntity
import com.example.data.local.UserEntity
import com.example.data.model.GroupType
import com.example.data.model.TopicItem
import com.example.data.model.TopicStatus
import com.example.data.repository.StudyRepository
import com.example.data.syllabus.IcaiSyllabusData
import com.example.ui.components.PaperTag
import com.example.ui.theme.StatusDone
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusRevise
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TestTrendPoint(
  val attemptName: String,
  val overallScorePct: Float,
  val mcqPct: Float,
  val descriptivePct: Float,
  val paperCode: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
  userId: Int,
  studyRepository: StudyRepository,
  user: UserEntity? = null,
  modifier: Modifier = Modifier
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val scope = rememberCoroutineScope()
  val allTopics by studyRepository.getTopicsForUser(userId).collectAsState(initial = emptyList())
  val mistakes by studyRepository.getUserMistakes(userId).collectAsState(initial = emptyList())
  val testHistory by studyRepository.getUserTestHistory(userId).collectAsState(initial = emptyList())

  var selectedTrendMetric by remember { mutableStateOf("Overall Score") } // "Overall Score", "30% MCQs", "70% Descriptive"
  var isExportingPdf by remember { mutableStateOf(false) }

  fun exportPdf() {
    scope.launch {
      isExportingPdf = true
      try {
        val file = com.example.data.export.PdfReportGenerator.generateStudyReport(
          context = context,
          user = user,
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
  }

  // Sample realistic test score progression for Jan 2027 aspirant if test history is new
  val trendPoints = remember(testHistory) {
    if (testHistory.isNotEmpty()) {
      testHistory.takeLast(7).mapIndexed { idx, t ->
        TestTrendPoint(
          attemptName = "T${idx + 1}",
          overallScorePct = if (t.totalMarks > 0) (t.marksScored * 100f / t.totalMarks) else 60f,
          mcqPct = 72f,
          descriptivePct = 58f,
          paperCode = "P${t.paperId}"
        )
      }
    } else {
      listOf(
        TestTrendPoint("Mock 1", 42f, 50f, 38f, "P1"),
        TestTrendPoint("Mock 2", 48f, 60f, 43f, "P2"),
        TestTrendPoint("Mock 3", 55f, 65f, 51f, "P3"),
        TestTrendPoint("Mock 4", 62f, 75f, 56f, "P4"),
        TestTrendPoint("Mock 5", 68f, 80f, 63f, "P5"),
        TestTrendPoint("Mock 6", 74f, 85f, 69f, "P6"),
        TestTrendPoint("Mock 7", 81f, 90f, 77f, "Full")
      )
    }
  }

  // Paper-wise syllabus completion calculations
  val paperStats = remember(allTopics) {
    IcaiSyllabusData.papers.map { paper ->
      val paperTopics = allTopics.filter { it.paperId == paper.id }
      val total = paperTopics.size
      val done = paperTopics.count { it.status == TopicStatus.DONE }
      val revise = paperTopics.count { it.status == TopicStatus.REVISE }
      val pending = paperTopics.count { it.status == TopicStatus.PENDING }
      val pct = if (total > 0) (done.toFloat() / total) else 0f
      PaperCompletionStat(
        paper = paper,
        totalTopics = total,
        doneTopics = done,
        reviseTopics = revise,
        pendingTopics = pending,
        completionPct = pct
      )
    }
  }

  val totalTopics = allTopics.size
  val totalDone = allTopics.count { it.status == TopicStatus.DONE }
  val overallPct = if (totalTopics > 0) (totalDone.toFloat() / totalTopics) else 0f

  val g1Stats = paperStats.filter { it.paper.group == GroupType.GROUP_1 }
  val g1Done = g1Stats.sumOf { it.doneTopics }
  val g1Total = g1Stats.sumOf { it.totalTopics }.coerceAtLeast(1)
  val g1Pct = g1Done.toFloat() / g1Total

  val g2Stats = paperStats.filter { it.paper.group == GroupType.GROUP_2 }
  val g2Done = g2Stats.sumOf { it.doneTopics }
  val g2Total = g2Stats.sumOf { it.totalTopics }.coerceAtLeast(1)
  val g2Pct = g2Done.toFloat() / g2Total

  val masteredMistakes = mistakes.count { it.isMastered }
  val totalMistakes = mistakes.size
  val vaultMasteryRate = if (totalMistakes > 0) (masteredMistakes * 100 / totalMistakes) else 100

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Aspirant Analytics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Syllabus & 30:70 Exam Accuracy Trends", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
          }
        },
        actions = {
          if (isExportingPdf) {
            CircularProgressIndicator(
              modifier = Modifier
                .padding(end = 12.dp)
                .size(20.dp),
              strokeWidth = 2.dp
            )
          } else {
            IconButton(
              onClick = { exportPdf() },
              modifier = Modifier.testTag("download_pdf_report_btn")
            ) {
              Icon(Icons.Default.PictureAsPdf, contentDescription = "Download PDF Report", tint = MaterialTheme.colorScheme.primary)
            }
          }
        }
      )
    },
    modifier = modifier
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // PDF Export Action Banner
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(38.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text("Performance & Mistake Dossier", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Download full A4 PDF study audit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }

            Button(
              onClick = { exportPdf() },
              enabled = !isExportingPdf,
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("download_pdf_banner_btn")
            ) {
              if (isExportingPdf) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              } else {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export PDF", fontSize = 12.sp)
              }
            }
          }
        }
      }

      // 1. Executive Summary Cards
      item {
        ExecutiveSummarySection(
          overallPct = overallPct,
          g1Pct = g1Pct,
          g2Pct = g2Pct,
          vaultMasteryRate = vaultMasteryRate
        )
      }

      // 2. 30:70 Test Accuracy Improvement Trend Line Chart
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column {
                Text(
                  text = "30:70 Test Accuracy Trend",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
                Text(
                  text = "Chronological improvement across mocks",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = "+39% Growth",
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metric Selector Pills
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              listOf("Overall Score", "30% MCQs", "70% Descriptive").forEach { metric ->
                FilterChip(
                  selected = selectedTrendMetric == metric,
                  onClick = { selectedTrendMetric = metric },
                  label = { Text(metric, fontSize = 11.sp) }
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Trend Line Chart
            TrendLineChart(
              points = trendPoints,
              selectedMetric = selectedTrendMetric,
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Chart Legend
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(selectedTrendMetric, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.width(16.dp))
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF10B981))
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Target (60%+ Pass / Exemption)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981))
            }
          }
        }
      }

      // 3. Paper-wise Syllabus Completion Bar Chart
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Paper-Wise Syllabus Completion",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Official ICAI Chapters, AS & SAs completed vs pending",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            paperStats.forEach { stat ->
              PaperCompletionBarRow(stat = stat)
              Spacer(modifier = Modifier.height(12.dp))
            }
          }
        }
      }

      // 4. Diagnostic Subject Readiness Matrix
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Jan 2027 Exam Readiness Matrix",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              val strongest = paperStats.maxByOrNull { it.completionPct }
              val focusNeeded = paperStats.minByOrNull { it.completionPct }

              Surface(
                color = Color(0xFF10B981).copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Strongest Subject", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "P${strongest?.paper?.id ?: 1}: ${strongest?.paper?.name?.take(18) ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "${((strongest?.completionPct ?: 0f) * 100).toInt()}% Prepared",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF10B981)
                  )
                }
              }

              Surface(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Focus Recommended", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "P${focusNeeded?.paper?.id ?: 2}: ${focusNeeded?.paper?.name?.take(18) ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "${((focusNeeded?.completionPct ?: 0f) * 100).toInt()}% Prepared",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ExecutiveSummarySection(
  overallPct: Float,
  g1Pct: Float,
  g2Pct: Float,
  vaultMasteryRate: Int
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    SummaryStatCard(
      title = "Overall Progress",
      value = "${(overallPct * 100).toInt()}%",
      subtitle = "All 6 Papers",
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.weight(1f)
    )
    SummaryStatCard(
      title = "Group 1",
      value = "${(g1Pct * 100).toInt()}%",
      subtitle = "Papers 1, 2, 3",
      color = Color(0xFF2563EB),
      modifier = Modifier.weight(1f)
    )
    SummaryStatCard(
      title = "Group 2",
      value = "${(g2Pct * 100).toInt()}%",
      subtitle = "Papers 4, 5, 6",
      color = Color(0xFF7C3AED),
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun SummaryStatCard(
  title: String,
  value: String,
  subtitle: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
private fun PaperCompletionBarRow(stat: PaperCompletionStat) {
  val animatedProgress by animateFloatAsState(
    targetValue = stat.completionPct.coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 600),
    label = "progress"
  )

  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        PaperTag(paperId = stat.paper.id)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stat.paper.name,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold
        )
      }
      Text(
        text = "${(stat.completionPct * 100).toInt()}% (${stat.doneTopics}/${stat.totalTopics})",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Multi-segment progress bar (Done, Revise, Pending)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(10.dp)
        .clip(RoundedCornerShape(5.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(fraction = animatedProgress)
          .fillMaxHeight()
          .background(
            Brush.horizontalGradient(
              listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary
              )
            )
          )
      )
    }
  }
}

@Composable
private fun TrendLineChart(
  points: List<TestTrendPoint>,
  selectedMetric: String,
  modifier: Modifier = Modifier
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
  val targetLineColor = Color(0xFF10B981)

  Canvas(modifier = modifier) {
    if (points.isEmpty()) return@Canvas

    val width = size.width
    val height = size.height
    val paddingBottom = 40f
    val paddingTop = 20f
    val paddingLeft = 40f
    val paddingRight = 40f

    val chartWidth = width - paddingLeft - paddingRight
    val chartHeight = height - paddingTop - paddingBottom

    // Draw horizontal grid lines (0%, 25%, 50%, 60% exemption target, 75%, 100%)
    val gridValues = listOf(0.25f, 0.50f, 0.60f, 0.75f, 1.0f)
    gridValues.forEach { ratio ->
      val y = paddingTop + chartHeight * (1f - ratio)
      val isTarget = ratio == 0.60f
      drawLine(
        color = if (isTarget) targetLineColor.copy(alpha = 0.7f) else gridColor,
        start = Offset(paddingLeft, y),
        end = Offset(width - paddingRight, y),
        strokeWidth = if (isTarget) 2f else 1f,
        pathEffect = if (isTarget) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null
      )
    }

    // Extract values based on metric
    val values = points.map { pt ->
      when (selectedMetric) {
        "30% MCQs" -> pt.mcqPct
        "70% Descriptive" -> pt.descriptivePct
        else -> pt.overallScorePct
      }.coerceIn(0f, 100f)
    }

    val stepX = chartWidth / (points.size - 1).coerceAtLeast(1)

    // Build line path
    val path = Path()
    val fillPath = Path()

    points.forEachIndexed { i, _ ->
      val x = paddingLeft + i * stepX
      val y = paddingTop + chartHeight * (1f - (values[i] / 100f))
      if (i == 0) {
        path.moveTo(x, y)
        fillPath.moveTo(x, paddingTop + chartHeight)
        fillPath.lineTo(x, y)
      } else {
        path.lineTo(x, y)
        fillPath.lineTo(x, y)
      }
    }

    fillPath.lineTo(paddingLeft + (points.size - 1) * stepX, paddingTop + chartHeight)
    fillPath.close()

    // Draw area fill under curve
    drawPath(
      path = fillPath,
      brush = Brush.verticalGradient(
        colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
        startY = paddingTop,
        endY = paddingTop + chartHeight
      )
    )

    // Draw main trend line
    drawPath(
      path = path,
      color = primaryColor,
      style = Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    )

    // Draw data points & attempt label markers
    points.forEachIndexed { i, pt ->
      val x = paddingLeft + i * stepX
      val y = paddingTop + chartHeight * (1f - (values[i] / 100f))

      // Outer point glow
      drawCircle(
        color = Color.White,
        radius = 6f,
        center = Offset(x, y)
      )
      // Inner colored dot
      drawCircle(
        color = primaryColor,
        radius = 4f,
        center = Offset(x, y)
      )
    }
  }
}

private data class PaperCompletionStat(
  val paper: com.example.data.model.PaperInfo,
  val totalTopics: Int,
  val doneTopics: Int,
  val reviseTopics: Int,
  val pendingTopics: Int,
  val completionPct: Float
)
