package np.com.petcareapplication.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import np.com.petcareapplication.ui.components.PetCareButton
import np.com.petcareapplication.ui.components.PetCareTextField
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.viewmodel.AuthViewModel

/**
 * ForgotPasswordScreen allows users to reset their password.
 * Fulfills the "User registration and login" core requirement.
 */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val isResetLoading by viewModel.isResetLoading.collectAsState()
    val resetError by viewModel.resetError.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    ForgotPasswordScreenContent(
        isResetLoading = isResetLoading,
        resetError = resetError,
        successMessage = successMessage,
        onBack = onBack,
        onResetPassword = { viewModel.resetPassword(it) },
        onClearErrors = { viewModel.clearErrors() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreenContent(
    isResetLoading: Boolean,
    resetError: String?,
    successMessage: String?,
    onBack: () -> Unit,
    onResetPassword: (String) -> Unit,
    onClearErrors: () -> Unit
) {
    var email by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enter your email address to receive a password reset link.",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                PetCareTextField(
                    value = email,
                    onValueChange = { email = it; onClearErrors() },
                    label = "Email Address",
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BluePrimary) }
                )

                if (resetError != null) {
                    Text(
                        text = resetError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                if (successMessage != null) {
                    Text(
                        text = successMessage,
                        color = Color(0xFF2E7D32),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                PetCareButton(
                    text = "Send Reset Link",
                    isLoading = isResetLoading,
                    onClick = {
                        onResetPassword(email)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    PetCareApplicationTheme {
        ForgotPasswordScreenContent(
            isResetLoading = false,
            resetError = null,
            successMessage = null,
            onBack = {},
            onResetPassword = {},
            onClearErrors = {}
        )
    }
}
