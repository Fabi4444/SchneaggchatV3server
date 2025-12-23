@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.group

import com.lerchenflo.schneaggchatv3server.group.model.Group
import com.lerchenflo.schneaggchatv3server.group.model.GroupMember
import com.lerchenflo.schneaggchatv3server.group.model.GroupResponse
import com.lerchenflo.schneaggchatv3server.group.model.toGroupMemberResponse
import com.lerchenflo.schneaggchatv3server.group.model.toGroupResponse
import com.lerchenflo.schneaggchatv3server.repository.GroupMemberRepository
import com.lerchenflo.schneaggchatv3server.repository.GroupRepository
import com.lerchenflo.schneaggchatv3server.user.UserController
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Component
class GroupService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository
) {

    fun createGroup(groupName: String, members: List<ObjectId>, creatorId: ObjectId, description: String, profilePic: MultipartFile) : Group {

        //TODO: Friendships check
        require(members.size > 2) { "A group must have at least 3 members" }
        require(groupName.length < 25) { "Groupname too long"}

        val currentTime = Clock.System.now()
        val group = groupRepository.save(
            Group(
                name = groupName.trim(),
                description = "",
                updatedAt = currentTime,
                createdAt = currentTime,
                creatorId = creatorId
            )
        )

        //Try to add creator (Set prevents duplicate members)
        val membersInternal: Set<ObjectId> = members.toSet() + creatorId

        groupMemberRepository.saveAll(
            membersInternal.map { userId ->
                GroupMember(
                    userid = userId,
                    groupId = group.id,
                    joinedAt = currentTime,
                    isAdmin = (userId == creatorId)
                )
            }
        )

        return group
    }


    fun getUserGroupIds(userId: ObjectId): List<ObjectId> {
        return groupMemberRepository.findByuserid(userId)
            .map { it.groupId }
    }

    fun getUserGroupIdsLastchanged(userId: ObjectId): List<UserController.IdTimeStamp> {
        val usergroups = getUserGroupIds(userId)

        return groupRepository.findAllById(usergroups).map { group ->
            UserController.IdTimeStamp(
                id = group.id.toHexString(),
                timeStamp = group.updatedAt.toEpochMilliseconds().toString()
            )
        }
    }
    
    fun getGroupAsGroupResponse(groupId: ObjectId): GroupResponse {

        val group = groupRepository.findById(groupId).get()
        val members = getGroupMembers(groupId)

        return GroupResponse(
            id = group.id.toHexString(),
            name = group.name,
            description = group.description,
            updatedAt = group.updatedAt.toEpochMilliseconds().toString(),
            createdAt = group.createdAt.toEpochMilliseconds().toString(),
            creatorId = group.creatorId.toHexString(),
            members = members.map { it.toGroupMemberResponse() }
        )
    }

    fun getGroupMembers(groupId: ObjectId): List<GroupMember> {
        return groupMemberRepository.findAllByGroupId(groupId)
    }

    data class GroupSyncResponse(
        val updatedGroups: List<GroupResponse>,
        val deletedGroups: List<String>
    )

    fun syncGroups(userId: String, ids: List<UserController.IdTimeStamp>): GroupSyncResponse {
        // Groups which the client has on their device
        val clientGroups = ids.associate {
            it.id to it.timeStamp
        }

        // All groups this user is part of on the server
        val serverGroups = getUserGroupIdsLastchanged(ObjectId(userId)).associate {
            it.id to it.timeStamp
        }

        // Find groups that need to be added (client doesn't have) or updated (server is newer)
        val groupsToSyncIds = serverGroups.filter { (groupId, serverTs) ->
            val clientTs = clientGroups[groupId]
            clientTs == null || serverTs > clientTs
        }.keys

        // Find groups that the client has but the server doesn't (should be removed from client)
        val serverGroupIds = serverGroups.keys
        val deletedGroups = clientGroups.keys.filter { it !in serverGroupIds }

        return GroupSyncResponse(
            updatedGroups = groupsToSyncIds.map { groupId ->
                getGroupAsGroupResponse(ObjectId(groupId))
            },
            deletedGroups = deletedGroups
        )
    }

}