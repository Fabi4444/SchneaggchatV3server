package com.lerchenflo.schneaggchatv3server.notifications

import com.lerchenflo.schneaggchatv3server.group.GroupLookupService
import com.lerchenflo.schneaggchatv3server.group.model.GroupMember
import com.lerchenflo.schneaggchatv3server.message.messagemodel.Message
import com.lerchenflo.schneaggchatv3server.message.messagemodel.toMessageResponse
import com.lerchenflo.schneaggchatv3server.notifications.firebase.FirebaseService
import com.lerchenflo.schneaggchatv3server.notifications.websocket.SocketConnectionHandler
import com.lerchenflo.schneaggchatv3server.notifications.websocket.model.SocketConnectionMessage
import com.lerchenflo.schneaggchatv3server.user.UserLookupService
import com.lerchenflo.schneaggchatv3server.user.usermodel.User
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
    private val firebaseMessagingService: FirebaseService,

    private val userLookupService: UserLookupService,
    private val groupLookupService: GroupLookupService
) {

    
    /**
     * Send a notification to a client device
     */
    fun notifyMessageUpdate(message: Message, newMessage: Boolean, deleted: Boolean) {

        val group = message.groupMessage

        if (group) {
            val groupMembers = groupLookupService.getGroupMembers(message.receiverId)
            val groupName = groupLookupService.getGroupById(message.receiverId)?.name
                ?: "Unknown Group"

            groupMembers.forEach { member ->

                //Exclude the sender
                if (member.userid == message.senderId) return@forEach

                if (!socketConnectionHandler.sendMessage(
                        message = SocketConnectionMessage.MessageChange(
                            message = message.toMessageResponse(),
                            deleted = deleted,
                            newMessage = newMessage,
                        ),
                        receiverId = member.userid,
                    )) {

                    if (newMessage) {
                        //Socket connection failed, use firebase
                        firebaseMessagingService.sendNewMessageNotificationToUser(
                            userId = member.userid,
                            messageContent = message.content,
                            msgId = message.id.toHexString(),
                            groupMessage = true,
                            groupName = groupName
                        )
                    }
                }

            }

        } else {
            //Single message

            //Try sending via socketconnection
            if (!socketConnectionHandler.sendMessage(
                    SocketConnectionMessage.MessageChange(
                        message = message.toMessageResponse(),
                        deleted = deleted,
                        newMessage = newMessage,
                    ),
                    receiverId = message.receiverId
                )) {

                //Message sending failed, use firebase if new, else ignore
                if (newMessage) {
                    firebaseMessagingService.sendNewMessageNotificationToUser(
                        userId = message.receiverId,
                        messageContent = message.content,
                        msgId = message.id.toHexString(),
                        groupMessage = false,
                        groupName = null
                    )
                }

            }
        }



    }


    fun notifyUserUpdate(user: User, deleted: Boolean) {
        //TODO
    }

    fun notifyFriendRequest(requestingUser: ObjectId, receivingUser: ObjectId, accepted: Boolean) {
        if (!socketConnectionHandler.sendMessage(
                SocketConnectionMessage.FriendRequest(
                    requestingUser = requestingUser.toHexString(),
                    requestingUserName = userLookupService.getUsername(requestingUser),
                    accepted = accepted,
                ),
                receiverId = receivingUser,
            )
        ) {
            firebaseMessagingService.sendFriendRequestNotificationToUser(
                senderId = requestingUser,
                receivingUserId = receivingUser,
                accepted = accepted
            )
        }
    }

}