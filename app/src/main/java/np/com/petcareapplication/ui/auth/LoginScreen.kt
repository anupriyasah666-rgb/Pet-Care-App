package np.com.petcareapplication.ui.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import np.com.petcareapplication.ui.components.PetCareButton
import np.com.petcareapplication.ui.components.PetCareTextField
import np.com.petcareapplication.ui.theme.BlueDark
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.ui.theme.PinkHighlight
import np.com.petcareapplication.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val user by viewModel.user.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    val resetError by viewModel.resetError.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val isLoginLoading by viewModel.isLoginLoading.collectAsState()
    val isResetLoading by viewModel.isResetLoading.collectAsState()

    LaunchedEffect(user) {
        if (user != null) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            if (it.contains("reset link", ignoreCase = true)) {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearSuccessMessage()
            }
        }
    }

    LaunchedEffect(loginError) {
        if (loginError != null) {
            delay(2000)
            viewModel.clearErrors()
        }
    }

    LaunchedEffect(resetError) {
        if (resetError != null) {
            delay(2000)
            viewModel.clearErrors()
        }
    }

    LoginScreenContent(
        loginError = loginError,
        resetError = resetError,
        successMessage = successMessage,
        isLoginLoading = isLoginLoading,
        isResetLoading = isResetLoading,
        onLogin = { email, password -> viewModel.login(email, password) },
        onResetPassword = { viewModel.resetPassword(it) },
        onNavigateToRegister = onNavigateToRegister,
        onClearErrors = { viewModel.clearErrors() }
    )
}

@Composable
fun LoginScreenContent(
    loginError: String?,
    resetError: String?,
    successMessage: String?,
    isLoginLoading: Boolean,
    isResetLoading: Boolean,
    onLogin: (String, String) -> Unit,
    onResetPassword: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onClearErrors: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    // Clear login fields if error occurs (as per original logic)
    LaunchedEffect(loginError) {
        if (loginError != null) {
            email = ""
            password = ""
        }
    }

    fun validate(): Boolean {
        var isValid = true
        if (email.isBlank()) {
            emailError = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Invalid email format"
            isValid = false
        } else {
            emailError = null
        }

        if (password.isBlank()) {
            passwordError = "Password is required"
            isValid = false
        } else {
            passwordError = null
        }
        return isValid
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!isResetLoading) {
                    showResetDialog = false 
                    onClearErrors()
                }
            },
            title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your email address to receive a password reset link.")
                    Spacer(modifier = Modifier.height(16.dp))
                    PetCareTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = "Email Address",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    
                    resetError?.let { error ->
                        val displayedResetError = if (error.contains("no user record", ignoreCase = true) || 
                                                     error.contains("user not found", ignoreCase = true)) {
                            "Email does not exist"
                        } else {
                            error
                        }
                        Text(
                            text = displayedResetError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onResetPassword(resetEmail) },
                    enabled = !isResetLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    if (isResetLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Send link")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showResetDialog = false 
                    onClearErrors()
                }, enabled = !isResetLoading) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
        
        // Auto-close dialog on success
        LaunchedEffect(successMessage) {
            if (successMessage != null && successMessage.contains("reset link")) {
                showResetDialog = false
            }
        }
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
            // Stylish Logo Area with "Happy Pets" in Cursive
            Surface(
                modifier = Modifier.size(160.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 24.dp,
                border = BorderStroke(4.dp, Brush.linearGradient(listOf(BluePrimary, PinkHighlight)))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(70.dp)
                        )
                        Text(
                            text = "Happy Pets",
                            color = BluePrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Cursive
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Welcome!",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White, Color.White.copy(alpha = 0.8f))
                    ),
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Serif
                )
            )

            Text(
                text = "Care for your best friends",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(40.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PetCareTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            if (emailError != null) emailError = null
                        },
                        label = "Email Address",
                        error = emailError,
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BluePrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    PetCareTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            if (passwordError != null) passwordError = null
                        },
                        label = "Password",
                        error = passwordError,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BluePrimary) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = { 
                            resetEmail = email
                            showResetDialog = true 
                        }) {
                            Text(
                                text = "Forgot Password?",
                                color = BluePrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    loginError?.let { error ->
                        val displayedError = if (error.contains("auth credential", ignoreCase = true)) {
                            "Invalid credentials"
                        } else {
                            error
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                        ) {
                            Text(
                                text = displayedError,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    successMessage?.let { message ->
                        if (!message.contains("reset link", ignoreCase = true)) {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                            ) {
                                Text(
                                    text = message,
                                    color = Color(0xFF2E7D32),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    PetCareButton(
                        text = "Login",
                        isLoading = isLoginLoading,
                        onClick = { 
                            if (validate()) {
                                onLogin(email, password)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text(
                    text = "New here? Create an Account",
                    color = if (MaterialTheme.colorScheme.primary == BluePrimary) BlueDark else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    PetCareApplicationTheme {
        LoginScreenContent(
            loginError = null,
            resetError = null,
            successMessage = null,
            isLoginLoading = false,
            isResetLoading = false,
            onLogin = { _, _ -> },
            onResetPassword = { _ -> },
            onNavigateToRegister = {},
            onClearErrors = {}
        )
    }
}
