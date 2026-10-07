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

// Holds everything to do with pets and their care tasks for the screens to show.
// The screens call these functions, and this class asks PetRepository to read or save the data in Firestore
class PetViewModel(private val repository: PetRepository = PetRepository()) : ViewModel() {

    // The user's pets
    private val _pets = MutableStateFlow<List<Pet>>(emptyList())
    val pets: StateFlow<List<Pet>> = _pets

    // Tasks for the pet currently open on Pet Detail
    private val _tasks = MutableStateFlow<List<CareTask>>(emptyList())
    val tasks: StateFlow<List<CareTask>> = _tasks

    // Tasks for all the user's pets, for Today's Routine on the Home screen
    private val _allTasks = MutableStateFlow<List<CareTask>>(emptyList())
    val allTasks: StateFlow<List<CareTask>> = _allTasks

    // True while saving, so buttons can show a spinner
    private val _isImageUploading = MutableStateFlow(false)
    val isImageUploading: StateFlow<Boolean> = _isImageUploading

    // Short messages shown at the bottom of the screen
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Each live Firestore listener is kept as a Job, so the old one can be stopped before a new one starts
    // and when the user logs out. Before this, every visit to a screen added another listener,
    // and the old ones failed after logout, which left "Error loading pets." stuck on the screen
    private var petsJob: Job? = null
    private var tasksJob: Job? = null
    private var allTasksJob: Job? = null

    // Start listening to this user's pets. Changes in Firestore show up straight away
    fun loadPets(ownerId: String) {
        petsJob?.cancel()
        petsJob = viewModelScope.launch {
            repository.getPets(ownerId)
                // Loading problems are written to Logcat instead of showing a banner on every screen
                .catch { e -> Log.e("PetViewModel", "Error loading pets", e) }
                .collectLatest { _pets.value = it }
        }
    }

    // Listen to the tasks for one pet
    fun loadTasks(petId: String) {
        tasksJob?.cancel()
        tasksJob = viewModelScope.launch {
            repository.getTasks(petId)
                .catch { e -> Log.e("PetViewModel", "Error loading tasks", e) }
                .collectLatest { _tasks.value = it }
        }
    }

    // Listen to every task the user owns, across all their pets
    fun loadAllTasks(ownerId: String) {
        allTasksJob?.cancel()
        allTasksJob = viewModelScope.launch {
            repository.getAllTasks(ownerId)
                .catch { e -> Log.e("PetViewModel", "Error loading consolidated tasks", e) }
                .collectLatest { _allTasks.value = it }
        }
    }

    // Called on logout. Stops all the live listeners and empties the lists,
    // so the next person who logs in never sees the previous user's pets or tasks
    fun clearSession() {
        petsJob?.cancel(); tasksJob?.cancel(); allTasksJob?.cancel()
        petsJob = null; tasksJob = null; allTasksJob = null
        _pets.value = emptyList()
        _tasks.value = emptyList()
        _allTasks.value = emptyList()
        _successMessage.value = null
        _errorMessage.value = null
    }

    // Shows an error message for 3 seconds, then hides it so it doesn't get stuck on screen
    private fun showError(message: String) {
        viewModelScope.launch {
            _errorMessage.value = message
            delay(3000)
            _errorMessage.value = null
        }
    }

    // Shows a success message for 2 seconds, then hides it
    fun showFeedback(message: String) {
        viewModelScope.launch {
            _successMessage.value = message
            delay(2000)
            _successMessage.value = null
        }
    }

    // Shake to reset on the Pet Detail screen. Unticks this pet's finished tasks only
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

    // Shake to reset on the Home screen. Unticks every finished task for ALL of the user's pets,
    // using the live list of all tasks (_allTasks) instead of just one pet's tasks
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

    // "Clear All Tasks" from the Pet Detail menu. Deletes every task for this pet
    fun clearRoutine(petId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRoutine(petId)
                showFeedback("Care routine cleared successfully")
            } catch (e: Exception) { showError("Failed to clear routine.") }
        }
    }

    // Used when a health record is also added to the checklist
    fun addTask(task: CareTask, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.addTask(task)
                showFeedback("Task added")
                // Short pause before onComplete runs, so the message can be seen first
                delay(1000)
                onComplete()
            } catch (e: Exception) {
                showError("Failed to add task.")
            }
        }
    }

    // Used by the Add Task screen. Saves the task with its photo link if one was given
    fun addTaskWithImage(task: CareTask, imageUri: Uri?, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isImageUploading.value = true
            try {
                var url = task.imageUrl
                // A file picked from the phone (content:// or file://) has to be uploaded to Firebase Storage,
                // which needs the paid plan, so this part fails. A web link (http) is saved as it is
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

    // Saves changes to a task without any photo handling. No screen uses this at the moment,
    // because Edit Task uses updateTaskWithImage instead
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

    // Used by the Edit Task screen. Works the same way as addTaskWithImage
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

    // Used by the Add Pet screen. Same photo handling as the task functions above
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

    // Used by the Edit Pet screen
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

    // Goes back and shows "Pet deleted" straight away, then deletes the pet and its tasks,
    // expenses and health records in the background so the user isn't left waiting
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

    // Swipe left (after confirming) on a task
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
                showFeedback("Task is deleted")
            } catch (e: Exception) { showError("Delete failed.") }
        }
    }

    // Tick or untick a task. Used by the tick circle and by swiping right
    fun toggleTaskCompletion(task: CareTask) {
        viewModelScope.launch {
            try {
                val newStatus = !task.isCompleted
                repository.updateTask(task.copy(isCompleted = newStatus))
                showFeedback(if (newStatus) "Marked as completed" else "Marked as undone")
            } catch (e: Exception) { showError("Update failed.") }
        }
    }

    // Let screens hide a message early
    fun clearSuccessMessage() { _successMessage.value = null }
    fun clearErrorMessage() { _errorMessage.value = null }
}