package np.com.petcareapplication.ui.pet

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import np.com.petcareapplication.model.CareTask
import np.com.petcareapplication.ui.components.PetCareButton
import np.com.petcareapplication.ui.components.PetCareTextField
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.viewmodel.PetViewModel
import java.text.SimpleDateFormat
import java.util.*

// Screen for changing a care task that already exists
@Composable
fun EditTaskScreen(
    taskId: String,
    onBack: () -> Unit,
    onTaskUpdated: () -> Unit,
    petViewModel: PetViewModel = viewModel()
) {
    val tasks by petViewModel.tasks.collectAsState()
    val allTasks by petViewModel.allTasks.collectAsState()
    // The task could have been opened from Pet Detail or from Home, so look in both lists
    val task = tasks.find { it.id == taskId } ?: allTasks.find { it.id == taskId }
    val isUploading by petViewModel.isImageUploading.collectAsState()
    val successMessage by petViewModel.successMessage.collectAsState()
    val errorMessage by petViewModel.errorMessage.collectAsState()

    // Show a loading spinner until the task has been found
    if (task == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        return
    }

    EditTaskScreenContent(
        task = task,
        isUploading = isUploading,
        successMessage = successMessage,
        errorMessage = errorMessage,
        onBack = onBack,
        onUpdateTask = { updatedTask, uri ->
            petViewModel.updateTaskWithImage(updatedTask, uri) { onTaskUpdated() }
        }
    )
}

// The form layout. Every box starts filled in with the task's current details
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreenContent(
    task: CareTask,
    isUploading: Boolean,
    successMessage: String?,
    errorMessage: String?,
    onBack: () -> Unit,
    onUpdateTask: (CareTask, Uri?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(task.title) }
    // Older tasks may not have a category saved, so fall back to "Other"
    var category by remember { mutableStateOf(task.category.ifBlank { "Other" }) }
    var notes by remember { mutableStateOf(task.notes) }
    var supplies by remember { mutableStateOf(task.supplies) }
    var type by remember { mutableStateOf(task.type) }
    var manualImageUrl by remember { mutableStateOf(task.imageUrl) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    // The schedule is saved as one line like "Every Monday at 08:00 AM",
    // so split it back into the day and the time to fill the form
    val initialDay = if (task.type == "WEEKLY" && task.schedule.contains("Every ")) task.schedule.substringAfter("Every ").substringBefore(" at") else "Monday"
    var selectedDay by remember { mutableStateOf(if (daysOfWeek.contains(initialDay)) initialDay else "Monday") }
    var timeSchedule by remember { mutableStateOf(if (task.schedule.contains(" at ")) task.schedule.substringAfter(" at ") else task.schedule) }
    // Use the saved date for one-time tasks, or today if there isn't one
    val calendar = Calendar.getInstance().apply { timeInMillis = if (task.dueDate > 0) task.dueDate else System.currentTimeMillis() }
    var selectedDate by remember { mutableStateOf(calendar) }
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    var dayExpanded by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var scheduleError by remember { mutableStateOf<String?>(null) }

    // A task still needs a title and a time
    fun validate(): Boolean {
        titleError = if (title.isBlank()) "Title is required" else null
        scheduleError = if (timeSchedule.isBlank()) "Please choose a time" else null
        return titleError == null && scheduleError == null
    }

    // Opens the gallery. Picking a picture clears the link box so only one photo is used
    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) { selectedImageUri = uri; manualImageUrl = "" }
    }
    // Android's own calendar pop-up, used for one-time tasks
    val datePickerDialog = DatePickerDialog(context, { _, y, m, d ->
        val newCal = Calendar.getInstance(); newCal.set(y, m, d); selectedDate = newCal
    }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH))

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Task", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(brush = Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))))) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Round photo preview. Tap it to choose a different picture
                Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).clickable(onClickLabel = "Choose a photo") { imagePickerLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                    val displayImage = selectedImageUri ?: manualImageUrl.ifEmpty { null }
                    if (displayImage != null) { AsyncImage(model = displayImage, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    else { Icon(Icons.Default.AddAPhoto, contentDescription = "Add photo", tint = MaterialTheme.colorScheme.primary) }
                }
                // Pasting a link is how photos are changed, since file uploads need Firebase Storage
                PetCareTextField(value = manualImageUrl, onValueChange = { manualImageUrl = it; if (it.isNotEmpty()) selectedImageUri = null }, label = "Image URL")
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Task Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        PetCareTextField(value = title, onValueChange = { title = it; if (titleError != null) titleError = null }, label = "Title", error = titleError)
                        // Same category list and time picker as the Add Task screen
                        CategoryDropdown(selected = category, onSelected = { category = it })
                        // Chips for choosing how often the task repeats
                        Text("How often?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = type == "DAILY", onClick = { type = "DAILY" }, label = { Text("Daily") })
                            FilterChip(selected = type == "WEEKLY", onClick = { type = "WEEKLY" }, label = { Text("Weekly") })
                            FilterChip(selected = type == "ONE-TIME", onClick = { type = "ONE-TIME" }, label = { Text("One-time") })
                        }
                        // Weekly tasks get a drop-down to choose the day
                        if (type == "WEEKLY") {
                            Box {
                                OutlinedTextField(value = selectedDay, onValueChange = {}, label = { Text("Day") }, modifier = Modifier.fillMaxWidth(), readOnly = true, trailingIcon = { IconButton(onClick = { dayExpanded = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose day") } }, shape = RoundedCornerShape(16.dp))
                                DropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) { daysOfWeek.forEach { day -> DropdownMenuItem(text = { Text(day) }, onClick = { selectedDay = day; dayExpanded = false }) } }
                            }
                        }
                        // One-time tasks get a date box. The calendar opens from the icon
                        if (type == "ONE-TIME") {
                            OutlinedTextField(value = dateFormatter.format(selectedDate.time), onValueChange = {}, label = { Text("Date") }, modifier = Modifier.fillMaxWidth(), readOnly = true, leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.EditCalendar, contentDescription = "Choose date") } }, shape = RoundedCornerShape(16.dp))
                        }
                        TimePickerField(time = timeSchedule, onTimeSelected = { timeSchedule = it; scheduleError = null }, error = scheduleError)
                    }
                }
                // Optional extras: what's needed for the task and any instructions
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Supplies & Instructions", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        PetCareTextField(value = supplies, onValueChange = { supplies = it }, label = "Supplies needed (optional)")
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Instructions / notes (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(16.dp))
                    }
                }
                PetCareButton(text = "Update Task", isLoading = isUploading, onClick = {
                    if (validate()) {
                        // Put the day or date and the time back together into one line
                        val finalSchedule = when(type) { "WEEKLY" -> "Every $selectedDay at $timeSchedule"; "ONE-TIME" -> "${dateFormatter.format(selectedDate.time)} at $timeSchedule"; else -> timeSchedule }
                        // copy() keeps the task's id, pet and completed state the same
                        val updatedTask = task.copy(title = title, category = category, schedule = finalSchedule, notes = notes, supplies = supplies, type = type, imageUrl = manualImageUrl, dueDate = selectedDate.timeInMillis)
                        // Use the picked file if there is one, otherwise the pasted link
                        val finalUri = selectedImageUri ?: if (manualImageUrl.isNotEmpty()) Uri.parse(manualImageUrl) else null
                        onUpdateTask(updatedTask, finalUri)
                    }
                })
            }
            // Green message when the changes are saved, red one if it fails
            if (successMessage != null) { Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp, start = 24.dp, end = 24.dp), shape = RoundedCornerShape(24.dp), color = Color(0xFF2E7D32), contentColor = Color.White, shadowElevation = 8.dp) { Text(text = successMessage, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) } }
            if (errorMessage != null) { Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp, start = 24.dp, end = 24.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError, shadowElevation = 8.dp) { Text(text = errorMessage, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) } }
        }
    }
}