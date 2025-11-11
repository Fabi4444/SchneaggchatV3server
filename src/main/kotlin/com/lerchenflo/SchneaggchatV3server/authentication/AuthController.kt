package com.lerchenflo.schneaggchatv3server.authentication

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

//https://schneaggchat.eu/auth

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {

    data class LoginRequest(
        val username: String,
        val password: String
    )

    data class RegisterRequest(
        val username: String,
        val password: String,
        val email: String,
        val birthDate: String,
        //TODO: Add fields (Profilepic)
    )

    data class RefreshRequest(
        val refreshToken: String
    )

    //https://schneaggchat.eu/auth/register
    @PostMapping("/register")
    fun register(
        @RequestBody registerRequest: RegisterRequest
    ) {
        authService.register(
            username = registerRequest.username,
            password = registerRequest.password,
            email = registerRequest.email,
            birthdate = registerRequest.birthDate
        )
    }


    @PostMapping("/login")
    fun login(
        @RequestBody loginRequest: LoginRequest
    ): AuthService.TokenPair {
        return authService.login(
            username = loginRequest.username,
            password = loginRequest.password,
        )
    }


    @PostMapping("/refresh")
    fun refresh(
        @RequestBody refreshRequest: RefreshRequest
    ): AuthService.TokenPair {
        return authService.refresh(
            refreshRequest.refreshToken,
        )
    }


}