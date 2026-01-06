@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.authentication

import com.lerchenflo.schneaggchatv3server.authentication.model.RefreshToken
import com.lerchenflo.schneaggchatv3server.core.security.HashEncoder
import com.lerchenflo.schneaggchatv3server.core.security.JwtService
import com.lerchenflo.schneaggchatv3server.repository.RefreshTokenRepository
import com.lerchenflo.schneaggchatv3server.user.usermodel.User
import com.lerchenflo.schneaggchatv3server.user.UserService
import com.lerchenflo.schneaggchatv3server.util.ImageManager
import com.lerchenflo.schneaggchatv3server.util.LogType
import com.lerchenflo.schneaggchatv3server.util.LoggingService
import com.lerchenflo.schneaggchatv3server.util.ValidationUtils
import org.bson.types.ObjectId
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.util.*
import java.util.Locale
import java.util.Locale.getDefault
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Service
class AuthService(
    private val jwtService: JwtService,
    private val userService: UserService,
    private val hashEncoder: HashEncoder,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val loggingService: LoggingService,
    private val imageManager: ImageManager
) {

    data class TokenPair(
        val accessToken: String,
        val refreshToken: String,
        val encryptionKey: String? = null
    )

    fun register(username: String, password: String, email: String, birthdate: String, profilePic: MultipartFile) : User {

        require(ValidationUtils.validateUsername(username)) { "Username invalid" }
        require(ValidationUtils.validatePassword(password)) { "Password invalid" }
        require(ValidationUtils.validateEmail(email)) { "Email invalid" }
        require(ValidationUtils.validatePicture(profilePic)) { "Picture invalid" }

        userService.checkExistingUser(username, email)

        val now = Clock.System.now()

        val user = User(
            username = username.trim().lowercase(getDefault()),
            hashedPassword = hashEncoder.encode(password),
            email = email,
            userDescription = "",
            userStatus = "",
            birthDate = birthdate,
            createdAt = now,
            updatedAt = now
        )

        //Save users profilepicture
        imageManager.saveProfilePic(
            image = profilePic,
            userId = user.id.toHexString(),
            group = false
        )

        return userService.save(user)
    }

    fun login(username: String, password: String) : TokenPair {
        //Does this user exist
        val user = userService.findByUsername(username) ?: throw BadCredentialsException("Invalid credentials")

        loggingService.log(
            userId = user.id,
            logType = LogType.USER_LOGIN
        )

        //Does the password match
        if (!hashEncoder.matches(password, user.hashedPassword)) {
            throw BadCredentialsException("Invalid credentials")
        }

        //Valid credentials entered
        val newAccessToken = jwtService.generateAccessToken(user.id.toHexString())
        val newRefreshToken = jwtService.generateRefreshToken(user.id.toHexString())

        storeRefreshToken(
            user.id,
            rawRefreshToken = newRefreshToken,
        )

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            encryptionKey = jwtService.getEncryptionKey()
        )
    }


    @Transactional //Only apply db operations if all succeed
    fun refresh(refreshToken: String) : TokenPair {

        //Is the token valid (Created from this server & Not changed & not expired)
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401) ,"Invalid refresh token")
        }

        //Get the userid contained in the token (User this token was issued to)
        val userId = jwtService.getUserIdFromToken(refreshToken)

        //find the user to the userid (If no user is found exception is thrown)
        val user = userService.findById(userId)
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401) ,"Invalid refresh token")

        val hashed = hashToken(refreshToken)

        //If there is no existing token saved exit
        val existingToken =
            refreshTokenRepository.findByUserIdAndHashedToken(user.id, hashed) ?: // Check if it was previously deleted
            //val deletedToken = refreshTokenRepository.findByUserIdAndHashedToken(user.id, hashed)
            //if (deletedToken?.deletedAt != null) {
            //    println("Attempted to reuse refresh token that was deleted at ${deletedToken.deletedAt} for user $userId")
            //}
            throw ResponseStatusException(
                HttpStatusCode.valueOf(401),
                "Refreshtoken not recognized (maybe used or expired)"
            )

        //generate new token
        val newAccessToken = jwtService.generateAccessToken(userId)
        val newRefreshToken = jwtService.generateRefreshToken(userId)

        // Soft delete old token
        existingToken.deletedAt = Clock.System.now()
        refreshTokenRepository.save(existingToken)
        //println("Refresh token for user $userId deleted at ${existingToken.deletedAt}")

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            encryptionKey = jwtService.getEncryptionKey()
        )
    }


    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Clock.System.now().toEpochMilliseconds() + expiryMs

        try {
            refreshTokenRepository.save(
                RefreshToken(
                    userId = userId,
                    hashedToken = hashed,
                    expiresAt = Instant.fromEpochMilliseconds(expiresAt),
                )
            )
        } catch (e: DuplicateKeyException) {
            //println("Error storing refresh token: duplicate")
        }
    }

    private fun hashToken(token: String) : String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashedBytes)
    }



}