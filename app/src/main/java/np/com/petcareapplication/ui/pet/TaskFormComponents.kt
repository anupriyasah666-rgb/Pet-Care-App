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

// The task categories to choose from on the Add Task and Edit Task screens
val TASK_CATEGORIES = listOf("Feeding", "Exercise", "Grooming", "Healthcare", "Medication", "Play", "Other")

// Category box that can't be typed in. The arrow opens a list to pick from.
// Using a fixed list means tasks group properly under the same headings on the Pet Detail screen
@Composable
fun CategoryDropdown(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: List<String> = TASK_CATEGORIES
) {
    // Whether the list of categories is showing
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

// Time box that can't be typed in. The pencil opens Material 3's clock picker,
// so only real times can be saved. It used to be a normal text box, which let people type anything.
// time is the chosen time as text, e.g. "07:30 AM", or empty if nothing has been picked yet.
// onTimeSelected gets the new time when the user presses OK
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    time: String,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    // Whether the clock pop-up is showing
    var showPicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = time,
        onValueChange = {},
        readOnly = true,
        label = { Text("Time") },
        placeholder = { Text("Tap the pencil to choose") },
        isError = error != null,
        // Show the error under the box, e.g. when Save is pressed without a time
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
        // Open the clock at the time already chosen, or 08:00 if there isn't one yet
        val initial = TimeUtils.parseTime(time) ?: (8 to 0)
        val state = rememberTimePickerState(
            initialHour = initial.first,
            initialMinute = initial.second,
            // 12-hour clock with AM and PM
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Select time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    // Turn the hour and minute into text like "07:30 AM"
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