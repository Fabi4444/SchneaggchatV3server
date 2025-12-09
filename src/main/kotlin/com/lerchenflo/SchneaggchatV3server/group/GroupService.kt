package com.lerchenflo.SchneaggchatV3server.group

import org.bson.types.ObjectId
import org.springframework.stereotype.Component

@Component
class GroupService {

    fun getUserGroupIds(userId: ObjectId): List<ObjectId> {
        return emptyList()    //TODO
    }
}