package np.com.petcareapplication.ui.pet

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import np.com.petcareapplication.ui.components.PetCareButton
import np.com.petcareapplication.ui.components.PetCareTextField
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PinkHighlight
import np.com.petcareapplication.viewmodel.PetViewModel
import androidx.compose.foundation.verticalScroll
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    taskId: String,
    onBack: () -> Unit,
    onTaskUpdated: () -> Unit,
    petViewModel: PetViewModel = viewModel()
) {
    val context = LocalContext.current
    val tasks by petViewModel.tasks.collectAsState()
    val task = tasks.find { it.id == taskId }

    if (task == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BluePrimary)
        }
        return
    }

    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    
    // UI State initialized with current task data
    var title by remember { mutableStateOf(task.title) }
    var category by remember { mutableStateOf(task.category) }
    var notes by remember { mutableStateOf(task.notes) }
    var supplies by remember { mutableStateOf(task.supplies) }
    var type by remember { mutableStateOf(task.type) }
    
    // Logic to parse existing schedule
    val initialDay = if (task.type == "WEEKLY" && task.schedule.startsWith("Every ")) {
        task.schedule.substringAfter("Every ").substringBefore(" at")
    } else "Monday"
    
    val initialTime = if (task.type == "WEEKLY" && task.schedule.contains(" at ")) {
        task.schedule.substringAfter(" at ")
    } else if (task.type == "ONE-TIME" && task.schedule.contains(" at ")) {
        task.schedule.substringAfter(" at ")
    } else task.schedule

    var selectedDay by remember { mutableStateOf(if (daysOfWeek.contains(initialDay)) initialDay else "Monday") }
    var timeSchedule by remember { mutableStateOf(initialTime) }
    
    // Date state for One-time appointments
    val calendar = Calendar.getInstance().apply { timeInMillis = if (task.dueDate > 0) task.dueDate else System.currentTimeMillis() }
    var selectedDate by remember { mutableStateOf(calendar) }
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    var categoryExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }
    val isUploading by petViewModel.isImageUploading.collectAsState()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val newCal = Calendar.getInstance()
            newCal.set(year, month, dayOfMonth)
            selectedDate = newCal
        },
        selectedDate.get(Calendar.YEAR),
        selectedDate.get(Calendar.MONTH),
        selectedDate.get(Calendar.DAY_OF_MONTH)
    )

    fun validate(): Boolean {
        var isValid = true
        if (title.isBlank()) isValid = false
        if (timeSchedule.isBlank()) isValid = false
        return isValid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Care Task", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(brush = Brush.verticalGradient(colors = listOf(Color.White, BluePrimary.copy(alpha = 0.05f))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 8.dp) {
                    val categories = listOf("Feeding", "Exercise", "Grooming", "Medication", "Healthcare", "Cleaning")
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = "Task Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                        
                        PetCareTextField(value = title, onValueChange = { title = it }, label = "Title")

                        // Frequency Switcher
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = type == "DAILY", onClick = { type = "DAILY" }, label = { Text("Daily") })
                            FilterChip(selected = type == "WEEKLY", onClick = { type = "WEEKLY" }, label = { Text("Weekly") })
                            FilterChip(selected = type == "ONE-TIME", onClick = { type = "ONE-TIME" }, label = { Text("One-time") })
                        }

                        if (type == "WEEKLY") {
                            OutlinedTextField(
                                value = selectedDay, onValueChange = {}, label = { Text("Day") },
                                modifier = Modifier.fillMaxWidth(), readOnly = true,
                                trailingIcon = { IconButton(onClick = { dayExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                            )
                            DropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                                daysOfWeek.forEach { day -> DropdownMenuItem(text = { Text(day) }, onClick = { selectedDay = day; dayExpanded = false }) }
                            }
                        }

                        if (type == "ONE-TIME") {
                            OutlinedTextField(
                                value = dateFormatter.format(selectedDate.time),
                                onValueChange = {}, label = { Text("Appointment Date") },
                                modifier = Modifier.fillMaxWidth(), readOnly = true,
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = BluePrimary) },
                                trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.EditCalendar, null) } }
                            )
                        }

                        PetCareTextField(value = timeSchedule, onValueChange = { timeSchedule = it }, label = "Time")
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = "Additional Info", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                        PetCareTextField(value = supplies, onValueChange = { supplies = it }, label = "Required Supplies")
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Instructions") }, modifier = Modifier.fillMaxWidth().height(80.dp), shape = RoundedCornerShape(16.dp))
                    }
                }

                PetCareButton(
                    text = "Update Task",
                    isLoading = isUploading,
                    onClick = {
                        if (validate()) {
                            val finalSchedule = when(type) {
                                "WEEKLY" -> "Every $selectedDay at $timeSchedule"
                                "ONE-TIME" -> "${dateFormatter.format(selectedDate.time)} at $timeSchedule"
                                else -> timeSchedule
                            }
                            val updatedTask = task.copy(
                                title = title, category = category, schedule = finalSchedule,
                                notes = notes, supplies = supplies, type = type,
                                dueDate = if (type == "ONE-TIME") selectedDate.timeInMillis else task.dueDate
                            )
                            petViewModel.updateTask(updatedTask) { onTaskUpdated() }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
