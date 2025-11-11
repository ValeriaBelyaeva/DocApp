package com.example.docapp.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.docapp.ui.document.DocumentEditScreen
import com.example.docapp.ui.document.DocumentViewScreen
import com.example.docapp.ui.home.HomePager
import com.example.docapp.ui.pin.PinScreenNew
import com.example.docapp.ui.design.DesignShowcase
import com.example.docapp.ui.template.TemplateSelectorScreen
import com.example.docapp.ui.template.TemplateFillScreen
import com.example.docapp.ui.theme.DocTheme
import com.example.docapp.core.AppLogger
import com.example.docapp.ui.navigation.AppDestination
import com.example.docapp.ui.navigation.rememberAppNavigator
import kotlinx.coroutines.delay

@Composable
fun AppRoot(content: @Composable () -> Unit) {
    content()
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    AppLogger.log("App", "App composable started")
    DocTheme {
        val nav = rememberNavController()
        val navigator = rememberAppNavigator(nav)
        var lastInteraction by remember { mutableStateOf(System.currentTimeMillis()) }
        val inactivityTimeoutMs = 30_000L
        LaunchedEffect(Unit) {
            while (true) {
                delay(1_000L)
                val currentRoute = nav.currentBackStackEntry?.destination?.route
                if (currentRoute != AppDestination.Pin.route &&
                    System.currentTimeMillis() - lastInteraction >= inactivityTimeoutMs
                ) {
                    navigator.openPin(popUpTo = AppDestination.Pin, inclusive = true)
                    lastInteraction = System.currentTimeMillis()
                }
            }
        }

        AppLogger.log("App", "Navigation controller created")
        NavHost(
            navController = nav,
            startDestination = AppDestination.Pin.route,
            modifier = Modifier.pointerInteropFilter {
                lastInteraction = System.currentTimeMillis()
                false
            }
        ) {
            composable(AppDestination.Pin.route) {
                PinScreenNew(onSuccess = {
                    lastInteraction = System.currentTimeMillis()
                    navigator.openHome(popUpTo = AppDestination.Pin, inclusive = true)
                })
            }
            composable(AppDestination.Showcase.route) {
                DesignShowcase()
            }
            composable(AppDestination.Home.route) {
                HomePager(
                    openDoc = { id -> navigator.openDocView(id) },
                    createNew = { folderId -> navigator.openTemplateSelector(folderId) }
                )
            }
            composable(
                route = AppDestination.TemplateSelector.route,
                arguments = listOf(navArgument("folderId") {
                    type = NavType.StringType; defaultValue = ""
                })
            ) { backStack ->
                val folderIdArg = backStack.arguments?.getString("folderId").orEmpty()
                    .ifBlank { null }
                TemplateSelectorScreen(
                    folderId = folderIdArg,
                    onCreateDocFromTemplate = { templateId, fId ->
                        navigator.openTemplateFill(templateId, fId)
                    },
                    onCreateEmpty = { fId ->
                        navigator.openDocEditor(templateId = null, folderId = fId)
                    }
                )
            }
            composable(
                route = AppDestination.TemplateFill.route,
                arguments = listOf(
                    navArgument("templateId") { type = NavType.StringType },
                    navArgument("folderId") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStack ->
                val args = backStack.arguments
                    ?: throw IllegalStateException("Missing navigation arguments")
                val templateId = args.getString("templateId")
                    ?: throw IllegalStateException("Missing templateId argument")
                val folderIdArg = args.getString("folderId")
                    ?.ifBlank { null }
                TemplateFillScreen(
                    templateId = templateId,
                    folderId = folderIdArg,
                    onDocumentCreated = {
                        navigator.openHome(popUpTo = AppDestination.Home)
                    },
                    onCancel = {
                        navigator.popBack()
                    }
                )
            }
            composable(
                route = AppDestination.DocView.route,
                arguments = listOf(navArgument("docId") { type = NavType.StringType })
            ) { backStack ->
                val docId = backStack.arguments?.getString("docId")
                    ?: throw IllegalStateException("Missing docId argument")
                DocumentViewScreen(
                    docId = docId,
                    onEdit = {
                        navigator.openDocEditor(docId = docId)
                    },
                    onDeleted = {
                        navigator.popBack()
                    }
                )
            }
            composable(
                route = AppDestination.DocEdit.route,
                arguments = listOf(
                    navArgument("docId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("templateId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("folderId") { type = NavType.StringType; defaultValue = "" },
                )
            ) { backStack ->
                val args = backStack.arguments
                    ?: throw IllegalStateException("Missing navigation arguments")
                DocumentEditScreen(
                    existingDocId = args.getString("docId")
                        ?.ifBlank { null },
                    templateId = args.getString("templateId")
                        ?.ifBlank { null },
                    folderId = args.getString("folderId")
                        ?.ifBlank { null },
                    onSaved = { id ->
                        navigator.openDocView(id, popUpTo = AppDestination.Home)
                    }
                )
            }
        }
    }
}
