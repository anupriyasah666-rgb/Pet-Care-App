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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import np.com.petcareapplication.model.Pet
import np.com.petcareapplication.ui.components.PetCareButton
import np.com.petcareapplication.ui.components.PetCareTextField
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PinkHighlight
import np.com.petcareapplication.viewmodel.AuthViewModel
import np.com.petcareapplication.viewmodel.PetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPetScreen(
    onBack: () -> Unit,
    onPetAdded: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    petViewModel: PetViewModel = viewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var dietary by remember { mutableStateOf("") }
    var vaccination by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var toys by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") } // Input for web link
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val user by authViewModel.user.collectAsState()
    val isUploading by petViewModel.isImageUploading.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> 
        if (uri != null) {
            selectedImageUri = uri
            imageUrl = "" // Clear text input if file picked
        }
    }

    fun validate(): Boolean {
        return name.isNotBlank() && breed.isNotBlank()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Pet", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).background(brush = Brush.verticalGradient(colors = listOf(Color.White, BluePrimary.copy(alpha = 0.05f))))
                    .padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Photo Section
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

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), shadowElevation = 4.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Photo Link (Optional)", fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 14.sp)
                        PetCareTextField(
                            value = imageUrl, 
                            onValueChange = { 
                                imageUrl = it
                                if (it.isNotEmpty()) selectedImageUri = null // Clear file if link entered
                            }, 
                            label = "Image URL (e.g. Pinterest link)"
                        )
                        Text("Or tap the circle above to pick a file", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), shadowElevation = 4.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Details", fontWeight = FontWeight.Bold, color = BluePrimary)
                        PetCareTextField(value = name, onValueChange = { name = it }, label = "Pet Name")
                        PetCareTextField(value = breed, onValueChange = { breed = it }, label = "Breed")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PetCareTextField(value = age, onValueChange = { age = it }, label = "Age (years)", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            PetCareTextField(value = weight, onValueChange = { weight = it }, label = "Weight (kg)", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), shadowElevation = 4.dp) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Health Info", fontWeight = FontWeight.Bold, color = BluePrimary)
                        PetCareTextField(value = dietary, onValueChange = { dietary = it }, label = "Dietary Preferences")
                        PetCareTextField(value = vaccination, onValueChange = { vaccination = it }, label = "Vaccination History")
                        PetCareTextField(value = allergies, onValueChange = { allergies = it }, label = "Allergies")
                        PetCareTextField(value = toys, onValueChange = { toys = it }, label = "Favorite Toys")
                        
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("General Notes") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                PetCareButton(
                    text = "Save Pet Profile",
                    isLoading = isUploading,
                    onClick = {
                        if (validate()) {
                            val newPet = Pet(
                                ownerId = user?.uid ?: "",
                                name = name, breed = breed,
                                age = age.toIntOrNull() ?: 0,
                                weight = weight.toDoubleOrNull() ?: 0.0,
                                dietaryPreferences = dietary,
                                vaccinationHistory = vaccination,
                                allergies = allergies,
                                favoriteToys = toys,
                                notes = notes,
                                imageUrl = imageUrl // Uses the URL if no file is uploaded
                            )
                            // If user provided a link and no file, pass the link as Uri. Or handle in ViewModel.
                            val finalUri = if (selectedImageUri != null) selectedImageUri else if (imageUrl.isNotEmpty()) Uri.parse(imageUrl) else null
                            petViewModel.addPetWithImage(newPet, finalUri) { onPetAdded() }
                        } else {
                            Toast.makeText(context, "Name and Breed are required", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}
