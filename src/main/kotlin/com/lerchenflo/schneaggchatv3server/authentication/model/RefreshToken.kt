@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.authentication.model

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Document("refreshTokens")
data class RefreshToken(
    val userId: ObjectId,
    val hashedToken: String,

    @Indexed(expireAfter = "0s")
    val expiresAt: Instant,
    val createdAt: Instant = Clock.System.now()
    )
