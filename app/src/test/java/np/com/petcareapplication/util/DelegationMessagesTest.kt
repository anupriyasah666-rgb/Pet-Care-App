package np.com.petcareapplication.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

// Checks the text message that gets sent when one task is handed over to a sitter.
// These are plain JUnit tests, so they run on the computer without an emulator
class DelegationMessagesTest {

    // A message with every field filled in, shared by the first few tests
    private fun fullMessage() = DelegationMessages.forTask(
        petName = "Luna", petAllergies = "Chicken", petDiet = "Grain-free",
        taskTitle = "Morning walk", taskSchedule = "07:00 AM",
        taskSupplies = "Lead", taskNotes = "Avoid the park"
    )

    // The sitter needs to know what to do and when
    @Test fun message_includesTaskTitleAndTime() {
        val msg = fullMessage()
        assertTrue(msg.contains("Task: Morning walk"))
        assertTrue(msg.contains("When: 07:00 AM"))
    }

    @Test fun message_includesSuppliesAndInstructions() {
        val msg = fullMessage()
        assertTrue(msg.contains("Supplies: Lead"))
        assertTrue(msg.contains("Instructions: Avoid the park"))
    }

    // Allergies and diet should always be in the message, so the pet stays safe
    @Test fun message_includesPetSafetyInfo() {
        val msg = fullMessage()
        assertTrue(msg.contains("Allergies: Chicken"))
        assertTrue(msg.contains("Diet: Grain-free"))
    }

    // If the task has no supplies or notes, those lines shouldn't appear at all
    @Test fun message_blankSuppliesAndNotes_areLeftOut() {
        val msg = DelegationMessages.forTask("Luna", "", "", "Feed", "08:00 AM", "", "")
        assertFalse(msg.contains("Supplies:"))
        assertFalse(msg.contains("Instructions:"))
    }

    // Empty allergies or diet should show "None" and "Standard" instead of nothing
    @Test fun message_blankPetInfo_usesSensibleDefaults() {
        val msg = DelegationMessages.forTask("Luna", "", "", "Feed", "08:00 AM", "", "")
        assertTrue(msg.contains("Allergies: None"))
        assertTrue(msg.contains("Diet: Standard"))
    }

    @Test fun message_startsWithGreetingNamingThePet() =
        assertTrue(fullMessage().startsWith("Hi! Could you please take care of this for Luna?"))

    // A task with no time set should still say when, rather than leaving it blank
    @Test fun message_blankSchedule_saysAnyTime() =
        assertTrue(DelegationMessages.forTask("Luna", "", "", "Feed", "", "", "").contains("When: Any time today"))
}