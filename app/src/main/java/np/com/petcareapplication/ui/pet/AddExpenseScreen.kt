package np.com.petcareapplication.ui.pet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import np.com.petcareapplication.ui.components.PetCareButton
import np.com.petcareapplication.ui.components.PetCareTextField
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.viewmodel.ExpenseViewModel

@Composable
fun AddExpenseScreen(
    petId: String,
    onBack: () -> Unit,
    onExpenseAdded: () -> Unit,
    viewModel: ExpenseViewModel = viewModel()
) {
    val successMessage by viewModel.successMessage.collectAsState()

    AddExpenseScreenContent(
        successMessage = successMessage,
        onBack = onBack,
        onAddExpense = { category, amount, description ->
            val newExpense = Expense(
                petId = petId,
                category = category,
                amount = amount,
                description = description,
                date = System.currentTimeMillis()
            )
            viewModel.addExpense(newExpense, onExpenseAdded)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreenContent(
    successMessage: String?,
    onBack: () -> Unit,
    onAddExpense: (String, Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    
    var amountError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    
    val categories = listOf("Food", "Grooming", "Medical", "Toys", "Medication", "Other")
    var categoryExpanded by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        var isValid = true
        if (amount.isBlank()) {
            amountError = "Amount is required"
            isValid = false
        } else if (amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
            amountError = "Invalid amount"
            isValid = false
        } else amountError = null

        if (description.isBlank()) {
            descriptionError = "Description is required"
            isValid = false
        } else descriptionError = null

        return isValid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Expense", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
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
                            color = BluePrimary
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                label = { Text("Category") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                shape = RoundedCornerShape(16.dp),
                                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = BluePrimary) },
                                trailingIcon = {
                                    IconButton(onClick = { categoryExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BluePrimary,
                                    unfocusedBorderColor = Color.LightGray
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

                        PetCareTextField(
                            value = amount,
                            onValueChange = { amount = it; amountError = null },
                            label = "Amount ($)",
                            error = amountError,
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = BluePrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

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
                                focusedBorderColor = BluePrimary,
                                unfocusedBorderColor = Color.LightGray
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
                    onClick = {
                        if (validate()) {
                            onAddExpense(category, amount.toDouble(), description)
                        }
                    }
                )
            }

            // SUCCESS MESSAGE OVERLAY IN GREEN COLOR
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
        }
    }
}

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
