@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.group

import com.lerchenflo.schneaggchatv3server.group.model.Group
import com.lerchenflo.schneaggchatv3server.group.model.GroupMember
import com.lerchenflo.schneaggchatv3server.group.model.GroupResponse
import com.lerchenflo.schneaggchatv3server.group.model.toGroupMemberResponse
import com.lerchenflo.schneaggchatv3server.group.model.toGroupResponse
import com.lerchenflo.schneaggchatv3server.repository.GroupMemberRepository
import com.lerchenflo.schneaggchatv3server.repository.GroupRepository
import com.lerchenflo.schneaggchatv3server.user.FriendsService
import com.lerchenflo.schneaggchatv3server.user.UserController
import com.lerchenflo.schneaggchatv3server.user.usermodel.UserService
import com.lerchenflo.schneaggchatv3server.util.ColorGenerator
import com.lerchenflo.schneaggchatv3server.util.ImageManager
import com.lerchenflo.schneaggchatv3server.util.LogType
import com.lerchenflo.schneaggchatv3server.util.LoggingService
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Component
class GroupService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val imageManager: ImageManager,
    private val friendsService: FriendsService,
    private val loggingService: LoggingService,
) {

    fun createGroup(groupName: String, members: List<ObjectId>, creatorId: ObjectId, description: String, profilePic: MultipartFile) : Group {

        //Try to add creator (Set prevents duplicate members)
        val membersInternal: Set<ObjectId> = members.toSet() + creatorId

        require(membersInternal.size > 2) { "A group must have at least 3 members" }
        require(groupName.length < 25) { "Groupname too long"}
        require(groupName.length > 2) { "Group name too short" }

        //TODO: Image validation

        //Creator needs to be friends with everyone
        members.forEach { member ->
            if (member == creatorId) return@forEach //Exclude self
            require(friendsService.areFriends(creatorId, member)){ "You need to be friends with everyone in the group"}
        }

        val currentTime = Clock.System.now()
        val group = groupRepository.save(
            Group(
                name = groupName.trim(),
                description = description,
                updatedAt = currentTime,
                createdAt = currentTime,
                creatorId = creatorId
            )
        )

        imageManager.saveProfilePic(
            image = profilePic,
            userId = group.id.toHexString(),
            group = true
        )

        // Generate unique colors for group members (per-group uniqueness)
        val existingColors = emptySet<Int>() // New group has no existing colors
        val memberColors = ColorGenerator.generateUniqueColorsForGroup(existingColors, membersInternal.size)
        val memberColorMap = membersInternal.zip(memberColors).toMap()

        groupMemberRepository.saveAll(
            membersInternal.mapIndexed { index, userId ->
                GroupMember(
                    userid = userId,
                    groupId = group.id,
                    joinedAt = currentTime,
                    admin = (userId == creatorId),
                    color = memberColorMap[userId]!!
                )
            }
        )

        loggingService.log(
            userId = creatorId,
            logType = LogType.GROUP_CREATED,
        )

        return group
    }


    fun getUserGroupIds(userId: ObjectId): List<ObjectId> {
        return groupMemberRepository.findByuserid(userId)
            .map { it.groupId }
    }

    fun getUserGroupIdsLastchanged(userId: ObjectId): List<UserService.IdTimeStamp> {
        val usergroups = getUserGroupIds(userId)

        return groupRepository.findAllById(usergroups).map { group ->
            UserService.IdTimeStamp(
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

    fun isUserInGroup(userId: ObjectId, groupId: ObjectId): Boolean {
        return groupMemberRepository.findByuserid(userId)
            .any { it.groupId == groupId }
    }

    fun getGroupById(groupId: ObjectId): Group? {
        val group = groupRepository.findById(groupId)
        return if (group.isPresent) {
            group.get()
        } else null
    }

    data class GroupSyncResponse(
        val updatedGroups: List<GroupResponse>,
        val deletedGroups: List<String>
    )

    fun syncGroups(userId: String, ids: List<UserService.IdTimeStamp>): GroupSyncResponse {
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


    fun getGroupProfilePic(groupId: ObjectId): ResponseEntity<ByteArray> {
        return try {
            val imageName = imageManager.getProfilePicFileName(groupId.toHexString(), true)
            val imageBytes = imageManager.loadImageFromFile(imageName)
            ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }


    fun changeGroupProfilePic(userId: ObjectId, groupId: ObjectId, image: MultipartFile) {

        require(isUserInGroup(userId, groupId))
        //TODO: Image validation

        val group = getGroupById(groupId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")

        imageManager.saveProfilePic(
            image = image,
            userId = groupId.toHexString(),
            group = true
        )

        groupRepository.save(group.copy(
            updatedAt = Clock.System.now()
        ))

    }

    fun changeGroupDescription(userId: ObjectId, groupId: ObjectId, newDescription: String) {

        require(isUserInGroup(userId, groupId))
        //TODO: string validation?

        val group = getGroupById(groupId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")

        groupRepository.save(group.copy(
            updatedAt = Clock.System.now(),
            description = newDescription
        ))

    }

}