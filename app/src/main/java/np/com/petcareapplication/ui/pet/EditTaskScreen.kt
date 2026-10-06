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

@Composable
fun EditTaskScreen(
    taskId: String,
    onBack: () -> Unit,
    onTaskUpdated: () -> Unit,
    petViewModel: PetViewModel = viewModel()
) {
    val tasks by petViewModel.tasks.collectAsState()
    val allTasks by petViewModel.allTasks.collectAsState()
    val task = tasks.find { it.id == taskId } ?: allTasks.find { it.id == taskId }
    val isUploading by petViewModel.isImageUploading.collectAsState()
    val successMessage by petViewModel.successMessage.collectAsState()
    val errorMessage by petViewModel.errorMessage.collectAsState()

    if (task == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BluePrimary) }
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
    var category by remember { mutableStateOf(task.category) }
    var notes by remember { mutableStateOf(task.notes) }
    var supplies by remember { mutableStateOf(task.supplies) }
    var type by remember { mutableStateOf(task.type) }
    var manualImageUrl by remember { mutableStateOf(task.imageUrl) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val initialDay = if (task.type == "WEEKLY" && task.schedule.contains("Every ")) task.schedule.substringAfter("Every ").substringBefore(" at") else "Monday"
    var selectedDay by remember { mutableStateOf(if (daysOfWeek.contains(initialDay)) initialDay else "Monday") }
    var timeSchedule by remember { mutableStateOf(if (task.schedule.contains(" at ")) task.schedule.substringAfter(" at ") else task.schedule) }
    val calendar = Calendar.getInstance().apply { timeInMillis = if (task.dueDate > 0) task.dueDate else System.currentTimeMillis() }
    var selectedDate by remember { mutableStateOf(calendar) }
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    var categoryExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? -> 
        if (uri != null) { selectedImageUri = uri; manualImageUrl = "" }
    }
    val datePickerDialog = DatePickerDialog(context, { _, y, m, d ->
        val newCal = Calendar.getInstance(); newCal.set(y, m, d); selectedDate = newCal
    }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH))

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Task", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(brush = Brush.verticalGradient(colors = listOf(Color.White, BluePrimary.copy(alpha = 0.05f))))) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(BluePrimary.copy(alpha = 0.1f)).clickable { imagePickerLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                    val displayImage = selectedImageUri ?: manualImageUrl.ifEmpty { null }
                    if (displayImage != null) { AsyncImage(model = displayImage, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    else { Icon(Icons.Default.AddAPhoto, null, tint = BluePrimary) }
                }
                PetCareTextField(value = manualImageUrl, onValueChange = { manualImageUrl = it; if (it.isNotEmpty()) selectedImageUri = null }, label = "Image URL")
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        PetCareTextField(value = title, onValueChange = { title = it }, label = "Title")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = type == "DAILY", onClick = { type = "DAILY" }, label = { Text("Daily") })
                            FilterChip(selected = type == "WEEKLY", onClick = { type = "WEEKLY" }, label = { Text("Weekly") })
                            FilterChip(selected = type == "ONE-TIME", onClick = { type = "ONE-TIME" }, label = { Text("One-time") })
                        }
                        if (type == "WEEKLY") {
                            Box {
                                OutlinedTextField(value = selectedDay, onValueChange = {}, label = { Text("Day") }, modifier = Modifier.fillMaxWidth(), readOnly = true, trailingIcon = { IconButton(onClick = { dayExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }, shape = RoundedCornerShape(16.dp))
                                DropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) { daysOfWeek.forEach { day -> DropdownMenuItem(text = { Text(day) }, onClick = { selectedDay = day; dayExpanded = false }) } }
                            }
                        }
                        if (type == "ONE-TIME") {
                            OutlinedTextField(value = dateFormatter.format(selectedDate.time), onValueChange = {}, label = { Text("Date") }, modifier = Modifier.fillMaxWidth(), readOnly = true, leadingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = BluePrimary) }, trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.EditCalendar, null) } }, shape = RoundedCornerShape(16.dp))
                        }
                        PetCareTextField(value = timeSchedule, onValueChange = { timeSchedule = it }, label = "Time")
                    }
                }
                PetCareButton(text = "Update Task", isLoading = isUploading, onClick = {
                    val finalSchedule = when(type) { "WEEKLY" -> "Every $selectedDay at $timeSchedule"; "ONE-TIME" -> "${dateFormatter.format(selectedDate.time)} at $timeSchedule"; else -> timeSchedule }
                    val updatedTask = task.copy(title = title, schedule = finalSchedule, notes = notes, supplies = supplies, type = type, imageUrl = manualImageUrl, dueDate = selectedDate.timeInMillis)
                    val finalUri = selectedImageUri ?: if (manualImageUrl.isNotEmpty()) Uri.parse(manualImageUrl) else null
                    onUpdateTask(updatedTask, finalUri)
                })
            }
            if (successMessage != null) { Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp, start = 24.dp, end = 24.dp), shape = RoundedCornerShape(24.dp), color = Color(0xFF2E7D32), contentColor = Color.White, shadowElevation = 8.dp) { Text(text = successMessage, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) } }
            if (errorMessage != null) { Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp, start = 24.dp, end = 24.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError, shadowElevation = 8.dp) { Text(text = errorMessage, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) } }
        }
    }
}
