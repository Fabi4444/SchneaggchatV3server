package com.lerchenflo.schneaggchatv3server.user

import com.lerchenflo.schneaggchatv3server.repository.UserRepository
import com.lerchenflo.schneaggchatv3server.user.model.User
import com.lerchenflo.schneaggchatv3server.user.model.UserRequest
import com.lerchenflo.schneaggchatv3server.user.model.UserResponse
import org.springframework.context.annotation.Profile
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userRepository: UserRepository,
) {

    @Profile("debug")
    @GetMapping("/getall")
    fun getAllUsers(): List<User> = userRepository.findAll()


    @GetMapping("/own")
    fun getOwnUserConfig(): UserResponse {
        val userId = SecurityContextHolder.getContext().authentication?.principal as? String

        return UserResponse(
            id = userId ?: "nulle"
        )
    }


}