package np.com.petcareapplication.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data model for a single care task.
 */
data class CareTask(
    @DocumentId val id: String = "",
    val petId: String = "",
    val ownerId: String = "",
    val title: String = "",
    val category: String = "",
    val schedule: String = "",
    val notes: String = "",
    val supplies: String = "",
    @get:PropertyName("completed")
    @set:PropertyName("completed")
    var isCompleted: Boolean = false,
    val dueDate: Long = 0,
    val type: String = "DAILY",
    val imageUrl: String = ""
)
