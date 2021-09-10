package com.horselinc.utils

import com.horselinc.App
import java.io.File

object StorageUtil {
    fun getAppExternalDataDirectoryPath (): String {
        val sb = StringBuilder()
        sb.append(App.instance.applicationContext.getExternalFilesDir(App.instance.packageName))
            .append(File.separator)

        return sb.toString()
    }

    fun getAppExternalDataDirectoryPathForCache (): String {
        val sb = StringBuilder()
        sb.append(App.instance.applicationContext.getExternalFilesDir(App.instance.packageName))
                .append(File.separator)
                .append("cache")
                .append(File.separator)

        return sb.toString()
    }

    fun getAppExternalDataDirectoryFileForCache (): File {
        val dataDirectoryFile = File(getAppExternalDataDirectoryPathForCache())
        if (dataDirectoryFile.mkdirs()) {
        }

        return dataDirectoryFile
    }

    fun clearAppExternalDataDirectoryFileForCache(): File {
        val dataDirectoryFile = File(getAppExternalDataDirectoryPathForCache())
        dataDirectoryFile.mkdirs()

        if (dataDirectoryFile.isDirectory) {
            val children = dataDirectoryFile.list()
            for (i in children!!.indices) {
                File(dataDirectoryFile, children[i]).delete()
            }
        }

        return dataDirectoryFile
    }

    fun getAppExternalDataDirectoryFile(): File {
        val dataDirectoryFile = File(getAppExternalDataDirectoryPath())
        dataDirectoryFile.mkdirs()

        return dataDirectoryFile
    }
}