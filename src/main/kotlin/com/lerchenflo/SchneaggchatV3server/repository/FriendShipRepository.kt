package com.lerchenflo.SchneaggchatV3server.repository

import com.lerchenflo.SchneaggchatV3server.user.friendshipmodel.Friendship
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface FriendshipRepository : MongoRepository<Friendship, ObjectId> {

    fun findByUserId1AndUserId2(userId1: ObjectId, userId2: ObjectId): Friendship?
    fun findByUserId1OrUserId2(userId1: ObjectId, userId2: ObjectId): List<Friendship>
}