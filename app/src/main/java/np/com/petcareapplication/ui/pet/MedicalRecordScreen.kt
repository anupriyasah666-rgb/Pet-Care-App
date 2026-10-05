package np.com.petcareapplication.ui.pet

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import np.com.petcareapplication.model.CareTask
import np.com.petcareapplication.model.MedicalRecord
import np.com.petcareapplication.ui.components.PetCareCard
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PinkHighlight
import np.com.petcareapplication.viewmodel.AuthViewModel
import np.com.petcareapplication.viewmodel.MedicalViewModel
import np.com.petcareapplication.viewmodel.PetViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * MedicalRecordScreen: Tracks health history.
 * Fulfills core requirement: "Manage healthcare records".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordScreen(
    petId: String,
    onBack: () -> Unit,
    medicalViewModel: MedicalViewModel = viewModel(),
    petViewModel: PetViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val records by medicalViewModel.records.collectAsState()
    val successMessage by medicalViewModel.successMessage.collectAsState()
    val user by authViewModel.user.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(petId) { medicalViewModel.loadRecords(petId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Records", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BluePrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Log Health Event") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(brush = Brush.verticalGradient(colors = listOf(Color.White, BluePrimary.copy(alpha = 0.05f))))) {
            if (records.isEmpty()) { EmptyMedicalPlaceholder() } 
            else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(records) { record ->
                        MedicalRecordItem(record = record, onDelete = { medicalViewModel.deleteRecord(record.id) })
                    }
                }
            }

            // SUCCESS MESSAGE OVERLAY WITH DYNAMIC COLOR
            if (successMessage != null) {
                val bgColor = if (successMessage == "Health record deleted") PinkHighlight else Color(0xFF2E7D32)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp, start = 24.dp, end = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = bgColor,
                    contentColor = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = successMessage!!,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            if (showAddDialog) {
                AddMedicalRecordDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { type, notes, timestamp, addToChecklist ->
                        // 1. Log the record
                        medicalViewModel.addRecord(MedicalRecord(petId = petId, type = type, notes = notes, date = timestamp))
                        
                        // 2. SCENARIO SYNC: If Luna's vaccination is due, add it to the routine checklist
                        if (addToChecklist) {
                            val df = SimpleDateFormat("MMM dd", Locale.getDefault())
                            petViewModel.addTask(
                                CareTask(
                                    petId = petId,
                                    ownerId = user?.uid ?: "",
                                    title = "$type Appointment",
                                    category = "Healthcare",
                                    schedule = df.format(Date(timestamp)),
                                    notes = notes,
                                    type = "ONE-TIME",
                                    dueDate = timestamp
                                ),
                                onComplete = { /* Task added successfully */ }
                            )
                        }
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun MedicalRecordItem(record: MedicalRecord, onDelete: () -> Unit) {
    val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    PetCareCard {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = BluePrimary.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MedicalServices, null, tint = BluePrimary) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = record.type, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = record.notes, fontSize = 13.sp, color = Color.Gray)
                Text(text = df.format(Date(record.date)), fontSize = 11.sp, color = Color.LightGray)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.LightGray, modifier = Modifier.size(20.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicalRecordDialog(onDismiss: () -> Unit, onConfirm: (String, String, Long, Boolean) -> Unit) {
    val context = LocalContext.current
    var type by remember { mutableStateOf("Vaccination") }
    var notes by remember { mutableStateOf("") }
    var addToChecklist by remember { mutableStateOf(true) }
    
    // NEW: Date Picker support for medical records
    val calendar = Calendar.getInstance()
    var selectedDate by remember { mutableStateOf(calendar) }
    val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val datePickerDialog = DatePickerDialog(context, { _, y, m, d ->
        val newCal = Calendar.getInstance(); newCal.set(y, m, d)
        selectedDate = newCal
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Health Event", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Vaccination", "Checkup", "Medication").forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t, fontSize = 10.sp) })
                    }
                }
                
                OutlinedTextField(
                    value = df.format(selectedDate.time), onValueChange = {}, label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth(), readOnly = true,
                    trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.Event, null) } }
                )

                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth().height(80.dp), shape = RoundedCornerShape(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = addToChecklist, onCheckedChange = { addToChecklist = it })
                    Text("Add to care checklist", fontSize = 12.sp)
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(type, notes, selectedDate.timeInMillis, addToChecklist) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EmptyMedicalPlaceholder() {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.HealthAndSafety, null, modifier = Modifier.size(80.dp), tint = Color.LightGray.copy(alpha = 0.5f))
        Text("No health records logged", color = Color.Gray)
    }
}
