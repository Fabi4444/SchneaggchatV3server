package com.lerchenflo.SchneaggchatV3server.authentication

import com.lerchenflo.SchneaggchatV3server.core.security.JwtService
import com.lerchenflo.SchneaggchatV3server.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.stereotype.Service
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Service
class EmailService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val mailSender: JavaMailSender
) {


    /**
     * Send a verification email to a client
     */
    fun sendVerificationEmail(userId: ObjectId, email: String) {


        val token = jwtService.generateEmailToken(userId.toHexString(), email)
        val verificationUrl = "https://schneaggchat.lerchenflo.eu/auth/verify_email?token=$token"

        val mail = SimpleMailMessage()
        mail.setTo(email)
        mail.subject = "Schneaggchat email verification"
        mail.text = "Click here to validate your email:\n$verificationUrl"
        mailSender.send(mail)
    }

    /**
     * Client pressed on the link, verify
     */
    fun verifyEmailRequest(token: String) : Boolean {
        //TODO: Check if token still valid
        val (email, userId) = jwtService.validateEmailToken(token) ?: return false

        val user = userRepository.findByEmail(email) ?: return false

        if (user.id != userId) return false

        //TODO: Update user even if only the email got verified (All friends resync)
        userRepository.save(user.copy(
            emailVerifiedAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        ))
        return true
    }


}