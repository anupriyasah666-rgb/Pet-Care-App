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
import np.com.petcareapplication.ui.components.PetCareCard
import np.com.petcareapplication.ui.theme.BluePrimary
import np.com.petcareapplication.ui.theme.PetCareApplicationTheme
import np.com.petcareapplication.ui.theme.PinkHighlight
import np.com.petcareapplication.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExpenseScreen(
    petId: String,
    onBack: () -> Unit,
    onAddExpenseClick: () -> Unit,
    viewModel: ExpenseViewModel = viewModel()
) {
    val expenses by viewModel.expenses.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()

    LaunchedEffect(petId) {
        viewModel.loadExpenses(petId)
    }

    ExpenseScreenContent(
        expenses = expenses,
        totalSpent = totalSpent,
        onBack = onBack,
        onAddExpenseClick = onAddExpenseClick,
        onDeleteExpense = { viewModel.deleteExpense(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreenContent(
    expenses: List<Expense>,
    totalSpent: Double,
    onBack: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onDeleteExpense: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Summary", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddExpenseClick,
                containerColor = BluePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Log Expense") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, BluePrimary.copy(alpha = 0.05f))
                    )
                )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                color = BluePrimary,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Amount Spent", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", totalSpent)}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

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
                    color = BluePrimary
                )
                Surface(
                    color = BluePrimary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${expenses.size} records",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
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
                    items(expenses) { expense ->
                        ExpenseItem(expense = expense, onDelete = { onDeleteExpense(expense.id) })
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

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
                color = PinkHighlight.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when(expense.category.lowercase()) {
                            "food" -> Icons.Default.Restaurant
                            "medical" -> Icons.Default.MedicalServices
                            "grooming" -> Icons.Default.ContentCut
                            "toys" -> Icons.Default.Toys
                            else -> Icons.AutoMirrored.Filled.ReceiptLong
                        },
                        contentDescription = null,
                        tint = PinkHighlight,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = expense.category, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = expense.description, fontSize = 13.sp, color = Color.Gray, maxLines = 1)
                Text(
                    text = dateFormatter.format(Date(expense.date)),
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-$${String.format(Locale.US, "%.2f", expense.amount)}",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFD32F2F),
                    fontSize = 16.sp
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

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
            color = Color.LightGray.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.LightGray
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("No expenses yet", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Keep track of your pet spending here", color = Color.LightGray, fontSize = 14.sp)
    }
}

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
