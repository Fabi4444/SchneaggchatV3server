package com.lerchenflo.SchneaggchatV3server.core.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import javax.annotation.Nonnull

@Component
class HashEncoder {

    private val bcrypt = BCryptPasswordEncoder()

    /**
     * Get the hash value of a string
     * @param raw the string to encode
     */
    fun encode(@Nonnull raw: String) : String = bcrypt.encode(raw)!!

    /**
     * Checks if a raw string matches the hashed version
     * @param raw The unhashed string
     * @param hashed the hashed value
     */
    fun matches(raw: String, hashed: String) : Boolean = bcrypt.matches(raw, hashed)

}