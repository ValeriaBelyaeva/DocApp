package com.example.docapp.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@Composable
fun App() {
    AppLogger.log("App", "App composable started")
    DocTheme {
        val nav = rememberNavController()
        AppLogger.log("App", "Navigation controller created")
        NavHost(navController = nav, startDestination = "pin") {
            composable("pin") {
                PinScreenNew(onSuccess = { nav.navigate("home") })
            }
            composable("showcase") {
                DesignShowcase()
            }
            composable("home") {
                HomePager(
                    openDoc = { id -> nav.navigate("doc/view/$id") },
                    createNew = { folderId ->
                        nav.navigate("templates?folderId=${folderId ?: ""}")
                    }
                )
            }
            composable(
                route = "templates?folderId={folderId}",
                arguments = listOf(navArgument("folderId") {
                    type = NavType.StringType; defaultValue = ""
                })
            ) { backStack ->
                val folderIdArg = backStack.arguments?.getString("folderId").orEmpty()
                    .ifBlank { null }
                TemplateSelectorScreen(
                    folderId = folderIdArg,
                    onCreateDocFromTemplate = { templateId, fId ->
                        nav.navigate("template/fill?templateId=$templateId&folderId=${fId ?: ""}")
                    },
                    onCreateEmpty = { fId ->
                        nav.navigate("doc/edit?docId=&templateId=&folderId=${fId ?: ""}")
                    }
                )
            }
            composable(
                route = "template/fill?templateId={templateId}&folderId={folderId}",
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
                    onDocumentCreated = { docId ->
                        nav.navigate("home") { popUpTo("home") }
                    },
                    onCancel = {
                        nav.popBackStack()
                    }
                )
            }
            composable(
                route = "doc/view/{docId}",
                arguments = listOf(navArgument("docId") { type = NavType.StringType })
            ) { backStack ->
                val docId = backStack.arguments?.getString("docId") 
                    ?: throw IllegalStateException("Missing docId argument")
                DocumentViewScreen(
                    docId = docId,
                    onEdit = {
                        nav.navigate("doc/edit?docId=$docId&templateId=&folderId=")
                    },
                    onDeleted = {
                        // Удалили документ — просто уходим назад на домашний
                        nav.popBackStack()
                    }
                )
            }
            composable(
                route = "doc/edit?docId={docId}&templateId={templateId}&folderId={folderId}",
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
                        nav.navigate("doc/view/$id") { popUpTo("home") }
                    }
                )
            }
        }
    }
}
