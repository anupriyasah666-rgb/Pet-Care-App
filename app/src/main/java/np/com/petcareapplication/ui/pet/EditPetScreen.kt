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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import np.com.petcareapplication.model.Pet
import np.com.petcareapplication.ui.components.PetCareButton
import np.com.petcareapplication.ui.components.PetCareTextField
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.viewmodel.AuthViewModel
import np.com.petcareapplication.viewmodel.PetViewModel

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
    val errorMessage by petViewModel.errorMessage.collectAsState()

    LaunchedEffect(user) {
        user?.uid?.let { petViewModel.loadPets(it) }
    }

    if (pet == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    EditPetScreenContent(
        pet = pet,
        isUploading = isUploading,
        successMessage = successMessage,
        errorMessage = errorMessage,
        onBack = onBack,
        onUpdatePet = { updatedPet, uri ->
            petViewModel.updatePetWithImage(updatedPet, uri) { onPetUpdated() }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPetScreenContent(
    pet: Pet,
    isUploading: Boolean,
    successMessage: String?,
    errorMessage: String?,
    onBack: () -> Unit,
    onUpdatePet: (Pet, Uri?) -> Unit
) {
    var name by remember { mutableStateOf(pet.name) }
    var breed by remember { mutableStateOf(pet.breed) }
    var age by remember { mutableStateOf(pet.age.toString()) }
    var weight by remember { mutableStateOf(pet.weight.toString()) }
    var dietary by remember { mutableStateOf(pet.dietaryPreferences) }
    var vaccination by remember { mutableStateOf(pet.vaccinationHistory) }
    var allergies by remember { mutableStateOf(pet.allergies) }
    var toys by remember { mutableStateOf(pet.favoriteToys) }
    var notes by remember { mutableStateOf(pet.notes) }
    var imageUrl by remember { mutableStateOf(pet.imageUrl) } 
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var breedError by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> 
        if (uri != null) {
            selectedImageUri = uri
            imageUrl = "" 
        }
    }

    fun validate(): Boolean {
        var isValid = true
        if (name.isBlank()) { nameError = "Required"; isValid = false } else nameError = null
        if (breed.isBlank()) { breedError = "Required"; isValid = false } else breedError = null
        return isValid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit ${pet.name}", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(brush = Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))))) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val displayImage = selectedImageUri ?: imageUrl.ifEmpty { null }
                    if (displayImage != null) {
                        AsyncImage(model = displayImage, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Photo Link (Optional)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        PetCareTextField(
                            value = imageUrl, 
                            onValueChange = { 
                                imageUrl = it
                                if (it.isNotEmpty()) selectedImageUri = null 
                            }, 
                            label = "Image URL"
                        )
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("General Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        PetCareTextField(value = name, onValueChange = { name = it }, label = "Pet Name", error = nameError)
                        PetCareTextField(value = breed, onValueChange = { breed = it }, label = "Breed", error = breedError)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PetCareTextField(value = age, onValueChange = { age = it }, label = "Age", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            PetCareTextField(value = weight, onValueChange = { weight = it }, label = "Weight", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Health & Routine", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        PetCareTextField(value = dietary, onValueChange = { dietary = it }, label = "Dietary Needs")
                        PetCareTextField(value = vaccination, onValueChange = { vaccination = it }, label = "Vaccination History")
                        PetCareTextField(value = allergies, onValueChange = { allergies = it }, label = "Allergies")
                        PetCareTextField(value = toys, onValueChange = { toys = it }, label = "Favorite Toys")
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("General Notes") }, modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(16.dp))
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
                                allergies = allergies, favoriteToys = toys, notes = notes,
                                imageUrl = imageUrl 
                            )
                            val finalUri = selectedImageUri ?: if (imageUrl.isNotEmpty()) Uri.parse(imageUrl) else null
                            onUpdatePet(updatedPet, finalUri)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (successMessage != null) {
                Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp, start = 24.dp, end = 24.dp), shape = RoundedCornerShape(24.dp), color = Color(0xFF2E7D32), contentColor = Color.White, shadowElevation = 8.dp) {
                    Text(text = successMessage, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            if (errorMessage != null) {
                Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp, start = 24.dp, end = 24.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError, shadowElevation = 8.dp) {
                    Text(text = errorMessage, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditPetScreenPreview() {
    val samplePet = Pet(id = "1", name = "Luna", breed = "Golden Retriever", age = 3, weight = 25.0)
    PetCareApplicationTheme {
        EditPetScreenContent(pet = samplePet, isUploading = false, successMessage = null, errorMessage = null, onBack = {}, onUpdatePet = { _, _ -> })
    }
}
