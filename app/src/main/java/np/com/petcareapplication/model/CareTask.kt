package np.com.petcareapplication.model

import com.google.firebase.firestore.DocumentId

/**
 * Data model for a single care task.
 */
data class CareTask(
    @DocumentId val id: String = "",
    val petId: String = "",
    // NEW: ownerId allows the app to "consolidate" tasks for all pets on the Home screen
    val ownerId: String = "",
    val title: String = "",
    val category: String = "",
    val schedule: String = "",
    val notes: String = "",
    val supplies: String = "",
    val imageUrl: String = "",
    val isCompleted: Boolean = false,
    val dueDate: Long = 0,
    val type: String = "DAILY"
)
