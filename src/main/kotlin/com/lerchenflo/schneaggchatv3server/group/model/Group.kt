@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.group.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Document("groups")
data class Group(
    @Id val id: ObjectId = ObjectId.get(),
    val name: String,
    val description: String,

    val updatedAt: Instant,

    val createdAt: Instant,
    val creatorId: ObjectId

)

fun Group.toGroupResponse(members: List<GroupMember>): GroupResponse {
    return GroupResponse(
        id = id.toHexString(),
        name = name,
        description = description,
        updatedAt = updatedAt.toEpochMilliseconds().toString(),
        createdAt = createdAt.toEpochMilliseconds().toString(),
        creatorId = creatorId.toHexString(),
        members = members.map {
            it.toGroupMemberResponse()
        }
    )
}
