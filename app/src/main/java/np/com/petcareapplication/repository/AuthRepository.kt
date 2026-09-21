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

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val usersCollection = firestore.collection("users")

    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun login(email: String, pass: String): FirebaseUser? {
        val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
        return result.user
    }

    suspend fun register(name: String, email: String, pass: String, phoneNumber: String): FirebaseUser? {
        val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
        val firebaseUser = result.user ?: throw Exception("Registration failed")
        val userModel = User(uid = firebaseUser.uid, email = email.trim(), name = name, phoneNumber = phoneNumber)
        usersCollection.document(firebaseUser.uid).set(userModel).await()
        return firebaseUser
    }

    suspend fun uploadProfileImage(imageUri: Uri): String {
        val ref = storage.reference.child("profiles/${UUID.randomUUID()}")
        ref.putFile(imageUri).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Updates profile. Uses a more reliable method to ensure completion.
     */
    suspend fun updateUserProfile(name: String, phoneNumber: String, profileImageUrl: String? = null) {
        val user = auth.currentUser ?: throw Exception("No user logged in")
        val uid = user.uid
        val updates = mutableMapOf<String, Any>(
            "name" to name,
            "phoneNumber" to phoneNumber
        )
        profileImageUrl?.let { updates["profileImageUrl"] = it }

        // Update Firestore
        usersCollection.document(uid).set(updates, SetOptions.merge()).await()

        // Non-blocking Auth update
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .apply { profileImageUrl?.let { setPhotoUri(Uri.parse(it)) } }
            .build()
        
        user.updateProfile(profileUpdates)
    }

    suspend fun getUserData(uid: String): User? {
        val snapshot = usersCollection.document(uid).get().await()
        return snapshot.toObject(User::class.java)
    }

    suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun logout() {
        auth.signOut()
    }
}
