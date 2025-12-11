package com.lerchenflo.schneaggchatv3server.notifications.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document("firebasetokens")
data class FirebaseToken(
    @Id val id: ObjectId = ObjectId.get(),

    @Indexed
    val userId: ObjectId,
    @Indexed(unique = true)
    val token: String,
)