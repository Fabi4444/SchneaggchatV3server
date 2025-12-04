package com.lerchenflo.schneaggchatv3server.message.messagemodel

import org.bson.types.ObjectId
import kotlin.time.Instant

data class MessageRequest(
    val messageId: String?, //Objectid

    val receiverId: String,
    val groupMessage: Boolean,
    val msgtype: MessageType,
    val content: String,
    val answerId: String?,
)
