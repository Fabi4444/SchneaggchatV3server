package com.lerchenflo.schneaggchatv3server.message

import com.lerchenflo.schneaggchatv3server.message.messagemodel.MessageRequest
import com.lerchenflo.schneaggchatv3server.message.messagemodel.MessageResponse
import com.lerchenflo.schneaggchatv3server.message.messagemodel.toMessageResponse
import com.lerchenflo.schneaggchatv3server.repository.MessageRepository
import jdk.internal.joptsimple.internal.Messages.message
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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

        //TODO: What returned in image message?
        return message.toMessageResponse()
    }


    /*
    @PostMapping("/send/image")

     */

}