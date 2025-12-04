@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.message.messagemodel

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Document("messages")
data class Message(
    val id: ObjectId = ObjectId.get(),
    val senderId: ObjectId,
    val receiverId: ObjectId,
    val groupMessage: Boolean,
    val msgtype: MessageType,
    val content: String,
    val answerId: ObjectId?,

    val sendDate: Instant,
    val lastChanged: Instant,
    val deleted: Boolean = false,

    )

enum class MessageType {
    TEXT,
    IMAGE
}