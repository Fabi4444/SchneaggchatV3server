package com.lerchenflo.SchneaggchatV3server.repository

import com.lerchenflo.SchneaggchatV3server.message.messagemodel.Message
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface MessageRepository : MongoRepository<Message, ObjectId> {

}