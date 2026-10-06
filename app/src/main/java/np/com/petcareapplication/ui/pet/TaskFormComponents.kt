package np.com.petcareapplication.ui.pet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import np.com.petcareapplication.util.TimeUtils

/** Categories offered when creating or editing a care task. */
val TASK_CATEGORIES = listOf("Feeding", "Exercise", "Grooming", "Healthcare", "Medication", "Play", "Other")

/**
 * Read-only field that opens a menu of categories. Choosing from a fixed list
 * (rather than typing) keeps the Pet Detail screen's category grouping consistent.
 */
@Composable
fun CategoryDropdown(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<String> = TASK_CATEGORIES
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            leadingIcon = {
                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose category")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Read-only time field backed by the Material 3 TimePicker dialog.
 * Replaces the old free-text "Time" field so only valid times can be saved.
 *
 * @param time the currently selected time as text, e.g. "07:30 AM" (empty if none chosen)
 * @param onTimeSelected called with the formatted time when the user taps OK
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    time: String,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = time,
        onValueChange = {},
        readOnly = true,
        label = { Text("Time") },
        placeholder = { Text("Tap the pencil to choose") },
        isError = error != null,
        supportingText = if (error != null) { { Text(error) } } else null,
        leadingIcon = {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Choose time")
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    )

    if (showPicker) {
        // Open the picker at the task's saved time if there is one, otherwise 08:00.
        val initial = TimeUtils.parseTime(time) ?: (8 to 0)
        val state = rememberTimePickerState(
            initialHour = initial.first,
            initialMinute = initial.second,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Select time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onTimeSelected(TimeUtils.formatTime(state.hour, state.minute))
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        )
    }
}