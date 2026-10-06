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
import np.com.petcareapplication.ui.components.PetCareButton
import np.com.petcareapplication.ui.components.PetCareTextField
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val userData by viewModel.userData.collectAsState()
    val isProfileLoading by viewModel.isProfileLoading.collectAsState()
    val profileError by viewModel.profileError.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUserData()
    }

    ProfileScreenContent(
        userData = userData,
        isProfileLoading = isProfileLoading,
        profileError = profileError,
        successMessage = successMessage,
        onBack = onBack,
        onUpdateProfile = { name, phone -> viewModel.updateProfile(name, phone, null) },
        onClearErrors = { viewModel.clearErrors() }
    )
}

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

    LaunchedEffect(userData) {
        userData?.let {
            name = it.name
            phone = it.phoneNumber
        }
    }

    fun validate(): Boolean {
        var isValid = true
        val trimmedName = name.trim()

        if (trimmedName.isEmpty()) {
            nameError = "Full name is required"
            isValid = false
        } else if (trimmedName.split("\\s+".toRegex()).filter { it.isNotBlank() }.size < 2) {
            nameError = "Please enter at least two names (First and Last name)"
            isValid = false
        } else if (!trimmedName.all { it.isLetter() || it.isWhitespace() }) {
            nameError = "Name should only contain letters"
            isValid = false
        } else {
            nameError = null
        }

        if (phone.length != 10) {
            phoneError = "Phone number must be exactly 10 digits"
            isValid = false
        } else {
            phoneError = null
        }

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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, BluePrimary.copy(alpha = 0.05f))
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
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = BluePrimary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            tint = BluePrimary
                        )
                    }
                }

                Text(
                    text = userData?.email ?: "",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                PetCareTextField(
                    value = name,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isLetter() || it.isWhitespace() }
                        name = filtered
                        nameError = null
                    },
                    label = "Full Name",
                    error = nameError,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BluePrimary) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text
                    )
                )

                PetCareTextField(
                    value = phone,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }
                        if (filtered.length <= 10) {
                            phone = filtered
                        }
                        phoneError = null
                    },
                    label = "Phone Number",
                    error = phoneError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BluePrimary) }
                )

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

                Spacer(modifier = Modifier.weight(1f))

                PetCareButton(
                    text = "Update Profile",
                    isLoading = isProfileLoading,
                    onClick = {
                        if (validate()) {
                            onUpdateProfile(name.trim(), phone)
                        } else {
                            Toast.makeText(context, "Please fix input errors", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

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
