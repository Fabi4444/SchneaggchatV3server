@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.user

import com.lerchenflo.schneaggchatv3server.repository.FriendshipRepository
import com.lerchenflo.schneaggchatv3server.repository.UserRepository
import com.lerchenflo.schneaggchatv3server.user.friendshipmodel.FriendshipStatus
import com.lerchenflo.schneaggchatv3server.user.usermodel.User
import com.lerchenflo.schneaggchatv3server.user.usermodel.UserResponse
import com.lerchenflo.schneaggchatv3server.util.ImageManager
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@RestController
@RequestMapping("/users")
class UserController(
    private val userRepository: UserRepository,
    private val friendshipsService: FriendsService,
    //TODO: Friendsettingsservice
    private val imageManager: ImageManager
) {

    data class IdTimeStamp(val id: String, val timeStamp: String)

    data class UserSyncResponse(val updatedUsers: List<UserResponse>, val deletedUsers: List<String>)

    @PostMapping("/sync")
    fun syncUsers(
        @RequestBody requestBody: List<IdTimeStamp>
    ) : UserSyncResponse {
        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String ?: throw ResponseStatusException(
                /* status = */ HttpStatus.FORBIDDEN,
                /* reason = */ "Not logged in"
            )

        //Users which the client has on his device
        val clientUsers = requestBody.associate {
            it.id to it.timeStamp
        }

        //All users this current client has interacted with (Friends, requested, blocked etc)
        var allFriendInteractions = friendshipsService.getAllInteractions(ObjectId(requestingUserId))

        //Add own user (also needs to be synced)
        allFriendInteractions = allFriendInteractions + FriendsService.UserInteraction(
            userId = ObjectId(requestingUserId),
            status = FriendshipStatus.ACCEPTED,
            requesterId = ObjectId(requestingUserId),
        )

        // Create a map for easy lookup
        val interactionMap = allFriendInteractions.associateBy { it.userId }

        //Find all friend objects from interactions
        val currentFriends = userRepository.findAllById(allFriendInteractions.map { interaction ->
            interaction.userId
        })

        val usersToAdd = currentFriends
            .filter { it.id.toHexString() !in clientUsers.keys }

        val usersToUpdate = currentFriends
            .filter { user ->
                val clientTimestamp = clientUsers[user.id.toHexString()]
                clientTimestamp != null && user.updatedAt.toEpochMilliseconds() > clientTimestamp.toLong()
            }

        val currentFriendIdStrings = allFriendInteractions.map { it.userId.toHexString() }.toSet()
        val usersToRemove = clientUsers.keys.filter { it !in currentFriendIdStrings && it != requestingUserId } //do not remove own user

        val finalExistingToUpdate = usersToUpdate + usersToAdd

        val addusers = finalExistingToUpdate.map { user ->
            serializeUser(
                user = user,
                requestingUserId = ObjectId(requestingUserId),
                friendshipStatus = interactionMap[user.id]?.status,
                requesterId = interactionMap[user.id]?.requesterId,
            )
        }

        return UserSyncResponse(
            updatedUsers = addusers,
            deletedUsers = usersToRemove
        )
    }


    //TODO: Check user profilepic settings (implement first)
    @GetMapping("/profilepic/{id}")
    fun getProfilePic(@PathVariable("id") userId: String): ResponseEntity<ByteArray> {
        return try {
            val imageName = imageManager.getProfilePicFileName(userId, false)
            val imageBytes = imageManager.loadImageFromFile(imageName)
            ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }





    /**
     * Serialize a user into a specific response according to the friendship status
     * @param User the user to be serialized
     * @param requestingUserId the user which requested the serialisation
     */
    private fun serializeUser(user: User, requestingUserId : ObjectId, friendshipStatus: FriendshipStatus?, requesterId: ObjectId?): UserResponse {
        //User requests his own data
        if (requestingUserId == user.id) {
            return UserResponse.SelfUserResponse(
                id = user.id.toString(),
                username = user.username,
                userDescription = user.userDescription,
                userStatus = user.userStatus,
                updatedAt = user.updatedAt.toEpochMilliseconds(),
                birthDate = user.birthDate,
                email = user.email,
                createdAt = user.createdAt.toEpochMilliseconds(),
            )
        }

        //User requests friends data
        else if (friendshipStatus == FriendshipStatus.ACCEPTED) {
            return UserResponse.FriendUserResponse(
                id = user.id.toString(),
                username = user.username,
                userDescription = user.userDescription,
                userStatus = user.userStatus,
                updatedAt = user.updatedAt.toEpochMilliseconds(),
                birthDate = user.birthDate,
                requesterId = requesterId?.toHexString(),
            )
        }

        //User requests random other users data
        else {
            return UserResponse.SimpleUserResponse(
                id = user.id.toString(),
                username = user.username,
                updatedAt = user.updatedAt.toEpochMilliseconds(),
                friendShipStatus = friendshipStatus,
                requesterId = requesterId?.toHexString(),
            )
        }
    }

}