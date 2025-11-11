package com.example.docapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController

sealed class AppDestination(val route: String) {
    data object Pin : AppDestination("pin")
    data object Showcase : AppDestination("showcase")
    data object Home : AppDestination("home")
    data object TemplateSelector : AppDestination("templates?folderId={folderId}") {
        fun build(folderId: String?): String = "templates?folderId=${folderId.orEmpty()}"
    }
    data object TemplateFill : AppDestination("template/fill?templateId={templateId}&folderId={folderId}") {
        fun build(templateId: String, folderId: String?): String =
            "template/fill?templateId=$templateId&folderId=${folderId.orEmpty()}"
    }
    data object DocView : AppDestination("doc/view/{docId}") {
        fun build(docId: String): String = "doc/view/$docId"
    }
    data object DocEdit : AppDestination("doc/edit?docId={docId}&templateId={templateId}&folderId={folderId}") {
        fun build(docId: String?, templateId: String?, folderId: String?): String =
            "doc/edit?docId=${docId.asNavArg()}&templateId=${templateId.asNavArg()}&folderId=${folderId.asNavArg()}"
    }
}

class AppNavigator(private val navController: NavHostController) {

    fun openHome(popUpTo: AppDestination? = null, inclusive: Boolean = false) {
        navigate(AppDestination.Home.route, popUpTo, inclusive)
    }

    fun openPin(popUpTo: AppDestination? = null, inclusive: Boolean = false) {
        navigate(AppDestination.Pin.route, popUpTo, inclusive)
    }

    fun openShowcase() {
        navController.navigate(AppDestination.Showcase.route)
    }

    fun openTemplateSelector(folderId: String?) {
        navController.navigate(AppDestination.TemplateSelector.build(folderId))
    }

    fun openTemplateFill(templateId: String, folderId: String?) {
        navController.navigate(AppDestination.TemplateFill.build(templateId, folderId))
    }

    fun openDocView(docId: String, popUpTo: AppDestination? = null, inclusive: Boolean = false) {
        navigate(AppDestination.DocView.build(docId), popUpTo, inclusive)
    }

    fun openDocEditor(
        docId: String? = null,
        templateId: String? = null,
        folderId: String? = null,
        popUpTo: AppDestination? = null,
        inclusive: Boolean = false
    ) {
        navigate(AppDestination.DocEdit.build(docId, templateId, folderId), popUpTo, inclusive)
    }

    fun popBack() {
        navController.popBackStack()
    }

    private fun navigate(route: String, popUpTo: AppDestination?, inclusive: Boolean) {
        navController.navigate(route) {
            popUpTo?.let { destination ->
                popUpTo(destination.route) {
                    this.inclusive = inclusive
                }
            }
            launchSingleTop = true
        }
    }
}

@Composable
fun rememberAppNavigator(navController: NavHostController): AppNavigator = remember(navController) {
    AppNavigator(navController)
}

private fun String?.asNavArg(): String = this ?: ""
