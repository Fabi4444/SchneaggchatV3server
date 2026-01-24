package com.lerchenflo.schneaggchatv3server.notifications

import com.lerchenflo.schneaggchatv3server.message.messagemodel.Message
import com.lerchenflo.schneaggchatv3server.message.messagemodel.toMessageResponse
import com.lerchenflo.schneaggchatv3server.notifications.firebase.FirebaseService
import com.lerchenflo.schneaggchatv3server.notifications.websocket.SocketConnectionHandler
import com.lerchenflo.schneaggchatv3server.notifications.websocket.model.SocketConnectionMessage
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import org.springframework.web.socket.TextMessage


/**
 * Notificationservice to notify the user about updates. if the user is connected via socket connection, send
 * via socket, else via firebase. not all updates are sent via firebase (Message updates etc do not matter if the
 * client is offline, he will sync on app start)
 */
@Service
class NotificationService(
    private val socketConnectionHandler: SocketConnectionHandler,
    private val firebaseMessagingService: FirebaseService
) {

    /**
     * Send a notification to a client device
     */
    fun notifyMessageUpdate(message: Message, receiverId: ObjectId) {

        //Try sending via socket connection (if fail (not connected) or group use firebase)
        if (group || !socketConnectionHandler.sendMessage(
                SocketConnectionMessage.MessageChange(
                    message = message.toMessageResponse(),
                    deleted = message.deleted,
                    newMessage = newMessage
                ),
                receiverId = receiverId
            )) {

            //Message sending failed, use firebase if new, else ignore

        }
    }

    private fun notifyNewMessageFirebase(receiverId: ObjectId, group: Boolean) {
        firebaseMessagingService.sendNewMessageNotificationToUser(
            userId = receiverId,
            messageContent = TODO(),
            msgId = TODO(),
            groupMessage = TODO(),
            groupName = TODO()
        )
    }

}