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
import np.com.petcareapplication.ui.components.PetCareButton
import np.com.petcareapplication.ui.components.PetCareTextField
import np.com.petcareapplication.ui.theme.BlueDark
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.viewmodel.AuthViewModel

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

    LaunchedEffect(user) {
        if (user != null) {
            onRegisterSuccess()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
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

    fun validate(): Boolean {
        var isValid = true
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            nameError = "Full name is required"
            isValid = false
        } else if (trimmedName.split("\\s+".toRegex()).size < 2) {
            nameError = "Please include both First and Last name"
            isValid = false
        } else {
            nameError = null
        }

        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Valid email is required"
            isValid = false
        } else {
            emailError = null
        }

        if (phone.length != 10) {
            phoneError = "Phone must be exactly 10 digits"
            isValid = false
        } else {
            phoneError = null
        }

        if (password.length < 8) {
            passwordError = "Password must be at least 8 characters"
            isValid = false
        } else {
            passwordError = null
        }

        if (confirmPassword != password) {
            confirmPasswordError = "Passwords do not match"
            isValid = false
        } else {
            confirmPasswordError = null
        }

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
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                Icon(if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )

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
                        onClick = { if (validate()) onRegister(name.trim(), email, password, phone) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
