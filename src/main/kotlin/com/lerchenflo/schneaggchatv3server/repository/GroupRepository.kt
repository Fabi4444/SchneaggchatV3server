package com.lerchenflo.schneaggchatv3server.repository

import com.lerchenflo.schneaggchatv3server.group.model.Group
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface GroupRepository : MongoRepository<Group, ObjectId> {

}