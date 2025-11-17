package com.lerchenflo.schneaggchatv3server.user

import com.lerchenflo.schneaggchatv3server.repository.UserRepository
import com.lerchenflo.schneaggchatv3server.user.model.User
import com.lerchenflo.schneaggchatv3server.user.model.UserRequest
import com.lerchenflo.schneaggchatv3server.user.model.UserResponse
import com.lerchenflo.schneaggchatv3server.util.ImageManager
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.awt.PageAttributes

@RestController
@RequestMapping("/users")
class UserController(
    private val userRepository: UserRepository,
    private val imageManager: ImageManager
) {


    @GetMapping("/own")
    fun getOwnUserConfig(): UserResponse {
        val userId = SecurityContextHolder.getContext().authentication?.principal as? String

        return UserResponse(
            id = userId ?: "nulle"
        )
    }


    //TODO: Check user profilepic settings (implement first)
    @GetMapping("/profilepic/{id}")
    fun getProfilePic(@PathVariable userId: String): ResponseEntity<ByteArray> {
        return try {
            val imageName = imageManager.getProfilePicFileName(userId)
            val imageBytes = imageManager.loadImageFromFile(imageName)
            ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        }
    }

}