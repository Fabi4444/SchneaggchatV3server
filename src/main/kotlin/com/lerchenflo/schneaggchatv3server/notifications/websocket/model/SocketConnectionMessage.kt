package com.lerchenflo.schneaggchatv3server.notifications.websocket.model

import com.lerchenflo.schneaggchatv3server.message.messagemodel.MessageResponse
import com.lerchenflo.schneaggchatv3server.user.usermodel.User
import org.bson.types.ObjectId

sealed interface SocketConnectionMessage {

    //For message updates on client side (updates, deletion)
    data class MessageChange(val message: MessageResponse, val newMessage: Boolean, val deleted: Boolean) : SocketConnectionMessage

    data class UserChange(val user: User, val deleted: Boolean) : SocketConnectionMessage

    data class FriendRequest(val requestingUser: ObjectId, val receivingUser: ObjectId, val sendingUserName: String) : SocketConnectionMessage



}