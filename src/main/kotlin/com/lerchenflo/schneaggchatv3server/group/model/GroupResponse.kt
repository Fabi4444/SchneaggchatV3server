package com.lerchenflo.schneaggchatv3server.group.model

import org.bson.types.ObjectId

data class GroupResponse(
    val id: String,
    val name: String,
    val description: String,

    val updatedAt: String,

    val createdAt: String,
    val creatorId: String,
    val members: List<GroupMemberResponse>
)

data class GroupMemberResponse(
    val userid: String,
    val joinedAt: String,
    val admin: Boolean
)
