package com.lerchenflo.schneaggchatv3server.authentication

import com.lerchenflo.SchneaggchatV3server.authentication.emailverification.EmailService
import com.lerchenflo.schneaggchatv3server.util.ImageManager
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.awt.PageAttributes

//https://schneaggchat.eu/auth

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val emailService: EmailService
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
    )

    data class RefreshRequest(
        val refreshToken: String
    )

    //https://schneaggchat.eu/auth/register
    @PostMapping("/register", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun register(
        @RequestParam("username") username: String,
        @RequestParam("password") password: String,
        @RequestParam("email") email: String,
        @RequestParam("birthDate") birthDate: String,
        @RequestParam("profilepic") profilePic: MultipartFile
    ) {
        authService.register(
            username = username,
            password = password,
            email = email,
            birthdate = birthDate,
            profilePic = profilePic
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


    @GetMapping("/auth/verify_email")
    fun verifyEmail(
        @RequestParam("token") token: String,
    ) : String {
        println("Verifying email for token $token")

        return if (emailService.verifyEmailRequest(token)){
            //Email verified
            "Email verified"
        }else {
            //Email not verified
            "Your email could not be verified"
        }
    }


}