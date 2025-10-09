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
import com.example.docapp.ui.pin.PinScreen
import com.example.docapp.ui.template.TemplateSelectorScreen
import com.example.docapp.ui.theme.DocTheme

@Composable
fun App() {
    DocTheme {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = "pin") {
            composable("pin") {
                PinScreen(
                    onSuccess = {
                        nav.navigate("home") {
                            popUpTo("pin") { inclusive = true }
                        }
                    }
                )
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
                        nav.navigate("doc/edit?docId=&templateId=$templateId&folderId=${fId ?: ""}")
                    },
                    onCreateEmpty = { fId ->
                        nav.navigate("doc/edit?docId=&templateId=&folderId=${fId ?: ""}")
                    }
                )
            }
            composable(
                route = "doc/view/{docId}",
                arguments = listOf(navArgument("docId") { type = NavType.StringType })
            ) { backStack ->
                val docId = backStack.arguments!!.getString("docId")!!
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
                DocumentEditScreen(
                    existingDocId = backStack.arguments!!.getString("docId")
                        ?.ifBlank { null },
                    templateId = backStack.arguments!!.getString("templateId")
                        ?.ifBlank { null },
                    folderId = backStack.arguments!!.getString("folderId")
                        ?.ifBlank { null },
                    onSaved = { id ->
                        nav.navigate("doc/view/$id") { popUpTo("home") }
                    }
                )
            }
        }
    }
}
