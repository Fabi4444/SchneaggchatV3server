package com.lerchenflo.schneaggchatv3server.message.messagemodel

data class MessageRequest(
    val messageId: String?, //Objectid

    val receiverId: String,
    val groupMessage: Boolean,
    val msgType: MessageType,
    val content: String,
    val answerId: String?,
    val encrypted: Boolean,
    )
