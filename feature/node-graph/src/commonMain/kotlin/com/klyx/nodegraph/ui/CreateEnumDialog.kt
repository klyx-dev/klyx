package com.klyx.nodegraph.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.klyx.nodegraph.GraphColors
import com.klyx.nodegraph.icon.Add
import com.klyx.nodegraph.icon.Close
import com.klyx.nodegraph.icon.Icons

@Composable
internal fun CreateEnumDialog(
    colors: GraphColors,
    onDismiss: () -> Unit,
    onSave: (name: String, entries: List<String>) -> Unit
) {
    var enumName by remember { mutableStateOf("") }
    val entries = remember { mutableStateListOf("Item 1", "Item 2") }

    val hasDuplicates by remember(entries) {
        derivedStateOf {
            val validEntries = entries.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            validEntries.size != validEntries.distinct().size
        }
    }

    val isValid by remember {
        derivedStateOf {
            enumName.isNotBlank() && entries.isNotEmpty() && entries.all { it.isNotBlank() } && !hasDuplicates
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.border(1.dp, colors.nodeOutlineColor, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = colors.panelBackgroundColor),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Create Custom Enum",
                    color = colors.titleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Enum Name", color = colors.labelColor, fontSize = 12.sp)

                    BasicTextField(
                        value = enumName,
                        onValueChange = { enumName = it },
                        singleLine = true,
                        textStyle = TextStyle(color = colors.titleColor, fontSize = 13.sp),
                        cursorBrush = SolidColor(colors.titleColor),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0D0D1A), RoundedCornerShape(4.dp))
                                    .padding(10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (enumName.isEmpty()) {
                                    Text(
                                        "e.g. TargetPlatform",
                                        style = TextStyle(
                                            color = colors.labelColor.copy(alpha = 0.5f),
                                            fontSize = 13.sp
                                        ),
                                        modifier = Modifier.defaultMinSize()
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }

                val listState = rememberLazyListState()

                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Entries", color = colors.labelColor, fontSize = 12.sp)

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(entries) { index, entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BasicTextField(
                                    value = entry,
                                    onValueChange = { newValue -> entries[index] = newValue },
                                    singleLine = true,
                                    textStyle = TextStyle(color = colors.titleColor, fontSize = 13.sp),
                                    cursorBrush = SolidColor(colors.titleColor),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { inner ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0D0D1A), RoundedCornerShape(4.dp))
                                                .padding(10.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (entry.isEmpty()) {
                                                Text(
                                                    "Entry Name",
                                                    style = TextStyle(
                                                        color = colors.labelColor.copy(alpha = 0.5f),
                                                        fontSize = 13.sp
                                                    ),
                                                    modifier = Modifier.defaultMinSize()
                                                )
                                            }
                                            inner()
                                        }
                                    }
                                )

                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (entries.size > 1) Color(0xFFEF5350).copy(alpha = 0.1f) else Color.Transparent)
                                        .clickable(enabled = entries.size > 1) { entries.removeAt(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Close,
                                        contentDescription = "Remove Entry",
                                        tint = if (entries.size > 1) Color(0xFFEF5350) else colors.labelColor.copy(alpha = 0.3f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (hasDuplicates) {
                        Text(
                            "Duplicate entries are not allowed.",
                            color = Color(0xFFEF5350),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            entries.add("")
                            listState.requestScrollToItem(listState.layoutInfo.totalItemsCount)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = ButtonDefaults.TextButtonWithIconContentPadding
                    ) {
                        Icon(
                            Icons.Add,
                            contentDescription = null,
                            tint = Color(0xFF4FC3F7),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Add Entry",
                            color = Color(0xFF4FC3F7),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    TextButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Cancel", color = colors.labelColor)
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (isValid) {
                                onSave(enumName.trim(), entries.map { it.trim() })
                            }
                        },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF216BFF),
                            disabledContainerColor = Color(0xFF216BFF).copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Create", color = if (isValid) Color.White else Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}
