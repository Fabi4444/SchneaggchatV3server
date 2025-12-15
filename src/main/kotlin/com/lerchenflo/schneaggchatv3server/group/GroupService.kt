@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.group

import com.lerchenflo.schneaggchatv3server.group.model.Group
import com.lerchenflo.schneaggchatv3server.group.model.GroupMember
import com.lerchenflo.schneaggchatv3server.repository.GroupMemberRepository
import com.lerchenflo.schneaggchatv3server.repository.GroupRepository
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Component
class GroupService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository
) {

    fun createGroup(groupName: String, members: List<ObjectId>, creatorId: ObjectId) {

        //TODO: Check groupname (Length etc)


        val currentTime = Clock.System.now()
        val group = groupRepository.save(
            Group(
                name = groupName,
                description = "",
                updatedAt = currentTime,
                createdAt = currentTime,
                creatorId = creatorId
            )
        )

        groupMemberRepository.saveAll(
            members.map { userId ->
                GroupMember(
                    userid = userId,
                    groupId = group.id,
                    joinedAt = currentTime,
                    isAdmin = (userId == creatorId)
                )
            }
        )

    }


    fun getUserGroupIds(userId: ObjectId): List<ObjectId> {
        return groupMemberRepository.findByuserid(userId)
            .map { it.groupId }
    }

}