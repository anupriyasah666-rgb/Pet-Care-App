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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import np.com.petcareapplication.model.Pet
import np.com.petcareapplication.model.CareTask
import np.com.petcareapplication.ui.components.PetCareCard
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PinkHighlight
import np.com.petcareapplication.viewmodel.AuthViewModel
import np.com.petcareapplication.viewmodel.PetViewModel
import kotlin.math.sqrt

/**
 * HomeScreen: The central entry point.
 * Fulfills CORE REQUIREMENT: Consolidates all tasks for all pets.
 * Fulfills DESIRABLE FEATURE: Shake to reset today's consolidated checklist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel = viewModel(),
    petViewModel: PetViewModel = viewModel(),
    onPetClick: (String) -> Unit,
    onAddPetClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val user by authViewModel.user.collectAsState()
    val pets by petViewModel.pets.collectAsState()
    val allTasks by petViewModel.allTasks.collectAsState()

    // --- GESTURE CONTROL: Shake to Reset ALL tasks across all pets ---
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
                    if (currentAcceleration - lastAcceleration > 13f) {
                        // Reset every pet's checklist at once from the Home dashboard
                        pets.forEach { pet -> petViewModel.resetTasks(pet.id) }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(shakeListener) }
    }

    LaunchedEffect(user) {
        user?.uid?.let { uid ->
            petViewModel.loadPets(uid)
            petViewModel.loadAllTasks(uid)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Happy pets", fontWeight = FontWeight.Black, fontSize = 28.sp, fontFamily = FontFamily.Cursive, color = BluePrimary)
                        Text(text = "Dashboard", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                },
                actions = {
                    // Profile - Optimized layout to avoid cropping
                    Column(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onProfileClick() }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AccountCircle, null, tint = BluePrimary, modifier = Modifier.size(24.dp))
                        Text("Profile", color = BluePrimary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    
                    // Logout - Optimized layout to avoid cropping
                    Column(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onLogout() }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = PinkHighlight, modifier = Modifier.size(24.dp))
                        Text("Logout", color = PinkHighlight, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPetClick, containerColor = BluePrimary, contentColor = Color.White, shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add Pet") }
            )
        }
    ) { padding ->
        val successMessage by petViewModel.successMessage.collectAsState()
        
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                
                // Dynamic Banner showing tasks left
                item { 
                    ConsolidatedStatusSection(
                        pendingTasks = allTasks.count { !it.isCompleted }, 
                        totalTasks = allTasks.size 
                    ) 
                }

                item { Text(text = "My Pets", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BluePrimary) }
                if (pets.isEmpty()) { item { EmptyHomeState() } }
                else {
                    items(pets, key = { it.id }) { pet -> PetSummaryItem(pet = pet, onClick = { onPetClick(pet.id) }) }
                }

                // Show both pending and completed routine tasks as per request to see completion state on Home
                if (allTasks.isNotEmpty()) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    item { Text(text = "Today's Routine", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BluePrimary) }
                    
                    // Sorting to keep pending tasks at top
                    val sortedTasks = allTasks.sortedBy { it.isCompleted }
                    
                    items(sortedTasks, key = { it.id }) { task ->
                        val petName = pets.find { it.id == task.petId }?.name ?: "Pet"
                        HomeTaskItem(
                            task = task, 
                            petName = petName, 
                            onComplete = { petViewModel.toggleTaskCompletion(task) }
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }

            if (successMessage != null) {
                // Color logic: Green for positive actions, Pink for "Pet deleted", else black
                val bgColor = when (successMessage) {
                    "Pet added", "Marked as completed", "Marked as undone", "Today's checklist reset!", "Task added", "Profile updated successfully", "Task updated successfully", "Care routine cleared successfully", "Health record saved" -> Color(0xFF2E7D32) // Green
                    "Pet deleted" -> PinkHighlight
                    else -> Color.Black.copy(alpha = 0.8f)
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp, start = 24.dp, end = 24.dp), // Lifted higher to avoid FAB overlap
                    shape = RoundedCornerShape(24.dp), 
                    color = bgColor, 
                    contentColor = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = successMessage!!, 
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), 
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ConsolidatedStatusSection(pendingTasks: Int, totalTasks: Int) {
    PetCareCard(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().background(brush = Brush.horizontalGradient(colors = listOf(BluePrimary, BluePrimary.copy(alpha = 0.8f)))).padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Today's Checklist", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = when {
                            totalTasks == 0 -> "No tasks scheduled."
                            pendingTasks == 0 -> "All tasks completed! Good job."
                            else -> "$pendingTasks items left to complete."
                        },
                        color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp
                    )
                }
                Icon(Icons.Default.TaskAlt, null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
        }
    }
}

@Composable
fun HomeTaskItem(task: CareTask, petName: String, onComplete: () -> Unit) {
    PetCareCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Task Circle on top left - FILL WITH PINK WHEN COMPLETED
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (task.isCompleted) PinkHighlight else Color.White) // Changed Transparent to White to ensure it's "filled"
                    .border(
                        width = 2.dp,
                        color = if (task.isCompleted) PinkHighlight else Color.LightGray.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                    .clickable { onComplete() },
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
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = petName, color = PinkHighlight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(text = " • ", color = Color.Gray)
                    Text(
                        text = task.title, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 15.sp,
                        color = if (task.isCompleted) Color.Gray else Color.Black
                    )
                }
                Text(text = task.schedule, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun PetSummaryItem(pet: Pet, onClick: () -> Unit) {
    PetCareCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(60.dp), shape = CircleShape, color = BluePrimary.copy(alpha = 0.1f)) {
                if (pet.imageUrl.isNotEmpty()) {
                    AsyncImage(model = pet.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Pets, null, tint = BluePrimary.copy(alpha = 0.5f)) }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = pet.name, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BluePrimary)
                Text(text = pet.breed, color = Color.Gray, fontSize = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

@Composable
fun EmptyHomeState() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Pets, null, modifier = Modifier.size(80.dp), tint = Color.LightGray.copy(alpha = 0.3f))
        Text(text = "Your pet list is empty", color = Color.Gray)
    }
}
