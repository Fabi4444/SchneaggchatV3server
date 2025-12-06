@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.message

import com.lerchenflo.schneaggchatv3server.message.messagemodel.Message
import com.lerchenflo.schneaggchatv3server.message.messagemodel.MessageType
import com.lerchenflo.schneaggchatv3server.repository.MessageRepository
import com.lerchenflo.schneaggchatv3server.user.FriendsService
import com.lerchenflo.schneaggchatv3server.util.ImageManager
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Component
class MessageService(
    private val messageRepository: MessageRepository,
    private val friendsService: FriendsService,
    private val imageManager: ImageManager,
) {


    public sealed class MessageContent {
        data class Text(val message: String) : MessageContent()
        data class Image(val image: MultipartFile) : MessageContent()
    }

    fun sendMessage(sender: ObjectId, receiver: ObjectId, groupMessage: Boolean, messageType: MessageType, content: MessageContent, answerId: ObjectId?) : Message {

        require(sender != receiver) { "You can not send messages to yourself" }

        require(friendsService.areFriends(sender, receiver)) { "You can not send messages to users who are not your friends" }

        //TODO: Group validation logic
        if (groupMessage) {

        }

        val savedObjectId = ObjectId()

        val storedContent = when(content) {
            is MessageContent.Image -> {
                imageManager.saveImageMessage(content.image, savedObjectId.toHexString())
            }
            is MessageContent.Text -> {
                content.message
            }
        }

        //TODO: Firebase notification

        val sendDate = Clock.System.now()

        return messageRepository.save(Message(
            id = savedObjectId,
            senderId = sender,
            receiverId = receiver,
            groupMessage = groupMessage,
            msgType = messageType,
            content = storedContent,
            answerId = answerId,
            sendDate = sendDate,
            lastChanged = sendDate,
            deleted = false
        ))
    }

    fun updateMessage(messageId: ObjectId, content: Any, answerId: ObjectId?, deleted: Boolean){
        //TODO: Change message
    }

}