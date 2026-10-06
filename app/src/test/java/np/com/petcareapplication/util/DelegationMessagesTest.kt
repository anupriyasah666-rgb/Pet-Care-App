package np.com.petcareapplication.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Tests the SMS text sent when a single task is delegated.
 */
class DelegationMessagesTest {

    private fun fullMessage() = DelegationMessages.forTask(
        petName = "Luna", petAllergies = "Chicken", petDiet = "Grain-free",
        taskTitle = "Morning walk", taskSchedule = "07:00 AM",
        taskSupplies = "Lead", taskNotes = "Avoid the park"
    )

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

    @Test fun message_includesPetSafetyInfo() {
        val msg = fullMessage()
        assertTrue(msg.contains("Allergies: Chicken"))
        assertTrue(msg.contains("Diet: Grain-free"))
    }

    @Test fun message_blankSuppliesAndNotes_areLeftOut() {
        val msg = DelegationMessages.forTask("Luna", "", "", "Feed", "08:00 AM", "", "")
        assertFalse(msg.contains("Supplies:"))
        assertFalse(msg.contains("Instructions:"))
    }

    @Test fun message_blankPetInfo_usesSensibleDefaults() {
        val msg = DelegationMessages.forTask("Luna", "", "", "Feed", "08:00 AM", "", "")
        assertTrue(msg.contains("Allergies: None"))
        assertTrue(msg.contains("Diet: Standard"))
    }

    @Test fun message_startsWithGreetingNamingThePet() =
        assertTrue(fullMessage().startsWith("Hi! Could you please take care of this for Luna?"))

    @Test fun message_blankSchedule_saysAnyTime() =
        assertTrue(DelegationMessages.forTask("Luna", "", "", "Feed", "", "", "").contains("When: Any time today"))
}