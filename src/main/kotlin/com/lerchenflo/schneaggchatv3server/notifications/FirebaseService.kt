package com.lerchenflo.schneaggchatv3server.notifications

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import com.google.firebase.messaging.AndroidConfig.Priority
import com.lerchenflo.schneaggchatv3server.core.security.JwtService
import com.lerchenflo.schneaggchatv3server.notifications.model.FirebaseToken
import com.lerchenflo.schneaggchatv3server.repository.FirebaseTokenRepository
import com.lerchenflo.schneaggchatv3server.repository.RefreshTokenRepository
import com.lerchenflo.schneaggchatv3server.util.LogType
import com.lerchenflo.schneaggchatv3server.util.LoggingService
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.io.FileInputStream

@Service
class FirebaseService(
    private val tokenRepository: FirebaseTokenRepository,
    private val loggingService: LoggingService,
    private val jwtService: JwtService
) {

    init {
        run {
            val resourceName = "schneaggchatv3-firebase-admin.json"

            val credentialsStream = this::class.java.classLoader
                .getResourceAsStream(resourceName)
                ?: try {
                    // fallback to expected mounted path inside container
                    FileInputStream("/app/$resourceName")
                } catch (e: Exception) {
                    println("Firebase json not found in path /app/$resourceName")

                    return@run //Return to not crash the server docker
                }



            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                println("Firebase initialized successfully")
            } else {
                println("Firebase already initialized")
            }
        }

    }


    fun saveToken(userId: ObjectId, token: String) {
        try {
            // Check if token already exists for this user
            val existingToken = tokenRepository.findByUserIdAndToken(userId, token)

            if (existingToken != null) {
                return
            }

            loggingService.log(
                userId = userId,
                logType = LogType.FIREBASE_TOKEN_REGISTERED,
            )

            // Save new token
            tokenRepository.save(
                FirebaseToken(
                    userId = userId,
                    token = token
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteToken(token: String) {
        tokenRepository.deleteByToken(token)
    }

    fun getTokensForUser(userId: ObjectId): List<FirebaseToken> {
        return tokenRepository.findAllByUserId(userId)
    }




    private fun sendFirebaseMessageToAllDevices(userId: ObjectId, data: Map<String, String>): Int {
        val tokens = getTokensForUser(userId)

        if (tokens.isEmpty()) {
            println("No tokens for user $userId found")
            return 0
        }

        var successCount = 0
        tokens.forEach { firebaseToken ->
            val success = safeSend(
                message = constructMessage(firebaseToken.token, data),
                token = firebaseToken.token
            )
            if (success) {
                successCount++
            }
        }

        println("Sent message to $successCount/${tokens.size} devices for user $userId")
        return successCount
    }


    fun sendNewMessageNotificationToUser(userId: ObjectId, messagecontent: String, senderName: String, msgId: String) {
        val tokens = getTokensForUser(userId)

        if (tokens.isEmpty()) {
            println("Firebase: no tokens for user $userId found")
            return
        }

        val encodedContent = runBlocking {CryptoUtil.encrypt(messagecontent, jwtService.getEncryptionKey()) }

        println("Content encoding finished: $encodedContent")

        tokens.forEach { firebaseToken ->
            val message = constructMessage(
                firebaseToken = firebaseToken.token,
                data = mapOf(
                    "encodedContent" to encodedContent,
                    "senderName" to senderName,
                    "msgId" to msgId)
            )

            safeSend(
                message = message,
                token = firebaseToken.token
            )

            println("Firebase message to user $userId sent: $messagecontent")
        }

    }
    /*
    data class NotificationObject(
        val msgId: String,
        val senderName: String,
        val encodedcontent: String
    )

     */

    private fun safeSend(message: Message, token : String) : Boolean {
        try {
            val response = FirebaseMessaging.getInstance().send(message)
            println("Firebase send response: $response")
            return true

        } catch (e: Exception) {

            // If token is invalid, delete it
            if (e.message?.contains("invalid-registration-token") == true ||
                e.message?.contains("registration-token-not-registered") == true) {
                deleteToken(token)
                return false
            }
        }
        return false
    }

    private fun constructMessage(firebaseToken: String, data: Map<String, String>) : Message {
        return Message.builder()
            .setToken(firebaseToken)
            .putAllData(data)
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(Priority.HIGH) //for immediate delivery: https://firebase.google.com/docs/cloud-messaging/android-message-priority?hl=de
                    .build()
            )
            .setApnsConfig(
                /*
                Priority:
                Apple docs say 10 for immediate: //https://developer.apple.com/library/archive/documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/CommunicatingwithAPNs.html#//apple_ref/doc/uid/TP40008194-CH11-SW1
                Firebasee docs say 10 gets blocked: https://firebase.google.com/docs/cloud-messaging/customize-messages/setting-message-priority?hl=de
                 */
                ApnsConfig.builder()
                    .putHeader("apns-priority", "5")
                    .setAps(Aps.builder()
                        .setContentAvailable(true) //Allow background work on ios
                        .build())
                    .build()
            )
            .build()
    }


}
