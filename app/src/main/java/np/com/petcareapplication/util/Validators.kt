package np.com.petcareapplication.util

import java.util.Locale

/**
 * Pure Kotlin validation rules shared by the Register, Profile, Login and Add Expense screens.
 *
 * Keeping these rules out of the Composables means they can be unit tested on the JVM
 * without an emulator. android.util.Patterns is deliberately NOT used here because it is
 * not available in local unit tests, so a standard email regex is used instead.
 *
 * Every function returns null when the input is valid, or a user-facing error message.
 */
object Validators {

    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> "Email is required"
            !EMAIL_REGEX.matches(trimmed) -> "Invalid email format"
            else -> null
        }
    }

    fun validateFullName(name: String): String? {
        val trimmed = name.trim()
        val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            trimmed.isEmpty() -> "Full name is required"
            parts.size < 2 -> "Please include both first and last name"
            !trimmed.all { it.isLetter() || it.isWhitespace() } -> "Name should only contain letters"
            else -> null
        }
    }

    fun validatePhone(phone: String): String? = when {
        phone.isEmpty() -> "Phone number is required"
        !phone.all { it.isDigit() } -> "Phone number must contain digits only"
        phone.length != 10 -> "Phone number must be exactly 10 digits"
        else -> null
    }

    private fun Char.isSpecial() = !isLetterOrDigit() && !isWhitespace()

    /**
     * Checks the password strength rules and, if any are broken, returns ONE suggestion
     * message listing everything that is missing, e.g.
     * "Password must contain a capital letter and a number".
     * Returns null when the password is strong enough.
     */
    fun validatePassword(password: String): String? {
        if (password.isEmpty()) return "Password is required"
        val missing = mutableListOf<String>()
        if (password.length < 8) missing.add("at least 8 characters")
        if (password.none { it.isUpperCase() }) missing.add("a capital letter")
        if (password.none { it.isDigit() }) missing.add("a number")
        if (password.none { it.isSpecial() }) missing.add("a special character (e.g. ! @ # \$)")
        if (missing.isEmpty()) return null
        val list = if (missing.size == 1) missing[0]
        else missing.dropLast(1).joinToString(", ") + " and " + missing.last()
        return "Password must contain $list"
    }

    fun validateConfirmPassword(password: String, confirm: String): String? =
        if (password != confirm) "Passwords do not match" else null

    fun validateAmount(amount: String): String? {
        val value = amount.trim().toDoubleOrNull()
        return when {
            amount.isBlank() -> "Amount is required"
            value == null || value <= 0.0 -> "Enter an amount greater than 0"
            else -> null
        }
    }

    fun validateRequired(value: String, fieldName: String): String? =
        if (value.isBlank()) "$fieldName is required" else null
}

/**
 * Helpers for converting between the Material 3 TimePicker (24-hour values)
 * and the 12-hour text stored in a CareTask's schedule, e.g. "07:05 AM".
 */
object TimeUtils {

    fun formatTime(hour: Int, minute: Int): String {
        require(hour in 0..23) { "hour must be 0-23" }
        require(minute in 0..59) { "minute must be 0-59" }
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%02d:%02d %s", displayHour, minute, period)
    }

    /**
     * Parses text such as "07:05 AM" back into (hour, minute) in 24-hour form so the
     * Edit Task screen can open the TimePicker at the task's saved time.
     * Returns null if the text is not in the expected format (e.g. older free-text tasks).
     */
    fun parseTime(text: String): Pair<Int, Int>? {
        val match = Regex("^(\\d{1,2}):(\\d{2})\\s*([AaPp][Mm])$").find(text.trim()) ?: return null
        val (h, m, p) = match.destructured
        val hour12 = h.toInt()
        val minute = m.toInt()
        if (hour12 !in 1..12 || minute !in 0..59) return null
        val isPm = p.equals("PM", ignoreCase = true)
        val hour24 = when {
            hour12 == 12 && !isPm -> 0
            hour12 == 12 && isPm -> 12
            isPm -> hour12 + 12
            else -> hour12
        }
        return hour24 to minute
    }
}