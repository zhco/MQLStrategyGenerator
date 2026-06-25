package com.marvis.mql.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marvis.mql.library.MqlLibrary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceScreen() {
    val allFunctions = remember { MqlLibrary.allFunctions }
    val categories = remember { MqlLibrary.Category.entries.associateBy { it.label } }

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var expandedFunction by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val filteredFunctions = remember(searchQuery.text, selectedCategory) {
        allFunctions.filter { func ->
            val matchSearch = searchQuery.text.isEmpty() ||
                func.name.contains(searchQuery.text, ignoreCase = true) ||
                func.description.contains(searchQuery.text, ignoreCase = true) ||
                func.category.contains(searchQuery.text, ignoreCase = true)
            val matchCategory = selectedCategory == null || func.category == selectedCategory
            matchSearch && matchCategory
        }.sortedBy { it.category }
    }

    // Group by category for display
    val grouped = remember(filteredFunctions) {
        filteredFunctions.groupBy { it.category }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("搜索函数名、描述或分类…", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索", modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.text.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = TextFieldValue("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )

        // Category chips
        ScrollableTabRow(
            selectedTabIndex = categories.keys.indexOf(selectedCategory).coerceAtLeast(0),
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth(),
            divider = {}
        ) {
            // "全部" tab
            Tab(
                selected = selectedCategory == null,
                onClick = {
                    selectedCategory = null
                    expandedFunction = null
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    "全部(${allFunctions.size})",
                    fontSize = 12.sp,
                    fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
            // Category tabs
            categories.keys.forEach { cat ->
                val count = allFunctions.count { it.category == cat }
                Tab(
                    selected = selectedCategory == cat,
                    onClick = {
                        selectedCategory = if (selectedCategory == cat) null else cat
                        expandedFunction = null
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        "$cat($count)",
                        fontSize = 12.sp,
                        fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // Results
        if (filteredFunctions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("未找到匹配的函数", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                grouped.forEach { (category, funcs) ->
                    item {
                        Text(
                            category,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                        )
                    }
                    items(funcs, key = { it.name }) { func ->
                        val isExpanded = expandedFunction == func.name
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable {
                                    expandedFunction = if (isExpanded) null else func.name
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isExpanded)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 1.dp else 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        func.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        func.description,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                    Icon(
                                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "收起" else "展开",
                                        modifier = Modifier.size(20.dp).padding(start = 4.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        // Syntax
                                        if (func.syntax.isNotEmpty()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = MaterialTheme.shapes.small,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    func.syntax,
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }
                                        }

                                        // Parameters
                                        if (func.params.isNotEmpty()) {
                                            Spacer(Modifier.height(8.dp))
                                            Text("参数：", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            func.params.forEach { param ->
                                                Text(
                                                    buildString {
                                                        append("  ${param.name}")
                                                        append(" — ${param.description}")
                                                        if (!param.required) append(" [可选]")
                                                        if (param.defaultValue.isNotEmpty()) append(" 默认=${param.defaultValue}")
                                                    },
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                                )
                                            }
                                        }

                                        // Return
                                        if (func.returns.isNotEmpty()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                "返回：${func.returns}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Example
                                        if (func.example.isNotEmpty()) {
                                            Spacer(Modifier.height(8.dp))
                                            Text("示例：", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = MaterialTheme.shapes.small,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    func.example,
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
