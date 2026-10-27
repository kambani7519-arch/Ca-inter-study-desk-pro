package com.example.ui.planner

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.local.StudyScheduleEntity
import com.example.data.model.TopicItem
import com.example.data.model.TopicStatus
import com.example.data.repository.StudyRepository
import com.example.data.syllabus.IcaiSyllabusData
import com.example.ui.components.PaperTag
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

data class DayInfo(
  val dateString: String, // "YYYY-MM-DD"
  val dayOfWeekName: String, // "Mon", "Tue"
  val dayOfMonth: Int,
  val monthName: String,
  val isToday: Boolean
)

fun getWeekDates(weekOffset: Int = 0): List<DayInfo> {
  val cal = Calendar.getInstance()
  cal.firstDayOfWeek = Calendar.MONDAY
  cal.set(Calendar.HOUR_OF_DAY, 0)
  cal.set(Calendar.MINUTE, 0)
  cal.set(Calendar.SECOND, 0)
  cal.set(Calendar.MILLISECOND, 0)

  val todayCal = Calendar.getInstance()
  val todayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  val todayStr = todayFormat.format(todayCal.time)

  cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
  cal.add(Calendar.WEEK_OF_YEAR, weekOffset)

  val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
  val sdfDayName = SimpleDateFormat("EEE", Locale.getDefault())
  val sdfMonth = SimpleDateFormat("MMM", Locale.getDefault())

  val list = mutableListOf<DayInfo>()
  for (i in 0 until 7) {
    val dateStr = sdfDate.format(cal.time)
    val dayName = sdfDayName.format(cal.time)
    val monthName = sdfMonth.format(cal.time)
    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
    val isToday = (dateStr == todayStr)

    list.add(DayInfo(dateStr, dayName, dayOfMonth, monthName, isToday))
    cal.add(Calendar.DAY_OF_MONTH, 1)
  }
  return list
}

class DragDropPlannerState {
  var isDragging by mutableStateOf(false)
  var draggedTopic by mutableStateOf<TopicItem?>(null)
  var draggedScheduleItem by mutableStateOf<StudyScheduleEntity?>(null)
  var dragPosition by mutableStateOf(Offset.Zero)
  var hoveredDate by mutableStateOf<String?>(null)
  val dayBounds = mutableMapOf<String, Rect>()

  fun startDragTopic(topic: TopicItem, startPos: Offset) {
    isDragging = true
    draggedTopic = topic
    draggedScheduleItem = null
    dragPosition = startPos
  }

  fun startDragSchedule(schedule: StudyScheduleEntity, startPos: Offset) {
    isDragging = true
    draggedTopic = null
    draggedScheduleItem = schedule
    dragPosition = startPos
  }

  fun updateDrag(dragAmount: Offset) {
    dragPosition += dragAmount
    // Check which day bound contains current position
    hoveredDate = dayBounds.entries.firstOrNull { it.value.contains(dragPosition) }?.key
  }

  fun endDrag(): Pair<String?, Any?> {
    val targetDate = hoveredDate
    val payload = draggedTopic ?: draggedScheduleItem
    isDragging = false
    draggedTopic = null
    draggedScheduleItem = null
    hoveredDate = null
    return Pair(targetDate, payload)
  }

  fun cancelDrag() {
    isDragging = false
    draggedTopic = null
    draggedScheduleItem = null
    hoveredDate = null
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlannerScreen(
  userId: Int,
  studyRepository: StudyRepository,
  modifier: Modifier = Modifier,
  onNavigateBack: (() -> Unit)? = null
) {
  val scope = rememberCoroutineScope()
  var weekOffset by remember { mutableStateOf(0) }
  val weekDays = remember(weekOffset) { getWeekDates(weekOffset) }
  val startDate = weekDays.first().dateString
  val endDate = weekDays.last().dateString

  val scheduledItems by studyRepository.getWeeklySchedule(userId, startDate, endDate)
    .collectAsState(initial = emptyList())
  val allTopics by studyRepository.getTopicsForUser(userId)
    .collectAsState(initial = emptyList())

  val dragDropState = remember { DragDropPlannerState() }

  var showTraySheet by remember { mutableStateOf(false) }
  var showQuickScheduleDialog by remember { mutableStateOf<TopicItem?>(null) }
  var showAutoPlanDialog by remember { mutableStateOf(false) }
  var selectedFilterPaperId by remember { mutableStateOf<Int?>(null) }
  var traySearchQuery by remember { mutableStateOf("") }
  var selectedDaySlotFilter by remember { mutableStateOf<String?>(null) } // null = view all days

  // Filter topics for tray (unscheduled or pending)
  val scheduledTopicIds = remember(scheduledItems) { scheduledItems.map { it.topicId }.toSet() }
  val availableTopics = remember(allTopics, scheduledTopicIds, selectedFilterPaperId, traySearchQuery) {
    allTopics.filter { topic ->
      val matchesPaper = if (selectedFilterPaperId != null) topic.paperId == selectedFilterPaperId else true
      val matchesSearch = if (traySearchQuery.isBlank()) true else {
        topic.topicTitle.contains(traySearchQuery, ignoreCase = true) ||
          topic.icaiRef.contains(traySearchQuery, ignoreCase = true) ||
          topic.chapterName.contains(traySearchQuery, ignoreCase = true)
      }
      matchesPaper && matchesSearch
    }
  }

  // Calculate statistics for current week
  val totalScheduled = scheduledItems.size
  val completedScheduled = scheduledItems.count { it.isCompleted }
  val weekProgress = if (totalScheduled > 0) completedScheduled.toFloat() / totalScheduled else 0f

  val snackbarHostState = remember { SnackbarHostState() }

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("weekly_planner_screen")
  ) {
    Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        TopAppBar(
          title = {
            Column {
              Text(
                text = "Weekly Revision Planner",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${weekDays.first().monthName} ${weekDays.first().dayOfMonth} - ${weekDays.last().monthName} ${weekDays.last().dayOfMonth}, 2026",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          },
          navigationIcon = {
            if (onNavigateBack != null) {
              IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
              }
            } else {
              Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.padding(start = 12.dp)
              )
            }
          },
          actions = {
            // Auto-plan button
            IconButton(
              onClick = { showAutoPlanDialog = true },
              modifier = Modifier.testTag("auto_plan_btn")
            ) {
              Icon(
                Icons.Default.AutoFixHigh,
                contentDescription = "Auto Plan Week",
                tint = MaterialTheme.colorScheme.primary
              )
            }
            // Open topics drawer
            IconButton(
              onClick = { showTraySheet = true },
              modifier = Modifier.testTag("open_topics_tray_btn")
            ) {
              BadgedBox(
                badge = {
                  val unscheduledCount = allTopics.count { it.status != TopicStatus.DONE && it.id !in scheduledTopicIds }
                  if (unscheduledCount > 0) {
                    Badge { Text(if (unscheduledCount > 99) "99+" else "$unscheduledCount") }
                  }
                }
              ) {
                Icon(Icons.Default.LibraryAdd, contentDescription = "Syllabus Drawer")
              }
            }
          }
        )
      },
      floatingActionButton = {
        ExtendedFloatingActionButton(
          onClick = { showTraySheet = true },
          icon = { Icon(Icons.Default.Add, contentDescription = null) },
          text = { Text("Add Topics") },
          modifier = Modifier.testTag("fab_add_topics_to_planner")
        )
      }
    ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        // Week Switcher Bar
        WeekNavigationHeader(
          weekOffset = weekOffset,
          startDate = weekDays.first(),
          endDate = weekDays.last(),
          onPrevWeek = { weekOffset-- },
          onNextWeek = { weekOffset++ },
          onToday = { weekOffset = 0 }
        )

        // Weekly Summary Card
        WeeklySummaryCard(
          totalScheduled = totalScheduled,
          completedScheduled = completedScheduled,
          progress = weekProgress,
          onClearWeek = {
            scope.launch {
              studyRepository.clearWeeklySchedule(userId, startDate, endDate)
              snackbarHostState.showSnackbar("Cleared schedule for this week")
            }
          }
        )

        // Day Selector Pills Row
        DayPillsRow(
          weekDays = weekDays,
          scheduledItems = scheduledItems,
          selectedDay = selectedDaySlotFilter,
          onSelectDay = { selectedDaySlotFilter = if (selectedDaySlotFilter == it) null else it }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Days List / Column
        val displayedDays = remember(selectedDaySlotFilter, weekDays) {
          if (selectedDaySlotFilter == null) weekDays
          else weekDays.filter { it.dateString == selectedDaySlotFilter }
        }

        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 80.dp)
        ) {
          items(displayedDays, key = { it.dateString }) { day ->
            val daySchedules = scheduledItems.filter { it.scheduledDate == day.dateString }
            DayCard(
              day = day,
              schedules = daySchedules,
              dragDropState = dragDropState,
              onToggleComplete = { id, completed ->
                scope.launch {
                  studyRepository.toggleScheduleCompleted(id, completed)
                }
              },
              onRemoveSchedule = { id ->
                scope.launch {
                  studyRepository.removeScheduleItem(id)
                  snackbarHostState.showSnackbar("Removed topic from day")
                }
              },
              onRolloverNextDay = { schedule ->
                val nextDayIndex = weekDays.indexOfFirst { it.dateString == schedule.scheduledDate } + 1
                if (nextDayIndex in weekDays.indices) {
                  val nextDate = weekDays[nextDayIndex].dateString
                  scope.launch {
                    studyRepository.moveScheduleSlot(schedule.id, nextDate, schedule.timeSlot)
                    snackbarHostState.showSnackbar("Shifted to ${weekDays[nextDayIndex].dayOfWeekName}")
                  }
                } else {
                  scope.launch {
                    snackbarHostState.showSnackbar("Cannot roll over past current week. Use Next Week.")
                  }
                }
              },
              onAddTopicClick = {
                showTraySheet = true
              }
            )
          }
        }
      }
    }

    // Floating drag preview that follows the user's touch
    if (dragDropState.isDragging) {
      val title = dragDropState.draggedTopic?.topicTitle
        ?: dragDropState.draggedScheduleItem?.topicTitle
        ?: "Dragging Topic"
      val ref = dragDropState.draggedTopic?.icaiRef
        ?: dragDropState.draggedScheduleItem?.icaiRef
        ?: ""
      val pId = dragDropState.draggedTopic?.paperId
        ?: dragDropState.draggedScheduleItem?.paperId
        ?: 1

      Box(
        modifier = Modifier
          .fillMaxSize()
          .zIndex(999f)
      ) {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .offset {
              IntOffset(
                x = (dragDropState.dragPosition.x - 140).roundToInt().coerceAtLeast(16),
                y = (dragDropState.dragPosition.y - 80).roundToInt().coerceAtLeast(16)
              )
            }
            .width(280.dp)
            .scale(1.05f)
            .shadow(16.dp, RoundedCornerShape(12.dp))
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.DragIndicator,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                PaperTag(paperId = pId)
                if (ref.isNotBlank()) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = ref,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }

    // Unscheduled Syllabus Topics Drawer/Sheet
    if (showTraySheet) {
      ModalBottomSheet(
        onDismissRequest = { showTraySheet = false },
        modifier = Modifier.testTag("syllabus_tray_sheet")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 16.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "ICAI Syllabus Topics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Long-press to drag into calendar day, or tap '+' to schedule",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            IconButton(onClick = { showTraySheet = false }) {
              Icon(Icons.Default.Close, contentDescription = "Close")
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Search bar
          OutlinedTextField(
            value = traySearchQuery,
            onValueChange = { traySearchQuery = it },
            placeholder = { Text("Search AS, Section, Chapter...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
              if (traySearchQuery.isNotEmpty()) {
                IconButton(onClick = { traySearchQuery = "" }) {
                  Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
              }
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("tray_search_field")
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Paper Filter Chips
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            item {
              FilterChip(
                selected = selectedFilterPaperId == null,
                onClick = { selectedFilterPaperId = null },
                label = { Text("All Papers") }
              )
            }
            items(IcaiSyllabusData.papers) { paper ->
              FilterChip(
                selected = selectedFilterPaperId == paper.id,
                onClick = {
                  selectedFilterPaperId = if (selectedFilterPaperId == paper.id) null else paper.id
                },
                label = { Text("P${paper.id}: ${paper.code}") }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Topics List
          if (availableTopics.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "No matching topics found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              contentPadding = PaddingValues(vertical = 8.dp)
            ) {
              items(availableTopics, key = { it.id }) { topic ->
                val isAlreadyScheduled = topic.id in scheduledTopicIds
                TrayTopicCard(
                  topic = topic,
                  isScheduledThisWeek = isAlreadyScheduled,
                  dragDropState = dragDropState,
                  onQuickSchedule = {
                    showQuickScheduleDialog = topic
                  },
                  onDragCompleted = { targetDate ->
                    if (targetDate != null) {
                      scope.launch {
                        studyRepository.scheduleTopic(userId, topic, targetDate, "Morning")
                        snackbarHostState.showSnackbar("Scheduled '${topic.topicTitle}' on $targetDate")
                      }
                      showTraySheet = false
                    }
                  }
                )
              }
            }
          }
        }
      }
    }

    // Quick Schedule Dialog (One-Tap fallback for effortless ergonomics)
    showQuickScheduleDialog?.let { topic ->
      QuickScheduleDialog(
        topic = topic,
        weekDays = weekDays,
        onDismiss = { showQuickScheduleDialog = null },
        onSchedule = { selectedDate, slot ->
          scope.launch {
            studyRepository.scheduleTopic(userId, topic, selectedDate, slot)
            snackbarHostState.showSnackbar("Scheduled on $selectedDate ($slot)")
          }
          showQuickScheduleDialog = null
        }
      )
    }

    // Auto-Plan Confirmation Dialog
    if (showAutoPlanDialog) {
      AlertDialog(
        onDismissRequest = { showAutoPlanDialog = false },
        icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Auto-Plan This Week?") },
        text = {
          Text(
            "The planner will automatically distribute pending syllabus topics across Monday to Sunday (2 topics per day with Morning & Evening revision slots)."
          )
        },
        confirmButton = {
          Button(
            onClick = {
              scope.launch {
                val dates = weekDays.map { it.dateString }
                studyRepository.autoPlanWeek(userId, dates)
                snackbarHostState.showSnackbar("Generated 7-day revision schedule!")
              }
              showAutoPlanDialog = false
            },
            modifier = Modifier.testTag("confirm_auto_plan_btn")
          ) {
            Text("Generate Plan")
          }
        },
        dismissButton = {
          TextButton(onClick = { showAutoPlanDialog = false }) {
            Text("Cancel")
          }
        }
      )
    }
  }
}

@Composable
fun WeekNavigationHeader(
  weekOffset: Int,
  startDate: DayInfo,
  endDate: DayInfo,
  onPrevWeek: () -> Unit,
  onNextWeek: () -> Unit,
  onToday: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    IconButton(
      onClick = onPrevWeek,
      modifier = Modifier.testTag("prev_week_btn")
    ) {
      Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Week")
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = "${startDate.dayOfMonth} ${startDate.monthName} – ${endDate.dayOfMonth} ${endDate.monthName}",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
      )
      if (weekOffset != 0) {
        AssistChip(
          onClick = onToday,
          label = { Text("Today", style = MaterialTheme.typography.labelSmall) },
          leadingIcon = {
            Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp))
          },
          modifier = Modifier
            .height(28.dp)
            .testTag("today_btn")
        )
      }
    }

    IconButton(
      onClick = onNextWeek,
      modifier = Modifier.testTag("next_week_btn")
    ) {
      Icon(Icons.Default.ChevronRight, contentDescription = "Next Week")
    }
  }
}

@Composable
fun WeeklySummaryCard(
  totalScheduled: Int,
  completedScheduled: Int,
  progress: Float,
  onClearWeek: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "$completedScheduled of $totalScheduled Topics Revised",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
      }

      if (totalScheduled > 0) {
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
          onClick = onClearWeek,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            Icons.Default.DeleteSweep,
            contentDescription = "Clear Week",
            tint = MaterialTheme.colorScheme.outline
          )
        }
      }
    }
  }
}

@Composable
fun DayPillsRow(
  weekDays: List<DayInfo>,
  scheduledItems: List<StudyScheduleEntity>,
  selectedDay: String?,
  onSelectDay: (String) -> Unit
) {
  LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    items(weekDays) { day ->
      val isSelected = selectedDay == day.dateString
      val dayCount = scheduledItems.count { it.scheduledDate == day.dateString }
      val completedCount = scheduledItems.count { it.scheduledDate == day.dateString && it.isCompleted }

      Surface(
        onClick = { onSelectDay(day.dateString) },
        shape = RoundedCornerShape(12.dp),
        color = when {
          isSelected -> MaterialTheme.colorScheme.primary
          day.isToday -> MaterialTheme.colorScheme.primaryContainer
          else -> MaterialTheme.colorScheme.surfaceVariant
        },
        border = if (day.isToday && !isSelected) {
          androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else null,
        modifier = Modifier
          .width(52.dp)
          .height(64.dp)
          .testTag("day_pill_${day.dateString}")
      ) {
        Column(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = day.dayOfWeekName.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else if (day.isToday) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "${day.dayOfMonth}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else if (day.isToday) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
          )
          // Indicator dots or badge
          if (dayCount > 0) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(
                  if (completedCount == dayCount) StatusDone
                  else if (isSelected) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.primary
                )
            )
          } else {
            Spacer(modifier = Modifier.height(6.dp))
          }
        }
      }
    }
  }
}

@Composable
fun DayCard(
  day: DayInfo,
  schedules: List<StudyScheduleEntity>,
  dragDropState: DragDropPlannerState,
  onToggleComplete: (Long, Boolean) -> Unit,
  onRemoveSchedule: (Long) -> Unit,
  onRolloverNextDay: (StudyScheduleEntity) -> Unit,
  onAddTopicClick: () -> Unit
) {
  val isHovered = dragDropState.isDragging && dragDropState.hoveredDate == day.dateString

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isHovered) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
      } else {
        MaterialTheme.colorScheme.surface
      }
    ),
    border = if (isHovered) {
      androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else if (day.isToday) {
      androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
    } else {
      androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    },
    modifier = Modifier
      .fillMaxWidth()
      .onGloballyPositioned { coords ->
        dragDropState.dayBounds[day.dateString] = coords.boundsInRoot()
      }
      .testTag("day_card_${day.dateString}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      // Day Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "${day.dayOfWeekName}, ${day.monthName} ${day.dayOfMonth}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          if (day.isToday) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              color = MaterialTheme.colorScheme.primary,
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = "TODAY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "${schedules.size} topics",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.width(4.dp))
          IconButton(
            onClick = onAddTopicClick,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              Icons.Default.AddCircleOutline,
              contentDescription = "Add topic to ${day.dayOfWeekName}",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Scheduled items in day
      if (schedules.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
              if (isHovered) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (isHovered) "Release to drop topic here" else "No topics planned • Drag here or tap +",
            style = MaterialTheme.typography.bodySmall,
            color = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          schedules.forEach { item ->
            ScheduledTopicItemRow(
              item = item,
              dragDropState = dragDropState,
              onToggleComplete = { onToggleComplete(item.id, it) },
              onRemove = { onRemoveSchedule(item.id) },
              onRollover = { onRolloverNextDay(item) }
            )
          }
        }
      }
    }
  }
}

@Composable
fun ScheduledTopicItemRow(
  item: StudyScheduleEntity,
  dragDropState: DragDropPlannerState,
  onToggleComplete: (Boolean) -> Unit,
  onRemove: () -> Unit,
  onRollover: () -> Unit
) {
  var itemPosition by remember { mutableStateOf(Offset.Zero) }

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (item.isCompleted) {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
      MaterialTheme.colorScheme.surfaceVariant
    },
    modifier = Modifier
      .fillMaxWidth()
      .onGloballyPositioned { coords ->
        itemPosition = coords.boundsInRoot().topLeft
      }
      .testTag("schedule_item_${item.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Drag Handle with long press
      Box(
        modifier = Modifier
          .size(36.dp)
          .pointerInput(item) {
            detectDragGesturesAfterLongPress(
              onDragStart = { offset ->
                dragDropState.startDragSchedule(item, itemPosition + offset)
              },
              onDrag = { change, dragAmount ->
                change.consume()
                dragDropState.updateDrag(dragAmount)
              },
              onDragEnd = {
                val (targetDate, payload) = dragDropState.endDrag()
                if (targetDate != null && payload is StudyScheduleEntity) {
                  // Handled by parent or moveSlot
                }
              },
              onDragCancel = {
                dragDropState.cancelDrag()
              }
            )
          },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.DragHandle,
          contentDescription = "Long press to move",
          tint = MaterialTheme.colorScheme.outline
        )
      }

      // Completion Checkbox
      Checkbox(
        checked = item.isCompleted,
        onCheckedChange = onToggleComplete,
        modifier = Modifier.testTag("checkbox_${item.id}")
      )

      // Content info
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 4.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          PaperTag(paperId = item.paperId)
          if (item.icaiRef.isNotBlank()) {
            Text(
              text = item.icaiRef,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
          // Slot badge
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface
          ) {
            Text(
              text = item.timeSlot,
              style = MaterialTheme.typography.labelSmall,
              fontSize = 9.sp,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = item.topicTitle,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
          textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
          color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      // Actions: Rollover (->) and Delete
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onRollover,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            Icons.Default.Redo,
            contentDescription = "Shift to Next Day",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
        }
        IconButton(
          onClick = onRemove,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            Icons.Default.Close,
            contentDescription = "Remove",
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
fun TrayTopicCard(
  topic: TopicItem,
  isScheduledThisWeek: Boolean,
  dragDropState: DragDropPlannerState,
  onQuickSchedule: () -> Unit,
  onDragCompleted: (String?) -> Unit
) {
  var cardPosition by remember { mutableStateOf(Offset.Zero) }

  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .onGloballyPositioned { coords ->
        cardPosition = coords.boundsInRoot().topLeft
      }
      .testTag("tray_topic_${topic.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Drag touch target
      Box(
        modifier = Modifier
          .size(40.dp)
          .pointerInput(topic) {
            detectDragGesturesAfterLongPress(
              onDragStart = { offset ->
                dragDropState.startDragTopic(topic, cardPosition + offset)
              },
              onDrag = { change, dragAmount ->
                change.consume()
                dragDropState.updateDrag(dragAmount)
              },
              onDragEnd = {
                val (targetDate, _) = dragDropState.endDrag()
                onDragCompleted(targetDate)
              },
              onDragCancel = {
                dragDropState.cancelDrag()
              }
            )
          },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.DragIndicator,
          contentDescription = "Long press to drag",
          tint = MaterialTheme.colorScheme.primary
        )
      }

      Column(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 6.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          PaperTag(paperId = topic.paperId)
          if (topic.icaiRef.isNotBlank()) {
            Text(
              text = topic.icaiRef,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
          if (isScheduledThisWeek) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = MaterialTheme.colorScheme.primaryContainer
            ) {
              Text(
                text = "Planned",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = topic.topicTitle,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = topic.chapterName,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      // Quick-Tap Add Button
      FilledTonalButton(
        onClick = onQuickSchedule,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier
          .height(36.dp)
          .testTag("btn_schedule_${topic.id}")
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Schedule", style = MaterialTheme.typography.labelMedium)
      }
    }
  }
}

@Composable
fun QuickScheduleDialog(
  topic: TopicItem,
  weekDays: List<DayInfo>,
  onDismiss: () -> Unit,
  onSchedule: (String, String) -> Unit
) {
  var selectedDate by remember { mutableStateOf(weekDays.first().dateString) }
  var selectedSlot by remember { mutableStateOf("Morning") }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
    title = { Text("Schedule Topic Revision", style = MaterialTheme.typography.titleMedium) },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = topic.topicTitle,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold
        )
        if (topic.icaiRef.isNotBlank()) {
          Text(
            text = "ICAI Ref: ${topic.icaiRef}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Select Day:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        // Day Selection Chips
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(weekDays) { day ->
            FilterChip(
              selected = selectedDate == day.dateString,
              onClick = { selectedDate = day.dateString },
              label = { Text("${day.dayOfWeekName} ${day.dayOfMonth}") }
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Revision Time Slot:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          listOf("Morning", "Afternoon", "Evening").forEach { slot ->
            FilterChip(
              selected = selectedSlot == slot,
              onClick = { selectedSlot = slot },
              label = { Text(slot) }
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSchedule(selectedDate, selectedSlot) },
        modifier = Modifier.testTag("confirm_schedule_btn")
      ) {
        Text("Schedule")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
