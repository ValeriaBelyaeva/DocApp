package com.example.docapp.core
import android.content.Context
import android.content.SharedPreferences
object FolderStateStore {
    private const val PREFS_NAME = "folder_state_prefs"
    private const val KEY_COLLAPSED = "collapsed_folders"
    private lateinit var prefs: SharedPreferences
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    fun getCollapsedFolders(): Set<String> {
        return if (::prefs.isInitialized) {
            prefs.getStringSet(KEY_COLLAPSED, emptySet())?.toSet() ?: emptySet()
        } else {
            emptySet()
        }
    }
    fun saveCollapsedFolders(ids: Set<String>) {
        if (!::prefs.isInitialized) return
        prefs.edit().putStringSet(KEY_COLLAPSED, ids.toSet()).apply()
    }
}
