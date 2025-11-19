@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.user

import com.lerchenflo.schneaggchatv3server.repository.FriendshipRepository
import com.lerchenflo.schneaggchatv3server.repository.UserRepository
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
    private val imageManager: ImageManager
) {


    @GetMapping("/getbyid/{userId}")
    fun getUserById(
        @PathVariable("userId") requestedUserId: String,
    ): UserResponse {
        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String ?: throw ResponseStatusException(
                /* status = */ HttpStatus.FORBIDDEN,
                /* reason = */ "Not logged in"
            )

        val areFriends =
            friendshipsService.areFriends(ObjectId(requestingUserId), ObjectId(requestedUserId))

        val requestedUser =
            userRepository.findById(ObjectId(requestedUserId)).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Not found") }


        return serializeUser(
            user = requestedUser,
            requestingUserId = ObjectId(requestingUserId),
            areFriends = areFriends
        )
    }


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

        val allFriendIds = friendshipsService.getFriends(ObjectId(requestingUserId))

        val clientUsers = requestBody.associate {
            it.id to it.timeStamp
        }

        val currentFriends = userRepository.findAllById(allFriendIds)

        val usersToAdd = currentFriends
            .filter { it.id.toString() !in clientUsers.keys }

        val usersToUpdate = currentFriends
            .filter { user ->
                val clientTimestamp = clientUsers[user.id.toString()]
                clientTimestamp != null && user.updatedAt.toEpochMilliseconds() > clientTimestamp.toLong()
            }

        val currentFriendIdStrings = allFriendIds.map { it.toString() }.toSet()
        val usersToRemove = clientUsers.keys.filter { it !in currentFriendIdStrings }

        val finalExistingToUpdate = usersToUpdate + usersToAdd

        val addusers = finalExistingToUpdate.map { user ->
            serializeUser(
                user = user,
                requestingUserId = ObjectId(requestingUserId),
                areFriends = true,
            )
        }

        return UserSyncResponse(
            updatedUsers = addusers,
            deletedUsers = usersToRemove
        )
    }


    //TODO: Check user profilepic settings (implement first)
    @GetMapping("/profilepic/{id}")
    fun getProfilePic(@PathVariable userId: String): ResponseEntity<ByteArray> {
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
     * @param areFriends is the requesting user friends with the requested user
     * @param deleted is the requested user deleted
     */
    private fun serializeUser(user: User, requestingUserId : ObjectId, areFriends: Boolean,): UserResponse {
        //User requests his own data
        if (requestingUserId == user.id) {
            return UserResponse.SelfUserResponse(
                id = user.id.toString(),
                username = user.username,
                userDescription = user.userDescription,
                userStatus = user.userStatus,
                updatedAt = user.updatedAt,
                birthDate = user.birthDate,
                email = user.email,
                createdAt = user.createdAt,
            )
        }

        //User requests friends data
        else if (areFriends) {
            return UserResponse.FriendUserResponse(
                id = user.id.toString(),
                username = user.username,
                userDescription = user.userDescription,
                userStatus = user.userStatus,
                updatedAt = user.updatedAt,
                birthDate = user.birthDate,
                )
        }

        //User requests random other users data
        else {
            return UserResponse.SimpleUserResponse(
                id = user.id.toString(),
                username = user.username,
                userDescription = user.userDescription,
                userStatus = user.userStatus,
                updatedAt = user.updatedAt,
                commonFriendCount = friendshipsService.getFriends(requestingUserId).count(),
                )
        }
    }

}