package np.com.petcareapplication.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import np.com.petcareapplication.model.User
import np.com.petcareapplication.repository.AuthRepository

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    // Holds the currently signed-in Firebase user. Null means "no one is logged in".
    // Screens observe this to decide whether to navigate to Home or stay on Login.
    private val _user = MutableStateFlow<FirebaseUser?>(repository.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    // Extra profile info (name, phone, etc.) pulled from Firestore, separate from FirebaseAuth.
    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData

    private val _profileError = MutableStateFlow<String?>(null)
    val profileError: StateFlow<String?> = _profileError

    private val _isProfileLoading = MutableStateFlow(false)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading

    // Shared success banner used across login/register/profile update.
    // Screens just watch this and show a green message whenever it's non-null.
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    private val _isLoginLoading = MutableStateFlow(false)
    val isLoginLoading: StateFlow<Boolean> = _isLoginLoading

    private val _isRegisterLoading = MutableStateFlow(false)
    val isRegisterLoading: StateFlow<Boolean> = _isRegisterLoading

    private val _isResetLoading = MutableStateFlow(false)
    val isResetLoading: StateFlow<Boolean> = _isResetLoading

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    private val _registerError = MutableStateFlow<String?>(null)
    val registerError: StateFlow<String?> = _registerError

    private val _resetError = MutableStateFlow<String?>(null)
    val resetError: StateFlow<String?> = _resetError

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isLoginLoading.value = true
            _loginError.value = null
            _successMessage.value = null
            try {
                val loggedInUser = withTimeout(15000) {
                    repository.login(email, pass)
                }

                // Stop the spinner as soon as we have a result — don't make the user
                // wait through the success delay while the button still looks "busy".
                _isLoginLoading.value = false
                _successMessage.value = "Login successful!"

                // Let the green message sit on screen for a couple seconds before we move on.
                delay(2000)

                // Only now do we flip _user, since that's what triggers navigation in the UI.
                // Doing this after the delay is what keeps the message visible for the full 2s.
                _user.value = loggedInUser
                _successMessage.value = null
            } catch (e: Exception) {
                _isLoginLoading.value = false
                _loginError.value = e.message
                Log.e("AuthViewModel", "Login error", e)
            } finally {
                // Belt-and-suspenders: guarantees the spinner never gets stuck even if
                // something above throws in a way we didn't anticipate.
                _isLoginLoading.value = false
            }
        }
    }

    fun register(name: String, email: String, pass: String, phoneNumber: String) {
        // Guard against double taps / slow network double-submits. Without this, tapping
        // Sign Up twice quickly can start two overlapping coroutines that stomp on each
        // other's loading state, which is what made the spinner look like it never stops.
        if (_isRegisterLoading.value) return

        viewModelScope.launch {
            _isRegisterLoading.value = true
            _registerError.value = null
            _successMessage.value = null
            try {
                val newUser = withTimeout(15000) {
                    repository.register(name, email, pass, phoneNumber)
                }

                // Firestore write is done at this point — safe to stop the spinner
                // and show the success message right away.
                _isRegisterLoading.value = false
                _successMessage.value = "Registration successful!"

                // Keep the success message on screen for 2 seconds before navigating away.
                delay(2000)

                // This is what RegisterScreen's LaunchedEffect(user) is watching for —
                // setting it now (after the delay) is what triggers the redirect to Login.
                _user.value = newUser
                _successMessage.value = null
            } catch (e: Exception) {
                _isRegisterLoading.value = false
                _registerError.value = e.message
                Log.e("AuthViewModel", "Registration error", e)
            } finally {
                _isRegisterLoading.value = false
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _resetError.value = "Please enter your email address"
            return
        }
        viewModelScope.launch {
            _isResetLoading.value = true
            _resetError.value = null
            try {
                // The repository now checks Firestore for a matching email first, and
                // throws if nothing is found — Firebase Auth itself always "succeeds"
                // here for privacy reasons, so we can't rely on it alone.
                repository.sendPasswordResetEmail(email)
                _successMessage.value = "Password reset link sent to $email"
            } catch (e: Exception) {
                _resetError.value = e.message
                Log.e("AuthViewModel", "Reset password error", e)
            } finally {
                _isResetLoading.value = false
            }
        }
    }

    fun loadUserData() {
        val currentUser = repository.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    _userData.value = repository.getUserData(currentUser.uid)
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Load user data error", e)
                }
            }
        }
    }

    fun updateProfile(name: String, phoneNumber: String, imageUri: Uri?) {
        if (_isProfileLoading.value) return

        viewModelScope.launch {
            _isProfileLoading.value = true
            _profileError.value = null
            _successMessage.value = null

            try {
                withTimeout(10000) {
                    var finalImageUrl: String? = null
                    // Only re-upload if this is a brand-new local image (not already a URL
                    // we previously fetched from Firebase Storage).
                    if (imageUri != null && !imageUri.toString().startsWith("http")) {
                        finalImageUrl = repository.uploadProfileImage(imageUri)
                    }
                    repository.updateUserProfile(name, phoneNumber, finalImageUrl)
                }

                _isProfileLoading.value = false
                _successMessage.value = "Profile information updated successfully"

                // Refresh local state so the Profile screen reflects the new data immediately.
                loadUserData()

                delay(2000)
                _successMessage.value = null

            } catch (e: Exception) {
                _isProfileLoading.value = false
                _profileError.value = "Update failed. Please check permissions."
                Log.e("AuthViewModel", "Update profile error", e)
            } finally {
                _isProfileLoading.value = false
            }
        }
    }

    fun logout() {
        repository.logout()
        _user.value = null
        _userData.value = null
    }

    fun clearSuccessMessage() { _successMessage.value = null }
    fun clearErrors() {
        _profileError.value = null
        _loginError.value = null
        _registerError.value = null
        _resetError.value = null
    }
}
