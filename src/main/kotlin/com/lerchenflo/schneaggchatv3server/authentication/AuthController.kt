package com.lerchenflo.schneaggchatv3server.authentication

import com.lerchenflo.schneaggchatv3server.user.UserService
import com.lerchenflo.schneaggchatv3server.util.LoggingService
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.Locale
import java.util.Locale.getDefault

//https://schneaggchatv3.eu/auth

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val emailService: EmailService,
    private val userService: UserService
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
        val user = authService.register(
            username = username.trim().lowercase(getDefault()),
            password = password,
            email = email,
            birthdate = birthDate,
            profilePic = profilePic
        )

        emailService.sendVerificationEmail(
            userId = user.id,
        )
    }


    @PostMapping("/login")
    fun login(
        @RequestBody loginRequest: LoginRequest
    ): AuthService.TokenPair {

        return authService.login(
            username = loginRequest.username.trim().lowercase(getDefault()),
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


    @GetMapping("/verify_email")
    fun verifyEmail(
        @RequestParam("token") token: String,
    ) : String {
        //println("Verifying email for token $token")

        return if (emailService.verifyEmailRequest(token)){
            //Email verified
            "Your email has been verified."
        }else {
            //Email not verified
            "Your email could not be verified"
        }
    }

    @PostMapping("/send_delete_email")
    fun sendDeleteAccEmail(
        @RequestParam("email") email: String,
    ){
        val user = userService.findByEmail(email)
        if (user == null){
            println("No user to delete found with email $email")
            return
        }

        println("Email delete request for $email")
        emailService.sendDelAccEmail(user.id, email)
    }


    @GetMapping("/delete_account")
    fun deleteAccount(
        @RequestParam("token") token: String,
    ) : String {
        //println("Delete account token: $token")

        return if (emailService.verifyDelAccRequest(token)){
            //Email verified
            "Your account has been deleted"
        }else {
            //Email not verified
            "Account deletion failed"
        }
    }


}