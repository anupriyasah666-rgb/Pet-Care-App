package np.com.petcareapplication.ui.pet

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import np.com.petcareapplication.model.CareTask
import np.com.petcareapplication.ui.components.PetCareButton
import np.com.petcareapplication.ui.components.PetCareTextField
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PinkHighlight
import np.com.petcareapplication.viewmodel.AuthViewModel
import np.com.petcareapplication.viewmodel.PetViewModel
import androidx.compose.foundation.verticalScroll
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    petId: String,
    onBack: () -> Unit,
    onTaskAdded: () -> Unit,
    authViewModel: AuthViewModel = viewModel(), // Added to get user ID
    petViewModel: PetViewModel = viewModel()
) {
    val context = LocalContext.current
    val user by authViewModel.user.collectAsState()
    val successMessage by petViewModel.successMessage.collectAsState()
    
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
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> selectedImageUri = uri }

    var titleError by remember { mutableStateOf<String?>(null) }
    var scheduleError by remember { mutableStateOf<String?>(null) }
    val categories = listOf("Feeding", "Exercise", "Grooming", "Medication", "Healthcare", "Cleaning")
    var categoryExpanded by remember { mutableStateOf(false) }
    val isUploading by petViewModel.isImageUploading.collectAsState()

    val datePickerDialog = DatePickerDialog(context, { _, y, m, d ->
        selectedDate.set(y, m, d)
        val newDate = Calendar.getInstance(); newDate.timeInMillis = selectedDate.timeInMillis; selectedDate = newDate
    }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH))

    fun validate(): Boolean {
        var isValid = true
        if (title.isBlank()) { titleError = "Required"; isValid = false }
        if (timeSchedule.isBlank()) { scheduleError = "Required"; isValid = false }
        return isValid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Care Task", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).background(brush = Brush.verticalGradient(colors = listOf(Color.White, BluePrimary.copy(alpha = 0.05f))))
                .padding(16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Task Image
                Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(20.dp)).background(BluePrimary.copy(alpha = 0.05f)).clickable { imagePickerLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                    if (selectedImageUri != null) AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Icon(Icons.Default.AddAPhoto, null, tint = BluePrimary, modifier = Modifier.size(32.dp))
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Task Details", fontWeight = FontWeight.Bold, color = BluePrimary)
                        PetCareTextField(value = title, onValueChange = { title = it }, label = "Title", error = titleError)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = type == "DAILY", onClick = { type = "DAILY" }, label = { Text("Daily") })
                            FilterChip(selected = type == "WEEKLY", onClick = { type = "WEEKLY" }, label = { Text("Weekly") })
                            FilterChip(selected = type == "ONE-TIME", onClick = { type = "ONE-TIME" }, label = { Text("One-time") })
                        }

                        if (type == "WEEKLY") {
                            OutlinedTextField(value = selectedDay, onValueChange = {}, label = { Text("Day") }, modifier = Modifier.fillMaxWidth(), readOnly = true,
                                trailingIcon = { IconButton(onClick = { dayExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                            )
                            DropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                                daysOfWeek.forEach { day -> DropdownMenuItem(text = { Text(day) }, onClick = { selectedDay = day; dayExpanded = false }) }
                            }
                        }

                        if (type == "ONE-TIME") {
                            OutlinedTextField(value = dateFormatter.format(selectedDate.time), onValueChange = {}, label = { Text("Date") }, modifier = Modifier.fillMaxWidth(), readOnly = true,
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = BluePrimary) },
                                trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.EditCalendar, null) } }
                            )
                        }
                        PetCareTextField(value = timeSchedule, onValueChange = { timeSchedule = it }, label = "Time", error = scheduleError)
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Additional Info", fontWeight = FontWeight.Bold, color = BluePrimary)
                        PetCareTextField(value = supplies, onValueChange = { supplies = it }, label = "Required Supplies")
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Instructions") }, modifier = Modifier.fillMaxWidth().height(80.dp), shape = RoundedCornerShape(16.dp))
                    }
                }

                PetCareButton(
                    text = "Save Task",
                    isLoading = isUploading,
                    onClick = {
                        if (validate()) {
                            val finalSchedule = when(type) {
                                "WEEKLY" -> "Every $selectedDay at $timeSchedule"
                                "ONE-TIME" -> "${dateFormatter.format(selectedDate.time)} at $timeSchedule"
                                else -> timeSchedule
                            }
                            val task = CareTask(
                                petId = petId,
                                ownerId = user?.uid ?: "", // Requirement: Associate with owner for consolidation
                                title = title, category = category, schedule = finalSchedule,
                                notes = notes, supplies = supplies, type = type,
                                dueDate = selectedDate.timeInMillis
                            )
                            petViewModel.addTaskWithImage(task, selectedImageUri) { onTaskAdded() }
                        }
                    }
                )
            }

            // SUCCESS MESSAGE OVERLAY IN GREEN COLOR
            if (successMessage != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF2E7D32), // Dark Green Background
                    contentColor = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = successMessage!!,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
