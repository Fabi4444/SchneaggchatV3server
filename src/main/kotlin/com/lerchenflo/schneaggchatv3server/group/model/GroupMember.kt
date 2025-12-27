@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.group.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Document("groupmembers")
data class GroupMember(
    //Id not needed

    @Indexed //Index for query on all user groups
    val userid: ObjectId,

    @Indexed //Index for query on all members for a group
    val groupId: ObjectId,
    val joinedAt: Instant,
    val admin: Boolean,
    val color: Int
)

fun GroupMember.toGroupMemberResponse(): GroupMemberResponse {
    return GroupMemberResponse(
        userid = userid.toHexString(),
        joinedAt = joinedAt.toEpochMilliseconds().toString(),
        admin = admin,
        color = color
    )
}