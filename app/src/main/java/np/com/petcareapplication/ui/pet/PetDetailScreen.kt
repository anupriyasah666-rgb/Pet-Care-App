package np.com.petcareapplication.ui.pet

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import np.com.petcareapplication.model.CareTask
import np.com.petcareapplication.model.Pet
import np.com.petcareapplication.ui.components.PetCareCard
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PinkHighlight
import np.com.petcareapplication.viewmodel.AuthViewModel
import np.com.petcareapplication.viewmodel.ExpenseViewModel
import np.com.petcareapplication.viewmodel.PetViewModel
import java.util.Locale
import kotlin.math.sqrt

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
    
    val pet = pets.find { it.id == petId }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val groupedTasks = tasks.groupBy { it.category }

    // GESTURE: Shake to Reset Checklist
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val shakeListener = object : SensorEventListener {
            private var lastAcceleration = 0f
            private var currentAcceleration = 0f
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                    lastAcceleration = currentAcceleration
                    currentAcceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                    if (currentAcceleration - lastAcceleration > 12f) { petViewModel.resetTasks(petId) }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(shakeListener) }
    }

    LaunchedEffect(user) { user?.uid?.let { petViewModel.loadPets(it) } }
    LaunchedEffect(petId) {
        petViewModel.loadTasks(petId)
        expenseViewModel.loadExpenses(petId)
    }

    if (pet == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BluePrimary) }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pet.name, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { onEditPetClick(pet.id) }) { Icon(Icons.Default.Edit, null, tint = BluePrimary) }
                    
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
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:")
                            putExtra("sms_body", header + taskList)
                        }
                        context.startActivity(intent)
                    }) { Icon(Icons.Default.Share, null, tint = BluePrimary) }

                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Clear All Tasks", color = Color.Black) },
                            onClick = { petViewModel.clearRoutine(petId); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, null, tint = Color.Black) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Pet", color = Color.Black) },
                            onClick = { 
                                showDeleteDialog = true
                                showMenu = false 
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Black) }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddTaskClick, containerColor = PinkHighlight, contentColor = Color.White, shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add Task") }
            )
        }
    ) { padding ->
        val successMessage by petViewModel.successMessage.collectAsState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).background(brush = Brush.verticalGradient(colors = listOf(Color.White, BluePrimary.copy(alpha = 0.05f)))),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { 
                    PetDetailedInfoCard(pet, totalSpent, onClickExpenses = { onViewExpensesClick(pet.id) }, onClickMedical = { onViewMedicalClick(pet.id) }) 
                }

                item {
                    PetCareCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Pet Profile Details", fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 18.sp)
                            
                            PetDetailInfoItem(label = "Dietary Preferences", value = pet.dietaryPreferences, icon = Icons.Default.Restaurant)
                            PetDetailInfoItem(label = "Allergies", value = pet.allergies, icon = Icons.Default.Warning)
                            PetDetailInfoItem(label = "Favorite Toys", value = pet.favoriteToys, icon = Icons.Default.Toys)
                            
                            if (pet.notes.isNotEmpty()) {
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                Column {
                                    Text("General Notes", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                                    Text(text = pet.notes, fontSize = 14.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("Routine Checklist", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                        Text("Swipe tasks to manage • Shake to reset", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                groupedTasks.forEach { (category, categoryTasks) ->
                    if (category != "Feeding") {
                        stickyHeader {
                            Surface(modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.9f)) {
                                Text(text = category, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp), color = BluePrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    items(categoryTasks, key = { it.id }) { task ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.StartToEnd -> { 
                                        petViewModel.toggleTaskCompletion(task)
                                        false 
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> { 
                                        petViewModel.deleteTask(task.id)
                                        true 
                                    }
                                    else -> false
                                }
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Color(0xFF4CAF50) else Color(0xFFE57373)
                                Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(color).padding(horizontal = 24.dp),
                                    contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                ) { Icon(if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Icons.Default.CheckCircle else Icons.Default.Delete, null) }
                            },
                            content = { 
                                EnhancedTaskItem(
                                    task = task, 
                                    onToggle = { petViewModel.toggleTaskCompletion(task) }, 
                                    onLongClick = { onEditTaskClick(task.id) }
                                ) 
                            }
                        )
                    }
                }
                
                if (tasks.isEmpty()) { item { EmptyRoutinePlaceholder() } }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            // UI MESSAGE OVERLAY
            if (successMessage != null) {
                // Color logic: Green for positive actions, else standard black
                val bgColor = when (successMessage) {
                    "Task added", "Profile updated successfully", "Task updated successfully", "Today's checklist reset!", "Care routine cleared successfully", "Pet added", "Health record saved" -> Color(0xFF2E7D32)
                    else -> Color.Black.copy(alpha = 0.85f)
                }
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

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Pet Profile?", fontWeight = FontWeight.Bold) },
                    text = { Text("Are you sure you want to remove ${pet.name}? This will delete all tasks and records associated with them.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                petViewModel.deletePet(petId) { onBack() }
                                showDeleteDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
fun PetDetailInfoItem(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = BluePrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(text = value.ifEmpty { "Not specified" }, fontSize = 14.sp, color = Color.Black)
        }
    }
}

@Composable
fun PetDetailedInfoCard(pet: Pet, totalSpent: Double, onClickExpenses: () -> Unit, onClickMedical: () -> Unit) {
    PetCareCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(80.dp), shape = RoundedCornerShape(20.dp), color = BluePrimary.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (pet.imageUrl.isNotEmpty()) {
                            AsyncImage(model = pet.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
                        } else { Icon(Icons.Default.Pets, null, modifier = Modifier.size(40.dp), tint = BluePrimary) }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = pet.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = BluePrimary)
                    Text(text = pet.breed, color = Color.Gray, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        InfoBadge(text = "${pet.age} yrs", icon = Icons.Default.Cake)
                        InfoBadge(text = "${pet.weight} kg", icon = Icons.Default.Scale, color = PinkHighlight)
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Spent", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(text = "$" + String.format(Locale.US, "%.2f", totalSpent), fontWeight = FontWeight.Black, color = BluePrimary, fontSize = 18.sp)
                }
                
                Button(
                    onClick = onClickExpenses,
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary, contentColor = Color.White),
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
                    colors = ButtonDefaults.buttonColors(containerColor = PinkHighlight, contentColor = Color.White),
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

@Composable
fun InfoBadge(text: String, icon: ImageVector, color: Color = BluePrimary) {
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.1f)) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(11.dp), tint = color)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedTaskItem(task: CareTask, onToggle: () -> Unit, onLongClick: () -> Unit) {
    PetCareCard(
        modifier = Modifier.combinedClickable(
            onClick = {}, // Disabled card-level click for completion
            onLongClick = onLongClick
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Task Circle on top left
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (task.isCompleted) PinkHighlight else Color.Transparent)
                        .border(
                            width = 2.dp,
                            color = if (task.isCompleted) PinkHighlight else Color.LightGray.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = task.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (task.isCompleted) Color.Gray else Color.Black)
                    Text(text = task.schedule, fontSize = 12.sp, color = Color.Gray)
                }
            }
            if (task.imageUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(model = task.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            }
            if (task.supplies.isNotEmpty() || task.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                if (task.supplies.isNotEmpty()) {
                    Text(text = "Supplies: ${task.supplies}", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
                if (task.notes.isNotEmpty()) {
                    Text(text = "Instructions: ${task.notes}", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun EmptyRoutinePlaceholder() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.AutoMirrored.Filled.EventNote, null, modifier = Modifier.size(40.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No tasks added for this pet", color = Color.Gray, fontWeight = FontWeight.Medium)
    }
}
