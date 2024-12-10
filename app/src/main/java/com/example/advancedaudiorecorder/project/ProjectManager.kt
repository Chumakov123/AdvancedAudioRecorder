package com.example.advancedaudiorecorder.project

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.advancedaudiorecorder.model.ProjectData
import com.example.advancedaudiorecorder.utils.FileUtils
import com.example.advancedaudiorecorder.utils.LogUtils
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProjectManager(
    private val context: Context
) {
    private val projectFileName = "project"
    //region Project operations
    fun saveProject(projectFolder: DocumentFile, project: ProjectData) {
        try {
            val json = Json.encodeToString(project)

            // Если файл уже существует, удаляем его
            projectFolder.findFile("$projectFileName.json")?.delete()

            // Создаем новый файл
            val jsonFile = projectFolder.createFile("application/json", projectFileName)
                ?: throw IllegalStateException("Не удалось создать JSON-файл")

            // Записываем данные в файл
            context.contentResolver.openOutputStream(jsonFile.uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            }
        } catch (e: Exception) {
            LogUtils.showError(context, "Ошибка при создании проекта: ${e.message}", e)
        }
    }
    fun saveProject(uri: Uri, project: ProjectData) {
        val projectFolder = FileUtils.getDirectory(context, uri)
        if (projectFolder != null) {
            saveProject(projectFolder, project)
        }
    }
    fun deleteProject(title: String) {
        //TODO удаление проекта
        // Нужна ли эта функция?
    }
    fun renameProject(title: String, newTitle: String) {
        //TODO переименование существующего проекта
        // Нужна ли эта функция?
    }

    fun loadProject(uri: Uri) : ProjectData? {
        try {
            val projectFolder = FileUtils.getDirectory(context, uri)
                ?: throw IllegalStateException("Не удалось получить директорию проекта")

            //val jsonFile = FileUtils.searchFile(projectFolder, "$projectFileName.json")
            val jsonFile = FileUtils.findFileInDirectory(projectFolder, "$projectFileName.json")
                ?: throw IllegalStateException("JSON-файл проекта не найден")

            val project = FileUtils.readJson<ProjectData>(context, jsonFile)
                ?: throw IllegalStateException("Не удалось прочитать данные проекта")

            return project
        } catch (e: Exception) {
            LogUtils.showError(context, "Ошибка при загрузке проекта: ${e.message}", e)
        }
        return null
    }
    //endregion
}