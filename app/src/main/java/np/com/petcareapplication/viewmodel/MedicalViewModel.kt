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
import np.com.petcareapplication.model.MedicalRecord
import np.com.petcareapplication.repository.PetRepository

// Looks after a pet's health records (vaccinations, checkups and medication)
// for the Health Records screen
class MedicalViewModel(private val repository: PetRepository = PetRepository()) : ViewModel() {

    // This pet's health records
    private val _records = MutableStateFlow<List<MedicalRecord>>(emptyList())
    val records: StateFlow<List<MedicalRecord>> = _records

    // Short messages so the user knows whether saving or deleting worked
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Listen to this pet's records in Firestore, newest first
    fun loadRecords(petId: String) {
        viewModelScope.launch {
            repository.getMedicalRecords(petId)
                .catch { e ->
                    _errorMessage.value = "Failed to load health records."
                    Log.e("MedicalViewModel", "Error loading records", e)
                }
                .collectLatest {
                    _records.value = it.sortedByDescending { record -> record.date }
                }
        }
    }

    // Saves a new record from the "Log Health Event" pop-up and shows "Health record saved" for 2 seconds
    fun addRecord(record: MedicalRecord) {
        viewModelScope.launch {
            try {
                repository.addMedicalRecord(record)
                _successMessage.value = "Health record saved"
                delay(2000)
                _successMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add record."
            }
        }
    }

    // Deletes a record after the user confirms. The screen checks for this exact message
    // ("Health record deleted") to show it in pink instead of green
    fun deleteRecord(recordId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMedicalRecord(recordId)
                _successMessage.value = "Health record deleted"
                delay(2000)
                _successMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete record."
            }
        }
    }

    // Hide the error message
    fun clearError() {
        _errorMessage.value = null
    }
}