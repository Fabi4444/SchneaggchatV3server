@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.util

import com.lerchenflo.schneaggchatv3server.repository.LogRepository
import com.lerchenflo.schneaggchatv3server.repository.MessageRepository
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.stereotype.Service
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

enum class LogType {
    USER_LOGIN,
    SERVER_START,
    MESSAGE_DELETED,
    GROUP_CREATED,
    GROUP_DELETED,
    FIREBASE_TOKEN_REGISTERED,
    FRIEND_REQUEST_SENT,
    MESSAGE_SENT,
    EXCEPTION_THROWN
}

@Document("logs")
data class Log(
    @Id val id: ObjectId = ObjectId.get(),
    val userId: ObjectId?,
    val logType: LogType,
    val message : String? = null,
    val timestamp: Instant = Clock.System.now(),
)

@Service
class LoggingService(
    private val logRepository: LogRepository,
    private val messageRepository: MessageRepository,
) {

    init {
        log(
            userId = null,
            logType = LogType.SERVER_START,
        )
    }


    fun log(userId: ObjectId?, logType: LogType, message: String? = null) {
        logRepository.save(
            Log(
                userId = userId,
                logType = logType,
                message = message,
            )
        )
    }

    fun getStats() : Map<String, Long> {
        val stats = LogType.entries.associate { logType ->
            if (logType == LogType.MESSAGE_SENT){
                logType.name to messageRepository.count()
            }else {
                logType.name to logRepository.countByLogType(logType)
            }
        }

        return stats
    }
}