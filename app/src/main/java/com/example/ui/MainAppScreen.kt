package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.example.data.ai.GeminiApiClient
import com.example.data.local.UserEntity
import com.example.data.repository.AuthRepository
import com.example.data.repository.StudyRepository
import com.example.ui.analytics.AnalyticsScreen
import com.example.ui.desk.StudyDeskScreen
import com.example.ui.examiner.ExaminerScreen
import com.example.ui.mistakevault.MistakeVaultScreen
import com.example.ui.syllabus.SyllabusScreen
import com.example.ui.testgen.TestGenScreen

sealed class NavItem(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
) {
  object Syllabus : NavItem("syllabus", "Syllabus", Icons.Filled.ListAlt, Icons.Outlined.ListAlt)
  object TestGen : NavItem("testgen", "30:70 Test", Icons.Filled.Quiz, Icons.Outlined.Quiz)
  object Examiner : NavItem("examiner", "Examiner", Icons.Filled.FactCheck, Icons.Outlined.FactCheck)
  object Vault : NavItem("vault", "Vault", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder)
  object Analytics : NavItem("analytics", "Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart)
  object Desk : NavItem("desk", "Desk", Icons.Filled.Headphones, Icons.Outlined.Headphones)
}

@Composable
fun MainAppScreen(
  user: UserEntity,
  apiClient: GeminiApiClient,
  studyRepository: StudyRepository,
  authRepository: AuthRepository,
  onLogout: () -> Unit
) {
  var currentTab by remember { mutableStateOf<NavItem>(NavItem.Syllabus) }

  // State passed to Examiner from TestGen if requested
  var examinerPaperName by remember { mutableStateOf("Paper 2: Corporate & Other Laws") }
  var examinerQuestionText by remember {
    mutableStateOf(
      "The Board of Directors of Neptune Ltd. proposed a final dividend of 15% out of accumulated profits of previous years following net losses in current year. Explain conditions under Rule 3 of Dividend Rules 2014."
    )
  }
  var examinerMarks by remember { mutableStateOf(7) }
  var examinerRef by remember { mutableStateOf("Section 123 & Rule 3 of Dividend Rules 2014") }
  var examinerModelAnswer by remember {
    mutableStateOf(
      "1. Provision: Under Section 123(1) and Rule 3 of Companies (Declaration and Payment of Dividend) Rules, 2014...\n2. Analysis: All 4 conditions (Rate, Withdrawal limit, Set-off, Residual Reserve) must be satisfied.\n3. Conclusion: The declaration is subject to strict adherence."
    )
  }
  var examinerKeyPhrases by remember {
    mutableStateOf(
      listOf("Section 123", "Rule 3", "Accumulated Profits", "Paid-up Share Capital", "Free Reserves", "Preceding three years average")
    )
  }

  val navItems = listOf(
    NavItem.Syllabus,
    NavItem.TestGen,
    NavItem.Examiner,
    NavItem.Vault,
    NavItem.Analytics,
    NavItem.Desk
  )

  Scaffold(
    bottomBar = {
      NavigationBar(
        modifier = Modifier.testTag("main_bottom_nav")
      ) {
        navItems.forEach { item ->
          val isSelected = currentTab.route == item.route
          NavigationBarItem(
            selected = isSelected,
            onClick = { currentTab = item },
            icon = {
              Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label
              )
            },
            label = { Text(item.label) },
            modifier = Modifier.testTag("nav_item_${item.route}")
          )
        }
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      when (currentTab) {
        NavItem.Syllabus -> {
          SyllabusScreen(
            userId = user.id,
            studyRepository = studyRepository
          )
        }
        NavItem.TestGen -> {
          TestGenScreen(
            apiClient = apiClient,
            onNavigateToExaminer = { pName, qText, marks, ref, modelAns, keyPhrases ->
              examinerPaperName = pName
              examinerQuestionText = qText
              examinerMarks = marks
              examinerRef = ref
              examinerModelAnswer = modelAns
              examinerKeyPhrases = keyPhrases
              currentTab = NavItem.Examiner
            }
          )
        }
        NavItem.Examiner -> {
          ExaminerScreen(
            userId = user.id,
            apiClient = apiClient,
            studyRepository = studyRepository,
            initialPaperName = examinerPaperName,
            initialQuestionText = examinerQuestionText,
            initialMarks = examinerMarks,
            initialRef = examinerRef,
            initialModelAnswer = examinerModelAnswer,
            initialKeyPhrases = examinerKeyPhrases
          )
        }
        NavItem.Vault -> {
          MistakeVaultScreen(
            userId = user.id,
            studyRepository = studyRepository
          )
        }
        NavItem.Analytics -> {
          AnalyticsScreen(
            userId = user.id,
            studyRepository = studyRepository,
            user = user
          )
        }
        NavItem.Desk -> {
          StudyDeskScreen(
            user = user,
            apiClient = apiClient,
            studyRepository = studyRepository,
            authRepository = authRepository,
            onLogout = onLogout
          )
        }
      }
    }
  }
}
