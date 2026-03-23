package com.klyx.nodegraph

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.klyx.nodegraph.icon.Add
import com.klyx.nodegraph.icon.Close
import com.klyx.nodegraph.icon.Delete
import com.klyx.nodegraph.icon.Icons
import com.klyx.nodegraph.icon.Info
import com.klyx.nodegraph.ui.CreateEnumDialog
import com.klyx.nodegraph.util.generateId
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

@Composable
internal fun VariablesPanel(
    state: GraphState,
    onDismiss: () -> Unit,
    colors: GraphColors,
    isViewOnly: Boolean,
) {
    val vars = state.variables
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingVariableId by remember { mutableStateOf<Uuid?>(null) }

    val availableTypes by remember(state.registry, state.customEnums) {
        derivedStateOf {
            buildList {
                addAll(listOf(PinType.Float, PinType.Integer, PinType.Boolean, PinType.String()))

                state.registry.customTypes.values.forEach { customDef ->
                    add(PinType.Custom(customDef.typeName, customDef.color))
                }

                state.registry.enumTypes.values.forEach(::add)
                state.customEnums.forEach(::add)
            }
        }
    }

    Card(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .padding(8.dp)
            .border(1.dp, colors.nodeOutlineColor, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.panelBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .background(colors.nodeBackgroundColor)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "Variables",
                    color = colors.titleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )

                if (!isViewOnly) {
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Add, "Add Variable", tint = colors.titleColor, modifier = Modifier.size(18.dp))
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Close, "Close", tint = colors.titleColor, modifier = Modifier.size(16.dp))
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (vars.isEmpty()) {
                    item {
                        Text(
                            "No variables yet.\nClick + to add one.",
                            color = colors.labelColor.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            modifier = Modifier
                                .padding(vertical = 24.dp)
                                .fillMaxWidth()
                                .wrapContentWidth(Alignment.CenterHorizontally),
                        )
                    }
                }

                items(vars, key = { it.id.toString() }) { variable ->
                    VariableRow(
                        variable = variable,
                        allVariables = vars,
                        isEditing = editingVariableId == variable.id,
                        onStartEdit = { editingVariableId = variable.id },
                        onEditDone = { editingVariableId = null },
                        onGet = { state.createGetNode(variable, state.viewCentreGraph(800f, 600f)) },
                        onSet = { state.createSetNode(variable, state.viewCentreGraph(800f, 600f) + Offset(0f, 80f)) },
                        onRename = { newName -> state.renameVariable(variable.id, newName) },
                        onDelete = { state.removeVariable(variable.id) },
                        colors = colors,
                        isViewOnly = isViewOnly
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        val defaultName = remember(vars) { nextVariableName(vars) }

        CreateVariableDialog(
            defaultName = defaultName,
            availableTypes = availableTypes,
            existingNames = vars.map { it.name.lowercase() },
            colors = colors,
            onDismiss = { showCreateDialog = false },
            onCreate = { newName, type ->
                state.addVariable(generateId(), newName, type)
                showCreateDialog = false
            },
            onAddEnum = { enumName, entries ->
                state.addCustomEnum(enumName, entries)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VariableRow(
    variable: GraphVariable,
    allVariables: List<GraphVariable>,
    isEditing: Boolean,
    onStartEdit: () -> Unit,
    onEditDone: () -> Unit,
    onGet: () -> Unit,
    onSet: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    colors: GraphColors,
    isViewOnly: Boolean
) {
    var draft by remember(variable.name) { mutableStateOf(variable.name) }

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isEditing) Color(0xFF2C2C3E) else Color.Transparent)
            .clickable { onStartEdit() }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
//        Box(
//            Modifier
//                .size(12.dp)
//                .clip(CircleShape)
//                .background(variable.type.color)
//        )
//
//        Spacer(Modifier.width(4.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isEditing && !variable.isSystem && !isViewOnly) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            val finalName = draft.trim()
                            val isDuplicate =
                                allVariables.any {
                                    it.name.equals(
                                        finalName,
                                        ignoreCase = true
                                    ) && it.id != variable.id
                                }

                            if (finalName.isNotBlank() && !isDuplicate) {
                                onRename(finalName)
                            } else {
                                draft = variable.name
                            }
                            onEditDone()
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF0D0D1A), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        cursorBrush = SolidColor(Color.White)
                    )
                } else {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = if (variable.isSystem) Color(0xFFFC1B1B) else colors.titleColor)) {
                                append(variable.name)
                            }

                            append(": ")

                            withStyle(SpanStyle(color = Color(0xFFFF9441))) {
                                append(variable.type.typeName)
                            }
                        },
                        color = colors.titleColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!isViewOnly) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(
                        onClick = onGet,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.height(20.dp),
                    ) {
                        Text("Get", color = Color(0xFF4FC3F7), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = onSet,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.height(20.dp),
                    ) {
                        Text("Set", color = Color(0xFF81C784), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (variable.isSystem) {
            val tooltipState = rememberTooltipState()
            val coroutineScope = rememberCoroutineScope()

            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Start),
                state = tooltipState,
                tooltip = {
                    PlainTooltip {
                        Text("This is a system variable. You cannot delete it.")
                    }
                }
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            tooltipState.show()
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Info,
                        contentDescription = "Variable info",
                        tint = colors.titleColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else if (!isViewOnly) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Delete,
                    "Delete",
                    tint = colors.titleColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun CreateVariableDialog(
    defaultName: String,
    availableTypes: List<PinType>,
    existingNames: List<String>,
    colors: GraphColors,
    onDismiss: () -> Unit,
    onCreate: (String, PinType) -> Unit,
    onAddEnum: (String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf(defaultName) }
    var selectedType by remember { mutableStateOf(availableTypes.firstOrNull() ?: PinType.Float) }
    var typeExpanded by remember { mutableStateOf(false) }
    var showEnumDialog by remember { mutableStateOf(false) }

    var newlyCreatedEnumName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(availableTypes, newlyCreatedEnumName) {
        if (newlyCreatedEnumName != null) {
            val newlyAdded = availableTypes.find { it.typeName == newlyCreatedEnumName }
            if (newlyAdded != null) {
                selectedType = newlyAdded
                newlyCreatedEnumName = null
            }
        }
    }

    val isDuplicate = remember(name, existingNames) {
        existingNames.contains(name.trim().lowercase())
    }
    val isValid = name.isNotBlank() && !isDuplicate

    if (showEnumDialog) {
        CreateEnumDialog(
            colors = colors,
            onDismiss = { showEnumDialog = false },
            onSave = { enumName, entries ->
                onAddEnum(enumName, entries)
                newlyCreatedEnumName = enumName
                showEnumDialog = false
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.border(1.dp, colors.nodeOutlineColor, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = colors.panelBackgroundColor),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Create Variable", color = colors.titleColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Column {
                    Text("Name", color = colors.labelColor, fontSize = 12.sp)
                    Spacer(Modifier.height(2.dp))
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0D0D1A), RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        cursorBrush = SolidColor(Color.White)
                    )
                    if (isDuplicate) {
                        Text(
                            "A variable with this name already exists.",
                            color = Color(0xFFE53935),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Column {
                    Text("Type", color = colors.labelColor, fontSize = 12.sp)
                    Spacer(Modifier.height(2.dp))
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0D0D1A), RoundedCornerShape(4.dp))
                                .clickable { typeExpanded = true }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(selectedType.color))
                            Text(selectedType.typeName, color = Color.White, fontSize = 14.sp)
                        }

                        DropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false },
                            modifier = Modifier
                                .background(colors.panelBackgroundColor)
                                .border(1.dp, colors.nodeOutlineColor, MenuDefaults.shape),
                        ) {
                            availableTypes.forEach { pt ->
                                DropdownItem(
                                    leadingIcon = {
                                        Box(Modifier.size(10.dp).clip(CircleShape).background(pt.color))
                                    },
                                    text = {
                                        Text(
                                            text = buildString {
                                                append(pt.typeName)
                                                if (pt is PinType.Enum) {
                                                    append(" (enum)")
                                                }
                                            },
                                            color = colors.titleColor,
                                            fontSize = 12.sp
                                        )
                                    },
                                    onClick = {
                                        selectedType = pt
                                        typeExpanded = false
                                    }
                                )
                            }

                            HorizontalDivider(
                                color = colors.nodeOutlineColor,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            DropdownItem(
                                leadingIcon = { Icon(Icons.Add, contentDescription = null, tint = Color(0xFF4FC3F7)) },
                                text = { Text("Create Enum", color = Color(0xFF4FC3F7), fontSize = 12.sp) },
                                onClick = {
                                    typeExpanded = false
                                    showEnumDialog = true
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Cancel", color = colors.labelColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onCreate(name.trim(), selectedType) },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF216BFF),
                            disabledContainerColor = Color(0xFF216BFF).copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Create", color = if (isValid) Color(0xFFFFFFFF) else Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

private fun nextVariableName(vars: List<GraphVariable>): String {
    var i = 0
    while (true) {
        val name = "NewVar_$i"
        if (vars.none { it.name.equals(name, ignoreCase = true) }) return name
        i++
    }
}
