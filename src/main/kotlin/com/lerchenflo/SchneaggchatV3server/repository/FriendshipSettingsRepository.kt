package com.lerchenflo.SchneaggchatV3server.repository

import com.lerchenflo.SchneaggchatV3server.user.friendshipmodel.FriendshipSettings
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface FriendshipSettingsRepository : MongoRepository<FriendshipSettings, ObjectId> {
}