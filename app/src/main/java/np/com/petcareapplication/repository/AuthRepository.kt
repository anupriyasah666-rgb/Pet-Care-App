package np.com.petcareapplication.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import np.com.petcareapplication.model.User
import java.util.UUID

/**
 * Everything to do with user accounts goes through here: logging in and out,
 * signing up, resetting passwords and saving profile details.
 */
class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val usersCollection = firestore.collection("users")

    // Whoever is logged in right now, or null if nobody is
    val currentUser: FirebaseUser? get() = auth.currentUser

    // Logs in with email and password. Firebase throws an error if the details are wrong.
    suspend fun login(email: String, pass: String): FirebaseUser? {
        val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
        return result.user
    }

    // Creates the account first, then saves the name and phone number in the "users"
    // collection. The document is named after the user's ID so it's easy to find later.
    suspend fun register(name: String, email: String, pass: String, phoneNumber: String): FirebaseUser? {
        val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
        val firebaseUser = result.user ?: throw Exception("Registration failed")
        val userModel = User(uid = firebaseUser.uid, email = email.trim(), name = name, phoneNumber = phoneNumber)
        usersCollection.document(firebaseUser.uid).set(userModel).await()
        return firebaseUser
    }

    // Uploads a profile picture and gives back its link. Not used at the moment,
    // since Firebase Storage needs the paid Blaze plan.
    suspend fun uploadProfileImage(imageUri: Uri): String {
        val ref = storage.reference.child("profiles/${UUID.randomUUID()}")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    // Saves the new name and phone number for the logged-in user
    suspend fun updateUserProfile(name: String, phoneNumber: String, profileImageUrl: String? = null) {
        val user = auth.currentUser ?: throw Exception("No user logged in")
        val uid = user.uid
        val updates = mutableMapOf<String, Any>(
            "name" to name,
            "phoneNumber" to phoneNumber
        )
        profileImageUrl?.let { updates["profileImageUrl"] = it }

        // merge() only changes these fields and leaves the rest of the document alone
        usersCollection.document(uid).set(updates, SetOptions.merge()).await()

        // Update the display name in Firebase Auth too. No need to wait for this one,
        // because the app reads the name from Firestore.
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .apply { profileImageUrl?.let { setPhotoUri(Uri.parse(it)) } }
            .build()

        user.updateProfile(profileUpdates)
    }

    // Gets the saved name and phone number for this user
    suspend fun getUserData(uid: String): User? {
        val snapshot = usersCollection.document(uid).get().await()
        return snapshot.toObject(User::class.java)
    }

    // Firebase emails the user a link to set a new password
    suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun logout() {
        auth.signOut()
    }
}