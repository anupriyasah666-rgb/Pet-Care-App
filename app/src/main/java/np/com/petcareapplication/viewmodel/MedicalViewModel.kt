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

/**
 * ViewModel for managing pet medical records (vaccinations, checkups, etc.).
 * Fulfills the prototype requirement for "managing healthcare records".
 */
class MedicalViewModel(private val repository: PetRepository = PetRepository()) : ViewModel() {

    private val _records = MutableStateFlow<List<MedicalRecord>>(emptyList())
    val records: StateFlow<List<MedicalRecord>> = _records

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * Loads all medical records for a specific pet from Firestore.
     */
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

    /**
     * Adds a new health record.
     * Displays message: "Health record saved"
     */
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

    /**
     * Deletes a specific medical entry.
     * Displays message: "Health record deleted"
     */
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

    fun clearError() {
        _errorMessage.value = null
    }
}
