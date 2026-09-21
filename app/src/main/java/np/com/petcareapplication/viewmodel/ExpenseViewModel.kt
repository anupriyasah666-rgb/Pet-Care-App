package np.com.petcareapplication.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import np.com.petcareapplication.model.Expense
import np.com.petcareapplication.repository.PetRepository

/**
 * ViewModel responsible for managing pet-related expenses.
 */
class ExpenseViewModel(private val repository: PetRepository = PetRepository()) : ViewModel() {

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses

    private val _totalSpent = MutableStateFlow(0.0)
    val totalSpent: StateFlow<Double> = _totalSpent

    // Feedback states for better UX
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun loadExpenses(petId: String) {
        viewModelScope.launch {
            repository.getExpenses(petId)
                .catch { e -> Log.e("ExpenseViewModel", "Error loading expenses", e) }
                .collectLatest { list ->
                    _expenses.value = list
                    _totalSpent.value = list.sumOf { it.amount }
                }
        }
    }

    /**
     * Fulfills: Track expenses.
     * Displays: "Record Saved"
     */
    fun addExpense(expense: Expense, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.addExpense(expense)
                _successMessage.value = "Record Saved"
                delay(1500)
                _successMessage.value = null
                onComplete()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add expense."
            }
        }
    }

    /**
     * Fulfills: Delete items (Expense).
     * Displays: "Expense record removed"
     */
    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(expenseId)
                _successMessage.value = "Expense record removed"
                delay(2000)
                _successMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete expense."
            }
        }
    }

    fun clearErrorMessage() { _errorMessage.value = null }
}
