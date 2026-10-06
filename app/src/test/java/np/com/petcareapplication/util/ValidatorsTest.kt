package np.com.petcareapplication.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Local JVM unit tests for the validation rules used by the
 * Login, Register, Profile and Add Expense screens.
 * Run in Android Studio: right-click this file > Run 'ValidatorsTest'.
 */
class ValidatorsTest {

    // ---------- Email ----------
    @Test fun email_valid_returnsNull() =
        assertNull(Validators.validateEmail("owner@example.com"))

    @Test fun email_withSurroundingSpaces_isTrimmedAndValid() =
        assertNull(Validators.validateEmail("  owner@example.com  "))

    @Test fun email_empty_returnsRequired() =
        assertEquals("Email is required", Validators.validateEmail(""))

    @Test fun email_missingAtSymbol_returnsInvalid() =
        assertEquals("Invalid email format", Validators.validateEmail("owner.example.com"))

    @Test fun email_missingDomainSuffix_returnsInvalid() =
        assertEquals("Invalid email format", Validators.validateEmail("owner@example"))

    // ---------- Full name ----------
    @Test fun name_firstAndLast_returnsNull() =
        assertNull(Validators.validateFullName("Sita Sharma"))

    @Test fun name_singleWord_returnsError() =
        assertEquals("Please include both first and last name", Validators.validateFullName("Sita"))

    @Test fun name_containsDigits_returnsError() =
        assertEquals("Name should only contain letters", Validators.validateFullName("Sita Sharma2"))

    @Test fun name_blank_returnsRequired() =
        assertEquals("Full name is required", Validators.validateFullName("   "))

    // ---------- Phone ----------
    @Test fun phone_tenDigits_returnsNull() =
        assertNull(Validators.validatePhone("9812345678"))

    @Test fun phone_nineDigits_returnsError() =
        assertEquals("Phone number must be exactly 10 digits", Validators.validatePhone("981234567"))

    @Test fun phone_containsLetters_returnsError() =
        assertEquals("Phone number must contain digits only", Validators.validatePhone("98123abc78"))

    // ---------- Password ----------
    @Test fun password_strong_returnsNull() =
        assertNull(Validators.validatePassword("Abcd123!"))

    @Test fun password_empty_returnsRequired() =
        assertEquals("Password is required", Validators.validatePassword(""))

    @Test fun password_tooShort_suggestsLength() =
        assertEquals("Password must contain at least 8 characters", Validators.validatePassword("Ab1!"))

    @Test fun password_noCapital_suggestsCapital() =
        assertEquals("Password must contain a capital letter", Validators.validatePassword("abcd123!"))

    @Test fun password_noDigit_suggestsNumber() =
        assertEquals("Password must contain a number", Validators.validatePassword("Abcdefg!"))

    @Test fun password_noSpecialCharacter_suggestsSpecial() =
        assertEquals("Password must contain a special character (e.g. ! @ # \$)", Validators.validatePassword("Abcd1234"))

    @Test fun password_severalMissing_listsAllInOneMessage() =
        assertEquals(
            "Password must contain a capital letter, a number and a special character (e.g. ! @ # \$)",
            Validators.validatePassword("abcdefgh")
        )

    @Test fun confirmPassword_mismatch_returnsError() =
        assertEquals("Passwords do not match", Validators.validateConfirmPassword("abcd1234", "abcd1235"))

    @Test fun confirmPassword_match_returnsNull() =
        assertNull(Validators.validateConfirmPassword("abcd1234", "abcd1234"))

    // ---------- Expense amount ----------
    @Test fun amount_positiveDecimal_returnsNull() =
        assertNull(Validators.validateAmount("12.50"))

    @Test fun amount_zero_returnsError() =
        assertNotNull(Validators.validateAmount("0"))

    @Test fun amount_negative_returnsError() =
        assertNotNull(Validators.validateAmount("-5"))

    @Test fun amount_text_returnsError() =
        assertEquals("Enter an amount greater than 0", Validators.validateAmount("ten"))

    @Test fun amount_blank_returnsRequired() =
        assertEquals("Amount is required", Validators.validateAmount(""))
}