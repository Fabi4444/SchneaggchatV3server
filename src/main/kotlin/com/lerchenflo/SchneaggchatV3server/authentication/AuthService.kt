@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.authentication

import com.lerchenflo.schneaggchatv3server.authentication.model.RefreshToken
import com.lerchenflo.schneaggchatv3server.core.security.HashEncoder
import com.lerchenflo.schneaggchatv3server.core.security.JwtService
import com.lerchenflo.schneaggchatv3server.repository.RefreshTokenRepository
import com.lerchenflo.schneaggchatv3server.repository.UserRepository
import com.lerchenflo.schneaggchatv3server.user.model.User
import com.lerchenflo.schneaggchatv3server.util.ImageManager
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.util.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Component
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val refreshTokenRepository: RefreshTokenRepository,

    private val imageManager: ImageManager
) {

    data class TokenPair(
        val accessToken: String,
        val refreshToken: String
    )

    //TODO: ADD PROFILEPICTURE
    fun register(username: String, password: String, email: String, birthdate: String, profilePic: MultipartFile) : User {

        val usernameexists = userRepository.findByUsername(username)
        if (usernameexists != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "A user with this username already exists")
        }

        val emailexists = userRepository.findByEmail(email.trim())
        if (emailexists != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists")
        }


        val now = Clock.System.now()

        val user = User(
            username = username,
            hashedPassword = hashEncoder.encode(password),
            email = email,
            profilePictureUrl = "",
            userDescription = "",
            userStatus = "",
            birthDate = birthdate,
            firebaseTokens = emptyList(),
            createdAt = now, //TODO: Fix return value
            updatedAt = now
        )

        val profilepicurl = imageManager.saveProfilePic(profilePic, user.id.toHexString())

        user.profilePictureUrl = profilepicurl

        return userRepository.save(user)
    }

    fun login(username: String, password: String) : TokenPair {
        //Does this user exist
        val user = userRepository.findByUsername(username) ?: throw BadCredentialsException("Invalid credentials")

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
            refreshToken = newRefreshToken
        )
    }


    @Transactional //Only apply db operations if all succeed
    fun refresh(refreshToken: String) : TokenPair {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401) ,"Invalid refresh token")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user = userRepository.findById(ObjectId(userId)).orElseThrow{
            throw ResponseStatusException(HttpStatusCode.valueOf(404) ,"Invalid refresh token")
        }

        val hashed = hashToken(refreshToken)
        refreshTokenRepository.findByUserIdAndHashedToken(user.id, hashed)
            ?: throw ResponseStatusException(HttpStatusCode.valueOf(401),"Refreshtoken not recognized (maybe used or expired)")

        println("Deleted refreshtokens: ${refreshTokenRepository.deleteByUserIdAndHashedToken(user.id, hashed)}")

        val newAccessToken = jwtService.generateAccessToken(userId)
        val newRefreshToken = jwtService.generateRefreshToken(userId)

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )

    }


    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Clock.System.now().toEpochMilliseconds() + expiryMs

        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                hashedToken = hashed,
                expiresAt = Instant.fromEpochMilliseconds(expiresAt),
            )
        )
    }

    private fun hashToken(token: String) : String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashedBytes)
    }


}