@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.user.usermodel

import kotlin.time.ExperimentalTime
import kotlin.time.Instant


//TODO: Request when the user changes something about his profile

interface UserResponse {

    //Common data which every response contains
    val id: String
    val username: String
    val userDescription: String
    val userStatus: String
    val updatedAt: Instant


    //Response for a user (Not yourself and not your friend)
    data class SimpleUserResponse(
        override val id: String,
        override val username: String,
        override val userDescription: String,
        override val userStatus: String,
        override val updatedAt: Instant,

        //Custom to simpleuserresponse:
        val commonFriendCount: Int,

    ) : UserResponse

    //Response for a friend (He accepted your request)
    data class FriendUserResponse(
        override val id: String,
        override val username: String,
        override val userDescription: String,
        override val userStatus: String,
        override val updatedAt: Instant,


        //Custom to friend response:
        val birthDate: String,



        ) : UserResponse

    //Response for yourself (You request your own data)
    data class SelfUserResponse(
        override val id: String,
        override val username: String,
        override val userDescription: String,
        override val userStatus: String,
        override val updatedAt: Instant,


        //Custom to friend response
        val birthDate: String,

        //Custom to own user response:
        val email: String,
        val createdAt: Instant,


        //TODO: User profile pic privacy settings??


    ) : UserResponse
}




