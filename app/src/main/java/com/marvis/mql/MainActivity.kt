package com.marvis.mql

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.marvis.mql.ui.screen.ConditionBuilderScreen
import com.marvis.mql.ui.screen.IndicatorEditorScreen
import com.marvis.mql.ui.screen.OutputScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var outputTitle by remember { mutableStateOf("") }
    var outputCode by remember { mutableStateOf("") }
    var isStrategy by remember { mutableStateOf(true) }

    val title = when (currentRoute) {
        "condition_builder" -> "条件构建策略"
        "indicator_editor" -> "自定义指标编辑"
        "output" -> "生成结果"
        else -> "MQL交易脚本生成器"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (currentRoute != "output") {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Build, contentDescription = "策略") },
                        label = { Text("条件策略", fontSize = 11.sp) },
                        selected = currentRoute == "condition_builder",
                        onClick = {
                            if (currentRoute != "condition_builder")
                                navController.navigate("condition_builder") {
                                    popUpTo("condition_builder") { inclusive = true }
                                }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Code, contentDescription = "指标") },
                        label = { Text("自定义指标", fontSize = 11.sp) },
                        selected = currentRoute == "indicator_editor",
                        onClick = {
                            if (currentRoute != "indicator_editor")
                                navController.navigate("indicator_editor") {
                                    popUpTo("indicator_editor") { inclusive = true }
                                }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "condition_builder",
            modifier = Modifier.padding(padding)
        ) {
            composable("condition_builder") {
                ConditionBuilderScreen(
                    onGenerate = { title, code, strategy ->
                        outputTitle = title
                        outputCode = code
                        isStrategy = strategy
                        navController.navigate("output")
                    }
                )
            }
            composable("indicator_editor") {
                IndicatorEditorScreen(
                    onCompile = { title, code ->
                        outputTitle = title
                        outputCode = code
                        isStrategy = false
                        navController.navigate("output")
                    }
                )
            }
            composable("output") {
                OutputScreen(
                    title = outputTitle,
                    code = outputCode,
                    isStrategy = isStrategy,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun lightColorScheme() = lightColorScheme(
    primary = com.marvis.mql.ui.theme.Primary,
    onPrimary = com.marvis.mql.ui.theme.OnPrimary,
    secondary = com.marvis.mql.ui.theme.Secondary,
    error = com.marvis.mql.ui.theme.Error,
    background = com.marvis.mql.ui.theme.Background,
    surface = com.marvis.mql.ui.theme.Surface,
    onBackground = com.marvis.mql.ui.theme.OnBackground,
    onSurface = com.marvis.mql.ui.theme.OnSurface
)
