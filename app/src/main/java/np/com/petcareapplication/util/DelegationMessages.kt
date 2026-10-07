package np.com.petcareapplication.util

// Builds the text message for handing one task over to someone else, like a pet sitter or a family member.
// It only uses plain Kotlin and Strings, with nothing from Android or Firebase,
// so it can be checked with normal unit tests (see DelegationMessagesTest)
object DelegationMessages {

    // Puts the task details first, then the pet's allergies and diet so the sitter knows what to watch out for
    fun forTask(
        petName: String,
        petAllergies: String,
        petDiet: String,
        taskTitle: String,
        taskSchedule: String,
        taskSupplies: String,
        taskNotes: String
    ): String = buildString {
        append("Hi! Could you please take care of this for $petName?\n\n")
        append("Task: $taskTitle\n")
        // If the task has no time set, say "Any time today" instead of leaving it blank
        append("When: ${taskSchedule.ifBlank { "Any time today" }}\n")
        // Supplies and instructions are only added if the task has them
        if (taskSupplies.isNotBlank()) append("Supplies: $taskSupplies\n")
        if (taskNotes.isNotBlank()) append("Instructions: $taskNotes\n")
        append("\nAbout $petName\n")
        // Empty allergies or diet show a sensible default rather than nothing
        append("Allergies: ${petAllergies.ifBlank { "None" }}\n")
        append("Diet: ${petDiet.ifBlank { "Standard" }}\n")
        append("\nSent from Happy Pets")
    }
}