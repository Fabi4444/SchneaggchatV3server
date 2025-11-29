package com.lerchenflo.schneaggchatv3server.message.messagemodel

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document

@Document("messages")
data class Message(
    val id: ObjectId = ObjectId.get()
)
