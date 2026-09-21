package np.com.petcareapplication.model

/**
 * Data model for a PetCare app user.
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String = ""
)
