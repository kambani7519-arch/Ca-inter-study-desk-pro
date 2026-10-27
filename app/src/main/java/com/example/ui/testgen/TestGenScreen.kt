package com.example.ui.testgen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeminiApiClient
import com.example.data.model.CaseScenarioMcq
import com.example.data.model.DescriptiveQuestion
import com.example.data.model.GeneratedTestPaper
import com.example.data.questionbank.CuratedQuestionBank
import com.example.data.syllabus.IcaiSyllabusData
import com.example.ui.components.PaperTag
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestGenScreen(
  apiClient: GeminiApiClient,
  onNavigateToExaminer: (paperName: String, questionText: String, marks: Int, ref: String, modelAnswer: String, keyPhrases: List<String>) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedPaperId by remember { mutableStateOf(1) }
  var selectedChapter by remember { mutableStateOf("All Important Chapters / High Yield") }
  var customChapterInput by remember { mutableStateOf("") }
  var selectedMarks by remember { mutableStateOf(30) } // 20, 30, 50, 100
  var isGenerating by remember { mutableStateOf(false) }
  var currentTestPaper by remember { mutableStateOf<GeneratedTestPaper?>(null) }

  val scope = rememberCoroutineScope()

  val activePaper = remember(selectedPaperId) {
    IcaiSyllabusData.papers.find { it.id == selectedPaperId } ?: IcaiSyllabusData.papers.first()
  }

  val sampleChapters = remember(selectedPaperId) {
    val initial = IcaiSyllabusData.getInitialTopics().filter { it.paperId == selectedPaperId }
    val chNames = initial.map { it.chapterName }.distinct()
    listOf("All Important Chapters / High Yield") + chNames
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("AI Test Generator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("30:70 ICAI Pattern • Case MCQs & Descriptive", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
          }
        },
        actions = {
          if (currentTestPaper != null) {
            TextButton(
              onClick = { currentTestPaper = null },
              modifier = Modifier.testTag("new_test_btn")
            ) {
              Text("Create New")
            }
          }
        }
      )
    },
    modifier = modifier
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      if (currentTestPaper == null) {
        // Generator Setup Screen
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
          Text(
            text = "1. Select Paper (New Scheme)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))

          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(IcaiSyllabusData.papers) { paper ->
              val isSelected = selectedPaperId == paper.id
              Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .clickable { selectedPaperId = paper.id }
                  .testTag("test_paper_select_${paper.id}")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                  PaperTag(paperId = paper.id)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = paper.name.take(16),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "2. Chapter / Accounting Standard / SA",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(6.dp))

          var dropdownExpanded by remember { mutableStateOf(false) }
          ExposedDropdownMenuBox(
            expanded = dropdownExpanded,
            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
          ) {
            OutlinedTextField(
              value = selectedChapter,
              onValueChange = {},
              readOnly = true,
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
              modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
            )
            ExposedDropdownMenu(
              expanded = dropdownExpanded,
              onDismissRequest = { dropdownExpanded = false }
            ) {
              sampleChapters.forEach { chapter ->
                DropdownMenuItem(
                  text = { Text(chapter, fontSize = 13.sp) },
                  onClick = {
                    selectedChapter = chapter
                    dropdownExpanded = false
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = customChapterInput,
            onValueChange = { customChapterInput = it },
            placeholder = { Text("Or specify custom topic (e.g. SA 500 & AS 10)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "3. Select Exam Marks & ICAI Weightage",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(20, 30, 50, 100).forEach { marks ->
              val isSelected = selectedMarks == marks
              Card(
                colors = CardDefaults.cardColors(
                  containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .weight(1f)
                  .clickable { selectedMarks = marks }
                  .testTag("marks_option_$marks")
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                ) {
                  Text(
                    text = "$marks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = if (marks == 100) "Full Mock" else "Marks",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // 30:70 Pattern Preview Box
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Balance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ICAI 30:70 Blueprint Breakdown:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "• 30% (${(selectedMarks * 0.3).toInt()} Marks): Integrated Case Scenario MCQs with 4 options and statutory citations.\n• 70% (${selectedMarks - (selectedMarks * 0.3).toInt()} Marks): Descriptive Practical & Theory questions requiring 4-step framework evaluation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.weight(1f))

          Button(
            onClick = {
              scope.launch {
                isGenerating = true
                val targetChapter = if (customChapterInput.isNotBlank()) customChapterInput else selectedChapter
                val test = CuratedQuestionBank.generateTest(selectedPaperId, targetChapter, selectedMarks)
                currentTestPaper = test
                isGenerating = false
              }
            },
            enabled = !isGenerating,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("generate_test_button")
          ) {
            if (isGenerating) {
              CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Synthesizing ICAI Exam Paper...")
            } else {
              Icon(Icons.Default.AutoAwesome, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Generate ICAI Exam Paper ($selectedMarks Marks)")
            }
          }
        }
      } else {
        // Active Exam Taking Session
        ExamSessionView(
          testPaper = currentTestPaper!!,
          onSendToExaminer = { descQ ->
            val paperName = activePaper.name
            onNavigateToExaminer(
              paperName,
              descQ.questionText,
              descQ.marks,
              descQ.icaiReference,
              descQ.officialModelAnswer,
              descQ.keyPhrases
            )
          }
        )
      }
    }
  }
}

@Composable
private fun ExamSessionView(
  testPaper: GeneratedTestPaper,
  onSendToExaminer: (DescriptiveQuestion) -> Unit
) {
  var activeTab by remember { mutableStateOf(0) } // 0 = 30% MCQs, 1 = 70% Descriptive

  Column(modifier = Modifier.fillMaxSize()) {
    // Header Banner
    Surface(
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
          text = testPaper.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
          text = "Total: ${testPaper.totalMarks} Marks • MCQs: ${testPaper.mcqMarks}M • Descriptive: ${testPaper.descriptiveMarks}M",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary
        )
      }
    }

    // Tab Switcher
    TabRow(selectedTabIndex = activeTab) {
      Tab(
        selected = activeTab == 0,
        onClick = { activeTab = 0 },
        text = { Text("Part I: Case MCQs (${testPaper.mcqMarks}M)") }
      )
      Tab(
        selected = activeTab == 1,
        onClick = { activeTab = 1 },
        text = { Text("Part II: Descriptive (${testPaper.descriptiveMarks}M)") }
      )
    }

    if (activeTab == 0) {
      // Case Scenario MCQs
      LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        itemsIndexed(testPaper.mcqs) { index, mcq ->
          McqCardItem(index = index + 1, mcq = mcq)
        }
      }
    } else {
      // Descriptive Practical Questions
      LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(testPaper.descriptiveQuestions) { q ->
          DescriptiveCardItem(question = q, onEvaluate = { onSendToExaminer(q) })
        }
      }
    }
  }
}

@Composable
private fun McqCardItem(index: Int, mcq: CaseScenarioMcq) {
  var selectedOption by remember { mutableStateOf<Int?>(null) }
  var showExplanation by remember { mutableStateOf(false) }

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Case Scenario $index (2 Marks)",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(4.dp)
        ) {
          Text(
            text = "30% ICAI Compulsory",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Scenario Box
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = mcq.scenarioText,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(10.dp)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = mcq.question,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(10.dp))

      // 4 Linked Options
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        mcq.options.forEachIndexed { optIndex, optionText ->
          val isChosen = selectedOption == optIndex
          val isCorrect = optIndex == mcq.correctOptionIndex
          val optionColor = when {
            selectedOption == null -> MaterialTheme.colorScheme.surface
            isChosen && isCorrect -> Color(0xFF10B981).copy(alpha = 0.15f)
            isChosen && !isCorrect -> Color(0xFFEF4444).copy(alpha = 0.15f)
            !isChosen && isCorrect -> Color(0xFF10B981).copy(alpha = 0.10f)
            else -> MaterialTheme.colorScheme.surface
          }
          val borderColor = when {
            selectedOption == null -> MaterialTheme.colorScheme.outlineVariant
            isChosen && isCorrect -> Color(0xFF10B981)
            isChosen && !isCorrect -> Color(0xFFEF4444)
            !isChosen && isCorrect -> Color(0xFF10B981)
            else -> MaterialTheme.colorScheme.outlineVariant
          }

          Surface(
            color = optionColor,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                if (selectedOption == null) {
                  selectedOption = optIndex
                  showExplanation = true
                }
              }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(10.dp)
            ) {
              val letter = ('A' + optIndex).toString()
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.surfaceVariant)
              ) {
                Text(text = letter, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text(text = optionText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))

              if (selectedOption != null) {
                if (isCorrect) {
                  Icon(Icons.Default.Check, contentDescription = "Correct", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                } else if (isChosen) {
                  Icon(Icons.Default.Close, contentDescription = "Incorrect", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }

      AnimatedVisibility(visible = showExplanation) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "ICAI Citation: ${mcq.icaiCitation}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = mcq.detailedExplanation,
                style = MaterialTheme.typography.bodySmall
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DescriptiveCardItem(
  question: DescriptiveQuestion,
  onEvaluate: () -> Unit
) {
  var showRubric by remember { mutableStateOf(false) }

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "${question.questionNumber} (${question.marks} Marks)",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Surface(
          color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = question.icaiReference,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = question.questionText,
        style = MaterialTheme.typography.bodyMedium
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = { showRubric = !showRubric },
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Rule, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (showRubric) "Hide Rubric" else "Step Rubric", fontSize = 12.sp)
        }

        Button(
          onClick = onEvaluate,
          modifier = Modifier.weight(1.2f)
        ) {
          Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("ICAI Answer Check", fontSize = 12.sp)
        }
      }

      AnimatedVisibility(visible = showRubric) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("ICAI Step-Marking Scheme:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(4.dp))
              question.rubricSteps.forEach { step ->
                Text("• $step", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
              }
            }
          }
        }
      }
    }
  }
}
