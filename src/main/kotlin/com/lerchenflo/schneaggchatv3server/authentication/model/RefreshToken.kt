@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.authentication.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Document("refreshTokens")
@CompoundIndex(name = "user_token_deleted", def = "{'userId': 1, 'hashedToken': 1, 'deletedAt': 1}", unique = true)
data class RefreshToken(
    @Id val id: ObjectId = ObjectId.get(),
    val userId: ObjectId,
    //@Indexed(unique = true)
    val hashedToken: String,

    @Indexed(expireAfter = "0s")
    val expiresAt: Instant,
    val createdAt: Instant = Clock.System.now(),

    var deletedAt: Instant? = null
    )
