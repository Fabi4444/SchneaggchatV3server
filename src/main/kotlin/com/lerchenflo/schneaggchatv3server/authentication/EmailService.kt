package com.lerchenflo.schneaggchatv3server.authentication

import com.lerchenflo.schneaggchatv3server.core.security.JwtService
import com.lerchenflo.schneaggchatv3server.repository.GroupMemberRepository
import com.lerchenflo.schneaggchatv3server.repository.RefreshTokenRepository
import com.lerchenflo.schneaggchatv3server.user.UserLookupService
import com.lerchenflo.schneaggchatv3server.user.UserService
import com.lerchenflo.schneaggchatv3server.util.LogType
import com.lerchenflo.schneaggchatv3server.util.LoggingService
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Service
class EmailService(
    private val jwtService: JwtService,
    private val userService: UserService,
    private val userLookupService: UserLookupService,
    private val mailSender: JavaMailSender,
    private val loggingService: LoggingService,
    private val refreshTokenRepository: RefreshTokenRepository,
) {


    /**
     * Send a verification email to a client
     */
    fun sendVerificationEmail(userId: ObjectId) {

        val user = userLookupService.findByObjectId(userId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User $userId not found")

        if (user.emailVerifiedAt != null) {
            return //Email already verified
        }

        val lastemailsenttimestamp = getLastEmailTimestamp(userId, LogType.EMAIL_VERIFICATION_EMAIL_SENT)
        if (lastemailsenttimestamp != null &&
            (lastemailsenttimestamp.plus(Duration.parse("5m")) > Clock.System.now())) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "You need to wait 5 minutes before sending the next mail")
        }

        val token = jwtService.generateEmailToken(userId.toHexString(), user.email)
        val verificationUrl = "https://schneaggchatv3.lerchenflo.eu/auth/verify_email?token=$token"

        val mail = SimpleMailMessage()
        mail.setTo(user.email)
        mail.subject = "Schneaggchat email verification"
        mail.text = "Click here to validate your email:\n$verificationUrl"
        try {
            mailSender.send(mail)
            loggingService.log(userId, LogType.EMAIL_VERIFICATION_EMAIL_SENT)
        } catch (e: Exception) {
            println("Mail not sent, error")
        }
    }

    /**
     * Client pressed on the link, verify
     */
    fun verifyEmailRequest(token: String) : Boolean {
        val (email, userId) = jwtService.validateEmailToken(token) ?: return false

        val user = userLookupService.findByEmail(email) ?: return false

        if (user.id != userId) return false

        //TODO: Update user even if only the email got verified (All friends resync)?
        userLookupService.save(user.copy(
            emailVerifiedAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        ))
        return true
    }



    /**
     * Send a delete account email to a client
     */
    fun sendDelAccEmail(userId: ObjectId, email: String) {

        val lastemailsenttimestamp = getLastEmailTimestamp(userId, LogType.ACCOUNT_DELETION_EMAIL_SENT)
        if (lastemailsenttimestamp != null &&
            (lastemailsenttimestamp.plus(Duration.parse("15m")) > Clock.System.now())) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "You need to wait 15 minutes before sending the next mail")
        }

        val token = jwtService.generateDelAccEmailToken(userId.toHexString(), email)
        val verificationUrl = "https://schneaggchatv3.lerchenflo.eu/auth/delete_account?token=$token"

        val mail = SimpleMailMessage()
        mail.setTo(email)
        mail.subject = "Schneaggchat account deletion"
        mail.text = "Someone requested to delete your account. If this was not you, please ignore this email.\nIf you really want to delete your account, click the link below.\nYOUR ACCOUNT WILL BE DELETED IMMEDIATELY!:\n$verificationUrl"
        try {
            mailSender.send(mail)
            loggingService.log(userId, LogType.ACCOUNT_DELETION_EMAIL_SENT)
        } catch (e: Exception) {
            println("Mail not sent, error")
        }
    }

    /**
     * Client pressed on the link, verify
     */
    fun verifyDelAccRequest(token: String) : Boolean {
        val (email, userId) = jwtService.validateDelAccEmailToken(token) ?: return false

        val user = userLookupService.findByEmail(email) ?: return false

        if (user.id != userId) return false

        //TODO: Delete messages from this user and leave all groups
        refreshTokenRepository.deleteByUserId(user.id)
        println("Account with name ${user.username} has been deleted")
        //userService.deleteUser(user.id)
        return true
    }


    fun getLastEmailTimestamp(userId: ObjectId, logType: LogType) : Instant? {
        return loggingService.getLastLogByLogtype(logType = logType, userId = userId)?.timestamp
    }

}