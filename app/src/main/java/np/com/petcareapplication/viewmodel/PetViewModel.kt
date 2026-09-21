package np.com.petcareapplication.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import np.com.petcareapplication.model.CareTask
import np.com.petcareapplication.model.Pet
import np.com.petcareapplication.repository.PetRepository

/**
 * PetViewModel manages the UI state for all pet-related operations.
 */
class PetViewModel(private val repository: PetRepository = PetRepository()) : ViewModel() {

    private val _pets = MutableStateFlow<List<Pet>>(emptyList())
    val pets: StateFlow<List<Pet>> = _pets

    private val _tasks = MutableStateFlow<List<CareTask>>(emptyList())
    val tasks: StateFlow<List<CareTask>> = _tasks

    private val _allTasks = MutableStateFlow<List<CareTask>>(emptyList())
    val allTasks: StateFlow<List<CareTask>> = _allTasks

    private val _isImageUploading = MutableStateFlow(false)
    val isImageUploading: StateFlow<Boolean> = _isImageUploading

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun loadPets(ownerId: String) {
        viewModelScope.launch {
            repository.getPets(ownerId)
                .catch { e -> _errorMessage.value = "Error loading pets." }
                .collectLatest { _pets.value = it }
        }
    }

    fun loadTasks(petId: String) {
        viewModelScope.launch {
            repository.getTasks(petId)
                .catch { e -> _errorMessage.value = "Failed to load tasks." }
                .collectLatest { _tasks.value = it }
        }
    }

    fun loadAllTasks(ownerId: String) {
        viewModelScope.launch {
            repository.getAllTasks(ownerId)
                .catch { e -> Log.e("PetViewModel", "Error loading consolidated tasks", e) }
                .collectLatest { _allTasks.value = it }
        }
    }

    fun showFeedback(message: String) {
        viewModelScope.launch {
            _successMessage.value = message
            delay(2000)
            _successMessage.value = null
        }
    }

    fun resetTasks(petId: String) {
        viewModelScope.launch {
            try {
                _tasks.value.forEach { task ->
                    if (task.isCompleted) {
                        repository.updateTask(task.copy(isCompleted = false))
                    }
                }
                showFeedback("Today's checklist reset!")
            } catch (e: Exception) { _errorMessage.value = "Reset failed." }
        }
    }

    fun clearRoutine(petId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRoutine(petId)
                showFeedback("Care routine cleared successfully")
            } catch (e: Exception) { _errorMessage.value = "Failed to clear routine." }
        }
    }

    fun addTaskWithImage(task: CareTask, imageUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isImageUploading.value = true
            try {
                var url = ""
                if (imageUri != null) url = repository.uploadPetImage(imageUri)
                repository.addTask(task.copy(imageUrl = url))
                _isImageUploading.value = false
                showFeedback("Task added")
                delay(1000)
                onComplete()
            } catch (e: Exception) { _isImageUploading.value = false }
        }
    }

    fun updateTaskWithImage(task: CareTask, newUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isImageUploading.value = true
            try {
                var url = task.imageUrl
                if (newUri != null && !newUri.toString().startsWith("http")) {
                    url = repository.uploadPetImage(newUri)
                }
                repository.updateTask(task.copy(imageUrl = url))
                _isImageUploading.value = false
                showFeedback("Task updated successfully")
                delay(1000)
                onComplete()
            } catch (e: Exception) { _isImageUploading.value = false }
        }
    }

    fun addPetWithImage(pet: Pet, imageUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isImageUploading.value = true
            try {
                var url = ""
                if (imageUri != null) url = repository.uploadPetImage(imageUri)
                repository.addPet(pet.copy(imageUrl = url))
                _isImageUploading.value = false
                
                // Fast Redirect: Call navigation immediately
                onComplete()
                
                // Show feedback on Home screen
                showFeedback("Pet added")
            } catch (e: Exception) { 
                _isImageUploading.value = false
                Log.e("PetViewModel", "Error adding pet", e)
            }
        }
    }

    /**
     * Updates pet details including image upload if a new local image is selected.
     * Uses a more robust check for local vs remote URIs to ensure saving actually works.
     */
    fun updatePetWithImage(pet: Pet, newUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isImageUploading.value = true
            try {
                var finalImageUrl = pet.imageUrl
                
                if (newUri != null) {
                    val uriString = newUri.toString()
                    // If it's a local content URI or a file path, we MUST upload it to save it permanently.
                    // If it's already an http link, it's already saved in Firebase Storage.
                    if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                        finalImageUrl = repository.uploadPetImage(newUri)
                    } else if (uriString.startsWith("http")) {
                        finalImageUrl = uriString
                    }
                }
                
                // Update Firestore document with the correct Image URL and all other modified fields
                repository.updatePet(pet.copy(imageUrl = finalImageUrl))
                
                _isImageUploading.value = false
                
                // Fast Redirect: Navigate back immediately
                onComplete()
                
                // Show feedback on Home screen
                showFeedback("Profile updated successfully")
            } catch (e: Exception) { 
                _isImageUploading.value = false
                _errorMessage.value = "Failed to update profile."
                Log.e("PetViewModel", "Error updating pet image/data", e)
            }
        }
    }

    fun deletePet(petId: String, onComplete: () -> Unit) {
        // Fast Redirect: Call navigation immediately as requested
        onComplete()
        _successMessage.value = "Pet deleted"
        
        viewModelScope.launch { 
            try {
                // Perform background deletion and cleanup
                repository.deletePet(petId)
                repository.cleanupPetData(petId)
                delay(2000)
                _successMessage.value = null
            } catch (e: Exception) { 
                Log.e("PetViewModel", "Failed to delete pet in background", e)
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
                showFeedback("Task is deleted")
            } catch (e: Exception) { _errorMessage.value = "Delete failed." }
        }
    }

    fun toggleTaskCompletion(task: CareTask) {
        viewModelScope.launch {
            try {
                val newStatus = !task.isCompleted
                repository.updateTask(task.copy(isCompleted = newStatus))
                showFeedback(if (newStatus) "Marked as completed" else "Task marked as uncompleted")
            } catch (e: Exception) { _errorMessage.value = "Update failed." }
        }
    }

    fun addTask(task: CareTask) {
        viewModelScope.launch { 
            try {
                repository.addTask(task)
                showFeedback("Task added")
            } catch (e: Exception) {}
        }
    }

    fun updateTask(task: CareTask) {
        viewModelScope.launch { try { repository.updateTask(task) } catch (e: Exception) {} }
    }

    fun clearSuccessMessage() { _successMessage.value = null }
    fun clearErrorMessage() { _errorMessage.value = null }
}
