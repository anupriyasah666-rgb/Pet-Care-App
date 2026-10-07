package np.com.petcareapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The main button used across the app. While isLoading is true it shows a spinner
// and can't be pressed, so the user can't send the same request twice
@Composable
fun PetCareButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        // Turn the button off while something is loading
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Text box used on every form, with the error message shown in red underneath.
// Autocorrect is switched off, and email and password boxes never start with a capital letter
@Composable
fun PetCareTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            isError = error != null,
            visualTransformation = visualTransformation,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions.copy(
                autoCorrectEnabled = false,
                capitalization = if (keyboardOptions.keyboardType == KeyboardType.Password ||
                    keyboardOptions.keyboardType == KeyboardType.Email)
                    KeyboardCapitalization.None else keyboardOptions.capitalization
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                errorBorderColor = MaterialTheme.colorScheme.error,
                errorLabelColor = MaterialTheme.colorScheme.error,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            singleLine = true
        )
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            )
        }
    }
}

// Rounded card used for pets, tasks and other items in lists
@Composable
fun PetCareCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        content = content
    )
}

// Used before anything gets deleted (a pet, a routine, a task, an expense or a health record).
// The delete button uses the theme's error colour, so it looks like a warning in light and dark mode
@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmText: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// The round tick button for marking a task as done on the Home and Pet Detail screens.
// The circle you see is 32dp, but the area you can tap is 48dp so it's easier to hit.
// Role.Checkbox lets TalkBack read it out as a checkbox and say if it's ticked
@Composable
fun TaskCheckCircle(
    checked: Boolean,
    taskTitle: String,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            // Tap anywhere in the 48dp area to tick or untick
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            )
            .semantics { contentDescription = "Mark $taskTitle as done" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (checked) MaterialTheme.colorScheme.secondary else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (checked) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null, // TalkBack already gets the ticked state from toggleable()
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}