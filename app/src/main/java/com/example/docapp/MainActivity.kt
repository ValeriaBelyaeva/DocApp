package com.example.docapp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.docapp.ui.App
import com.example.docapp.core.AppLogger
/**
 * Main activity that hosts the Compose UI and handles runtime permissions.
 * Sets up the main application UI and requests necessary permissions for file access.
 * 
 * Works by requesting storage permissions based on Android version, then setting up
 * the Compose UI with the main App composable.
 */
class MainActivity : ComponentActivity() {
    /**
     * Activity result launcher for requesting runtime permissions.
     * Handles the result of permission requests but currently does not enforce permission grants.
     * 
     * Works by registering a launcher that receives permission grant results when permissions are requested.
     * 
     * arguments:
     *     permissions - Map<String, Boolean>: Map of permission names to their grant status
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
        }
    }
    
    /**
     * Initializes the activity, requests necessary permissions, and sets up the Compose UI.
     * Called by Android system when the activity is created.
     * 
     * Works by requesting storage permissions (based on Android version), then setting the Compose content
     * to the main App composable. Logs activity lifecycle events.
     * 
     * arguments:
     *     savedInstanceState - Bundle?: Saved instance state from previous activity state, null for first creation
     * 
     * return:
     *     Unit - No return value
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.log("MainActivity", "MainActivity onCreate started")
        AppLogger.log("MainActivity", "MainActivity initialized")
        requestNecessaryPermissions()
        AppLogger.log("MainActivity", "Setting Compose content...")
        setContent { App() }
        AppLogger.log("MainActivity", "MainActivity onCreate completed")
    }
    /**
     * Called when the activity comes to the foreground and becomes interactive.
     * Logs the resume event for debugging purposes.
     * 
     * return:
     *     Unit - No return value
     */
    override fun onResume() {
        super.onResume()
        AppLogger.log("MainActivity", "MainActivity resumed")
    }
    
    /**
     * Called when the activity goes to the background and is no longer interactive.
     * Logs the pause event for debugging purposes.
     * 
     * return:
     *     Unit - No return value
     */
    override fun onPause() {
        super.onPause()
        AppLogger.log("MainActivity", "MainActivity paused")
    }
    
    /**
     * Requests necessary runtime permissions for file access based on Android version.
     * Requests READ_EXTERNAL_STORAGE for Android < 13, or READ_MEDIA_IMAGES for Android 13+.
     * 
     * Works by checking which permissions are needed based on Android SDK version, then launching
     * the permission request launcher if any permissions are missing.
     * 
     * return:
     *     Unit - No return value
     */
    private fun requestNecessaryPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
