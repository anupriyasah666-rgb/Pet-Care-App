package np.com.petcareapplication.ui.home

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import np.com.petcareapplication.model.Pet
import np.com.petcareapplication.model.CareTask
import np.com.petcareapplication.ui.components.PetCareCard
import np.com.petcareapplication.ui.components.TaskCheckCircle
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.ui.theme.PinkHighlight
import np.com.petcareapplication.viewmodel.AuthViewModel
import np.com.petcareapplication.viewmodel.PetViewModel
import kotlin.math.sqrt

// The first screen after login. It lists all the user's pets and puts every pet's tasks
// together in one "Today's Routine" list. Shaking the phone here resets today's checklist
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel = viewModel(),
    petViewModel: PetViewModel = viewModel(),
    onPetClick: (String) -> Unit,
    onAddPetClick: () -> Unit,
    onEditTaskClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val user by authViewModel.user.collectAsState()
    val pets by petViewModel.pets.collectAsState()
    val allTasks by petViewModel.allTasks.collectAsState()
    val successMessage by petViewModel.successMessage.collectAsState()

    // Shake to reset: listen to the accelerometer while the Home screen is open
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
                    // A big sudden jump in movement counts as a shake. One shake gives lots of readings,
                    // so the 1.5 second wait stops it resetting the list several times in a row
                    if (currentAcceleration - lastAcceleration > 13f && now - lastShakeTime > 1500) {
                        lastShakeTime = now
                        // Ticks are cleared through the ViewModel, which always has the up-to-date task list
                        petViewModel.resetAllTasks()
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        // Stop listening when the user leaves the screen, otherwise it keeps draining the battery
        onDispose { sensorManager.unregisterListener(shakeListener) }
    }

    // Start listening to this user's pets and tasks once we know who is logged in
    LaunchedEffect(user) {
        user?.uid?.let { uid ->
            petViewModel.loadPets(uid)
            petViewModel.loadAllTasks(uid)
        }
    }

    HomeScreenContent(
        pets = pets,
        allTasks = allTasks,
        successMessage = successMessage,
        onPetClick = onPetClick,
        onAddPetClick = onAddPetClick,
        onEditTaskClick = onEditTaskClick,
        onProfileClick = onProfileClick,
        onLogout = onLogout,
        onToggleTask = { petViewModel.toggleTaskCompletion(it) }
    )
}

// The layout of the Home screen. It only takes plain data, so the preview can use sample pets
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    pets: List<Pet>,
    allTasks: List<CareTask>,
    successMessage: String?,
    onPetClick: (String) -> Unit,
    onAddPetClick: () -> Unit,
    onEditTaskClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    onToggleTask: (CareTask) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Happy pets", fontWeight = FontWeight.Black, fontSize = 28.sp, fontFamily = FontFamily.Cursive, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Dashboard", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    // Profile button. Icon with a small label underneath, so the text doesn't get cut off
                    Column(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClickLabel = "Open profile", role = Role.Button) { onProfileClick() }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Text("Profile", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    // Logout button, laid out the same way
                    Column(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClickLabel = "Log out", role = Role.Button) { onLogout() }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                        Text("Logout", color = MaterialTheme.colorScheme.secondary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPetClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Pet") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Blue card at the top showing how many tasks are left
                item {
                    ConsolidatedStatusSection(
                        pendingTasks = allTasks.count { !it.isCompleted },
                        totalTasks = allTasks.size
                    )
                }

                item { Text(text = "My Pets", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                if (pets.isEmpty()) { item { EmptyHomeState() } }
                else {
                    // Pets and tasks are in the same list, so the keys get a prefix to stop two items sharing the same id
                    items(pets, key = { "pet_${it.id}" }) { pet -> PetSummaryItem(pet = pet, onClick = { onPetClick(pet.id) }) }
                }

                // Ticked tasks stay on the list too, so the user can see what's already been done today
                if (allTasks.isNotEmpty()) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    item { Text(text = "Today's Routine", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }

                    // Unfinished tasks go first, finished ones drop to the bottom
                    val sortedTasks = allTasks.sortedBy { it.isCompleted }

                    // Same idea as the pet keys above
                    items(sortedTasks, key = { "task_${it.id}" }) { task ->
                        // Each task shows which pet it belongs to
                        val petName = pets.find { it.id == task.petId }?.name ?: "Pet"
                        HomeTaskItem(
                            task = task,
                            petName = petName,
                            onComplete = { onToggleTask(task) },
                            onEdit = { onEditTaskClick(task.id) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            if (successMessage != null) {
                // Message pop-up near the bottom. The colours are dark on purpose so white text
                // is easy to read in light and dark mode: green for good news, pink for deletes, grey for anything else
                val bgColor = when (successMessage) {
                    "Pet added", "Marked as completed", "Marked as undone", "Today's checklist reset!", "Task added", "Profile updated successfully", "Task updated successfully", "Care routine cleared successfully", "Health record saved" -> Color(0xFF2E7D32)
                    "Pet deleted", "Task is deleted" -> Color(0xFFC2185B)
                    else -> Color(0xFF323232)
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp, start = 24.dp, end = 24.dp), // Sits above the Add Pet button so it doesn't cover it
                    shape = RoundedCornerShape(24.dp),
                    color = bgColor,
                    contentColor = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = successMessage,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Banner card that says how many tasks are left for today
@Composable
fun ConsolidatedStatusSection(pendingTasks: Int, totalTasks: Int) {
    PetCareCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Today's Checklist", color = MaterialTheme.colorScheme.onPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = when {
                            totalTasks == 0 -> "No tasks scheduled."
                            pendingTasks == 0 -> "All tasks completed! Good job."
                            else -> "$pendingTasks items left to complete."
                        },
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f), fontSize = 14.sp
                    )
                }
                Icon(Icons.Default.TaskAlt, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(44.dp))
            }
        }
    }
}

// One row in Today's Routine. Tap the circle to tick it off, or tap the row to edit it
@Composable
fun HomeTaskItem(task: CareTask, petName: String, onComplete: () -> Unit, onEdit: () -> Unit) {
    PetCareCard(modifier = Modifier.fillMaxWidth().clickable { onEdit() }) {
        Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TaskCheckCircle(checked = task.isCompleted, taskTitle = task.title, onToggle = onComplete)
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = petName, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(text = " • ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(text = task.schedule, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit ${task.title}",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// One pet card in the My Pets list. Shows the photo from the link if there is one, otherwise a paw icon
@Composable
fun PetSummaryItem(pet: Pet, onClick: () -> Unit) {
    PetCareCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(60.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                if (pet.imageUrl.isNotEmpty()) {
                    AsyncImage(model = pet.imageUrl, contentDescription = "Photo of ${pet.name}", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Pets, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = pet.name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Text(text = pet.breed, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

// What the user sees before adding any pets
@Composable
fun EmptyHomeState() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Pets, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
        Text(text = "Your pet list is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = "Tap \"Add Pet\" below to get started", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}

// Preview with two made-up pets and tasks
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val samplePets = listOf(
        Pet(id = "1", name = "Luna", breed = "Golden Retriever"),
        Pet(id = "2", name = "Milo", breed = "Calico Cat")
    )
    val sampleTasks = listOf(
        CareTask(id = "1", petId = "1", title = "Walk Luna", schedule = "07:00 AM", isCompleted = true),
        CareTask(id = "2", petId = "2", title = "Feed Milo", schedule = "08:00 AM", isCompleted = false)
    )
    PetCareApplicationTheme {
        HomeScreenContent(
            pets = samplePets,
            allTasks = sampleTasks,
            successMessage = null,
            onPetClick = {},
            onAddPetClick = {},
            onEditTaskClick = {},
            onProfileClick = {},
            onLogout = {},
            onToggleTask = {}
        )
    }
}