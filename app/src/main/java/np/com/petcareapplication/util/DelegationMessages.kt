package np.com.petcareapplication.util

/**
 * Builds the SMS text used to delegate a single care task to someone else
 * (e.g. a pet sitter or family member).
 *
 * Kept as plain Kotlin with simple String parameters so it can be unit tested
 * without Android or Firebase.
 */
object DelegationMessages {

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
        append("When: ${taskSchedule.ifBlank { "Any time today" }}\n")
        if (taskSupplies.isNotBlank()) append("Supplies: $taskSupplies\n")
        if (taskNotes.isNotBlank()) append("Instructions: $taskNotes\n")
        append("\nAbout $petName\n")
        append("Allergies: ${petAllergies.ifBlank { "None" }}\n")
        append("Diet: ${petDiet.ifBlank { "Standard" }}\n")
        append("\nSent from Happy Pets")
    }
}