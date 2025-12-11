@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.user.usermodel

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Document("users")
data class User(
    @Id val id: ObjectId = ObjectId.get(),
    val username: String,
    val hashedPassword: String,

    val email: String,
    val emailVerifiedAt: Instant? = null,

    val userDescription: String,
    val userStatus: String,
    val birthDate: String,

    val createdAt: Instant,
    val updatedAt: Instant
)