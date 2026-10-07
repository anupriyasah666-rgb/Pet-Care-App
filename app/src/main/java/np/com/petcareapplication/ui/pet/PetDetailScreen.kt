package np.com.petcareapplication.ui.pet

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import np.com.petcareapplication.model.CareTask
import np.com.petcareapplication.model.Pet
import np.com.petcareapplication.ui.components.ConfirmDeleteDialog
import np.com.petcareapplication.ui.components.PetCareCard
import np.com.petcareapplication.ui.components.TaskCheckCircle
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.util.DelegationMessages
import np.com.petcareapplication.ui.theme.PinkHighlight
import np.com.petcareapplication.viewmodel.AuthViewModel
import np.com.petcareapplication.viewmodel.ExpenseViewModel
import np.com.petcareapplication.viewmodel.PetViewModel
import java.util.Locale
import kotlin.math.sqrt

// Full profile for one pet, with its care routine, spending total and links to Bills and Health
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PetDetailScreen(
    petId: String,
    onBack: () -> Unit,
    onAddTaskClick: () -> Unit,
    onEditPetClick: (String) -> Unit,
    onEditTaskClick: (String) -> Unit,
    onViewExpensesClick: (String) -> Unit,
    onViewMedicalClick: (String) -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    petViewModel: PetViewModel = viewModel(),
    expenseViewModel: ExpenseViewModel = viewModel()
) {
    val context = LocalContext.current
    val user by authViewModel.user.collectAsState()
    val pets by petViewModel.pets.collectAsState()
    val tasks by petViewModel.tasks.collectAsState()
    val totalSpent by expenseViewModel.totalSpent.collectAsState()
    val successMessage by petViewModel.successMessage.collectAsState()

    // Find this pet in the list the ViewModel already has
    val pet = pets.find { it.id == petId }

    // Shake to reset: shaking the phone on this screen unticks all of this pet's tasks
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val shakeListener = object : SensorEventListener {
            private var lastAcceleration = SensorManager.GRAVITY_EARTH
            private var currentAcceleration = SensorManager.GRAVITY_EARTH
            private var lastShakeTime = 0L
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                    lastAcceleration = currentAcceleration
                    currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                    val now = System.currentTimeMillis()
                    // A sudden jump in movement counts as a shake. The 1.5 second wait means one shake only resets once
                    if (currentAcceleration - lastAcceleration > 12f && now - lastShakeTime > 1500) {
                        lastShakeTime = now
                        petViewModel.resetTasks(petId)
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        // Stop listening when the user leaves the screen
        onDispose { sensorManager.unregisterListener(shakeListener) }
    }

    // Make sure the pets, this pet's tasks and its expenses are loaded
    LaunchedEffect(user) { user?.uid?.let { petViewModel.loadPets(it) } }
    LaunchedEffect(petId) {
        petViewModel.loadTasks(petId)
        expenseViewModel.loadExpenses(petId)
    }

    // Show a loading spinner until the pet has been found
    if (pet == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        return
    }

    PetDetailScreenContent(
        pet = pet,
        tasks = tasks,
        totalSpent = totalSpent,
        successMessage = successMessage,
        onBack = onBack,
        onAddTaskClick = onAddTaskClick,
        onEditPetClick = onEditPetClick,
        onEditTaskClick = onEditTaskClick,
        onViewExpensesClick = onViewExpensesClick,
        onViewMedicalClick = onViewMedicalClick,
        onToggleTask = { petViewModel.toggleTaskCompletion(it) },
        onDeleteTask = { petViewModel.deleteTask(it) },
        onClearRoutine = { petViewModel.clearRoutine(petId) },
        onDeletePet = { petViewModel.deletePet(petId) { onBack() } }
    )
}

// The screen layout. It only takes plain data and callbacks, so the preview can use a sample pet
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PetDetailScreenContent(
    pet: Pet,
    tasks: List<CareTask>,
    totalSpent: Double,
    successMessage: String?,
    onBack: () -> Unit,
    onAddTaskClick: () -> Unit,
    onEditPetClick: (String) -> Unit,
    onEditTaskClick: (String) -> Unit,
    onViewExpensesClick: (String) -> Unit,
    onViewMedicalClick: (String) -> Unit,
    onToggleTask: (CareTask) -> Unit,
    onDeleteTask: (String) -> Unit,
    onClearRoutine: () -> Unit,
    onDeletePet: () -> Unit
) {
    val context = LocalContext.current
    // These control which menu or confirm dialog is open
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<CareTask?>(null) }
    // Group tasks by category (Feeding, Exercise...) so each group gets its own heading
    val groupedTasks = tasks.groupBy { it.category.ifBlank { "Other" } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pet.name, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    // Edit pet
                    IconButton(onClick = { onEditPetClick(pet.id) }) { Icon(Icons.Default.Edit, contentDescription = "Edit ${pet.name}", tint = MaterialTheme.colorScheme.primary) }

                    // Share the whole care routine by SMS: pet allergies and diet first, then every task
                    IconButton(onClick = {
                        val header = "Care Instructions for ${pet.name}\n" +
                                "Allergies: ${pet.allergies.ifEmpty { "None" }}\n" +
                                "Diet: ${pet.dietaryPreferences.ifEmpty { "Standard" }}\n\n"
                        val taskList = tasks.joinToString("\n\n") { task ->
                            var item = "* ${task.title} (${task.schedule})"
                            if (task.supplies.isNotEmpty()) item += "\n  Needs: ${task.supplies}"
                            if (task.notes.isNotEmpty()) item += "\n  Notes: ${task.notes}"
                            item
                        }
                        sendSms(context, header + taskList)
                    }) { Icon(Icons.Default.Share, contentDescription = "Share whole care routine by SMS", tint = MaterialTheme.colorScheme.primary) }

                    // Three-dot menu with Clear All Tasks and Delete Pet
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More options") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Clear All Tasks") },
                            onClick = { showClearDialog = true; showMenu = false },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Pet", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showDeleteDialog = true
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddTaskClick, containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary, shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) }, text = { Text("Add Task") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).background(brush = Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)))),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top card with photo, age, weight, total spent and the Bills and Health buttons
                item {
                    PetDetailedInfoCard(pet, totalSpent, onClickExpenses = { onViewExpensesClick(pet.id) }, onClickMedical = { onViewMedicalClick(pet.id) })
                }

                // Profile details. Empty fields show "Not specified"
                item {
                    PetCareCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Pet Profile Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)

                            PetDetailInfoItem(label = "Dietary Preferences", value = pet.dietaryPreferences, icon = Icons.Default.Restaurant)
                            PetDetailInfoItem(label = "Allergies", value = pet.allergies, icon = Icons.Default.Warning)
                            PetDetailInfoItem(label = "Favorite Toys", value = pet.favoriteToys, icon = Icons.Default.Toys)

                            if (pet.notes.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                Column {
                                    Text("General Notes", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = pet.notes, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("Routine Checklist", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        // Little hint so the user knows the gestures are there
                        Text("Swipe right to complete • Swipe left to delete • Send icon to delegate by SMS", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                groupedTasks.forEach { (category, categoryTasks) ->
                    // Category heading that sticks to the top while scrolling through that group
                    stickyHeader {
                        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)) {
                            Text(text = category, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    items(categoryTasks, key = { it.id }) { task ->
                        // Swipe gestures: right marks the task done or undone, left asks before deleting
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        onToggleTask(task)
                                        false
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        // Ask first; returning false snaps the card back into place
                                        taskToDelete = task
                                        false
                                    }
                                    else -> false
                                }
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                // Green with a tick when swiping right, red with a bin when swiping left
                                val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Color(0xFF4CAF50) else Color(0xFFE57373)
                                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(color).padding(horizontal = 24.dp),
                                    contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                ) { Icon(if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Icons.Default.CheckCircle else Icons.Default.Delete, contentDescription = null, tint = Color.White) }
                            },
                            content = {
                                EnhancedTaskItem(
                                    task = task,
                                    onToggle = { onToggleTask(task) },
                                    onEditClick = { onEditTaskClick(task.id) },
                                    // Delegate: text just this one task, plus the pet's allergies and diet, to a sitter
                                    onDelegateClick = {
                                        sendSms(
                                            context,
                                            DelegationMessages.forTask(
                                                petName = pet.name,
                                                petAllergies = pet.allergies,
                                                petDiet = pet.dietaryPreferences,
                                                taskTitle = task.title,
                                                taskSchedule = task.schedule,
                                                taskSupplies = task.supplies,
                                                taskNotes = task.notes
                                            )
                                        )
                                    }
                                )
                            }
                        )
                    }
                }

                if (tasks.isEmpty()) { item { EmptyRoutinePlaceholder() } }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            // Message at the bottom of the screen
            if (successMessage != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp, start = 24.dp, end = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    // Pink when a task is deleted, green for everything else. Fixed colours so white text is readable in dark mode
                    color = if (successMessage == "Task is deleted") Color(0xFFC2185B) else Color(0xFF2E7D32),
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

            // Confirm dialogs, so nothing gets deleted by mistake
            if (showDeleteDialog) {
                ConfirmDeleteDialog(
                    title = "Delete Pet Profile?",
                    message = "Are you sure you want to remove ${pet.name}? This will delete all tasks and records associated with them.",
                    onConfirm = { onDeletePet(); showDeleteDialog = false },
                    onDismiss = { showDeleteDialog = false }
                )
            }

            if (showClearDialog) {
                ConfirmDeleteDialog(
                    title = "Clear all tasks?",
                    message = "This removes every task in ${pet.name}'s care routine. This cannot be undone.",
                    confirmText = "Clear all",
                    onConfirm = { onClearRoutine(); showClearDialog = false },
                    onDismiss = { showClearDialog = false }
                )
            }

            taskToDelete?.let { task ->
                ConfirmDeleteDialog(
                    title = "Delete task?",
                    message = "\"${task.title}\" will be removed from ${pet.name}'s routine.",
                    onConfirm = { onDeleteTask(task.id); taskToDelete = null },
                    onDismiss = { taskToDelete = null }
                )
            }
        }
    }
}

// One row in the profile details: icon, label and value
@Composable
fun PetDetailInfoItem(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text(text = value.ifEmpty { "Not specified" }, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// Summary card at the top of the screen
@Composable
fun PetDetailedInfoCard(pet: Pet, totalSpent: Double, onClickExpenses: () -> Unit, onClickMedical: () -> Unit) {
    PetCareCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(80.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (pet.imageUrl.isNotEmpty()) {
                            AsyncImage(model = pet.imageUrl, contentDescription = "Photo of ${pet.name}", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
                        } else { Icon(Icons.Default.Pets, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary) }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = pet.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Text(text = pet.breed, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        // Small badges for age and weight
                        InfoBadge(text = "${pet.age} yrs", icon = Icons.Default.Cake)
                        InfoBadge(text = "${pet.weight} kg", icon = Icons.Default.Scale, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Spent", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(text = "$" + String.format(Locale.US, "%.2f", totalSpent), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                }

                Button(
                    onClick = onClickExpenses,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(48.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Bills", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onClickMedical,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(48.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.HealthAndSafety, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Health", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Small rounded label with an icon, used for age and weight
@Composable
fun InfoBadge(text: String, icon: ImageVector, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.1f)) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(11.dp), tint = color)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// One task card: tick circle, title, time, and buttons to delegate or edit
@Composable
fun EnhancedTaskItem(task: CareTask, onToggle: () -> Unit, onEditClick: () -> Unit, onDelegateClick: () -> Unit = {}) {
    PetCareCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TaskCheckCircle(checked = task.isCompleted, taskTitle = task.title, onToggle = onToggle)
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = task.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    Text(text = task.schedule, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Send button for delegating this task by SMS
                IconButton(onClick = onDelegateClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Delegate ${task.title} by SMS",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit ${task.title}",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            // Only show supplies and instructions if the task has them
            if (task.supplies.isNotEmpty() || task.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                if (task.supplies.isNotEmpty()) {
                    Text(text = "Supplies: ${task.supplies}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
                if (task.notes.isNotEmpty()) {
                    Text(text = "Instructions: ${task.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// What the user sees before any tasks are added
@Composable
fun EmptyRoutinePlaceholder() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.AutoMirrored.Filled.EventNote, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No tasks added for this pet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

// Preview with a made-up pet and three tasks
@Preview(showBackground = true)
@Composable
fun PetDetailScreenPreview() {
    val samplePet = Pet(
        id = "1",
        name = "Luna",
        breed = "Golden Retriever",
        age = 3,
        weight = 25.0,
        dietaryPreferences = "Grain-free kibble",
        allergies = "None",
        favoriteToys = "Tennis ball",
        notes = "Luna is a very active dog."
    )
    val sampleTasks = listOf(
        CareTask(id = "1", title = "Morning Walk", category = "Exercise", schedule = "07:00 AM", isCompleted = true),
        CareTask(id = "2", title = "Breakfast", category = "Feeding", schedule = "08:00 AM", isCompleted = false, supplies = "Kibble"),
        CareTask(id = "3", title = "Evening Walk", category = "Exercise", schedule = "06:00 PM", isCompleted = false)
    )
    PetCareApplicationTheme {
        PetDetailScreenContent(
            pet = samplePet,
            tasks = sampleTasks,
            totalSpent = 150.75,
            successMessage = null,
            onBack = {},
            onAddTaskClick = {},
            onEditPetClick = {},
            onEditTaskClick = {},
            onViewExpensesClick = {},
            onViewMedicalClick = {},
            onToggleTask = {},
            onDeleteTask = {},
            onClearRoutine = {},
            onDeletePet = {}
        )
    }
}

// Opens the phone's SMS app with the message already typed in, so the user just picks a contact and presses send.
// "smsto:" means only messaging apps open it, and the app doesn't need the SEND_SMS permission
// because the user sends the text themselves. If there's no SMS app (some tablets), a short message is shown instead of crashing
fun sendSms(context: Context, body: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:")
        putExtra("sms_body", body)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No SMS app found on this device", Toast.LENGTH_SHORT).show()
    }
}