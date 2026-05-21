package org.example.flow.notify

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image as SkImage
import java.io.File

/**
 * 从表情包文件夹中随机选取图片。
 * 支持 .jpg .jpeg .png .jfif 格式。
 */
object EmojiPicker {

    private val extensions = setOf("jpg", "jpeg", "png", "jfif")

    /**
     * 从指定文件夹随机选一张图片并加载为 Compose ImageBitmap。
     * @param folderPath 文件夹路径，空字符串表示未配置
     * @return 随机图片，文件夹为空或无有效文件时返回 null
     */
    fun pick(folderPath: String): ImageBitmap? {
        if (folderPath.isBlank()) {
            println("[EmojiPicker] No folder configured, returning null")
            return null
        }

        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) {
            println("[EmojiPicker] Folder not found: $folderPath")
            return null
        }

        val files = folder.listFiles { file ->
            file.isFile && file.extension.lowercase() in extensions
        }

        if (files.isNullOrEmpty()) {
            println("[EmojiPicker] No image files found in: $folderPath")
            return null
        }

        val chosen = files.random()
        println("[EmojiPicker] Picked: ${chosen.name} (${files.size} files available)")

        return try {
            val bytes = chosen.readBytes()
            val skImage = SkImage.makeFromEncoded(bytes)
            skImage.toComposeImageBitmap()
        } catch (e: Exception) {
            println("[EmojiPicker] Failed to load ${chosen.name}: ${e.message}")
            null
        }
    }
}
