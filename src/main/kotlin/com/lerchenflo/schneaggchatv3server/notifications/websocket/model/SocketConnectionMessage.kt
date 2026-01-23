package com.lerchenflo.schneaggchatv3server.notifications.websocket.model

import com.lerchenflo.schneaggchatv3server.message.messagemodel.MessageResponse
import com.lerchenflo.schneaggchatv3server.user.usermodel.User

sealed interface SocketConnectionMessage {

    //For message updates on client side (New messages, updates, deletion)
    data class MessageChange(val message: MessageResponse, val deleted: Boolean, val newMessage: Boolean) : SocketConnectionMessage

    data class UserChange(val user: User, val deleted: Boolean) : SocketConnectionMessage


}