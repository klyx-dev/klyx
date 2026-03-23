package com.klyx.nodegraph

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klyx.nodegraph.icon.ArrowRight
import com.klyx.nodegraph.icon.Close
import com.klyx.nodegraph.icon.Icons
import com.klyx.nodegraph.icon.Search

private class CategoryTreeNode(val path: String, val name: String) {
    val children = mutableMapOf<String, CategoryTreeNode>()
}

private data class CategoryItem(
    val name: String,
    val fullPath: String,
    val depth: Int,
    val hasChildren: Boolean
)

@Composable
internal fun AddNodePanel(
    state: GraphState,
    colors: GraphColors,
    onDismiss: () -> Unit,
    onSpawn: (Node) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // "" means "All Nodes". otherwise, it holds the exact path e.g., "Math/Basic"
    var selectedCategory by remember { mutableStateOf("") }
    val expandedCategories = remember { mutableStateSetOf<String>() }

    val allNodes = remember(state.registry) {
        state.registry.grouped().values.flatten()
    }

    val matchingNodes by remember(searchQuery, allNodes) {
        derivedStateOf {
            if (searchQuery.isBlank()) allNodes else {
                val q = searchQuery.lowercase()
                allNodes.filter { it.title.lowercase().contains(q) || it.category.lowercase().contains(q) }
            }
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            selectedCategory = ""
        }
    }

    val categoryItems by remember(matchingNodes, expandedCategories) {
        derivedStateOf {
            val paths = matchingNodes.map { it.category }.distinct()
            val result = mutableListOf<CategoryItem>()

            // start with the "All" category at the very top
            result.add(CategoryItem("All Nodes", "", 0, false))

            val root = CategoryTreeNode("", "")

            paths.forEach { path ->
                var current = root
                val parts = path.split("/")
                var currentPath = ""
                parts.forEach { part ->
                    currentPath = if (currentPath.isEmpty()) part else "$currentPath/$part"
                    current = current.children.getOrPut(part) { CategoryTreeNode(currentPath, part) }
                }
            }

            fun traverse(node: CategoryTreeNode, depth: Int) {
                if (node.path.isNotEmpty()) {
                    result.add(CategoryItem(node.name, node.path, depth, node.children.isNotEmpty()))
                }

                if (node.path.isEmpty() || expandedCategories.contains(node.path)) {
                    node.children.values.sortedBy { it.name }.forEach { child ->
                        traverse(child, depth + 1)
                    }
                }
            }
            traverse(root, -1)
            result
        }
    }

    val displayNodes by remember(matchingNodes, expandedCategories) {
        derivedStateOf {
            if (selectedCategory.isEmpty()) {
                matchingNodes
            } else {
                matchingNodes.filter {
                    it.category == selectedCategory || it.category.startsWith("$selectedCategory/")
                }.sortedBy { it.title }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                onClick = onDismiss,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .fillMaxHeight(0.9f)
                .border(1.dp, colors.nodeOutlineColor, RoundedCornerShape(8.dp))
                .clickable(
                    onClick = {},
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = colors.panelBackgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            // top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.nodeBackgroundColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Add Node", color = colors.labelColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))

                val searchFieldInteractionSource = remember { MutableInteractionSource() }
                val searchFieldColors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = colors.graphBackgroundColor,
                    focusedContainerColor = colors.graphBackgroundColor,
                    focusedBorderColor = colors.nodeOutlineColor,
                    unfocusedBorderColor = colors.nodeOutlineColor.copy(alpha = 0.3f),
                    focusedTextColor = colors.titleColor,
                    unfocusedTextColor = colors.titleColor,
                    focusedPlaceholderColor = colors.labelColor.copy(alpha = 0.5f),
                    unfocusedPlaceholderColor = colors.labelColor.copy(alpha = 0.5f),
                    cursorColor = colors.titleColor
                )

                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(color = colors.titleColor, fontSize = 14.sp),
                    cursorBrush = SolidColor(colors.titleColor),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        OutlinedTextFieldDefaults.DecorationBox(
                            value = searchQuery,
                            innerTextField = inner,
                            enabled = true,
                            singleLine = true,
                            visualTransformation = VisualTransformation.None,
                            interactionSource = searchFieldInteractionSource,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            placeholder = { Text("Search nodes...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            colors = searchFieldColors,
                            container = {
                                OutlinedTextFieldDefaults.Container(
                                    enabled = true,
                                    isError = false,
                                    interactionSource = searchFieldInteractionSource,
                                    colors = searchFieldColors,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        )
                    }
                )

                IconButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(5.dp, 5.dp)) {
                    Icon(
                        Icons.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp),
                        tint = colors.titleColor
                    )
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(6.dp)
                ) {
                    items(categoryItems, key = { it.fullPath }) { item ->
                        val isSelected = selectedCategory == item.fullPath
                        val isExpanded = expandedCategories.contains(item.fullPath)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF062C8B).copy(alpha = 0.8f) else Color.Transparent)
                                .clickable {
                                    selectedCategory = item.fullPath
                                    if (item.hasChildren) {
                                        if (isExpanded) {
                                            expandedCategories -= item.fullPath
                                        } else {
                                            expandedCategories += item.fullPath
                                        }
                                    }
                                }
                                .padding(
                                    start = 4.dp + (item.depth * 14).dp,
                                    top = 8.dp, bottom = 8.dp, end = 8.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.hasChildren) {
                                val rotation by animateFloatAsState(if (isExpanded) 90f else 0f)

                                Icon(
                                    Icons.ArrowRight,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer { rotationZ = rotation },
                                    tint = if (isSelected) Color.White else Color(0xFFA0A0A0)
                                )
                            } else {
                                Spacer(Modifier.width(20.dp))
                            }

                            Text(
                                text = item.name,
                                color = if (isSelected) Color.White else Color(0xFFA0A0A0),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                VerticalDivider(color = colors.nodeOutlineColor)

                LazyColumn(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight()
                        .padding(6.dp)
                ) {
                    if (displayNodes.isEmpty()) {
                        item {
                            Text(
                                "No nodes found in this category.",
                                color = Color(0xFFA0A0A0),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    items(displayNodes, key = { it.key }) { node ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    onSpawn(node)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(node.headerColor)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = node.title, color = colors.titleColor, fontSize = 14.sp)

                                if (node.description.isNotEmpty()) {
                                    Text(text = node.description, color = colors.labelColor, fontSize = 12.sp)
                                }
                            }

                            Text(
                                text = node.category.uppercase(),
                                color = colors.titleColor.copy(alpha = 0.7f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
