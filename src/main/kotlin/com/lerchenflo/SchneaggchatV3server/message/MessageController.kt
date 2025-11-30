package com.lerchenflo.schneaggchatv3server.message

import com.lerchenflo.schneaggchatv3server.message.messagemodel.MessageRequest
import com.lerchenflo.schneaggchatv3server.repository.MessageRepository
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/messages")
@RestController
class MessageController(
    private val messageRepository: MessageRepository
) {

    @PostMapping("/send/text")
    fun sendTextMessage(
        @RequestBody messageRequest: MessageRequest
    ){

    }


    /*
    @PostMapping("/send/image")

     */

}