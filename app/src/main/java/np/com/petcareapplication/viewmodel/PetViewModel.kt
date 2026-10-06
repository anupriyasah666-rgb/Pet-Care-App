package np.com.petcareapplication.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
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

    // Each live Firestore listener is kept as a Job so it can be cancelled before a new one
    // starts (and when the user logs out). Without this, every screen visit added another
    // listener, and old listeners failed after logout, showing "Error loading pets."
    private var petsJob: Job? = null
    private var tasksJob: Job? = null
    private var allTasksJob: Job? = null

    fun loadPets(ownerId: String) {
        petsJob?.cancel()
        petsJob = viewModelScope.launch {
            repository.getPets(ownerId)
                // Background loading problems are logged, not shown as a banner on every screen
                .catch { e -> Log.e("PetViewModel", "Error loading pets", e) }
                .collectLatest { _pets.value = it }
        }
    }

    fun loadTasks(petId: String) {
        tasksJob?.cancel()
        tasksJob = viewModelScope.launch {
            repository.getTasks(petId)
                .catch { e -> Log.e("PetViewModel", "Error loading tasks", e) }
                .collectLatest { _tasks.value = it }
        }
    }

    fun loadAllTasks(ownerId: String) {
        allTasksJob?.cancel()
        allTasksJob = viewModelScope.launch {
            repository.getAllTasks(ownerId)
                .catch { e -> Log.e("PetViewModel", "Error loading consolidated tasks", e) }
                .collectLatest { _allTasks.value = it }
        }
    }

    /**
     * Called on logout: stops all live listeners and clears the previous user's data,
     * so nothing from their account is shown to the next person who signs in.
     */
    fun clearSession() {
        petsJob?.cancel(); tasksJob?.cancel(); allTasksJob?.cancel()
        petsJob = null; tasksJob = null; allTasksJob = null
        _pets.value = emptyList()
        _tasks.value = emptyList()
        _allTasks.value = emptyList()
        _successMessage.value = null
        _errorMessage.value = null
    }

    /** Shows an error banner for 3 seconds, then hides it so it never gets "stuck". */
    private fun showError(message: String) {
        viewModelScope.launch {
            _errorMessage.value = message
            delay(3000)
            _errorMessage.value = null
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
            } catch (e: Exception) { showError("Reset failed.") }
        }
    }

    /**
     * DESIRABLE FEATURE (gesture): called when the phone is shaken on the Home dashboard.
     * Un-ticks every completed task across ALL of the owner's pets, using the live
     * consolidated list (_allTasks) rather than a single pet's tasks.
     */
    fun resetAllTasks() {
        viewModelScope.launch {
            try {
                _allTasks.value.filter { it.isCompleted }.forEach { task ->
                    repository.updateTask(task.copy(isCompleted = false))
                }
                showFeedback("Today's checklist reset!")
            } catch (e: Exception) { showError("Reset failed.") }
        }
    }

    fun clearRoutine(petId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRoutine(petId)
                showFeedback("Care routine cleared successfully")
            } catch (e: Exception) { showError("Failed to clear routine.") }
        }
    }

    fun addTask(task: CareTask, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.addTask(task)
                showFeedback("Task added")
                delay(1000)
                onComplete()
            } catch (e: Exception) {
                showError("Failed to add task.")
            }
        }
    }

    fun addTaskWithImage(task: CareTask, imageUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isImageUploading.value = true
            try {
                var url = task.imageUrl
                if (imageUri != null) {
                    val uriString = imageUri.toString()
                    if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                        url = repository.uploadPetImage(imageUri)
                    } else if (uriString.startsWith("http")) {
                        url = uriString
                    }
                }
                repository.addTask(task.copy(imageUrl = url))
                _isImageUploading.value = false
                onComplete()
                showFeedback("Task added")
            } catch (e: Exception) {
                _isImageUploading.value = false
                showError("Failed to add task.")
                Log.e("PetViewModel", "Error adding task", e)
            }
        }
    }

    fun updateTask(task: CareTask, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateTask(task)
                showFeedback("Task updated successfully")
                delay(1000)
                onComplete()
            } catch (e: Exception) {
                showError("Failed to update task.")
            }
        }
    }

    fun updateTaskWithImage(task: CareTask, newUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isImageUploading.value = true
            try {
                var finalImageUrl = task.imageUrl
                if (newUri != null) {
                    val uriString = newUri.toString()
                    if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                        finalImageUrl = repository.uploadPetImage(newUri)
                    } else if (uriString.startsWith("http")) {
                        finalImageUrl = uriString
                    }
                }
                repository.updateTask(task.copy(imageUrl = finalImageUrl))
                _isImageUploading.value = false
                onComplete()
                showFeedback("Task updated successfully")
            } catch (e: Exception) {
                _isImageUploading.value = false
                showError("Failed to update task.")
            }
        }
    }

    fun addPetWithImage(pet: Pet, imageUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isImageUploading.value = true
            try {
                var url = pet.imageUrl
                if (imageUri != null) {
                    val uriString = imageUri.toString()
                    if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                        url = repository.uploadPetImage(imageUri)
                    } else if (uriString.startsWith("http")) {
                        url = uriString
                    }
                }
                repository.addPet(pet.copy(imageUrl = url))
                _isImageUploading.value = false
                onComplete()
                showFeedback("Pet added")
            } catch (e: Exception) {
                _isImageUploading.value = false
                showError("Failed to add pet.")
                Log.e("PetViewModel", "Error adding pet", e)
            }
        }
    }

    fun updatePetWithImage(pet: Pet, newUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isImageUploading.value = true
            try {
                var finalImageUrl = pet.imageUrl
                if (newUri != null) {
                    val uriString = newUri.toString()
                    if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                        finalImageUrl = repository.uploadPetImage(newUri)
                    } else if (uriString.startsWith("http")) {
                        finalImageUrl = uriString
                    }
                }
                repository.updatePet(pet.copy(imageUrl = finalImageUrl))
                _isImageUploading.value = false
                onComplete()
                showFeedback("Profile updated successfully")
            } catch (e: Exception) {
                _isImageUploading.value = false
                showError("Failed to update profile.")
                Log.e("PetViewModel", "Error updating pet image/data", e)
            }
        }
    }

    fun deletePet(petId: String, onComplete: () -> Unit) {
        onComplete()
        _successMessage.value = "Pet deleted"
        viewModelScope.launch {
            try {
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
            } catch (e: Exception) { showError("Delete failed.") }
        }
    }

    fun toggleTaskCompletion(task: CareTask) {
        viewModelScope.launch {
            try {
                val newStatus = !task.isCompleted
                repository.updateTask(task.copy(isCompleted = newStatus))
                showFeedback(if (newStatus) "Marked as completed" else "Marked as undone")
            } catch (e: Exception) { showError("Update failed.") }
        }
    }

    fun clearSuccessMessage() { _successMessage.value = null }
    fun clearErrorMessage() { _errorMessage.value = null }
}