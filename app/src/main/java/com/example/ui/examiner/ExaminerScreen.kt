package com.example.ui.examiner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.GeminiApiClient
import com.example.data.ai.IcaiExaminerEvaluator
import com.example.data.model.EvaluationResult
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExaminerScreen(
  userId: Int,
  apiClient: GeminiApiClient,
  studyRepository: StudyRepository,
  initialPaperName: String = "Paper 2: Corporate & Other Laws",
  initialQuestionText: String = "The Board of Directors of Neptune Ltd. proposed a final dividend of 15% out of accumulated profits of previous years following net losses in current year. Explain conditions under Rule 3 of Dividend Rules 2014.",
  initialMarks: Int = 7,
  initialRef: String = "Section 123 & Rule 3 of Dividend Rules 2014",
  initialModelAnswer: String = "1. Provision: Under Section 123(1) and Rule 3 of Companies (Declaration and Payment of Dividend) Rules, 2014...\n2. Analysis: All 4 conditions (Rate, Withdrawal limit, Set-off, Residual Reserve) must be satisfied.\n3. Conclusion: The declaration is subject to strict adherence.",
  initialKeyPhrases: List<String> = listOf("Section 123", "Rule 3", "Accumulated Profits", "Paid-up Share Capital", "Free Reserves", "Preceding three years average"),
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var paperName by remember { mutableStateOf(initialPaperName) }
  var questionText by remember { mutableStateOf(initialQuestionText) }
  var maxMarks by remember { mutableStateOf(initialMarks) }
  var icaiRef by remember { mutableStateOf(initialRef) }
  var modelAnswer by remember { mutableStateOf(initialModelAnswer) }
  var keyPhrases by remember { mutableStateOf(initialKeyPhrases) }

  var studentTypedAnswer by remember {
    mutableStateOf(
      "As per Companies Act 2013, the company cannot give dividend without profit. However, rule 3 allows taking money from reserves. The rate cannot be more than last 3 years average. Also the company must keep some reserve left and adjust loss first."
    )
  }

  var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
  var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var imageBytes by remember { mutableStateOf<ByteArray?>(null) }

  var isEvaluating by remember { mutableStateOf(false) }
  var evaluationResult by remember { mutableStateOf<EvaluationResult?>(null) }
  var savedToVaultSuccess by remember { mutableStateOf(false) }

  // Photo Picker
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      selectedImageUri = uri
      scope.launch {
        try {
          context.contentResolver.openInputStream(uri)?.use { stream ->
            val bmp = BitmapFactory.decodeStream(stream)
            imageBitmap = bmp
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            imageBytes = baos.toByteArray()
          }
        } catch (e: Exception) {
          e.printStackTrace()
        }
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Strict ICAI Examiner Checker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Vision & Handwriting Step-Marking", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
      // Question Card
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(paperName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Surface(
              color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = "$maxMarks Marks",
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(text = questionText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
          Spacer(modifier = Modifier.height(4.dp))
          Text("Standard Ref: $icaiRef", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Answer Input Options (Handwritten Photo + Typed Text)
      Text("Your Answer Submission:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
      Spacer(modifier = Modifier.height(6.dp))

      // Upload Handwritten Answer Photo Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          modifier = Modifier
            .weight(1f)
            .testTag("upload_handwritten_btn")
        ) {
          Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (imageBitmap != null) "Change Photo" else "Attach Handwriting Photo", fontSize = 12.sp)
        }

        if (imageBitmap != null) {
          IconButton(onClick = {
            selectedImageUri = null
            imageBitmap = null
            imageBytes = null
          }) {
            Icon(Icons.Default.Delete, contentDescription = "Remove photo", tint = MaterialTheme.colorScheme.error)
          }
        }
      }

      if (imageBitmap != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
        ) {
          Image(
            bitmap = imageBitmap!!.asImageBitmap(),
            contentDescription = "Handwritten notebook page",
            modifier = Modifier.fillMaxSize()
          )
          Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(bottomStart = 8.dp),
            modifier = Modifier.align(Alignment.TopEnd)
          ) {
            Text(
              text = "Handwriting Vision Attached",
              color = Color.White,
              fontSize = 10.sp,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = studentTypedAnswer,
        onValueChange = { studentTypedAnswer = it },
        label = { Text("Typed Answer Text (Optional if photo attached)") },
        minLines = 4,
        maxLines = 10,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("student_answer_input")
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Evaluate Action
      Button(
        onClick = {
          scope.launch {
            isEvaluating = true
            savedToVaultSuccess = false
            val prompt = IcaiExaminerEvaluator.buildExaminerPrompt(
              paperName = paperName,
              questionText = questionText,
              maxMarks = maxMarks,
              icaiReference = icaiRef,
              modelAnswer = modelAnswer,
              keyPhrases = keyPhrases,
              studentTypedAnswer = studentTypedAnswer,
              hasHandwrittenImage = imageBytes != null
            )

            val res = apiClient.generateContentWithRetry(
              prompt = prompt,
              imageBytes = imageBytes,
              imageMimeType = "image/jpeg"
            )

            val rawOutput = res.getOrDefault("")
            val eval = IcaiExaminerEvaluator.parseEvaluationResponse(
              responseString = rawOutput,
              questionId = "q_active",
              defaultMaxMarks = maxMarks.toFloat(),
              studentAnswer = studentTypedAnswer,
              keyPhrases = keyPhrases,
              modelAnswer = modelAnswer
            )
            evaluationResult = eval
            isEvaluating = false
          }
        },
        enabled = !isEvaluating && (studentTypedAnswer.isNotBlank() || imageBytes != null),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("evaluate_answer_btn")
      ) {
        if (isEvaluating) {
          CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Auditing Answer via ICAI 4-Step Scheme...")
        } else {
          Icon(Icons.Default.FactCheck, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Evaluate with Strict ICAI Rules")
        }
      }

      // Evaluation Result View
      if (evaluationResult != null) {
        val eval = evaluationResult!!
        Spacer(modifier = Modifier.height(20.dp))

        // Score Card
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(16.dp)
          ) {
            Column {
              Text(
                text = "EXAMINER SCORECARD",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = "${eval.marksAwarded} / ${eval.totalMarks}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Percentage: ${(eval.marksAwarded * 100 / eval.totalMarks).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }

            Surface(
              color = if (eval.marksAwarded >= eval.totalMarks * 0.5f) Color(0xFF10B981) else Color(0xFFEF4444),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = if (eval.marksAwarded >= eval.totalMarks * 0.6f) "PASS / GOOD" else if (eval.marksAwarded >= eval.totalMarks * 0.4f) "BORDERLINE" else "REVISE REQUIRED",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4-Step Framework Breakdown
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text("Step-Marking Breakdown (ICAI 4-Step Standard):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            eval.stepBreakdown.forEach { step ->
              Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(text = step.stepName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                  Text(text = "${step.awardedMarks} / ${step.maxMarks} M", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                if (step.remark.isNotBlank()) {
                  Text(text = step.remark, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Missing Keywords
        if (eval.missingKeywords.isNotEmpty()) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text("Missing Technical Keywords / Phrases:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
              Spacer(modifier = Modifier.height(6.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                eval.missingKeywords.forEach { kw ->
                  Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = kw,
                      color = MaterialTheme.colorScheme.onErrorContainer,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
        }

        // Deductions & Examiner Comments
        if (eval.deductions.isNotEmpty()) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text("Specific Deductions & Remarks:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(4.dp))
              eval.deductions.forEach { d ->
                Text("• $d", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
        }

        // Topper's Model Answer / Suggested Rewrite
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Topper's Suggested Model Rewrite:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = eval.toppersRewrite,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(12.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save to Mistake Vault Action
        Button(
          onClick = {
            scope.launch {
              studyRepository.addMistake(
                userId = userId,
                paperId = 2,
                subjectName = paperName,
                topicOrStandard = icaiRef,
                question = questionText,
                studentAttempt = studentTypedAnswer,
                marksLost = eval.totalMarks - eval.marksAwarded,
                totalMarks = eval.totalMarks,
                missingKeywords = eval.missingKeywords,
                toppersSolution = eval.toppersRewrite
              )
              savedToVaultSuccess = true
            }
          },
          enabled = !savedToVaultSuccess,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("save_to_mistake_vault_btn")
        ) {
          Icon(if (savedToVaultSuccess) Icons.Default.Check else Icons.Default.BookmarkAdd, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (savedToVaultSuccess) "Saved to Mistake Vault!" else "Save Deficiencies to Mistake Vault")
        }
      }
    }
  }
}
