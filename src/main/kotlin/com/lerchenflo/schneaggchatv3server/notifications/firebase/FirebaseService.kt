package com.lerchenflo.schneaggchatv3server.notifications.firebase

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.lerchenflo.schneaggchatv3server.core.security.JwtService
import com.lerchenflo.schneaggchatv3server.util.CryptoUtil
import com.lerchenflo.schneaggchatv3server.notifications.firebase.model.FirebaseToken
import com.lerchenflo.schneaggchatv3server.notifications.firebase.model.NotificationResponse
import com.lerchenflo.schneaggchatv3server.repository.FirebaseTokenRepository
import com.lerchenflo.schneaggchatv3server.user.UserLookupService
import com.lerchenflo.schneaggchatv3server.util.LogType
import com.lerchenflo.schneaggchatv3server.util.LoggingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.io.FileInputStream

@Service
class FirebaseService(
    private val tokenRepository: FirebaseTokenRepository,
    private val loggingService: LoggingService,
    private val userLookupService: UserLookupService,
    private val jwtService: JwtService
) {

    private val objectMapper = jacksonObjectMapper()


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
                //println("Firebase token exists, not updating")
                return
            }

            // Save new token
            tokenRepository.save(
                FirebaseToken(
                    userId = userId,
                    token = token
                )
            )

            //Save here if duplicate key exception occurs
            loggingService.log(
                userId = userId,
                logType = LogType.FIREBASE_TOKEN_REGISTERED,
            )

            //println("Firebasetoken saved successfully")
        } catch (e: DuplicateKeyException) {
            println("Firebase Duplicate key already exists")
        }
    }

    fun deleteToken(token: String) {
        tokenRepository.deleteByToken(token)
    }

    fun getTokensForUser(userId: ObjectId): List<FirebaseToken> {
        return tokenRepository.findAllByUserId(userId)
    }


    fun sendNewMessageNotificationToUser(
        userId: ObjectId,
        messageContent: String,
        msgId: String,
        groupMessage: Boolean,
        groupName: String? = null
    ) {
        val senderName = userLookupService.getUsername(userId)

        val tokens = getTokensForUser(userId)

        if (tokens.isEmpty()) {
            println("Firebase: no tokens for user $userId found")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val encodedContent = CryptoUtil.Companion.encrypt(messageContent, jwtService.getEncryptionKey())

                // Build the NotificationResponse and delegate to generic sender
                val notification = NotificationResponse.MessageNotificationResponse(
                    msgId = msgId,
                    senderName = senderName,
                    groupMessage = groupMessage,
                    groupName = groupName ?: "",
                    encodedContent = encodedContent
                )

                // Reuse the generic sender
                sendNotificationToUser(userId, notification)

            } catch (e: Exception) {
                println("Error in Firebase notification coroutine: ${e.message}")
                e.printStackTrace()
                loggingService.log(
                    userId = userId,
                    logType = LogType.EXCEPTION_THROWN,
                    message = "Firebase notification error: ${e.message}"
                )
            }
        }
    }


    fun sendFriendRequestNotificationToUser(
        senderId: ObjectId,
        receivingUserId: ObjectId,
    ) {
        val sendingUserName = userLookupService.getUsername(senderId)

        val tokens = getTokensForUser(receivingUserId)

        if (tokens.isEmpty()) {
            println("Firebase: no tokens for user $receivingUserId found")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {

                // Build the NotificationResponse and delegate to generic sender
                val notification = NotificationResponse.FriendRequestNotificationResponse(
                    requesterId = receivingUserId.toHexString(),
                    requesterName = sendingUserName,
                )


                // Reuse the generic sender
                sendNotificationToUser(receivingUserId, notification)

            } catch (e: Exception) {
                println("Error in Firebase notification coroutine: ${e.message}")
                e.printStackTrace()
                loggingService.log(
                    userId = senderId,
                    logType = LogType.EXCEPTION_THROWN,
                    message = "Firebase notification error: ${e.message}"
                )
            }
        }
    }



    fun sendNotificationToUser(userId: ObjectId, notification: NotificationResponse) {
        val tokens = getTokensForUser(userId)
        if (tokens.isEmpty()) {
            println("Firebase: no tokens for user $userId found")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dataMap = notificationResponseToDataMap(notification)

                tokens.forEach { firebaseToken ->
                    if (firebaseToken.token.isEmpty()) return@forEach
                    try {
                        val message = constructMessage(
                            firebaseToken = firebaseToken.token,
                            data = dataMap
                        )
                        safeSend(message = message, token = firebaseToken.token)
                    } catch (e: Exception) {
                        println("Error sending to token ${firebaseToken.token}: ${e.message}")
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                println("Error in Firebase notification coroutine: ${e.message}")
                e.printStackTrace()
                loggingService.log(
                    userId = userId,
                    logType = LogType.EXCEPTION_THROWN,
                    message = "Firebase notification error: ${e.message}"
                )
            }
        }
    }



    private fun safeSend(message: Message, token: String): Boolean {
        try {
            val response = FirebaseMessaging.getInstance().send(message)
            //println("Firebase send response: $response")
            return true

        } catch (e: FirebaseMessagingException) {
            // Use error codes instead of string comparison
            val errorCode = e.messagingErrorCode

            //println("[Firebase] Exception: Code=${errorCode}, Message=${e.message}")

            // Remove tokens only for permanent failures
            when (errorCode) {
                MessagingErrorCode.UNREGISTERED,
                MessagingErrorCode.INVALID_ARGUMENT,
                MessagingErrorCode.SENDER_ID_MISMATCH -> {
                    //println("[Firebase] Removing invalid token: $token (Code: $errorCode)")
                    deleteToken(token)
                    return false
                }

                // Transient errors - don't remove token
                MessagingErrorCode.UNAVAILABLE,
                MessagingErrorCode.INTERNAL,
                MessagingErrorCode.QUOTA_EXCEEDED -> {
                    println("[Firebase] Transient error for token: $token (Code: $errorCode)")
                    return false
                }

                // Other errors - log but don't remove token
                else -> {
                    println("[Firebase] Error sending to token: $token (Code: $errorCode)")
                    return false
                }
            }

        } catch (e: Exception) {
            // Catch-all for network, JSON, etc.
            println(
                "[Firebase] Unexpected exception: ${e.javaClass.simpleName}: ${e.message}"
            )
            e.printStackTrace()
            loggingService.log(
                userId = null,
                logType = LogType.EXCEPTION_THROWN
            )
            return false
        }
    }



    private fun notificationResponseToDataMap(notification: NotificationResponse): Map<String, String> {
        // convert to a raw Map<*,*>
        val raw = objectMapper.convertValue(notification, Map::class.java) as Map<String, Any?>? ?: emptyMap()

        val result = raw.mapValues { (_, v) ->
            when (v) {
                null -> ""
                is String -> v
                is Number, is Boolean -> v.toString()
                else -> objectMapper.writeValueAsString(v) // nested / complex objects -> JSON string
            }
        }.toMutableMap()

        // ensure "type" field (JsonTypeInfo property)
        val typeName = when (notification) {
            is NotificationResponse.MessageNotificationResponse -> "message"
            is NotificationResponse.FriendRequestNotificationResponse -> "friend_request"
            is NotificationResponse.SystemNotificationResponse -> "system"
        }
        result["type"] = typeName

        // cast to Map<String,String>
        return result.mapValues { it.value as String }
    }

    private fun constructMessage(firebaseToken: String, data: Map<String, String>) : Message {
        return Message.builder()
            .setToken(firebaseToken)
            .putAllData(data)
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH) //for immediate delivery: https://firebase.google.com/docs/cloud-messaging/android-message-priority?hl=de
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
                    .setAps(
                        Aps.builder()
                        .setContentAvailable(true) //Allow background work on ios
                        .build())
                    .build()
            )
            .build()
    }


}