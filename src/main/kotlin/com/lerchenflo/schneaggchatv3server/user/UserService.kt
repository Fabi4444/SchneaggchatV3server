package com.lerchenflo.schneaggchatv3server.user

import com.lerchenflo.schneaggchatv3server.core.security.HashEncoder
import com.lerchenflo.schneaggchatv3server.repository.RefreshTokenRepository
import com.lerchenflo.schneaggchatv3server.repository.UserRepository
import com.lerchenflo.schneaggchatv3server.user.friendshipmodel.FriendshipStatus
import com.lerchenflo.schneaggchatv3server.user.usermodel.NewFriendsUserResponse
import com.lerchenflo.schneaggchatv3server.user.usermodel.User
import com.lerchenflo.schneaggchatv3server.user.usermodel.UserRequest
import com.lerchenflo.schneaggchatv3server.user.usermodel.UserResponse
import com.lerchenflo.schneaggchatv3server.util.ImageManager
import com.lerchenflo.schneaggchatv3server.util.ValidationUtils
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.Locale
import java.util.Locale.getDefault
import kotlin.time.Clock

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userLookupService: UserLookupService,

    private val friendshipsService : FriendsService,
    private val hashEncoder: HashEncoder,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val imageManager: ImageManager

) {




    data class IdTimeStamp(val id: String, val timeStamp: String)

    data class UserSyncResponse(val updatedUsers: List<UserResponse>, val deletedUsers: List<String>)
    fun userIdSync(idTimeStamps: List<IdTimeStamp>, requesterId: ObjectId) : UserSyncResponse{
        //Users which the client has on his device
        val clientUsers = idTimeStamps.associate {
            it.id to it.timeStamp
        }

        //All users this current client has interacted with (Friends, requested, blocked etc)
        var allFriendInteractions = friendshipsService.getAllInteractions(requesterId)

        //Add own user (also needs to be synced)
        allFriendInteractions = allFriendInteractions + FriendsService.UserInteraction(
            userId = requesterId,
            status = FriendshipStatus.ACCEPTED,
            requesterId = requesterId,
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
                clientUsers[user.id.toHexString()]?.toLong()?.let { clientTimestamp ->
                    user.updatedAt.toEpochMilliseconds() > clientTimestamp ||
                            (interactionMap[user.id]?.lastChanged?.toEpochMilliseconds() ?: 0) > clientTimestamp
                } ?: false
            }

        val currentFriendIdStrings = allFriendInteractions.map { it.userId.toHexString() }.toSet()
        val usersToRemove = clientUsers.keys.filter { it !in currentFriendIdStrings && it != requesterId.toHexString() } //do not remove own user

        val finalExistingToUpdate = usersToUpdate + usersToAdd

        val addusers = finalExistingToUpdate.map { user ->

            //Calculate newest lastchanged timestamp
            val userTimestamp = user.updatedAt.toEpochMilliseconds()
            val friendshipTimestamp = interactionMap[user.id]?.lastChanged?.toEpochMilliseconds() ?: 0
            val newestTimestamp = maxOf(userTimestamp, friendshipTimestamp)

            serializeSyncUser(
                user = user,
                requestingUserId = requesterId,
                friendshipStatus = interactionMap[user.id]?.status,
                requesterId = interactionMap[user.id]?.requesterId,
                lastChangedAt = newestTimestamp,
            )
        }

        return UserSyncResponse(
            updatedUsers = addusers,
            deletedUsers = usersToRemove
        )
    }

    fun getAvailableUsers(
        searchTerm: String?,
        requestingUserId: String
    ) : List<NewFriendsUserResponse>{
        //Is user searching?
        if (searchTerm.isNullOrBlank()) {
            //User is not searching
            val requestingUserObjectId = ObjectId(requestingUserId)
            val allUserIds = userRepository.findAll().map { it.id }

            // Check if requesting user has any friendships
            val hasFriendships = friendshipsService.getAllInteractions(requestingUserObjectId).isNotEmpty()

            // Get users with no interaction
            val usersWithNoInteraction = friendshipsService.getUsersWithNoInteraction(
                userId = requestingUserObjectId,
                allUserIds = allUserIds
            )

            val eligibleUsers = if (hasFriendships) {
                // Return only users with common friends (at least 1)
                userRepository.findAllById(usersWithNoInteraction)
                    .filter { user ->
                        friendshipsService.getCommonFriendCount(requestingUserObjectId, user.id) > 0
                    }
            } else {
                // Return all users with no interaction
                userRepository.findAllById(usersWithNoInteraction)
            }

            return eligibleUsers.map { user ->
                NewFriendsUserResponse(
                    id = user.id.toHexString(),
                    username = user.username,
                    commonFriendCount = friendshipsService.getCommonFriendCount(requestingUserObjectId, user.id),
                )
            }
        } else {
            //return users searched by searchterm
            val searchResults = userRepository.findByUsernameContainingIgnoreCase(
                searchTerm.trim().lowercase(getDefault())
            )

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


    fun changeUsername(requestingUserId: String, newName: String) {
        val normalizedNewName = newName.trim().lowercase(getDefault())
        val existingUser = userRepository.findByUsername(normalizedNewName)

        if (existingUser != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "A user with username $normalizedNewName already exists")
        }

        ValidationUtils.validateUsername(normalizedNewName)

        val user = userRepository.findById(ObjectId(requestingUserId)).get()

        userRepository.save(user.copy(
            username = normalizedNewName,
            updatedAt = Clock.System.now()
        ))
    }

    fun changeProfilepic(requestingUserId: String, newPic: MultipartFile){
        val user = userRepository.findById(ObjectId(requestingUserId)).get()

        require(ValidationUtils.validatePicture(newPic)) { "New picture is invalid" }

        imageManager.saveProfilePic(
            image = newPic,
            userId = requestingUserId,
            group = false
        )

        userRepository.save(user.copy(
            updatedAt = Clock.System.now(),
        ))
    }

    fun changeUserProfile(
        changingUserId: String,
        userRequest: UserRequest
    ) {
        val requestingUser = userLookupService.findById(changingUserId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        val user = userLookupService.findById(userRequest.userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")

        //Change something about yourself
        if (changingUserId == userRequest.userId) {

            val somethingChanged = userRequest.newStatus != null

                userLookupService.save(requestingUser.copy(
                updatedAt = if (somethingChanged) Clock.System.now() else requestingUser.updatedAt,
                userStatus = userRequest.newStatus ?: requestingUser.userStatus
            ))

        } else {
            require(friendshipsService.areFriends(requestingUser.id, user.id))

            val somethingChanged = userRequest.newDescription != null

                userLookupService.save(user.copy(
                updatedAt = if (somethingChanged) Clock.System.now() else user.updatedAt,
                userDescription = userRequest.newDescription ?: user.userDescription
            ))

        }
    }


    data class PasswordChangeRequest(
        val oldPassword: String,
        val newPassword: String
    )

    fun changePassword(requestingUserId: String, passwordChangeRequest: PasswordChangeRequest) {
        val user = userRepository.findById(ObjectId(requestingUserId)).get()

        require(
            hashEncoder.matches(passwordChangeRequest.oldPassword, user.hashedPassword)
        ) { "Old password does not match"}

        require(passwordChangeRequest.oldPassword != passwordChangeRequest.newPassword) { "Password can not be the same"}

        userRepository.save(user.copy(
            hashedPassword = hashEncoder.encode(passwordChangeRequest.newPassword)
        ))

        //All tokens invalidated
        refreshTokenRepository.deleteByUserId(user.id)

    }



    /**
     * Serialize a user into a specific response according to the friendship status
     * @param User the user to be serialized
     * @param requestingUserId the user which requested the serialisation
     */
    private fun serializeSyncUser(user: User, requestingUserId : ObjectId, friendshipStatus: FriendshipStatus?, requesterId: ObjectId?, lastChangedAt: Long? = null): UserResponse {
        //User requests his own data
        if (requestingUserId == user.id) {
            return UserResponse.SelfUserResponse(
                id = user.id.toString(),
                username = user.username,
                userDescription = user.userDescription,
                userStatus = user.userStatus,
                updatedAt = lastChangedAt ?: user.updatedAt.toEpochMilliseconds(),
                birthDate = user.birthDate,
                email = user.email,
                createdAt = user.createdAt.toEpochMilliseconds(),
                emailVerifiedAt = user.emailVerifiedAt?.toEpochMilliseconds(),
            )
        }

        //User requests friends data
        else if (friendshipStatus == FriendshipStatus.ACCEPTED) {
            return UserResponse.FriendUserResponse(
                id = user.id.toString(),
                username = user.username,
                userDescription = user.userDescription,
                userStatus = user.userStatus,
                updatedAt = lastChangedAt ?: user.updatedAt.toEpochMilliseconds(),
                birthDate = user.birthDate,
                requesterId = requesterId?.toHexString(),
            )
        }

        //User requests random other users data
        else {
            return UserResponse.SimpleUserResponse(
                id = user.id.toString(),
                username = user.username,
                updatedAt = lastChangedAt ?: user.updatedAt.toEpochMilliseconds(),
                friendShipStatus = friendshipStatus,
                requesterId = requesterId?.toHexString(),
            )
        }
    }


}