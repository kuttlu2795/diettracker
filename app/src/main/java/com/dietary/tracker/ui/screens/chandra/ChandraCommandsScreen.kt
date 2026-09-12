package com.dietary.tracker.ui.screens.chandra

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dietary.tracker.chandra.ChandraActionId
import com.dietary.tracker.chandra.ChandraDefaultPhrases
import com.dietary.tracker.data.entities.ChandraCustomCommand
import com.dietary.tracker.ui.theme.GreenPrimary

@Composable
fun ChandraCommandsScreen(viewModel: ChandraViewModel) {
    val customCommands by viewModel.customCommands.collectAsState()
    var addingForAction by remember { mutableStateOf<ChandraActionId?>(null) }
    var newPhraseText by remember { mutableStateOf("") }

    val categories = ChandraActionId.entries.groupBy { it.category }
    val categoryOrder = listOf("Health", "Nutrition", "Fitness", "Fasting", "Goals", "Reports", "Devices")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Chandra Voice Commands", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Every feature Chandra supports, and the phrases that trigger it. Add your own aliases below any feature.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Spacer(Modifier.height(12.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            categoryOrder.forEach { category ->
                val actions = categories[category] ?: return@forEach
                Text(
                    category.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                )
                actions.forEach { action ->
                    ActionCommandCard(
                        action = action,
                        customCommands = customCommands.filter { it.actionId == action.name },
                        isAdding = addingForAction == action,
                        newPhraseText = if (addingForAction == action) newPhraseText else "",
                        onAddClick = { addingForAction = action; newPhraseText = "" },
                        onPhraseChange = { newPhraseText = it },
                        onConfirmAdd = {
                            viewModel.addCustomCommand(action, newPhraseText)
                            addingForAction = null
                            newPhraseText = ""
                        },
                        onCancelAdd = { addingForAction = null },
                        onDelete = { viewModel.deleteCustomCommand(it) }
                    )
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ActionCommandCard(
    action: ChandraActionId,
    customCommands: List<ChandraCustomCommand>,
    isAdding: Boolean,
    newPhraseText: String,
    onAddClick: () -> Unit,
    onPhraseChange: (String) -> Unit,
    onConfirmAdd: () -> Unit,
    onCancelAdd: () -> Unit,
    onDelete: (ChandraCustomCommand) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(action.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onAddClick) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Command", style = MaterialTheme.typography.labelSmall)
                }
            }

            val defaults = ChandraDefaultPhrases.byAction[action].orEmpty()
            if (defaults.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Default Commands", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                defaults.forEach { phrase ->
                    Text("• $phrase", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                }
            }

            if (customCommands.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Custom Commands", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                customCommands.forEach { cmd ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("• ${cmd.phrase}", style = MaterialTheme.typography.bodySmall, color = GreenPrimary)
                        IconButton(onClick = { onDelete(cmd) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            if (isAdding) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newPhraseText,
                        onValueChange = onPhraseChange,
                        placeholder = { Text("e.g. My pulse") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onConfirmAdd) { Text("Save") }
                    TextButton(onClick = onCancelAdd) { Text("Cancel") }
                }
            }
        }
    }
}
