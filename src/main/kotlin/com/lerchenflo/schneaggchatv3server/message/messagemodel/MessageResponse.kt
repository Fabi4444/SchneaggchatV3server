package com.lerchenflo.schneaggchatv3server.message.messagemodel

data class MessageResponse(
    val messageId: String, //Objectid
    val senderId: String,
    val receiverId: String,
    val groupMessage: Boolean,
    val msgType: MessageType,
    val content: String,
    val answerId: String?,

    val sendDate: Long,
    val lastChanged: Long,
    val deleted: Boolean,
    val readers: List<ReaderResponse>

)


data class ReaderResponse(
    val userId: String,
    val readAt: Long
)