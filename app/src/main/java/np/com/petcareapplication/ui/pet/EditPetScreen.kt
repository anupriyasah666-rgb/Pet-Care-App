package np.com.petcareapplication.ui.pet

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import np.com.petcareapplication.ui.components.PetCareButton
import np.com.petcareapplication.ui.components.PetCareTextField
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PinkHighlight
import np.com.petcareapplication.viewmodel.AuthViewModel
import np.com.petcareapplication.viewmodel.PetViewModel

/**
 * EditPetScreen allows Emily to update pet information as per core requirements.
 * Fulfills: Edit items - remove unwanted pets, update pet information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPetScreen(
    petId: String,
    onBack: () -> Unit,
    onPetUpdated: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    petViewModel: PetViewModel = viewModel()
) {
    val user by authViewModel.user.collectAsState()
    val pets by petViewModel.pets.collectAsState()
    val pet = pets.find { it.id == petId }
    val isUploading by petViewModel.isImageUploading.collectAsState()
    val successMessage by petViewModel.successMessage.collectAsState()

    LaunchedEffect(user) {
        user?.uid?.let { petViewModel.loadPets(it) }
    }

    if (pet == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BluePrimary)
        }
        return
    }

    // State initialized with existing pet data
    var name by remember { mutableStateOf(pet.name) }
    var breed by remember { mutableStateOf(pet.breed) }
    var age by remember { mutableStateOf(pet.age.toString()) }
    var weight by remember { mutableStateOf(pet.weight.toString()) }
    var dietary by remember { mutableStateOf(pet.dietaryPreferences) }
    var vaccination by remember { mutableStateOf(pet.vaccinationHistory) }
    var allergies by remember { mutableStateOf(pet.allergies) }
    var toys by remember { mutableStateOf(pet.favoriteToys) }
    var notes by remember { mutableStateOf(pet.notes) }
    var imageUrl by remember { mutableStateOf(pet.imageUrl) } // Input for web link
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var breedError by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> 
        if (uri != null) {
            selectedImageUri = uri
            imageUrl = "" // Clear text input if file picked
        }
    }

    fun validate(): Boolean {
        var isValid = true
        if (name.isBlank()) { nameError = "Required"; isValid = false }
        if (breed.isBlank()) { breedError = "Required"; isValid = false }
        return isValid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit ${pet.name}", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
                .background(brush = Brush.verticalGradient(colors = listOf(Color.White, BluePrimary.copy(alpha = 0.05f))))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Photo selection for personalization
                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).background(BluePrimary.copy(alpha = 0.1f)).clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val displayImage = if (selectedImageUri != null) selectedImageUri else if (imageUrl.isNotEmpty()) imageUrl else null
                    if (displayImage != null) {
                        AsyncImage(model = displayImage, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.AddAPhoto, null, tint = BluePrimary)
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Photo Link (Optional)", fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 14.sp)
                        PetCareTextField(
                            value = imageUrl, 
                            onValueChange = { 
                                imageUrl = it
                                if (it.isNotEmpty()) selectedImageUri = null // Clear file if link entered
                            }, 
                            label = "Image URL"
                        )
                        Text("Or tap the circle above to pick a file", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("General Details", fontWeight = FontWeight.Bold, color = BluePrimary)
                        PetCareTextField(value = name, onValueChange = { name = it }, label = "Pet Name", error = nameError)
                        PetCareTextField(value = breed, onValueChange = { breed = it }, label = "Breed", error = breedError)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PetCareTextField(value = age, onValueChange = { age = it }, label = "Age (years)", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            PetCareTextField(value = weight, onValueChange = { weight = it }, label = "Weight (kg)", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Health & Routine", fontWeight = FontWeight.Bold, color = BluePrimary)
                        PetCareTextField(value = dietary, onValueChange = { dietary = it }, label = "Dietary Needs")
                        PetCareTextField(value = vaccination, onValueChange = { vaccination = it }, label = "Vaccination History")
                        PetCareTextField(value = allergies, onValueChange = { allergies = it }, label = "Allergies")
                        PetCareTextField(value = toys, onValueChange = { toys = it }, label = "Favorite Toys")
                        
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("General Notes") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BluePrimary)
                        )
                    }
                }

                PetCareButton(
                    text = "Update Profile",
                    isLoading = isUploading,
                    onClick = {
                        if (validate()) {
                            val updatedPet = pet.copy(
                                name = name, breed = breed, age = age.toIntOrNull() ?: pet.age,
                                weight = weight.toDoubleOrNull() ?: pet.weight,
                                dietaryPreferences = dietary, vaccinationHistory = vaccination,
                                allergies = allergies, favoriteToys = toys,
                                notes = notes,
                                imageUrl = imageUrl // Use current URL string if no new file selected
                            )
                            val finalUri = if (selectedImageUri != null) selectedImageUri else if (imageUrl.isNotEmpty()) Uri.parse(imageUrl) else null
                            petViewModel.updatePetWithImage(updatedPet, finalUri) { onPetUpdated() }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
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
