package com.example.advancedaudiorecorder.preferences

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Environment

class AppPreferences(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

    var lastOpenedProject: String?
        get() = sharedPreferences.getString("lastOpenedProject", null)
        set(value) = sharedPreferences.edit().putString("lastOpenedProject", value).apply()

    var lastOpenedProjectFolder: Uri?
        get() {
            val uriString = sharedPreferences.getString("lastOpenedProjectFolder", null)
            return uriString?.let { Uri.parse(it) } // Возвращаем Uri только если строка не null
        }
        set(value) {
            sharedPreferences.edit().putString("lastOpenedProjectFolder", value?.toString()).apply()
        }

    var projectsDirectory: Uri
        get() {
            val uriString = sharedPreferences.getString(
                "projectsDirectory",
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.toURI().toString()
            )
            return Uri.parse(uriString)
        }
        set(value) {
            sharedPreferences.edit().putString("projectsDirectory", value.toString()).apply()
        }

    var metronomeVolume: Float
        get() = sharedPreferences.getFloat("metronomeVolume", 0.5f)
        set(value) = sharedPreferences.edit().putFloat("metronomeVolume", value).apply()

    fun clearPreferences() {
        sharedPreferences.edit().clear().apply()
    }
}