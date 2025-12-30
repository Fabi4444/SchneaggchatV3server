package com.lerchenflo.schneaggchatv3server

import com.lerchenflo.schneaggchatv3server.util.ValidationUtils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.mock.web.MockMultipartFile

@DisplayName("ValidationUtils Tests")
class ValidationUtilsTest {

    // ===== EMAIL VALIDATION TESTS =====

    @Test
    @DisplayName("Valid email should pass validation")
    fun `validateEmail should return true for valid emails`() {
        assertTrue(ValidationUtils.validateEmail("user@example.com"))
        assertTrue(ValidationUtils.validateEmail("test.user@domain.co.uk"))
        assertTrue(ValidationUtils.validateEmail("user+tag@example.com"))
        assertTrue(ValidationUtils.validateEmail("user_name@example.com"))
        assertTrue(ValidationUtils.validateEmail("123@example.com"))
    }

    @Test
    @DisplayName("Invalid email should fail validation")
    fun `validateEmail should return false for invalid emails`() {
        assertFalse(ValidationUtils.validateEmail(""))
        assertFalse(ValidationUtils.validateEmail("   "))
        assertFalse(ValidationUtils.validateEmail("notanemail"))
        assertFalse(ValidationUtils.validateEmail("@example.com"))
        assertFalse(ValidationUtils.validateEmail("user@"))
        assertFalse(ValidationUtils.validateEmail("user@.com"))
        assertFalse(ValidationUtils.validateEmail("user name@example.com"))
    }

    @Test
    @DisplayName("Email exceeding max length should fail")
    fun `validateEmail should return false for emails longer than 254 characters`() {
        val longEmail = "a".repeat(250) + "@example.com"
        assertFalse(ValidationUtils.validateEmail(longEmail))
    }

    // ===== PASSWORD VALIDATION TESTS =====

    @Test
    @DisplayName("Valid password should pass validation")
    fun `validatePassword should return true for valid passwords`() {
        assertTrue(ValidationUtils.validatePassword("Password123!"))
        assertTrue(ValidationUtils.validatePassword("Str0ng!Pass"))
        assertTrue(ValidationUtils.validatePassword("MyP@ssw0rd"))
        assertTrue(ValidationUtils.validatePassword("Abcdef1!"))
    }

    @Test
    @DisplayName("Password missing requirements should fail")
    fun `validatePassword should return false for weak passwords`() {
        assertFalse(ValidationUtils.validatePassword("short1!")) // Too short
        assertFalse(ValidationUtils.validatePassword("password123!")) // No uppercase
        assertFalse(ValidationUtils.validatePassword("PASSWORD123!")) // No lowercase
        assertFalse(ValidationUtils.validatePassword("Password!")) // No digit
        assertFalse(ValidationUtils.validatePassword("Password123")) // No special char
        assertFalse(ValidationUtils.validatePassword("")) // Empty
    }

    @Test
    @DisplayName("Password exceeding max length should fail")
    fun `validatePassword should return false for passwords longer than 128 characters`() {
        val longPassword = "A1!" + "a".repeat(130)
        assertFalse(ValidationUtils.validatePassword(longPassword))
    }

    // ===== USERNAME VALIDATION TESTS =====

    @Test
    @DisplayName("Valid username should pass validation")
    fun `validateUsername should return true for valid usernames`() {
        assertTrue(ValidationUtils.validateUsername("john_doe"))
        assertTrue(ValidationUtils.validateUsername("user123"))
        assertTrue(ValidationUtils.validateUsername("test-user"))
        assertTrue(ValidationUtils.validateUsername("abcd"))
        assertTrue(ValidationUtils.validateUsername("User_Name-123"))
    }

    @Test
    @DisplayName("Invalid username should fail validation")
    fun `validateUsername should return false for invalid usernames`() {
        assertFalse(ValidationUtils.validateUsername("")) // Empty
        assertFalse(ValidationUtils.validateUsername("ab")) // Too short
        assertFalse(ValidationUtils.validateUsername("a".repeat(26))) // Too long
        assertFalse(ValidationUtils.validateUsername("_username")) // Starts with underscore
        assertFalse(ValidationUtils.validateUsername("-username")) // Starts with hyphen
        assertFalse(ValidationUtils.validateUsername("username_")) // Ends with underscore
        assertFalse(ValidationUtils.validateUsername("username-")) // Ends with hyphen
        assertFalse(ValidationUtils.validateUsername("user name")) // Contains space
        assertFalse(ValidationUtils.validateUsername("user@name")) // Contains special char
    }

    // ===== PICTURE VALIDATION TESTS =====

    @Test
    @DisplayName("Valid image should pass validation")
    fun `validatePicture should return true for valid images`() {
        val jpegFile = MockMultipartFile("file", "image.jpg", "image/jpeg", ByteArray(1024))
        val pngFile = MockMultipartFile("file", "image.png", "image/png", ByteArray(1024))
        val gifFile = MockMultipartFile("file", "image.gif", "image/gif", ByteArray(1024))
        val webpFile = MockMultipartFile("file", "image.webp", "image/webp", ByteArray(1024))

        assertTrue(ValidationUtils.validatePicture(jpegFile))
        assertTrue(ValidationUtils.validatePicture(pngFile))
        assertTrue(ValidationUtils.validatePicture(gifFile))
        assertTrue(ValidationUtils.validatePicture(webpFile))
    }

    @Test
    @DisplayName("Empty file should fail validation")
    fun `validatePicture should return false for empty files`() {
        val emptyFile = MockMultipartFile("file", "image.jpg", "image/jpeg", ByteArray(0))
        assertFalse(ValidationUtils.validatePicture(emptyFile))
    }

    @Test
    @DisplayName("File exceeding size limit should fail")
    fun `validatePicture should return false for files larger than 5MB`() {
        val largeFile = MockMultipartFile("file", "large.jpg", "image/jpeg", ByteArray(6 * 1024 * 1024))
        assertFalse(ValidationUtils.validatePicture(largeFile))
    }

    @Test
    @DisplayName("Invalid content type should fail")
    fun `validatePicture should return false for invalid content types`() {
        val pdfFile = MockMultipartFile("file", "document.pdf", "application/pdf", ByteArray(1024))
        val textFile = MockMultipartFile("file", "file.txt", "text/plain", ByteArray(1024))

        assertFalse(ValidationUtils.validatePicture(pdfFile))
        assertFalse(ValidationUtils.validatePicture(textFile))
    }

    @Test
    @DisplayName("Invalid file extension should fail")
    fun `validatePicture should return false for invalid extensions`() {
        val invalidExt = MockMultipartFile("file", "image.bmp", "image/jpeg", ByteArray(1024))
        assertFalse(ValidationUtils.validatePicture(invalidExt))
    }

    @Test
    @DisplayName("File with no extension should fail")
    fun `validatePicture should return false for files without extension`() {
        val noExt = MockMultipartFile("file", "image", "image/jpeg", ByteArray(1024))
        assertFalse(ValidationUtils.validatePicture(noExt))
    }

    @Test
    @DisplayName("File with null content type should fail")
    fun `validatePicture should return false for null content type`() {
        val nullContentType = MockMultipartFile("file", "image.jpg", null, ByteArray(1024))
        assertFalse(ValidationUtils.validatePicture(nullContentType))
    }
}