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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import np.com.petcareapplication.util.Validators
import np.com.petcareapplication.ui.components.PetCareButton
import np.com.petcareapplication.ui.components.PetCareTextField
import np.com.petcareapplication.ui.theme.BlueDark
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.viewmodel.AuthViewModel

// Connects the sign-up form to AuthViewModel
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = viewModel(),
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current
    val user by viewModel.user.collectAsState()
    val registerError by viewModel.registerError.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val isRegisterLoading by viewModel.isRegisterLoading.collectAsState()

    // Firebase signs the new user in straight after creating the account.
    // MainActivity then signs them out and sends them to Login to sign in with their new details
    LaunchedEffect(user) {
        if (user != null) {
            onRegisterSuccess()
        }
    }

    RegisterScreenContent(
        registerError = registerError,
        successMessage = successMessage,
        isRegisterLoading = isRegisterLoading,
        onRegister = { name, email, password, phone ->
            viewModel.register(name, email, password, phone)
        },
        onNavigateToLogin = onNavigateToLogin
    )
}

// The form itself. It doesn't know about the ViewModel, so the preview can show it on its own
@Composable
fun RegisterScreenContent(
    registerError: String?,
    successMessage: String?,
    isRegisterLoading: Boolean,
    onRegister: (String, String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Runs every check at once so all the problems show up together, not one at a time
    fun validate(): Boolean {
        nameError = Validators.validateFullName(name)
        emailError = Validators.validateEmail(email)
        phoneError = Validators.validatePhone(phone)
        passwordError = Validators.validatePassword(password)
        confirmPasswordError = Validators.validateConfirmPassword(password, confirmPassword)
        val isValid = listOf(nameError, emailError, phoneError, passwordError, confirmPasswordError).all { it == null }
        return isValid
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BluePrimary, MaterialTheme.colorScheme.background)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Heading at the top of the blue background
            Text(text = "Create Account", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(text = "Join our community of pet lovers", fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f))
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PetCareTextField(
                        value = name,
                        onValueChange = { input ->
                            // Ignore anything that isn't a letter or a space
                            if (input.all { it.isLetter() || it.isWhitespace() }) {
                                name = input
                                nameError = null
                            }
                        },
                        label = "Full Name",
                        error = nameError,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BluePrimary) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PetCareTextField(
                        value = email,
                        onValueChange = { email = it; emailError = null },
                        label = "Email Address",
                        error = emailError,
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BluePrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PetCareTextField(
                        value = phone,
                        onValueChange = { input ->
                            // Digits only, and stop at 10
                            val digitsOnly = input.filter { it.isDigit() }
                            if (digitsOnly.length <= 10) {
                                phone = digitsOnly
                                phoneError = null
                            }
                        },
                        label = "Phone Number",
                        error = phoneError,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BluePrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // A weak password shows one red message under this box that lists everything still missing.
                    // The message itself is built in Validators.validatePassword
                    PetCareTextField(
                        value = password,
                        onValueChange = { password = it; passwordError = null },
                        label = "Password",
                        error = passwordError,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BluePrimary) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = if (passwordVisible) "Hide password" else "Show password", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PetCareTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; confirmPasswordError = null },
                        label = "Confirm Password",
                        error = confirmPasswordError,
                        leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = BluePrimary) },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )

                    // Message from Firebase, for example when the email is already registered
                    if (registerError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
                        ) {
                            Text(text = registerError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
                        }
                    }

                    if (successMessage != null) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
                        ) {
                            Text(text = successMessage, color = Color(0xFF2E7D32), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    PetCareButton(
                        text = "Sign Up",
                        isLoading = isRegisterLoading,
                        // Only call Firebase when every field passes the checks
                        onClick = { if (validate()) onRegister(name.trim(), email, password, phone) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            // Back to the Login screen for people who already have an account
            TextButton(onClick = onNavigateToLogin) {
                Text(
                    text = "Already have an account? Login",
                    color = if (MaterialTheme.colorScheme.primary == BluePrimary) BlueDark else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// Preview for Android Studio's design view
@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    PetCareApplicationTheme {
        RegisterScreenContent(
            registerError = null,
            successMessage = null,
            isRegisterLoading = false,
            onRegister = { _, _, _, _ -> },
            onNavigateToLogin = {}
        )
    }
}