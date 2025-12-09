@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.user.friendshipmodel

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Document("friendship_settings")
data class FriendshipSettings(
    @Id val id: ObjectId = ObjectId.get(),

    @Indexed val friendshipId: String,

    @Indexed val userId: String,


    //TODO: Fix fields
    var shareLocation: Boolean = false,
    var shareLastSeen: Boolean = false,
    var nickname: String? = null,
    var muted: Boolean = false,


    var createdAt: Instant = Clock.System.now(),
    var updatedAt: Instant = Clock.System.now()
)