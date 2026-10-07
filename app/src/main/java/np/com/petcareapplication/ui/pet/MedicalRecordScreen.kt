package np.com.petcareapplication.ui.pet

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import np.com.petcareapplication.model.CareTask
import np.com.petcareapplication.model.MedicalRecord
import np.com.petcareapplication.ui.components.ConfirmDeleteDialog
import np.com.petcareapplication.ui.components.PetCareCard
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.ui.theme.PinkHighlight
import np.com.petcareapplication.viewmodel.AuthViewModel
import np.com.petcareapplication.viewmodel.MedicalViewModel
import np.com.petcareapplication.viewmodel.PetViewModel
import java.text.SimpleDateFormat
import java.util.*

// Health records for one pet: vaccinations, checkups and medication.
// Each one can also be added to the care checklist as a one-time task
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

    // Load this pet's records when the screen opens
    LaunchedEffect(petId) { medicalViewModel.loadRecords(petId) }

    MedicalRecordScreenContent(
        records = records,
        successMessage = successMessage,
        onBack = onBack,
        onDeleteRecord = { medicalViewModel.deleteRecord(it) },
        onAddRecord = { type, notes, timestamp, addToChecklist ->
            medicalViewModel.addRecord(MedicalRecord(petId = petId, type = type, notes = notes, date = timestamp))
            // If the box was ticked, also add a one-time task so the appointment shows on the checklist
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
                    onComplete = { /* nothing else needed here */ }
                )
            }
        }
    )
}

// The screen layout, kept separate from the ViewModels so the preview can use sample records
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordScreenContent(
    records: List<MedicalRecord>,
    successMessage: String?,
    onBack: () -> Unit,
    onDeleteRecord: (String) -> Unit,
    onAddRecord: (String, String, Long, Boolean) -> Unit
) {
    // showAddDialog opens the "Log Health Event" pop-up. recordToDelete opens the confirm dialog
    var showAddDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<MedicalRecord?>(null) }

    recordToDelete?.let { record ->
        ConfirmDeleteDialog(
            title = "Delete health record?",
            message = "The ${record.type} record will be permanently removed from this pet's history.",
            onConfirm = { onDeleteRecord(record.id); recordToDelete = null },
            onDismiss = { recordToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Records", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Log Health Event") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(brush = Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))))) {
            if (records.isEmpty()) { EmptyMedicalPlaceholder() }
            else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(records, key = { it.id }) { record ->
                        MedicalRecordItem(record = record, onDelete = { recordToDelete = record })
                    }
                }
            }

            // Message at the bottom: pink when a record is deleted, green otherwise
            if (successMessage != null) {
                // Fixed colours, so the white text is easy to read in light and dark mode
                val bgColor = if (successMessage == "Health record deleted") Color(0xFFC2185B) else Color(0xFF2E7D32)
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
                        text = successMessage,
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
                        onAddRecord(type, notes, timestamp, addToChecklist)
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

// One health record in the list: type, notes and date, with a bin button
@Composable
fun MedicalRecordItem(record: MedicalRecord, onDelete: () -> Unit) {
    val df = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    PetCareCard {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MedicalServices, null, tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = record.type, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = record.notes, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = df.format(Date(record.date)), fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete ${record.type} record", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
        }
    }
}

// Pop-up for adding a new health record
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicalRecordDialog(onDismiss: () -> Unit, onConfirm: (String, String, Long, Boolean) -> Unit) {
    val context = LocalContext.current
    var type by remember { mutableStateOf("Vaccination") }
    var notes by remember { mutableStateOf("") }
    // Ticked by default, so appointments end up on the checklist unless the user unticks it
    var addToChecklist by remember { mutableStateOf(true) }

    // The date starts as today, and the calendar lets the user pick another day
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
                // Chips split over two rows so the text stays readable inside the dialog
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "Vaccination", onClick = { type = "Vaccination" }, label = { Text("Vaccination") })
                    FilterChip(selected = type == "Checkup", onClick = { type = "Checkup" }, label = { Text("Checkup") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "Medication", onClick = { type = "Medication" }, label = { Text("Medication") })
                }

                // Date box can't be typed in. The calendar opens from the icon
                OutlinedTextField(
                    value = df.format(selectedDate.time), onValueChange = {}, label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth(), readOnly = true,
                    trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.Event, contentDescription = "Choose date") } }
                )

                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth().height(80.dp), shape = RoundedCornerShape(12.dp))

                // The whole row is the touch target, so tapping the label also toggles the box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = addToChecklist, role = Role.Checkbox, onValueChange = { addToChecklist = it }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = addToChecklist, onCheckedChange = null)
                    Text("Add to care checklist", fontSize = 14.sp)
                }
            }
        },
        // Save sends everything back to the screen, which saves it through the ViewModel
        confirmButton = { Button(onClick = { onConfirm(type, notes, selectedDate.timeInMillis, addToChecklist) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}


// What the user sees before adding any health records
@Composable
fun EmptyMedicalPlaceholder() {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.HealthAndSafety, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Text("No health records logged", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Tap \"Log Health Event\" to add a vaccination or checkup", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}

// Preview with three made-up records
@Preview(showBackground = true)
@Composable
fun MedicalRecordScreenPreview() {
    val sampleRecords = listOf(
        MedicalRecord(id = "1", type = "Vaccination", notes = "Rabies vaccine booster", date = System.currentTimeMillis()),
        MedicalRecord(id = "2", type = "Checkup", notes = "Annual physical exam", date = System.currentTimeMillis() - 86400000 * 30),
        MedicalRecord(id = "3", type = "Medication", notes = "Heartworm prevention", date = System.currentTimeMillis() - 86400000 * 5)
    )
    PetCareApplicationTheme {
        MedicalRecordScreenContent(
            records = sampleRecords,
            successMessage = null,
            onBack = {},
            onDeleteRecord = {},
            onAddRecord = { _, _, _, _ -> }
        )
    }
}