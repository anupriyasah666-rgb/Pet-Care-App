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
import np.com.petcareapplication.viewmodel.AuthViewModel
import np.com.petcareapplication.viewmodel.PetViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddTaskScreen(
    petId: String,
    onBack: () -> Unit,
    onTaskAdded: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    petViewModel: PetViewModel = viewModel()
) {
    val user by authViewModel.user.collectAsState()
    val successMessage by petViewModel.successMessage.collectAsState()
    val errorMessage by petViewModel.errorMessage.collectAsState()
    val isUploading by petViewModel.isImageUploading.collectAsState()

    AddTaskScreenContent(
        isUploading = isUploading,
        successMessage = successMessage,
        errorMessage = errorMessage,
        onBack = onBack,
        onAddTask = { task, uri ->
            petViewModel.addTaskWithImage(task.copy(ownerId = user?.uid ?: "", petId = petId), uri) { onTaskAdded() }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreenContent(
    isUploading: Boolean,
    successMessage: String?,
    errorMessage: String?,
    onBack: () -> Unit,
    onAddTask: (CareTask, Uri?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Feeding") }
    var timeSchedule by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var supplies by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("DAILY") }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    var selectedDay by remember { mutableStateOf("Monday") }
    var dayExpanded by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf("") }

    var titleError by remember { mutableStateOf<String?>(null) }
    var scheduleError by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            imageUrl = ""
        }
    }

    val datePickerDialog = DatePickerDialog(context, { _, y, m, d ->
        val newCal = Calendar.getInstance(); newCal.set(y, m, d); selectedDate = newCal
    }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH))

    fun validate(): Boolean {
        var isValid = true
        if (title.isBlank()) { titleError = "Title is required"; isValid = false } else titleError = null
        if (timeSchedule.isBlank()) { scheduleError = "Please choose a time"; isValid = false } else scheduleError = null
        return isValid
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Add Care Task", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(brush = Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))))) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).clickable(onClickLabel = "Choose a photo") { imagePickerLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                    val displayImage = selectedImageUri ?: imageUrl.ifEmpty { null }
                    if (displayImage != null) { AsyncImage(model = displayImage, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    else { Icon(Icons.Default.AddAPhoto, contentDescription = "Add photo", tint = MaterialTheme.colorScheme.primary) }
                }
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), shadowElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Photo Link", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        PetCareTextField(value = imageUrl, onValueChange = { imageUrl = it; if (it.isNotEmpty()) selectedImageUri = null }, label = "Image URL")
                    }
                }
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Task Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        PetCareTextField(value = title, onValueChange = { title = it; if (titleError != null) titleError = null }, label = "Title", error = titleError)
                        CategoryDropdown(selected = category, onSelected = { category = it })
                        Text("How often?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = type == "DAILY", onClick = { type = "DAILY" }, label = { Text("Daily") })
                            FilterChip(selected = type == "WEEKLY", onClick = { type = "WEEKLY" }, label = { Text("Weekly") })
                            FilterChip(selected = type == "ONE-TIME", onClick = { type = "ONE-TIME" }, label = { Text("One-time") })
                        }
                        if (type == "WEEKLY") {
                            Box {
                                OutlinedTextField(value = selectedDay, onValueChange = {}, label = { Text("Day") }, modifier = Modifier.fillMaxWidth(), readOnly = true, trailingIcon = { IconButton(onClick = { dayExpanded = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose day") } }, shape = RoundedCornerShape(16.dp))
                                DropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) { daysOfWeek.forEach { day -> DropdownMenuItem(text = { Text(day) }, onClick = { selectedDay = day; dayExpanded = false }) } }
                            }
                        }
                        if (type == "ONE-TIME") {
                            OutlinedTextField(value = dateFormatter.format(selectedDate.time), onValueChange = {}, label = { Text("Date") }, modifier = Modifier.fillMaxWidth(), readOnly = true, leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }, trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.EditCalendar, contentDescription = "Choose date") } }, shape = RoundedCornerShape(16.dp))
                        }
                        TimePickerField(time = timeSchedule, onTimeSelected = { timeSchedule = it; scheduleError = null }, error = scheduleError)
                    }
                }
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Supplies & Instructions", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        PetCareTextField(value = supplies, onValueChange = { supplies = it }, label = "Supplies needed (optional)")
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Instructions / notes (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(16.dp))
                    }
                }
                PetCareButton(text = "Save Task", isLoading = isUploading, onClick = {
                    if (validate()) {
                        val finalSchedule = when(type) { "WEEKLY" -> "Every $selectedDay at $timeSchedule"; "ONE-TIME" -> "${dateFormatter.format(selectedDate.time)} at $timeSchedule"; else -> timeSchedule }
                        val task = CareTask(title = title, category = category, schedule = finalSchedule, notes = notes, supplies = supplies, type = type, dueDate = selectedDate.timeInMillis, imageUrl = imageUrl)
                        val finalUri = selectedImageUri ?: if (imageUrl.isNotEmpty()) Uri.parse(imageUrl) else null
                        onAddTask(task, finalUri)
                    }
                })
            }
            if (successMessage != null) { Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp, start = 24.dp, end = 24.dp), shape = RoundedCornerShape(24.dp), color = Color(0xFF2E7D32), contentColor = Color.White, shadowElevation = 8.dp) { Text(text = successMessage, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) } }
            if (errorMessage != null) { Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp, start = 24.dp, end = 24.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError, shadowElevation = 8.dp) { Text(text = errorMessage, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) } }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddTaskScreenPreview() {
    PetCareApplicationTheme { AddTaskScreenContent(isUploading = false, successMessage = null, errorMessage = null, onBack = {}, onAddTask = { _, _ -> }) }
}