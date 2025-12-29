package com.lerchenflo.schneaggchatv3server.user

import com.lerchenflo.schneaggchatv3server.authentication.EmailService
import com.lerchenflo.schneaggchatv3server.repository.UserRepository
import com.lerchenflo.schneaggchatv3server.user.friendshipmodel.FriendshipStatus
import com.lerchenflo.schneaggchatv3server.user.usermodel.NewFriendsUserResponse
import com.lerchenflo.schneaggchatv3server.user.usermodel.User
import com.lerchenflo.schneaggchatv3server.user.usermodel.UserResponse
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import kotlin.collections.associateBy
import kotlin.collections.map
import kotlin.collections.plus
import kotlin.text.get
import kotlin.text.toHexString
import kotlin.text.toLong

@Service
class UserService(
    private val userRepository: UserRepository,
    private val friendshipsService: FriendsService,
    private val emailService: EmailService,

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


    fun sendVerificationEmail(requestingUserId: String) {
        val user = userRepository.findById(ObjectId(requestingUserId))

        emailService.sendVerificationEmail(
            userId = ObjectId(requestingUserId),
            email = user.get().email
        )
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