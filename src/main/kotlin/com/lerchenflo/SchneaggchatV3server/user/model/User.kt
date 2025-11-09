@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.user.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Document("users")
data class User(
    @Id val id: ObjectId = ObjectId.get(),
    val name: String,
    val hashedPassword: String,
    val email: String,
    val profilePicture: String?,
    val userDescription: String,
    val userStatus: String,

    val firebaseTokens: List<String>,

    val createdAt: Instant,
    val updatedAt: Instant
)