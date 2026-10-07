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

// Handles login, sign-up, password reset, the user's profile and logout.
// The screens call these functions, and this class talks to Firebase through AuthRepository
class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    // The Firebase user who is logged in, or null if nobody is.
    // Screens watch this to decide whether to go to Home or stay on Login
    private val _user = MutableStateFlow<FirebaseUser?>(repository.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    // Extra profile details (name, phone) saved in Firestore, since Firebase Auth only holds the email
    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData

    // Error and loading state for the Profile screen
    private val _profileError = MutableStateFlow<String?>(null)
    val profileError: StateFlow<String?> = _profileError

    private val _isProfileLoading = MutableStateFlow(false)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading

    // One success message shared by login, register and profile update.
    // Screens show a green box whenever it isn't null
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    // Each screen has its own loading flag and error, so a problem on one screen doesn't show up on another
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

    // Called by the Login button
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isLoginLoading.value = true
            _loginError.value = null
            _successMessage.value = null
            try {
                // Give up after 15 seconds so a bad connection doesn't leave the spinner going forever
                val loggedInUser = withTimeout(15000) {
                    repository.login(email, pass)
                }

                // Stop the spinner as soon as there's a result, so the button
                // doesn't keep looking busy during the 2 second wait below
                _isLoginLoading.value = false
                _successMessage.value = "Login successful!"

                // Leave the green message on screen for 2 seconds before moving on
                delay(2000)

                // Setting _user is what makes the screen go to Home, so it's done after the wait.
                // That way the message stays visible for the full 2 seconds
                _user.value = loggedInUser
                _successMessage.value = null
            } catch (e: Exception) {
                _isLoginLoading.value = false
                // Firebase's own error text. LoginScreen swaps the long ones for simpler wording
                _loginError.value = e.message
                Log.e("AuthViewModel", "Login error", e)
            } finally {
                // Extra safety net so the spinner can never get stuck, even after an error we didn't expect
                _isLoginLoading.value = false
            }
        }
    }

    // Called by the Sign Up button
    fun register(name: String, email: String, pass: String, phoneNumber: String) {
        // Ignore extra taps while a sign-up is already running. Before this, tapping Sign Up twice quickly
        // started two sign-ups at once and the spinner looked like it never stopped
        if (_isRegisterLoading.value) return

        viewModelScope.launch {
            _isRegisterLoading.value = true
            _registerError.value = null
            _successMessage.value = null
            try {
                val newUser = withTimeout(15000) {
                    repository.register(name, email, pass, phoneNumber)
                }

                // The account and the Firestore profile are both saved now,
                // so stop the spinner and show the success message
                _isRegisterLoading.value = false
                _successMessage.value = "Registration successful!"

                // Leave the success message on screen for 2 seconds before leaving the page
                delay(2000)

                // RegisterScreen's LaunchedEffect(user) is waiting for this. Setting it after the wait
                // is what sends the user on to the Login screen
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

    // Called from the Reset Password dialog on the Login screen
    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _resetError.value = "Please enter your email address"
            return
        }
        viewModelScope.launch {
            _isResetLoading.value = true
            _resetError.value = null
            try {
                // Firebase sends the reset link to this email. If the email isn't registered, Firebase
                // may still report success (it does this on purpose, for privacy), so the user won't always
                // see an error for an unknown email
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

    // Gets the user's name and phone from Firestore for the Profile screen
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

    // Saves a new name and phone number from the Profile screen
    fun updateProfile(name: String, phoneNumber: String, imageUri: Uri?) {
        // Ignore extra taps while an update is already running
        if (_isProfileLoading.value) return

        viewModelScope.launch {
            _isProfileLoading.value = true
            _profileError.value = null
            _successMessage.value = null

            try {
                withTimeout(10000) {
                    var finalImageUrl: String? = null
                    // Only upload if it's a new picture from the phone (not a web link).
                    // The Profile screen always passes null at the moment, because Firebase Storage isn't used
                    if (imageUri != null && !imageUri.toString().startsWith("http")) {
                        finalImageUrl = repository.uploadProfileImage(imageUri)
                    }
                    repository.updateUserProfile(name, phoneNumber, finalImageUrl)
                }

                _isProfileLoading.value = false
                _successMessage.value = "Profile information updated successfully"

                // Load the details again so the Profile screen shows the new name and phone straight away
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

    // Signs out and forgets the user, so Login shows next time
    fun logout() {
        repository.logout()
        _user.value = null
        _userData.value = null
    }

    // Let screens clear messages, e.g. when the user starts typing again
    fun clearSuccessMessage() { _successMessage.value = null }
    fun clearErrors() {
        _profileError.value = null
        _loginError.value = null
        _registerError.value = null
        _resetError.value = null
    }
}