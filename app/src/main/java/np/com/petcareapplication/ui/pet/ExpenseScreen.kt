package np.com.petcareapplication.ui.pet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import np.com.petcareapplication.model.Expense
import np.com.petcareapplication.ui.components.ConfirmDeleteDialog
import np.com.petcareapplication.ui.components.PetCareCard
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.ui.theme.PinkHighlight
import np.com.petcareapplication.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

// Shows everything spent on one pet, with the total at the top
@Composable
fun ExpenseScreen(
    petId: String,
    onBack: () -> Unit,
    onAddExpenseClick: () -> Unit,
    viewModel: ExpenseViewModel = viewModel()
) {
    val expenses by viewModel.expenses.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    // Load this pet's expenses when the screen opens, or if a different pet is shown
    LaunchedEffect(petId) {
        viewModel.loadExpenses(petId)
    }

    ExpenseScreenContent(
        expenses = expenses,
        totalSpent = totalSpent,
        onBack = onBack,
        onAddExpenseClick = onAddExpenseClick,
        onDeleteExpense = { viewModel.deleteExpense(it) },
        successMessage = successMessage
    )
}

// The screen layout, kept separate from the ViewModel so the preview can use sample expenses
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreenContent(
    expenses: List<Expense>,
    totalSpent: Double,
    onBack: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onDeleteExpense: (String) -> Unit,
    successMessage: String? = null
) {
    // Holds the expense the user wants to delete. While it's set, the confirm dialog is shown
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    expenseToDelete?.let { expense ->
        ConfirmDeleteDialog(
            title = "Delete expense?",
            // Show the amount with two decimal places, e.g. $12.50
            message = "Remove the ${expense.category} expense of $${String.format(Locale.US, "%.2f", expense.amount)}? Your total will be updated.",
            onConfirm = { onDeleteExpense(expense.id); expenseToDelete = null },
            onDismiss = { expenseToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Summary", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddExpenseClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Log Expense") }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                        )
                    )
            ) {
                // Big blue card with the total spent so far
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Amount Spent", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", totalSpent)}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // Heading with a small badge showing how many expenses there are
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${expenses.size} records",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (expenses.isEmpty()) {
                    EmptyExpensePlaceholder()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Newest spending first
                        items(expenses.sortedByDescending { it.date }, key = { it.id }) { expense ->
                            ExpenseItem(expense = expense, onDelete = { expenseToDelete = expense })
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }

            // Pink message after an expense is deleted. The colour is fixed so the white text is readable in dark mode too
            if (successMessage != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp, start = 24.dp, end = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFC2185B),
                    contentColor = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = successMessage,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// One expense in the list: category icon, description, date and amount
@Composable
fun ExpenseItem(expense: Expense, onDelete: () -> Unit) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    PetCareCard {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        // Pick an icon that matches the category
                        imageVector = when(expense.category.lowercase()) {
                            "food" -> Icons.Default.Restaurant
                            "medical" -> Icons.Default.MedicalServices
                            "grooming" -> Icons.Default.ContentCut
                            "toys" -> Icons.Default.Toys
                            "medication" -> Icons.Default.Medication
                            else -> Icons.AutoMirrored.Filled.ReceiptLong
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = expense.category, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = expense.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(
                    text = dateFormatter.format(Date(expense.date)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-$${String.format(Locale.US, "%.2f", expense.amount)}",
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp
                )
                // Bin button asks first, it doesn't delete straight away
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Delete ${expense.category} expense", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// What the user sees before logging any expenses
@Composable
fun EmptyExpensePlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("No expenses yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Keep track of your pet spending here", color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
    }
}

// Preview with three made-up expenses
@Preview(showBackground = true)
@Composable
fun ExpenseScreenPreview() {
    val sampleExpenses = listOf(
        Expense(id = "1", category = "Food", amount = 45.50, description = "Premium Kibble 10kg", date = System.currentTimeMillis()),
        Expense(id = "2", category = "Medical", amount = 120.00, description = "Annual Vaccination", date = System.currentTimeMillis() - 86400000 * 2),
        Expense(id = "3", category = "Toys", amount = 15.99, description = "Chew Toy", date = System.currentTimeMillis() - 86400000 * 5)
    )
    PetCareApplicationTheme {
        ExpenseScreenContent(
            expenses = sampleExpenses,
            totalSpent = 181.49,
            onBack = {},
            onAddExpenseClick = {},
            onDeleteExpense = {}
        )
    }
}