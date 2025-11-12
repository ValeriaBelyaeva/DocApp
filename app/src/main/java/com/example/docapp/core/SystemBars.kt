package com.example.docapp.core

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

fun View.setSystemBarColors(
    statusColor: Int,
    navColor: Int,
    darkIcons: Boolean
) {
    val window = (context as? Activity)?.window ?: return
    WindowCompat.setDecorFitsSystemWindows(window, false)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        @Suppress("DEPRECATION")
        window.statusBarColor = statusColor
        @Suppress("DEPRECATION")
        window.navigationBarColor = navColor
    }

    WindowInsetsControllerCompat(window, this).apply {
        isAppearanceLightStatusBars = darkIcons
        isAppearanceLightNavigationBars = darkIcons
    }
}

