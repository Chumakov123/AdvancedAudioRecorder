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

    var projectsDirectory: Uri
        get() {
            val uriString = sharedPreferences.getString(
                "projectsDirectory",
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.toString()
            )
            return Uri.parse(uriString)
        }
        set(value) {
            sharedPreferences.edit().putString("projectsDirectory", value.toString()).apply()
        }

    var metronomeVolume: Float
        get() = sharedPreferences.getFloat("metronomeVolume", 1.0f) // Default volume is 50%
        set(value) = sharedPreferences.edit().putFloat("metronomeVolume", value).apply()
}