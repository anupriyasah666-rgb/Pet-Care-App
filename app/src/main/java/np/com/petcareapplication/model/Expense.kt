package np.com.petcareapplication.model

import com.google.firebase.firestore.DocumentId

/**
 * Represents a single financial record related to a pet's care.
 * This can be used to track costs for food, medication, or medical visits.
 */
data class Expense(
    @DocumentId val id: String = "",
    // Links the expense to a specific pet profile
    val petId: String = "",
    // Category like "Food", "Medical", or "Grooming"
    val category: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    // Timestamp of when the transaction occurred
    val date: Long = System.currentTimeMillis()
)
