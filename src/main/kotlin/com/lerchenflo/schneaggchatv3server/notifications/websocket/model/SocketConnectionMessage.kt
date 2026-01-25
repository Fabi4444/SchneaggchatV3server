package com.lerchenflo.schneaggchatv3server.notifications.websocket.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.lerchenflo.schneaggchatv3server.message.messagemodel.MessageResponse
import com.lerchenflo.schneaggchatv3server.user.UserService
import com.lerchenflo.schneaggchatv3server.user.usermodel.User
import com.lerchenflo.schneaggchatv3server.user.usermodel.UserResponse
import org.bson.types.ObjectId


@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = SocketConnectionMessage.MessageChange::class, name = "messagechange"),
    JsonSubTypes.Type(value = SocketConnectionMessage.UserChange::class, name = "userchange"),
    JsonSubTypes.Type(value = SocketConnectionMessage.FriendRequest::class, name = "friendrequest")
)
sealed interface SocketConnectionMessage {

    //For message updates on client side (updates, deletion)
    data class MessageChange(val message: MessageResponse, val newMessage: Boolean, val deleted: Boolean) : SocketConnectionMessage

    data class UserChange(val user: UserService.UserSyncResponse, val deleted: Boolean) : SocketConnectionMessage

    data class FriendRequest(
        val requestingUser: String,
        val requestingUserName: String,
        val accepted: Boolean
    ) : SocketConnectionMessage



}