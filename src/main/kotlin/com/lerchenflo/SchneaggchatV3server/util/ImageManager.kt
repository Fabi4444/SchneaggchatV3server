package com.lerchenflo.SchneaggchatV3server.util

import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.File

@Component
class ImageManager {
    private fun saveImageToFile(image: MultipartFile, fileName: String) {
        val imagesDir = File("/app/images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val targetFile = File(imagesDir, fileName)
        image.inputStream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    fun loadImageFromFile(fileName: String): ByteArray {
        val imagesDir = File("/app/images")
        val targetFile = File(imagesDir, fileName)

        if (!targetFile.exists()) {
            throw IllegalArgumentException("Image file not found: $fileName")
        }

        return targetFile.readBytes()
    }

    fun saveProfilePic(image: MultipartFile, userId: String, group: Boolean): String {
        val filename = getProfilePicFileName(userId, group)
        saveImageToFile(image, filename)
        return filename
    }

    fun getProfilePicFileName(userId: String, group: Boolean): String {
        return "${if (group) "group" else "user"}_${userId}_profilepic.jpg"
    }


    fun saveImageMessage(image: MultipartFile, messageId: String): String {
        val filename = getImageMessageFileName(messageId)
        saveImageToFile(image, filename)
        return filename
    }

    fun getImageMessageFileName(messageId: String): String {
        return "image_${messageId}_message.jpg"
    }
}