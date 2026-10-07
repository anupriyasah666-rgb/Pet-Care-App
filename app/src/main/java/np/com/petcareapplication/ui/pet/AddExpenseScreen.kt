package np.com.petcareapplication.ui.pet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import np.com.petcareapplication.model.Expense
import np.com.petcareapplication.util.Validators
import np.com.petcareapplication.ui.components.PetCareButton
import np.com.petcareapplication.ui.components.PetCareTextField
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.viewmodel.ExpenseViewModel

// Screen for logging money spent on a pet, like food or a vet visit
@Composable
fun AddExpenseScreen(
    petId: String,
    onBack: () -> Unit,
    onExpenseAdded: () -> Unit,
    viewModel: ExpenseViewModel = viewModel()
) {
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    AddExpenseScreenContent(
        successMessage = successMessage,
        errorMessage = errorMessage,
        onErrorShown = { viewModel.clearErrorMessage() },
        onBack = onBack,
        onAddExpense = { category, amount, description ->
            // Today's date is saved automatically, so the user doesn't have to pick one
            val newExpense = Expense(
                petId = petId,
                category = category,
                amount = amount,
                description = description,
                date = System.currentTimeMillis()
            )
            // onExpenseAdded takes the user back once Firestore has saved it
            viewModel.addExpense(newExpense, onExpenseAdded)
        }
    )
}

// The form layout, kept separate from the ViewModel so the preview works
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreenContent(
    successMessage: String?,
    onBack: () -> Unit,
    onAddExpense: (String, Double, String) -> Unit,
    errorMessage: String? = null,
    onErrorShown: () -> Unit = {}
) {
    // Stops a quick double tap on "Save Record" from saving the same expense twice
    var isSaving by remember { mutableStateOf(false) }

    // If the save fails, let the user press the button again and hide the error after 3 seconds
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            isSaving = false
            kotlinx.coroutines.delay(3000)
            onErrorShown()
        }
    }

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }

    var amountError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }

    // Options for the category drop-down
    val categories = listOf("Food", "Grooming", "Medical", "Toys", "Medication", "Other")
    var categoryExpanded by remember { mutableStateOf(false) }

    // Amount must be a proper number above zero, and the description can't be empty
    fun validate(): Boolean {
        amountError = Validators.validateAmount(amount)
        descriptionError = Validators.validateRequired(description, "Description")
        val isValid = amountError == null && descriptionError == null
        return isValid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Expense", fontWeight = FontWeight.Bold) },
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Spending Details",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Category box can't be typed in. The arrow opens a list to pick from
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                label = { Text("Category") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                trailingIcon = {
                                    IconButton(onClick = { categoryExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose category")
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            category = cat
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Number keyboard with a decimal point, so amounts like 12.50 can be typed
                        PetCareTextField(
                            value = amount,
                            onValueChange = { amount = it; amountError = null },
                            label = "Amount ($)",
                            error = amountError,
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        // Bigger box with room for a few lines of description
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it; descriptionError = null },
                            label = { Text("What was this for?") },
                            isError = descriptionError != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        if (descriptionError != null) {
                            Text(
                                text = descriptionError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                PetCareButton(
                    text = "Save Record",
                    isLoading = isSaving,
                    onClick = {
                        if (!isSaving && validate()) {
                            isSaving = true
                            // validate() has already checked the amount, so toDouble() is safe here
                            onAddExpense(category, amount.trim().toDouble(), description.trim())
                        }
                    }
                )
            }

            // Green message at the bottom when the expense has been saved
            if (successMessage != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp, start = 24.dp, end = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF2E7D32),
                    contentColor = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = successMessage,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Red message at the bottom if saving fails, for example with no internet
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp, start = 24.dp, end = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Preview for Android Studio's design view
@Preview(showBackground = true)
@Composable
fun AddExpenseScreenPreview() {
    PetCareApplicationTheme {
        AddExpenseScreenContent(
            successMessage = null,
            onBack = {},
            onAddExpense = { _, _, _ -> }
        )
    }
}