@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.message

import com.lerchenflo.schneaggchatv3server.group.GroupService
import com.lerchenflo.schneaggchatv3server.message.MessageService.MessageContent.Image
import com.lerchenflo.schneaggchatv3server.message.MessageService.MessageContent.Text
import com.lerchenflo.schneaggchatv3server.message.messagemodel.Message
import com.lerchenflo.schneaggchatv3server.message.messagemodel.MessageResponse
import com.lerchenflo.schneaggchatv3server.message.messagemodel.MessageType
import com.lerchenflo.schneaggchatv3server.message.messagemodel.Reader
import com.lerchenflo.schneaggchatv3server.message.messagemodel.toMessageResponse
import com.lerchenflo.schneaggchatv3server.notifications.FirebaseService
import com.lerchenflo.schneaggchatv3server.repository.FirebaseTokenRepository
import com.lerchenflo.schneaggchatv3server.repository.MessageRepository
import com.lerchenflo.schneaggchatv3server.user.FriendsService
import com.lerchenflo.schneaggchatv3server.user.UserController
import com.lerchenflo.schneaggchatv3server.user.usermodel.UserService
import com.lerchenflo.schneaggchatv3server.util.ImageManager
import org.bson.types.ObjectId
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Component
class MessageService(
    private val mongoTemplate: MongoTemplate,
    private val messageRepository: MessageRepository,
    private val friendsService: FriendsService,
    private val groupService: GroupService,
    private val imageManager: ImageManager,
    private val firebaseService: FirebaseService,
    private val userService: UserService,
) {

    fun MessageContent.asString() : String {
        return when (this) {
            is Image -> "image"
            is Text -> message.take(300)
        }
    }
    public sealed class MessageContent {



        data class Text(val message: String) : MessageContent()
        data class Image(val image: MultipartFile) : MessageContent()
    }

    fun sendMessage(sender: ObjectId, receiver: ObjectId, groupMessage: Boolean, messageType: MessageType, content: MessageContent, answerId: ObjectId?) : Message {

        require(sender != receiver) { "You can not send messages to yourself" }

        //Only for single message
        require(friendsService.areFriends(sender, receiver)) { "You can not send messages to users who are not your friends" }

        println("Sendmessage: Group validation logic todo")
        //TODO: Group validation logic
        if (groupMessage) {

        }

        val savedObjectId = ObjectId()

        val storedContent = when(content) {
            is Image -> {
                imageManager.saveImageMessage(content.image, savedObjectId.toHexString())
            }
            is Text -> {
                content.message
            }
        }


        println("Sendmessage: Firebase sending")

        if (groupMessage) {
            println("Group message notification not implemented")
            //TODO: Group notifiation
        }else {
            println("Firebase message send start")
            firebaseService.sendNewMessageNotificationToUser(
                receiver, content.asString(),
                senderName = userService.getUsername(sender),
                msgId = savedObjectId.toString(),
            )
        }


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
            deleted = false,
            readers = listOf(Reader(
                userId = sender,
                readAt = sendDate
            )),
        ))
    }

    fun updateMessage(messageId: ObjectId, content: Any, answerId: ObjectId?, deleted: Boolean){
        //TODO: Change message
    }

    fun setMessagesRead(readingUser: ObjectId, chat: ObjectId, group: Boolean, timeStamp: Long) {
        val serverInstant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val clientInstant = Instant.fromEpochMilliseconds(timeStamp)

        // allowed difference: ±1 minute
        val maxDiff = Duration.parse("1m")

        val usedInstant =
            if ((serverInstant - clientInstant).absoluteValue <= maxDiff)
                clientInstant
            else
                serverInstant


        val conversationCriteria = Criteria().orOperator(
            Criteria().andOperator(
                Criteria.where("senderId").`is`(readingUser),
                Criteria.where("receiverId").`is`(chat)
            ),
            Criteria().andOperator(
                Criteria.where("senderId").`is`(chat),
                Criteria.where("receiverId").`is`(readingUser)
            )
        )

        val query = Query().addCriteria(
            conversationCriteria
                .and("groupMessage").`is`(group)
                // ensures we only touch docs that do NOT already have a readers entry for userA
                .and("readers.userId").ne(readingUser)
        )

        // Build reader object to push into the readers array
        val readerDoc = mapOf(
            "userId" to readingUser,
            "readAt" to usedInstant
        )

        val update = Update()
            .push("readers", readerDoc)
            .max("lastChanged", usedInstant)

        val result = mongoTemplate.updateMulti(query, update, "messages")

        println("Marked read — modifiedCount = ${result.modifiedCount}")
    }



    data class MessageSyncResponse(
        val updatedMessages: List<MessageResponse>,
        val deletedMessages: List<String>,
        val moreMessages: Boolean
    )

    fun messageSync(clientMessages: List<UserController.IdTimeStamp>, requestingUser: ObjectId, page: Int, pageSize: Int) : MessageSyncResponse {

        val clientMessagesMap = clientMessages.associate {
            it.id to it.timeStamp
        }

        val allMessages = getAllUserMessages(requestingUser)

        val messagesToAdd = allMessages
            .filter { it.id.toHexString() !in clientMessagesMap.keys }

        val messagesToUpdate = allMessages
            .filter { message ->
                clientMessagesMap[message.id.toHexString()]?.toLong()?.let { clientTimestamp ->
                    message.lastChanged.toEpochMilliseconds() > clientTimestamp
                } ?: false
            }

        val currentMessageIdStrings = allMessages.map { it.id.toHexString() }.toSet()
        val messagesToRemove = clientMessagesMap.keys.filter { it !in currentMessageIdStrings }

        // Combine and sort by sendDate descending (most recent first)
        val allMessagesToUpdate = (messagesToAdd + messagesToUpdate)
            .sortedByDescending { it.sendDate.toEpochMilliseconds() }

        // Apply pagination
        val startIndex = page * pageSize
        val endIndex = startIndex + pageSize

        val paginatedMessages = allMessagesToUpdate
            .drop(startIndex)
            .take(pageSize)
            .map { it.toMessageResponse() }

        val moreMessages = endIndex < allMessagesToUpdate.size

        return MessageSyncResponse(
            updatedMessages = paginatedMessages,
            deletedMessages = if (page == 0) messagesToRemove else emptyList(), // Only send deletes on first page
            moreMessages = moreMessages
        )
    }

    private fun getAllUserMessages(userId: ObjectId): List<Message> {
        val userGroups = groupService.getUserGroupIds(userId)

        val query = Query()
        // Build criteria: user is sender OR receiver OR (groupMessage AND receiver is in user's groups)
        val criteria = Criteria().orOperator(
            Criteria.where("senderId").`is`(userId),
            Criteria.where("receiverId").`is`(userId),
            Criteria().andOperator(
                Criteria.where("groupMessage").`is`(true),
                Criteria.where("receiverId").`in`(userGroups)
            )
        )

        query.addCriteria(criteria)

        query.addCriteria(Criteria.where("deleted").`is`(false))
        query.with(Sort.by(Sort.Direction.DESC, "sendDate"))


        return mongoTemplate.find(query, Message::class.java)
    }

}