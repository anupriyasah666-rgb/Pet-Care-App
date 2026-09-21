package np.com.petcareapplication.model

import com.google.firebase.firestore.DocumentId

/**
 * Data model for a health or medical event.
 * Fulfills the "Manage healthcare records" core requirement.
 */
data class MedicalRecord(
    @DocumentId val id: String = "",
    // Links the record to a specific pet
    val petId: String = "",
    // Type of record: e.g., "Vaccination", "Heartworm", "Checkup"
    val type: String = "",
    // The date the medical event occurred
    val date: Long = 0,
    // Detailed notes from the vet or regarding the procedure
    val notes: String = ""
)
