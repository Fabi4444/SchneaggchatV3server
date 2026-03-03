package com.lerchenflo.schneaggchatv3server.util

import org.springframework.web.multipart.MultipartFile
import java.util.Locale
import java.util.Locale.getDefault

object ValidationUtils {

    private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    private val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp")
    private const val MAX_IMAGE_SIZE = 3 * 1024 * 1024 // 3MB

    private val RESERVED_USERNAMES = setOf(
        "admin", "administrator", "root", "system", "api",
        "www", "mail", "ftp", "localhost", "test", "demo"
    )

    /**
     * Validates email address format
     * - Must follow standard email format
     * - Must not be empty or blank
     */
    fun validateEmail(email: String): Boolean {
        if (email.isBlank()) return false
        if (email.length > 254) return false // RFC 5321
        return EMAIL_REGEX.matches(email.trim())
    }

    /**
     * Validates password strength
     * - Minimum 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     */
    fun validatePassword(password: String): Boolean {
        if (password.length < 8) return false
        if (password.length > 128) return false // Reasonable upper limit

        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        return hasUpperCase && hasLowerCase && hasDigit && hasSpecial
    }

    /**
     * Validates username
     * - Length between 3 and 25 characters
     * - Only alphanumeric characters, underscores, and hyphens
     * - Must start with a letter or number
     * - Cannot end with underscore or hyphen
     */
    fun validateUsername(username: String): Boolean {
        if (username.isBlank()) return false
        if (username.length !in 3..25) return false

        // Must start with alphanumeric
        if (!username.first().isLetterOrDigit()) return false

        if (!username.last().isLetterOrDigit()) return false


        if (username.lowercase(getDefault()) in RESERVED_USERNAMES) return false

        // Only alphanumeric, underscore, and hyphen allowed
        val validChars = username.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' || it == ' ' }

        return validChars
    }

    /**
     * Validates uploaded profile picture
     * - File must not be empty
     * - Must be an image type (JPEG, PNG, GIF, WebP)
     * - Maximum size: 3MB
     * - Must have valid content type
     */
    fun validatePicture(picture: MultipartFile): Boolean {
        // Check if file is empty
        if (picture.isEmpty) return false

        // Check file size
        if (picture.size > MAX_IMAGE_SIZE) return false

        // Check content type
        val contentType = picture.contentType?.lowercase() ?: return false
        if (contentType !in ALLOWED_IMAGE_TYPES) return false

        // Check original filename exists and has valid extension
        val filename = picture.originalFilename ?: return false
        val extension = filename.substringAfterLast('.', "").lowercase()
        val validExtensions = setOf("jpg", "jpeg", "png", "gif", "webp")
        if (extension !in validExtensions) return false

        return true
    }

    fun validateDescription(string: String) : Boolean {
        if (string.length > 200) return false

        return true
    }

    fun validateStringMessage(string: String) : Boolean {
        if (string.length > 3000 || string.isEmpty()) return false

        return true
    }

    fun validatePollVoteText(text: String): Boolean {
        if (text.isBlank()) return false
        if (text.length > 250) return false

        return true
    }
}