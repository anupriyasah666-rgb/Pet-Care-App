package np.com.petcareapplication.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import np.com.petcareapplication.model.User
import np.com.petcareapplication.util.Validators
import np.com.petcareapplication.ui.components.PetCareButton
import np.com.petcareapplication.ui.components.PetCareTextField
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.viewmodel.AuthViewModel

// Profile screen where the user can change their name and phone number
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val userData by viewModel.userData.collectAsState()
    val isProfileLoading by viewModel.isProfileLoading.collectAsState()
    val profileError by viewModel.profileError.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    // Get the latest details from Firestore every time the screen opens
    LaunchedEffect(Unit) {
        viewModel.loadUserData()
    }

    ProfileScreenContent(
        userData = userData,
        isProfileLoading = isProfileLoading,
        profileError = profileError,
        successMessage = successMessage,
        onBack = onBack,
        // The photo is passed as null because profile pictures aren't used without Firebase Storage
        onUpdateProfile = { name, phone -> viewModel.updateProfile(name, phone, null) },
        onClearErrors = { viewModel.clearErrors() }
    )
}

// The form itself, kept separate from the ViewModel so it can be previewed
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    userData: User?,
    isProfileLoading: Boolean,
    profileError: String?,
    successMessage: String?,
    onBack: () -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onClearErrors: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    // Fill the boxes with the saved details once they have loaded
    LaunchedEffect(userData) {
        userData?.let {
            name = it.name
            phone = it.phoneNumber
        }
    }

    // Only enable "Update Profile" once something has actually changed
    val hasChanges = userData != null && (name.trim() != userData.name || phone != userData.phoneNumber)

    // Uses the same name and phone rules as the Register screen
    fun validate(): Boolean {
        nameError = Validators.validateFullName(name)
        phoneError = Validators.validatePhone(phone)
        val isValid = nameError == null && phoneError == null
        return isValid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Simple person icon in place of a profile photo
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Email is only shown here, it can't be edited on this screen
                Text(
                    text = if (userData?.email.isNullOrBlank()) "" else "Signed in as ${userData?.email}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                PetCareTextField(
                    value = name,
                    onValueChange = { input ->
                        // Only letters and spaces are allowed in a name
                        val filtered = input.filter { it.isLetter() || it.isWhitespace() }
                        name = filtered
                        nameError = null
                        onClearErrors()
                    },
                    label = "Full Name",
                    error = nameError,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text
                    )
                )

                PetCareTextField(
                    value = phone,
                    onValueChange = { input ->
                        // Digits only, and no more than 10 of them
                        val filtered = input.filter { it.isDigit() }
                        if (filtered.length <= 10) {
                            phone = filtered
                        }
                        phoneError = null
                        onClearErrors()
                    },
                    label = "Phone Number",
                    error = phoneError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                )

                // Message from Firebase if the update fails
                if (profileError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = profileError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Green box once the details are saved
                if (successMessage != null) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = successMessage,
                            color = Color(0xFF2E7D32),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                PetCareButton(
                    text = "Update Profile",
                    isLoading = isProfileLoading,
                    enabled = hasChanges,
                    onClick = {
                        if (validate()) {
                            onUpdateProfile(name.trim(), phone)
                        } else {
                            // A quick toast as well, in case the error text is scrolled out of view
                            Toast.makeText(context, "Please fix input errors", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

// Preview with a made-up user so the screen can be checked in Android Studio
@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    val sampleUser = User(
        uid = "1",
        name = "Emily Watson",
        email = "emily@example.com",
        phoneNumber = "9876543210"
    )
    PetCareApplicationTheme {
        ProfileScreenContent(
            userData = sampleUser,
            isProfileLoading = false,
            profileError = null,
            successMessage = null,
            onBack = {},
            onUpdateProfile = { _, _ -> },
            onClearErrors = {}
        )
    }
}