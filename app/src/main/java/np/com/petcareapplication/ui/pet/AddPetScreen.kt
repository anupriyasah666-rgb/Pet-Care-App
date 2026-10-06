package np.com.petcareapplication.ui.pet

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
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
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.viewmodel.AuthViewModel
import np.com.petcareapplication.viewmodel.PetViewModel

@Composable
fun AddPetScreen(
    onBack: () -> Unit,
    onPetAdded: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    petViewModel: PetViewModel = viewModel()
) {
    val context = LocalContext.current
    val user by authViewModel.user.collectAsState()
    val isUploading by petViewModel.isImageUploading.collectAsState()
    val successMessage by petViewModel.successMessage.collectAsState()
    val errorMessage by petViewModel.errorMessage.collectAsState()

    AddPetScreenContent(
        isUploading = isUploading,
        successMessage = successMessage,
        errorMessage = errorMessage,
        onBack = onBack,
        onAddPet = { pet, uri ->
            val ownerId = user?.uid
            if (ownerId != null) {
                petViewModel.addPetWithImage(pet.copy(ownerId = ownerId), uri) { onPetAdded() }
            } else {
                Toast.makeText(context, "Please sign in to add a pet", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetScreenContent(
    isUploading: Boolean,
    successMessage: String?,
    errorMessage: String?,
    onBack: () -> Unit,
    onAddPet: (Pet, Uri?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var dietary by remember { mutableStateOf("") }
    var vaccination by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var toys by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var breedError by remember { mutableStateOf<String?>(null) }
    var ageError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }

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
        if (name.isBlank()) { nameError = "Pet name is required"; isValid = false } else nameError = null
        if (breed.isBlank()) { breedError = "Breed is required"; isValid = false } else breedError = null
        // Age and weight are optional, but if entered they must be realistic numbers
        val ageValue = age.trim().toIntOrNull()
        if (age.isNotBlank() && (ageValue == null || ageValue !in 0..40)) { ageError = "Enter 0-40"; isValid = false } else ageError = null
        val weightValue = weight.trim().toDoubleOrNull()
        if (weight.isNotBlank() && (weightValue == null || weightValue <= 0.0 || weightValue > 200.0)) { weightError = "Enter 0.1-200"; isValid = false } else weightError = null
        return isValid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Pet", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(brush = Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))))) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Photo Section
                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).clickable(onClickLabel = "Choose a photo") { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val displayImage = selectedImageUri ?: imageUrl.ifEmpty { null }
                    if (displayImage != null) {
                        AsyncImage(model = displayImage, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Add photo", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), shadowElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Photo Link (Optional)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        PetCareTextField(
                            value = imageUrl,
                            onValueChange = {
                                imageUrl = it
                                if (it.isNotEmpty()) selectedImageUri = null
                            },
                            label = "Image URL (e.g. Pinterest link)"
                        )
                        Text("Or tap the circle above to pick a file", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), shadowElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        PetCareTextField(value = name, onValueChange = { name = it; nameError = null }, label = "Pet Name", error = nameError)
                        PetCareTextField(value = breed, onValueChange = { breed = it; breedError = null }, label = "Breed", error = breedError)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PetCareTextField(value = age, onValueChange = { age = it; ageError = null }, label = "Age (years)", error = ageError, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            PetCareTextField(value = weight, onValueChange = { weight = it; weightError = null }, label = "Weight (kg)", error = weightError, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), shadowElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Health Info", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        PetCareTextField(value = dietary, onValueChange = { dietary = it }, label = "Dietary Preferences")
                        PetCareTextField(value = vaccination, onValueChange = { vaccination = it }, label = "Vaccination History")
                        PetCareTextField(value = allergies, onValueChange = { allergies = it }, label = "Allergies")
                        PetCareTextField(value = toys, onValueChange = { toys = it }, label = "Favorite Toys")

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("General Notes") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }

                PetCareButton(
                    text = "Save Pet Profile",
                    isLoading = isUploading,
                    onClick = {
                        if (validate()) {
                            val newPet = Pet(
                                name = name, breed = breed,
                                age = age.trim().toIntOrNull() ?: 0,
                                weight = weight.trim().toDoubleOrNull() ?: 0.0,
                                dietaryPreferences = dietary,
                                vaccinationHistory = vaccination,
                                allergies = allergies,
                                favoriteToys = toys,
                                notes = notes,
                                imageUrl = imageUrl
                            )
                            val finalUri = selectedImageUri ?: if (imageUrl.isNotEmpty()) Uri.parse(imageUrl) else null
                            onAddPet(newPet, finalUri)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // FEEDBACK OVERLAYS
            if (successMessage != null) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                    shape = RoundedCornerShape(24.dp), color = Color(0xFF2E7D32), contentColor = Color.White, shadowElevation = 8.dp
                ) {
                    Text(text = successMessage, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                    shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError, shadowElevation = 8.dp
                ) {
                    Text(text = errorMessage, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddPetScreenPreview() {
    PetCareApplicationTheme {
        AddPetScreenContent(
            isUploading = false,
            successMessage = null,
            errorMessage = null,
            onBack = {},
            onAddPet = { _, _ -> }
        )
    }
}