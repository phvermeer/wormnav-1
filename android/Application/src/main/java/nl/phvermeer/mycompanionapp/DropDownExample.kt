package nl.phvermeer.mycompanionapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize

@Composable
fun DropDownExample(options: List<String> = listOf("Track1", "Track2", "Track2"), onSelected: (value: String) -> Unit = {}, value: String? = null) {
    var expanded by remember { mutableStateOf( false) }
    var selectedOption by remember { mutableStateOf( "" ) }
    var textFilledSize by remember { mutableStateOf( Size.Zero) }
    val icon = if(expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown

    value?.let{
        selectedOption = it
    }

    OutlinedTextField(
        value = selectedOption,
        onValueChange = { selectedOption = it },
        modifier = Modifier
            .fillMaxWidth()
            .onPlaced { coordinates -> textFilledSize = coordinates.size.toSize() }
            .background(color = Color.Red),
        label = { Text("Select Item") },
        trailingIcon = { Icon(icon, "", Modifier.clickable { expanded = !expanded }) },
        readOnly = true,
    )
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
            .width(with(LocalDensity.current) { textFilledSize.width.toDp() })

    ) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option) },
                onClick = {
                    selectedOption = option
                    onSelected(option)
                    expanded = false
                }
            )
        }
    }
}