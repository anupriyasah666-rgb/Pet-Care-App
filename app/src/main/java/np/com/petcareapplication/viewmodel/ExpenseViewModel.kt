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

// Looks after a pet's expenses for the Expense and Add Expense screens.
// It uses the same PetRepository as the pets and tasks, since everything is in Firestore
class ExpenseViewModel(private val repository: PetRepository = PetRepository()) : ViewModel() {

    // This pet's expenses
    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses

    // Running total shown on the Expense screen and the Pet Detail card
    private val _totalSpent = MutableStateFlow(0.0)
    val totalSpent: StateFlow<Double> = _totalSpent

    // Short messages so the user knows whether saving or deleting worked
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Listen to this pet's expenses in Firestore. Each time the list changes,
    // the total is worked out again, so it's always up to date
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

    // Saves a new expense from the Add Expense screen.
    // Shows "Record Saved" for a moment, then onComplete takes the user back
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

    // Deletes an expense after the user confirms on the Expense screen.
    // The list and the total update by themselves, because loadExpenses is still listening
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

    // Used by Add Expense to hide the error after a few seconds
    fun clearErrorMessage() { _errorMessage.value = null }
}