package com.example.docapp.ui.navigation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController

/**
 * Sealed class representing all possible navigation destinations in the application.
 * Each destination has a route string that is used for navigation.
 * 
 * Works by defining route patterns that can contain parameters, which are then built into
 * complete route strings using the build() method.
 * 
 * arguments:
 *     route - String: The route pattern for this destination, may contain parameter placeholders
 */
sealed class AppDestination(val route: String) {
    data object Pin : AppDestination("pin")
    data object Showcase : AppDestination("showcase")
    data object Home : AppDestination("home")
    data object TemplateSelector : AppDestination("templates?folderId={folderId}") {
        /**
         * Builds a complete route string for template selector screen with folder ID parameter.
         * 
         * Works by replacing the folderId placeholder in the route pattern with the actual folder ID value.
         * 
         * arguments:
         *     folderId - String?: Optional folder ID to filter templates, null or empty for root folder
         * 
         * return:
         *     route - String: Complete route string with folderId parameter
         */
        fun build(folderId: String?): String = "templates?folderId=${folderId.orEmpty()}"
    }
    data object TemplateFill : AppDestination("template/fill?templateId={templateId}&folderId={folderId}") {
        /**
         * Builds a complete route string for template fill screen with template and folder ID parameters.
         * 
         * Works by replacing placeholders in the route pattern with actual template and folder ID values.
         * 
         * arguments:
         *     templateId - String: ID of the template to fill
         *     folderId - String?: Optional folder ID where the document will be created, null for root folder
         * 
         * return:
         *     route - String: Complete route string with templateId and folderId parameters
         */
        fun build(templateId: String, folderId: String?): String =
            "template/fill?templateId=$templateId&folderId=${folderId.orEmpty()}"
    }
    data object DocView : AppDestination("doc/view/{docId}") {
        /**
         * Builds a complete route string for document view screen with document ID parameter.
         * 
         * Works by replacing the docId placeholder in the route pattern with the actual document ID value.
         * 
         * arguments:
         *     docId - String: ID of the document to view
         * 
         * return:
         *     route - String: Complete route string with docId parameter
         */
        fun build(docId: String): String = "doc/view/$docId"
    }
    data object DocEdit : AppDestination("doc/edit?docId={docId}&templateId={templateId}&folderId={folderId}") {
        /**
         * Builds a complete route string for document edit screen with optional document, template and folder ID parameters.
         * 
         * Works by replacing placeholders in the route pattern with actual parameter values, using empty string for null values.
         * 
         * arguments:
         *     docId - String?: Optional ID of existing document to edit, null for new document
         *     templateId - String?: Optional ID of template to use for new document, null if no template
         *     folderId - String?: Optional folder ID where document will be created/edited, null for root folder
         * 
         * return:
         *     route - String: Complete route string with docId, templateId and folderId parameters
         */
        fun build(docId: String?, templateId: String?, folderId: String?): String =
            "doc/edit?docId=${docId.asNavArg()}&templateId=${templateId.asNavArg()}&folderId=${folderId.asNavArg()}"
    }
}

/**
 * Navigation helper class that provides type-safe navigation methods for the application.
 * Wraps NavHostController and provides convenient methods to navigate between screens.
 * 
 * Works by using the underlying NavHostController to perform navigation operations, with additional
 * logic to prevent navigation to PIN screen from other screens.
 */
class AppNavigator(private val navController: NavHostController) {
    /**
     * Navigates to the home screen, optionally clearing the back stack up to a specified destination.
     * 
     * Works by navigating to the home route and optionally popping destinations from the back stack.
     * 
     * arguments:
     *     popUpTo - AppDestination?: Optional destination to pop back stack to, null to keep current stack
     *     inclusive - Boolean: Whether to include the popUpTo destination in the pop operation
     * 
     * return:
     *     Unit - No return value
     */
    fun openHome(popUpTo: AppDestination? = null, inclusive: Boolean = false) {
        navigate(AppDestination.Home.route, popUpTo, inclusive)
    }
    
    /**
     * Navigates to the PIN screen, optionally clearing the back stack up to a specified destination.
     * 
     * Works by navigating to the PIN route and optionally popping destinations from the back stack.
     * 
     * arguments:
     *     popUpTo - AppDestination?: Optional destination to pop back stack to, null to keep current stack
     *     inclusive - Boolean: Whether to include the popUpTo destination in the pop operation
     * 
     * return:
     *     Unit - No return value
     */
    fun openPin(popUpTo: AppDestination? = null, inclusive: Boolean = false) {
        navigate(AppDestination.Pin.route, popUpTo, inclusive)
    }
    
    /**
     * Navigates to the design showcase screen.
     * 
     * Works by navigating to the showcase route without modifying the back stack.
     * 
     * return:
     *     Unit - No return value
     */
    fun openShowcase() {
        navController.navigate(AppDestination.Showcase.route)
    }
    
    /**
     * Navigates to the template selector screen for a specific folder.
     * 
     * Works by building the route with folder ID parameter and navigating to it.
     * 
     * arguments:
     *     folderId - String?: Optional folder ID to filter templates, null for root folder
     * 
     * return:
     *     Unit - No return value
     */
    fun openTemplateSelector(folderId: String?) {
        navController.navigate(AppDestination.TemplateSelector.build(folderId))
    }
    
    /**
     * Navigates to the template fill screen for creating a document from a template.
     * 
     * Works by building the route with template and folder ID parameters and navigating to it.
     * 
     * arguments:
     *     templateId - String: ID of the template to fill
     *     folderId - String?: Optional folder ID where the document will be created, null for root folder
     * 
     * return:
     *     Unit - No return value
     */
    fun openTemplateFill(templateId: String, folderId: String?) {
        navController.navigate(AppDestination.TemplateFill.build(templateId, folderId))
    }
    
    /**
     * Navigates to the document view screen for a specific document, optionally clearing the back stack.
     * 
     * Works by building the route with document ID parameter and navigating to it, optionally popping destinations.
     * 
     * arguments:
     *     docId - String: ID of the document to view
     *     popUpTo - AppDestination?: Optional destination to pop back stack to, null to keep current stack
     *     inclusive - Boolean: Whether to include the popUpTo destination in the pop operation
     * 
     * return:
     *     Unit - No return value
     */
    fun openDocView(docId: String, popUpTo: AppDestination? = null, inclusive: Boolean = false) {
        navigate(AppDestination.DocView.build(docId), popUpTo, inclusive)
    }
    /**
     * Navigates to the document editor screen for creating or editing a document, optionally clearing the back stack.
     * 
     * Works by building the route with document, template and folder ID parameters and navigating to it.
     * 
     * arguments:
     *     docId - String?: Optional ID of existing document to edit, null for new document
     *     templateId - String?: Optional ID of template to use for new document, null if no template
     *     folderId - String?: Optional folder ID where document will be created/edited, null for root folder
     *     popUpTo - AppDestination?: Optional destination to pop back stack to, null to keep current stack
     *     inclusive - Boolean: Whether to include the popUpTo destination in the pop operation
     * 
     * return:
     *     Unit - No return value
     */
    fun openDocEditor(
        docId: String? = null,
        templateId: String? = null,
        folderId: String? = null,
        popUpTo: AppDestination? = null,
        inclusive: Boolean = false
    ) {
        navigate(AppDestination.DocEdit.build(docId, templateId, folderId), popUpTo, inclusive)
    }
    
    /**
     * Pops the current destination from the back stack, redirecting to home if previous destination is PIN screen.
     * 
     * Works by checking if the previous destination in the back stack is the PIN screen. If so, navigates to home
     * instead of popping, preventing users from returning to PIN screen via back button.
     * 
     * return:
     *     Unit - No return value
     */
    fun popBack() {
        val backStackEntry = navController.previousBackStackEntry
        val previousRoute = backStackEntry?.destination?.route
        if (previousRoute == AppDestination.Pin.route) {
            navController.navigate(AppDestination.Home.route) {
                popUpTo(AppDestination.Home.route) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        } else {
            navController.popBackStack()
        }
    }
    
    /**
     * Checks if it is safe to pop back from the current screen without returning to PIN screen.
     * 
     * Works by checking if the previous destination in the back stack exists and is not the PIN screen.
     * 
     * return:
     *     canPop - Boolean: True if it is safe to pop back (previous destination is not PIN), false otherwise
     */
    fun canPopBack(): Boolean {
        val backStackEntry = navController.previousBackStackEntry
        val previousRoute = backStackEntry?.destination?.route
        return previousRoute != null && previousRoute != AppDestination.Pin.route
    }
    
    /**
     * Safely pops the current destination from the back stack only if it won't return to PIN screen.
     * 
     * Works by checking if it's safe to pop back using canPopBack(), and only then performing the pop operation.
     * 
     * return:
     *     Unit - No return value
     */
    fun safePopBack() {
        if (canPopBack()) {
            navController.popBackStack()
        }
    }
    /**
     * Internal navigation method that performs the actual navigation with optional back stack manipulation.
     * 
     * Works by calling NavHostController.navigate() with the route and optional popUpTo configuration.
     * 
     * arguments:
     *     route - String: The destination route to navigate to
     *     popUpTo - AppDestination?: Optional destination to pop back stack to, null to keep current stack
     *     inclusive - Boolean: Whether to include the popUpTo destination in the pop operation
     * 
     * return:
     *     Unit - No return value
     */
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

/**
 * Composable function that remembers and provides an AppNavigator instance for the given NavHostController.
 * 
 * Works by using remember() to cache the AppNavigator instance, recreating it only when NavHostController changes.
 * 
 * arguments:
 *     navController - NavHostController: The navigation controller to wrap with AppNavigator
 * 
 * return:
 *     navigator - AppNavigator: The remembered AppNavigator instance
 */
@Composable
fun rememberAppNavigator(navController: NavHostController): AppNavigator = remember(navController) {
    AppNavigator(navController)
}

/**
 * Helper function that converts a nullable String to a non-null String for use in navigation arguments.
 * 
 * Works by returning the string if it's not null, or an empty string if it is null.
 * 
 * arguments:
 *     value - String?: The nullable string to convert
 * 
 * return:
 *     result - String: The non-null string value, empty string if input was null
 */
private fun String?.asNavArg(): String = this ?: ""
