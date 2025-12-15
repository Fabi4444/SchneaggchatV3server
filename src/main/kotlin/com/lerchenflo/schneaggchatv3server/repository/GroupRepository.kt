package com.lerchenflo.schneaggchatv3server.repository

import com.lerchenflo.schneaggchatv3server.group.model.Group
import com.lerchenflo.schneaggchatv3server.notifications.model.FirebaseToken
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface GroupRepository : MongoRepository<Group, ObjectId> {

}