package np.com.petcareapplication.model

import com.google.firebase.firestore.DocumentId

/**
 * Data class representing a Pet in our application.
 */
data class Pet(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val breed: String = "",
    val age: Int = 0,
    val weight: Double = 0.0,
    val dietaryPreferences: String = "",
    val vaccinationHistory: String = "",
    val allergies: String = "",
    val favoriteToys: String = "",
    // NEW: General notes field as per project requirements
    val notes: String = "",
    val imageUrl: String = ""
)
