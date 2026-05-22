package org.example.flow.notify

import java.io.File

/**
 * 从音效文件夹中随机选取一个 WAV 文件。
 * 与 EmojiPicker 对称设计，空路径 = 未配置 = 静音。
 */
object SoundPicker {

    /**
     * 从指定文件夹随机选一个 .wav 文件。
     * @param folderPath 文件夹路径，空字符串表示未配置
     * @return 随机 WAV 文件，文件夹为空或无有效文件时返回 null
     */
    fun pick(folderPath: String): File? {
        if (folderPath.isBlank()) {
            println("[SoundPicker] No folder configured, returning null")
            return null
        }

        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) {
            println("[SoundPicker] Folder not found: $folderPath")
            return null
        }

        val files = folder.listFiles { file ->
            file.isFile && file.extension.lowercase() == "wav"
        }

        if (files.isNullOrEmpty()) {
            println("[SoundPicker] No WAV files found in: $folderPath")
            return null
        }

        val chosen = files.random()
        println("[SoundPicker] Picked: ${chosen.name} (${files.size} files available)")

        return chosen
    }
}
