package com.lerchenflo.schneaggchatv3server.authentication

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
        @field:Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}\$",
            message = "Password must be at least 8 characters long and contain at least one digit, uppercase and lowercase character."
        )
        val password: String,
        @field:Email(message = "Invalid email format.")
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
        @Valid @RequestBody registerRequest: RegisterRequest //Valid for checking the constraints (Email etc)
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