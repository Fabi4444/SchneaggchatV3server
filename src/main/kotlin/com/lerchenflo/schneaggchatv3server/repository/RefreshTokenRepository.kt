package com.lerchenflo.schneaggchatv3server.repository

import com.lerchenflo.schneaggchatv3server.authentication.model.RefreshToken
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface RefreshTokenRepository: MongoRepository<RefreshToken, ObjectId> {

    fun findByUserIdAndHashedToken(userId: ObjectId, hashedToken: String): RefreshToken?
    fun deleteByUserIdAndHashedToken(userId: ObjectId, hashedToken: String) : Long

    fun findByUserIdAndHashedTokenAndDeletedAtIsNull(userId: ObjectId, hashedToken: String): RefreshToken?


    fun deleteByUserId(userId: ObjectId)

}