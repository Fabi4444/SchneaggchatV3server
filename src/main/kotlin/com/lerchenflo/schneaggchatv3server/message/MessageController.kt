package com.lerchenflo.schneaggchatv3server.message

import com.lerchenflo.schneaggchatv3server.message.messagemodel.MessageRequest
import com.lerchenflo.schneaggchatv3server.message.messagemodel.MessageResponse
import com.lerchenflo.schneaggchatv3server.message.messagemodel.toMessageResponse
import com.lerchenflo.schneaggchatv3server.user.UserController
import com.lerchenflo.schneaggchatv3server.user.usermodel.UserService
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RequestMapping("/messages")
@RestController
class MessageController(
    //private val messageRepository: MessageRepository //only use in the messageservice
    private val messageService: MessageService,
) {

    @PostMapping("/send/text")
    fun sendTextMessage(
        @RequestBody messageRequest: MessageRequest
    ): MessageResponse {

        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String ?: throw ResponseStatusException(
                /* status = */ HttpStatus.FORBIDDEN,
                /* reason = */ "Not logged in"
            )

        println("Message received: $messageRequest")
        val message = messageService.sendMessage(
            sender = ObjectId(requestingUserId),
            receiver = ObjectId(messageRequest.receiverId),
            groupMessage = messageRequest.groupMessage,
            messageType = messageRequest.msgType,
            content = MessageService.MessageContent.Text(messageRequest.content),
            answerId = if (messageRequest.answerId != null) ObjectId(messageRequest.answerId) else null
        )

        return message.toMessageResponse()
    }


    /*
    @PostMapping("/send/image")

     */


    @PostMapping("/sync")
    fun messageSync(
       @RequestParam(value = "page", defaultValue = "0") page: Int,
       @RequestParam(value = "page_size", defaultValue = "400") pageSize: Int,
       @RequestBody messageRequestList: List<UserService.IdTimeStamp>
    ): MessageService.MessageSyncResponse {

        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String ?: throw ResponseStatusException(
                /* status = */ HttpStatus.FORBIDDEN,
                /* reason = */ "Not logged in"
            )

        return messageService.messageSync(
            clientMessages = messageRequestList,
            requestingUser = ObjectId(requestingUserId),
            page = page,
            pageSize = pageSize,
        )

    }

    @PostMapping("/setread")
    fun setMessagesRead(
        @RequestParam(value = "userid") userId: String,
        @RequestParam(value = "group") group: Boolean,
        @RequestParam(value = "timestamp") timestamp: Long

    ){
        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String ?: throw ResponseStatusException(
                /* status = */ HttpStatus.FORBIDDEN,
                /* reason = */ "Not logged in"
            )

        messageService.setMessagesRead(
            ObjectId(requestingUserId), ObjectId(userId),
            group = group,
            timeStamp = timestamp,
        )
    }


    data class EditMessageRequest(
        val messageId: String,
        val newContent: String,
    )

    @PostMapping("/edit")
    fun editMessage(
        @RequestBody() editMessageRequest: EditMessageRequest
    ) : MessageResponse {

        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String ?: throw ResponseStatusException(
                /* status = */ HttpStatus.FORBIDDEN,
                /* reason = */ "Not logged in"
            )
        
        return messageService.editMessage(
            messageId = ObjectId(editMessageRequest.messageId),
            editingUserId = ObjectId(requestingUserId),
            newContent = editMessageRequest.newContent
        )
    }

    @DeleteMapping("/delete")
    fun deleteMessage(
        @RequestParam(value = "messageid") messageId: String
    ) {
        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String ?: throw ResponseStatusException(
                /* status = */ HttpStatus.FORBIDDEN,
                /* reason = */ "Not logged in"
            )

        messageService.deleteMessage(
            messageId = ObjectId(messageId),
            deletingUserId = ObjectId(requestingUserId),
        )
    }

}