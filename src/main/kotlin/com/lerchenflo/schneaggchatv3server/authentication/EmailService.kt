package com.lerchenflo.schneaggchatv3server.authentication

import com.lerchenflo.schneaggchatv3server.core.security.JwtService
import com.lerchenflo.schneaggchatv3server.user.usermodel.UserService
import org.bson.types.ObjectId
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Service
class EmailService(
    private val jwtService: JwtService,
    private val userService: UserService,
    private val mailSender: JavaMailSender
) {


    /**
     * Send a verification email to a client
     */
    fun sendVerificationEmail(userId: ObjectId, email: String) {

        if (userService.findByObjectId(userId)?.emailVerifiedAt != null) {
            return //Email already verified
        }

        val token = jwtService.generateEmailToken(userId.toHexString(), email)
        val verificationUrl = "https://schneaggchatv3.lerchenflo.eu/auth/verify_email?token=$token"

        val mail = SimpleMailMessage()
        mail.setTo(email)
        mail.subject = "Schneaggchat email verification"
        mail.text = "Click here to validate your email:\n$verificationUrl"
        try {
            mailSender.send(mail)
        } catch (e: Exception) {
            println("Mail not sent, error")
        }
    }

    /**
     * Client pressed on the link, verify
     */
    fun verifyEmailRequest(token: String) : Boolean {
        val (email, userId) = jwtService.validateEmailToken(token) ?: return false

        val user = userService.findByEmail(email) ?: return false

        if (user.id != userId) return false

        //TODO: Update user even if only the email got verified (All friends resync)?
        userService.save(user.copy(
            emailVerifiedAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        ))
        return true
    }



    /**
     * Send a delete account email to a client
     */
    fun sendDelAccEmail(userId: ObjectId, email: String) {


        val token = jwtService.generateDelAccEmailToken(userId.toHexString(), email)
        val verificationUrl = "https://schneaggchatv3.lerchenflo.eu/auth/delete_account?token=$token"

        val mail = SimpleMailMessage()
        mail.setTo(email)
        mail.subject = "Schneaggchat account deletion"
        mail.text = "Someone requested to delete your account. If this was not you, please ignore this email.\nIf you really want to delete your account, click the link below.\nYOUR ACCOUNT WILL BE DELETED IMMEDIATELY!:\n$verificationUrl"
        try {
            mailSender.send(mail)
        } catch (e: Exception) {
            println("Mail not sent, error")
        }
    }

    /**
     * Client pressed on the link, verify
     */
    fun verifyDelAccRequest(token: String) : Boolean {
        val (email, userId) = jwtService.validateDelAccEmailToken(token) ?: return false

        val user = userService.findByEmail(email) ?: return false

        if (user.id != userId) return false

        //TODO: Delete messages from this user and leave all groups
        println("Account with name ${user.username} has been deleted")
        //userService.deleteUser(user.id)
        return true
    }


}