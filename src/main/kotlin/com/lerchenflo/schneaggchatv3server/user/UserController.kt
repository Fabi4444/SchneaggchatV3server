@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.user

import com.lerchenflo.schneaggchatv3server.authentication.EmailService
import com.lerchenflo.schneaggchatv3server.notifications.FirebaseService
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

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
    private val friendshipsService: FriendsService,
    //TODO: Friendsettingsservice
    private val imageManager: ImageManager,


    private val firebaseService: FirebaseService
) {

    @PostMapping("/verificationemail")
    fun sendVerificationEmail(){
        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String ?: throw ResponseStatusException(
                /* status = */ HttpStatus.FORBIDDEN,
                /* reason = */ "Not logged in"
            )
        userService.sendVerificationEmail(requestingUserId)
    }

    @PostMapping("/setfirebasetoken")
    fun setFirebaseToken(
        @RequestParam token: String,
    ){
        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String ?: throw ResponseStatusException(
                /* status = */ HttpStatus.FORBIDDEN,
                /* reason = */ "Not logged in"
            )

        firebaseService.saveToken(userId = ObjectId(requestingUserId), token = token)

    }





    @PostMapping("/sync")
    fun syncUsers(
        @RequestBody requestBody: List<UserService.IdTimeStamp>
    ) : UserService.UserSyncResponse {
        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String ?: throw ResponseStatusException(
                /* status = */ HttpStatus.FORBIDDEN,
                /* reason = */ "Not logged in"
            )

        return userService.userIdSync(
            idTimeStamps = requestBody,
            requesterId = ObjectId(requestingUserId),
        )
    }





    //TODO: Check user profilepic settings (implement first)
    @GetMapping("/profilepic/{id}")
    fun getProfilePic(@PathVariable("id") userId: String): ResponseEntity<ByteArray> {
        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String ?: throw ResponseStatusException(
                /* status = */ HttpStatus.FORBIDDEN,
                /* reason = */ "Not logged in"
            )

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


        return userService.getAvailableUsers(
            searchTerm = searchTerm,
            requestingUserId = requestingUserId
        )

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

        val friendship = friendshipsService.sendFriendRequest(
            fromUserId = ObjectId(requestingUserId),
            toUserId = ObjectId(touserId)
        )

        println("Friend request: $friendship")
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