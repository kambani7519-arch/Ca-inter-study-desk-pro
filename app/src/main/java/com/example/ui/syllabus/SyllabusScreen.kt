package com.example.ui.syllabus

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GroupType
import com.example.data.model.TopicItem
import com.example.data.model.TopicStatus
import com.example.data.repository.StudyRepository
import com.example.data.syllabus.IcaiSyllabusData
import com.example.ui.components.PaperTag
import com.example.ui.components.ProgressBarWithLabel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusDone
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusRevise
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusScreen(
  userId: Int,
  studyRepository: StudyRepository,
  modifier: Modifier = Modifier,
  onOpenPlanner: (() -> Unit)? = null
) {
  val scope = rememberCoroutineScope()
  val allTopics by studyRepository.getTopicsForUser(userId).collectAsState(initial = emptyList())

  var selectedGroup by remember { mutableStateOf<GroupType?>(null) } // null = All
  var selectedPaperId by remember { mutableStateOf<Int?>(1) } // default Paper 1
  var searchQuery by remember { mutableStateOf("") }
  var showAddDialog by remember { mutableStateOf(false) }

  // Filter papers
  val availablePapers = remember(selectedGroup) {
    if (selectedGroup == null) IcaiSyllabusData.papers
    else IcaiSyllabusData.papers.filter { it.group == selectedGroup }
  }

  // Filter topics
  val filteredTopics = remember(allTopics, selectedPaperId, searchQuery, selectedGroup) {
    allTopics.filter { topic ->
      val matchesPaper = if (selectedPaperId != null) topic.paperId == selectedPaperId else true
      val matchesGroup = if (selectedGroup != null) {
        val paper = IcaiSyllabusData.papers.find { it.id == topic.paperId }
        paper?.group == selectedGroup
      } else true
      val matchesSearch = if (searchQuery.isBlank()) true else {
        topic.topicTitle.contains(searchQuery, ignoreCase = true) ||
          topic.chapterName.contains(searchQuery, ignoreCase = true) ||
          topic.icaiRef.contains(searchQuery, ignoreCase = true)
      }
      matchesPaper && matchesGroup && matchesSearch
    }
  }

  // Group topics by chapter
  val topicsByChapter = remember(filteredTopics) {
    filteredTopics.groupBy { it.chapterName }
  }

  // Overall statistics
  val totalTopicsCount = allTopics.size
  val doneTopicsCount = allTopics.count { it.status == TopicStatus.DONE }
  val overallProgress = if (totalTopicsCount > 0) doneTopicsCount.toFloat() / totalTopicsCount else 0f

  val currentPaperTopics = remember(allTopics, selectedPaperId) {
    if (selectedPaperId != null) allTopics.filter { it.paperId == selectedPaperId } else allTopics
  }
  val paperDoneCount = currentPaperTopics.count { it.status == TopicStatus.DONE }
  val paperProgress = if (currentPaperTopics.isNotEmpty()) paperDoneCount.toFloat() / currentPaperTopics.size else 0f

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "ICAI Syllabus Tracker",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Jan 2027 Attempt • New Scheme",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        },
        actions = {
          if (onOpenPlanner != null) {
            IconButton(
              onClick = onOpenPlanner,
              modifier = Modifier.testTag("open_planner_btn")
            ) {
              Icon(
                Icons.Default.CalendarMonth,
                contentDescription = "Weekly Planner",
                tint = MaterialTheme.colorScheme.primary
              )
            }
          }
          IconButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.testTag("add_custom_topic_btn")
          ) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Custom Topic")
          }
        }
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { showAddDialog = true },
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Add Topic") },
        modifier = Modifier.testTag("fab_add_topic")
      )
    },
    modifier = modifier
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search AS, SA, Section, Chapter...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(Icons.Default.Close, contentDescription = "Clear")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .testTag("syllabus_search_input")
      )

      // Group Selector Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = selectedGroup == null,
          onClick = { selectedGroup = null },
          label = { Text("Both Groups") }
        )
        FilterChip(
          selected = selectedGroup == GroupType.GROUP_1,
          onClick = { selectedGroup = GroupType.GROUP_1 },
          label = { Text("Group 1") }
        )
        FilterChip(
          selected = selectedGroup == GroupType.GROUP_2,
          onClick = { selectedGroup = GroupType.GROUP_2 },
          label = { Text("Group 2") }
        )
      }

      // Paper Selector Tabs
      LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(availablePapers) { paper ->
          val isSelected = selectedPaperId == paper.id
          val paperTopics = allTopics.filter { it.paperId == paper.id }
          val paperDone = paperTopics.count { it.status == TopicStatus.DONE }
          val pct = if (paperTopics.isNotEmpty()) (paperDone * 100 / paperTopics.size) else 0

          Surface(
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .clickable { selectedPaperId = paper.id }
              .testTag("paper_tab_${paper.id}")
          ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
              Text(
                text = "P${paper.id}: ${paper.name.take(18)}...",
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
              Text(
                text = "$pct% Done (${paperDone}/${paperTopics.size})",
                color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary,
                fontSize = 10.sp
              )
            }
          }
        }
      }

      // Progress Card
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          val activePaper = IcaiSyllabusData.papers.find { it.id == selectedPaperId }
          ProgressBarWithLabel(
            label = "${activePaper?.name ?: "Overall Progress"} Completion",
            progress = paperProgress
          )
        }
      }

      // Topic List grouped by Chapter
      LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        if (topicsByChapter.isEmpty()) {
          item {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp)
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = Icons.Default.SearchOff,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "No topics found matching search query.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        topicsByChapter.forEach { (chapterName, topics) ->
          item(key = chapterName) {
            ChapterCard(
              chapterName = chapterName,
              topics = topics,
              onStatusChange = { topicId, newStatus ->
                scope.launch {
                  studyRepository.updateTopicStatus(userId, topicId, newStatus)
                }
              }
            )
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddCustomTopicDialog(
      userId = userId,
      defaultPaperId = selectedPaperId ?: 1,
      onDismiss = { showAddDialog = false },
      onAdd = { pId, chName, title, ref ->
        scope.launch {
          studyRepository.addCustomTopic(userId, pId, chName, title, ref)
          showAddDialog = false
        }
      }
    )
  }
}

@Composable
private fun ChapterCard(
  chapterName: String,
  topics: List<TopicItem>,
  onStatusChange: (String, TopicStatus) -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(12.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = chapterName,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f)
        )
        val doneInChapter = topics.count { it.status == TopicStatus.DONE }
        Text(
          text = "$doneInChapter/${topics.size}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
      Spacer(modifier = Modifier.height(10.dp))

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        topics.forEach { topic ->
          TopicRowItem(topic = topic, onStatusChange = { newStatus -> onStatusChange(topic.id, newStatus) })
        }
      }
    }
  }
}

@Composable
private fun TopicRowItem(
  topic: TopicItem,
  onStatusChange: (TopicStatus) -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .testTag("topic_row_${topic.id}")
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = topic.topicTitle,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium
      )
      if (topic.icaiRef.isNotBlank()) {
        Text(
          text = "Ref: ${topic.icaiRef}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.secondary,
          fontWeight = FontWeight.SemiBold
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    // Interactive 3-way toggle
    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      StatusIconButton(
        isSelected = topic.status == TopicStatus.PENDING,
        color = StatusPending,
        icon = Icons.Default.HourglassEmpty,
        tooltip = "Pending",
        onClick = { onStatusChange(TopicStatus.PENDING) }
      )
      StatusIconButton(
        isSelected = topic.status == TopicStatus.REVISE,
        color = StatusRevise,
        icon = Icons.Default.Repeat,
        tooltip = "Revise",
        onClick = { onStatusChange(TopicStatus.REVISE) }
      )
      StatusIconButton(
        isSelected = topic.status == TopicStatus.DONE,
        color = StatusDone,
        icon = Icons.Default.CheckCircle,
        tooltip = "Done",
        onClick = { onStatusChange(TopicStatus.DONE) }
      )
    }
  }
}

@Composable
private fun StatusIconButton(
  isSelected: Boolean,
  color: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  tooltip: String,
  onClick: () -> Unit
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .size(32.dp)
      .clip(CircleShape)
      .background(if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent)
      .clickable { onClick() }
  ) {
    Icon(
      imageVector = icon,
      contentDescription = tooltip,
      tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
      modifier = Modifier.size(18.dp)
    )
  }
}

@Composable
fun AddCustomTopicDialog(
  userId: Int,
  defaultPaperId: Int,
  onDismiss: () -> Unit,
  onAdd: (paperId: Int, chapterName: String, topicTitle: String, icaiRef: String) -> Unit
) {
  var paperId by remember { mutableStateOf(defaultPaperId) }
  var chapterName by remember { mutableStateOf("") }
  var topicTitle by remember { mutableStateOf("") }
  var icaiRef by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Custom Topic / Chapter") },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Target Paper:", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(IcaiSyllabusData.papers) { p ->
            FilterChip(
              selected = paperId == p.id,
              onClick = { paperId = p.id },
              label = { Text("P${p.id}") }
            )
          }
        }
        OutlinedTextField(
          value = chapterName,
          onValueChange = { chapterName = it },
          label = { Text("Chapter Name / Standard") },
          placeholder = { Text("e.g. Schedule III Financial Statements") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = topicTitle,
          onValueChange = { topicTitle = it },
          label = { Text("Topic Title") },
          placeholder = { Text("e.g. Contingent Assets Disclosure") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = icaiRef,
          onValueChange = { icaiRef = it },
          label = { Text("ICAI Section / AS / SA Ref") },
          placeholder = { Text("e.g. AS 29, Para 31") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onAdd(paperId, chapterName, topicTitle, icaiRef) },
        enabled = topicTitle.isNotBlank()
      ) {
        Text("Save Topic")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
