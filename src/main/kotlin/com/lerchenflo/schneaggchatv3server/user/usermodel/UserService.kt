package com.lerchenflo.schneaggchatv3server.user.usermodel

import com.lerchenflo.schneaggchatv3server.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class UserService(
    private val userRepository: UserRepository
) {
    fun checkExistingUser(username: String, email: String) {
        val usernameexists = userRepository.findByUsername(username)
        if (usernameexists != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "A user with this username already exists")
        }

        val emailexists = userRepository.findByEmail(email.trim())
        if (emailexists != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "A user with this email already exists")
        }
    }

    fun save(user: User): User {
        return userRepository.save(user)
    }

    fun findById(id: String): User? {
        val objid = ObjectId(id)

        val optuser = userRepository.findById(objid)

        return if (optuser.isPresent) {
            optuser.get()
        }else null
    }

    fun findByUsername(username: String): User? {
        val optuser = userRepository.findByUsername(username)

        return optuser
    }

    fun findByEmail(email: String): User? {
        val optuser = userRepository.findByEmail(email)

        return optuser
    }

    fun deleteUser(userId: ObjectId) {
        userRepository.deleteById(userId)
    }

    fun getUsername(userId: ObjectId): String {
        return userRepository.findById(userId).get().username
    }

}