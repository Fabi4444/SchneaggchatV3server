@file:OptIn(ExperimentalTime::class)

package com.lerchenflo.schneaggchatv3server.message

import com.lerchenflo.schneaggchatv3server.group.GroupService
import com.lerchenflo.schneaggchatv3server.message.MessageService.MessageContent.Image
import com.lerchenflo.schneaggchatv3server.message.MessageService.MessageContent.Text
import com.lerchenflo.schneaggchatv3server.message.messagemodel.*
import com.lerchenflo.schneaggchatv3server.notifications.FirebaseService
import com.lerchenflo.schneaggchatv3server.repository.MessageRepository
import com.lerchenflo.schneaggchatv3server.user.FriendsService
import com.lerchenflo.schneaggchatv3server.user.usermodel.UserService
import com.lerchenflo.schneaggchatv3server.util.ImageManager
import org.bson.types.ObjectId
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
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
    sealed class MessageContent {
        data class Text(val message: String) : MessageContent()
        data class Image(val image: MultipartFile) : MessageContent()
    }

    fun sendMessage(sender: ObjectId, receiver: ObjectId, groupMessage: Boolean, messageType: MessageType, content: MessageContent, answerId: ObjectId?) : Message {

        canUserAccessMessage(
            sender = sender,
            receiver = receiver,
            groupMessage = groupMessage,
        )


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
            val members = groupService.getGroupMembers(receiver)
            val group = groupService.getGroupById(receiver)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found")

            members
                .filter { it.userid != sender } //Do not send notification to sender
                .forEach { member ->
                firebaseService.sendNewMessageNotificationToUser(
                    userId = member.userid,
                    messagecontent = content.asString(),
                    senderName = userService.getUsername(sender),
                    msgId = savedObjectId.toHexString(),
                    groupMessage = true,
                    groupName = group.name,
                )
            }


        }else {
            //println("Firebase message send start")
            firebaseService.sendNewMessageNotificationToUser(
                receiver, content.asString(),
                senderName = userService.getUsername(sender),
                msgId = savedObjectId.toString(),
                groupMessage = false
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

    fun editMessage(messageId: ObjectId, editingUserId: ObjectId, newContent: String) : MessageResponse {

        //TODO: Text validation

        val message = canUserAccessMessage(messageId, editingUserId)

        //User can access message, change content
        val now = Clock.System.now()

        val newmessage = messageRepository.save(message.copy(
            lastChanged = now,
            content = newContent,
            edited = true
        ))

        return newmessage.toMessageResponse()
    }

    fun deleteMessage(messageId: ObjectId, deletingUserId: ObjectId) {
        val message = canUserAccessMessage(messageId, deletingUserId)

        require(message.senderId == deletingUserId) { "Only the sender can delete a message" }

        messageRepository.save(message.copy(deleted = true))
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

        val query = if (group) {
            // For group messages: find all messages sent to this group
            // that the user hasn't read yet
            if (!groupService.isUserInGroup(readingUser, chat)) {
                println("User $readingUser is not a member of group $chat")
                return
            }
            
            Query().addCriteria(
                Criteria.where("receiverId").`is`(chat)
                    .and("groupMessage").`is`(true)
                    .and("readers.userId").ne(readingUser)
            )
        } else {
            // For direct messages: find messages between the two users
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
            
            Query().addCriteria(
                conversationCriteria
                    .and("groupMessage").`is`(false)
                    .and("readers.userId").ne(readingUser)
            )
        }

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

    fun messageSync(clientMessages: List<UserService.IdTimeStamp>, requestingUser: ObjectId, page: Int, pageSize: Int) : MessageSyncResponse {

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


    private fun canUserAccessMessage(
        messageId: ObjectId,
        userId: ObjectId,
    ) : Message {
        val message = messageRepository.findById(messageId).get()

        if (message.groupMessage) {
            canUserAccessMessage(userId, message.receiverId, true)
        } else {
            // For direct messages, user must be either sender or receiver
            require(message.senderId == userId || message.receiverId == userId) {
                "You do not have access to this message"
            }
        }

        return message
    }


    /**
     * Check if a user can access a message. throws if not
     */
    private fun canUserAccessMessage(sender: ObjectId, receiver: ObjectId, groupMessage: Boolean) {
        if (groupMessage) {
            require(groupService.isUserInGroup(sender, receiver)) {
                "You are not a member of this group"
            }
        } else {
            //Single message
            require(sender != receiver) {
                "You can not send messages to yourself"
            }
            require(friendsService.areFriends(sender, receiver)) {
                "You can not send messages to users who are not your friends"
            }
        }
    }

}