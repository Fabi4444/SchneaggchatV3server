package com.lerchenflo.schneaggchatv3server.notifications

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import com.google.firebase.messaging.AndroidConfig.Priority
import com.lerchenflo.schneaggchatv3server.notifications.model.FirebaseToken
import com.lerchenflo.schneaggchatv3server.repository.FirebaseTokenRepository
import com.lerchenflo.schneaggchatv3server.util.LogType
import com.lerchenflo.schneaggchatv3server.util.LoggingService
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.io.FileInputStream

@Service
class FirebaseService(
    private val tokenRepository: FirebaseTokenRepository,
    private val loggingService: LoggingService
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




    private fun sendFirebaseMessage(userId: ObjectId, data: Map<String, String>): Int {
        val tokens = getTokensForUser(userId)

        if (tokens.isEmpty()) {
            println("No tokens for user $userId found")
            return 0
        }

        var successCount = 0
        tokens.forEach { firebaseToken ->
            try {
                val message = Message.builder()
                    .setToken(firebaseToken.token)
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

                val response = FirebaseMessaging.getInstance().send(message)
                println("Successfully sent message to user $userId: $response")
                successCount++
            } catch (e: Exception) {
                println("Failed to send message to token ${firebaseToken.token}: $e")

                // If token is invalid, delete it
                if (e.message?.contains("invalid-registration-token") == true ||
                    e.message?.contains("registration-token-not-registered") == true) {
                    deleteToken(firebaseToken.token)
                }
            }
        }

        println("Sent message to $successCount/${tokens.size} devices for user $userId")
        return successCount
    }


    fun sendMessageToUser(userId: ObjectId, message: String): Int {
        return sendFirebaseMessage(
            userId = userId,
            data = mapOf("message" to message)
        )
    }



}