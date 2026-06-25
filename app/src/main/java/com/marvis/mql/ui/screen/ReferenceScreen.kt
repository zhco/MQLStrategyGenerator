
package com.marvis.mql.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marvis.mql.library.MqlLibrary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("全部") }
    var expandedFunction by remember { mutableStateOf<String?>(null) }
    var showOperatorRef by remember { mutableStateOf(false) }

    val allFunctions = MqlLibrary.allFunctions
    val categories = listOf("全部") + allFunctions.map { it.category }.distinct()

    val filtered = allFunctions.filter { func ->
        (selectedCategory == "全部" || func.category == selectedCategory) &&
        (searchQuery.isEmpty() || func.name.contains(searchQuery, ignoreCase = true) || func.description.contains(searchQuery, ignoreCase = true))
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        // 搜索栏
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            placeholder = { Text("搜索函数/指标...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Clear, "清除", modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
        )

        // 运算符快速参考按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${filtered.size} 个", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = { showOperatorRef = !showOperatorRef }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                Icon(if (showOperatorRef) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Text(if (showOperatorRef) "收起运算符" else "运算符参考", fontSize = 11.sp)
            }
        }

        // 运算符参考
        if (showOperatorRef) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    val rows = listOf(
                        listOf("=" to "赋值", ":=" to "定义", ";" to "语句结束", "//" to "注释"),
                        listOf(">" to "大于", "<" to "小于", ">=" to "大于等于", "<=" to "小于等于"),
                        listOf("==" to "等于", "!=" to "不等于", "<>" to "不等于", "&&" to "逻辑与"),
                        listOf("||" to "逻辑或", "!" to "逻辑非", "AND" to "逻辑与", "OR" to "逻辑或"),
                        listOf("CROSS" to "上穿", "REF" to "引用前值", "BARSLAST" to "上次满足", "IF" to "条件判断"),
                    )
                    rows.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                            row.forEach { (op, desc) ->
                                Surface(
                                    modifier = Modifier.weight(1f).padding(1.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(op, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }

        // 分类筛选
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 0.dp,
            divider = {}
        ) {
            categories.forEach { cat ->
                Tab(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    text = { Text(cat, fontSize = 11.sp) },
                    modifier = Modifier.padding(0.dp).defaultMinSize(minWidth = 1.dp)
                )
            }
        }
        Spacer(Modifier.height(2.dp))

        // 函数列表
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (filtered.isEmpty()) {
                item {
                    Text("无匹配结果", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurface)
                }
            }
            items(filtered) { func ->
                val isExpanded = expandedFunction == func.name
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { expandedFunction = if (isExpanded) null else func.name },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(func.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            Text(func.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(func.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        if (isExpanded) {
                            Spacer(Modifier.height(4.dp))
                            Text(func.syntax, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFE91E63))
                            if (func.params.isNotEmpty()) {
                                Spacer(Modifier.height(2.dp))
                                Text("参数:", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                func.params.forEach { param ->
                                    Text("  ${param.name}: ${param.description} (默认: ${param.defaultValue})", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            if (func.examples.isNotEmpty()) {
                                Spacer(Modifier.height(2.dp))
                                Text("示例:", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                func.examples.forEach { ex ->
                                    Text("  $ex", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
