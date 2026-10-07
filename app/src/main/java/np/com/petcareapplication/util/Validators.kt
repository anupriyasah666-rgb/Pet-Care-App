package np.com.petcareapplication.util

import java.util.Locale

// The input checks used by the Register, Profile, Login and Add Expense screens.
// They're kept out of the screens so they can be tested with normal unit tests, without an emulator.
// android.util.Patterns isn't used for the email check because it doesn't work in local unit tests,
// so a regular expression is used instead.
// Each check returns null if the input is fine, or the error message to show the user
object Validators {

    // Something before the @, a domain, a dot, then at least two letters (e.g. name@mail.com)
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

    // Needs a first and last name, using letters only
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

    // Exactly 10 digits
    fun validatePhone(phone: String): String? = when {
        phone.isEmpty() -> "Phone number is required"
        !phone.all { it.isDigit() } -> "Phone number must contain digits only"
        phone.length != 10 -> "Phone number must be exactly 10 digits"
        else -> null
    }

    // Anything that isn't a letter, number or space counts as a special character
    private fun Char.isSpecial() = !isLetterOrDigit() && !isWhitespace()

    // Checks every password rule and, if any are broken, builds ONE message that lists everything missing,
    // e.g. "Password must contain a capital letter and a number". Returns null if the password is strong enough
    fun validatePassword(password: String): String? {
        if (password.isEmpty()) return "Password is required"
        val missing = mutableListOf<String>()
        if (password.length < 8) missing.add("at least 8 characters")
        if (password.none { it.isUpperCase() }) missing.add("a capital letter")
        if (password.none { it.isDigit() }) missing.add("a number")
        if (password.none { it.isSpecial() }) missing.add("a special character (e.g. ! @ # \$)")
        if (missing.isEmpty()) return null
        // Join the missing items naturally: "a, b and c"
        val list = if (missing.size == 1) missing[0]
        else missing.dropLast(1).joinToString(", ") + " and " + missing.last()
        return "Password must contain $list"
    }

    // Both password boxes on Register must match
    fun validateConfirmPassword(password: String, confirm: String): String? =
        if (password != confirm) "Passwords do not match" else null

    // Must be a real number above zero
    fun validateAmount(amount: String): String? {
        val value = amount.trim().toDoubleOrNull()
        return when {
            amount.isBlank() -> "Amount is required"
            value == null || value <= 0.0 -> "Enter an amount greater than 0"
            else -> null
        }
    }

    // For any box that just can't be empty, e.g. the expense description
    fun validateRequired(value: String, fieldName: String): String? =
        if (value.isBlank()) "$fieldName is required" else null
}

// Converts between the clock picker, which uses 24-hour numbers,
// and the 12-hour text saved in a task's schedule, e.g. "07:05 AM"
object TimeUtils {

    // e.g. (7, 5) becomes "07:05 AM" and (19, 30) becomes "07:30 PM"
    fun formatTime(hour: Int, minute: Int): String {
        // Stop straight away if the numbers aren't a real time
        require(hour in 0..23) { "hour must be 0-23" }
        require(minute in 0..59) { "minute must be 0-59" }
        val period = if (hour < 12) "AM" else "PM"
        // Midnight shows as 12 AM, and afternoon hours have 12 taken off
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%02d:%02d %s", displayHour, minute, period)
    }

    // Turns text like "07:05 AM" back into a 24-hour hour and minute, so the clock on Edit Task
    // opens at the saved time. Returns null if the text isn't in that format (e.g. older tasks typed by hand)
    fun parseTime(text: String): Pair<Int, Int>? {
        // Matches things like "7:05 AM", "07:05PM" or "12:30 pm"
        val match = Regex("^(\\d{1,2}):(\\d{2})\\s*([AaPp][Mm])$").find(text.trim()) ?: return null
        val (h, m, p) = match.destructured
        val hour12 = h.toInt()
        val minute = m.toInt()
        if (hour12 !in 1..12 || minute !in 0..59) return null
        val isPm = p.equals("PM", ignoreCase = true)
        // 12 AM is 0 in 24-hour time, 12 PM stays 12, and other PM hours get 12 added
        val hour24 = when {
            hour12 == 12 && !isPm -> 0
            hour12 == 12 && isPm -> 12
            isPm -> hour12 + 12
            else -> hour12
        }
        return hour24 to minute
    }
}