@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.user

import com.lerchenflo.schneaggchatv3server.repository.FriendshipRepository
import com.lerchenflo.schneaggchatv3server.repository.UserRepository
import com.lerchenflo.schneaggchatv3server.user.friendshipmodel.FriendshipStatus
import com.lerchenflo.schneaggchatv3server.user.usermodel.NewFriendsUserResponse
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
import org.springframework.web.bind.annotation.RequestParam
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
                val userUpdated = user.updatedAt.toEpochMilliseconds() > (clientTimestamp?.toLong() ?: 0)
                val friendshipUpdated = interactionMap[user.id]?.lastChanged?.toEpochMilliseconds()
                    ?.let { it > (clientTimestamp?.toLong() ?: 0) } ?: false

                clientTimestamp != null && (userUpdated || friendshipUpdated)
            }

        val currentFriendIdStrings = allFriendInteractions.map { it.userId.toHexString() }.toSet()
        val usersToRemove = clientUsers.keys.filter { it !in currentFriendIdStrings && it != requestingUserId } //do not remove own user

        val finalExistingToUpdate = usersToUpdate + usersToAdd

        val addusers = finalExistingToUpdate.map { user ->
            serializeSyncUser(
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

    /**
     * Serialize a user into a specific response according to the friendship status
     * @param User the user to be serialized
     * @param requestingUserId the user which requested the serialisation
     */
    private fun serializeSyncUser(user: User, requestingUserId : ObjectId, friendshipStatus: FriendshipStatus?, requesterId: ObjectId?): UserResponse {
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

    @GetMapping("/availableusers")
    fun getAvailableUsers(
        @RequestParam("searchterm", required = false) searchTerm: String?,
    ) : List<NewFriendsUserResponse> {
        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String ?: throw ResponseStatusException(
                /* status = */ HttpStatus.FORBIDDEN,
                /* reason = */ "Not logged in"
            )

        //TODO: Very heavy db operation, fix??

        //Is user searching?
        if (searchTerm.isNullOrBlank()) {

            //User is not searching, return all users with common friends
            val alluserIds = userRepository.findAll().map { it.id }
            val newusers = friendshipsService.getUsersWithNoInteraction(
                userId = ObjectId(requestingUserId),
                allUserIds = alluserIds
            )

            return userRepository.findAllById(newusers).map { user ->
                NewFriendsUserResponse(
                    id = user.id.toHexString(),
                    username = user.username,
                    commonFriendCount = friendshipsService.getCommonFriendCount(ObjectId(requestingUserId), user.id),
                )
            }
        }else {
            //return users searched by searchterm
            val searchResults = userRepository.findByUsernameContainingIgnoreCase(searchTerm)

            val interactedUserIds = friendshipsService.getAllInteractions(ObjectId(requestingUserId))
                .map { it.userId }
                .toSet()

            return searchResults
                .filter { user ->
                    user.id != ObjectId(requestingUserId) && // Exclude self
                            user.id !in interactedUserIds // Exclude already interacted users
                }
                .map { user ->
                    NewFriendsUserResponse(
                        id = user.id.toHexString(),
                        username = user.username,
                        commonFriendCount = friendshipsService.getCommonFriendCount(
                            ObjectId(requestingUserId),
                            user.id
                        )
                    )
                }
                .sortedByDescending { it.commonFriendCount }
        }

    }


    @GetMapping("/addfriend/{id}")
    fun sendFriendRequest(
        @PathVariable("id") touserId: String
    ) {
        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String ?: throw ResponseStatusException(
                /* status = */ HttpStatus.FORBIDDEN,
                /* reason = */ "Not logged in"
            )

        friendshipsService.sendFriendRequest(
            fromUserId = ObjectId(requestingUserId),
            toUserId = ObjectId(touserId)
        )
    }

    @GetMapping("/denyfriend/{id}")
    fun denyFriendRequest(
        @PathVariable("id") touserId: String
    ) {
        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String ?: throw ResponseStatusException(
                /* status = */ HttpStatus.FORBIDDEN,
                /* reason = */ "Not logged in"
            )

        friendshipsService.declineFriendRequest(
            ObjectId(requestingUserId),
            ObjectId(touserId)
        )
    }
}