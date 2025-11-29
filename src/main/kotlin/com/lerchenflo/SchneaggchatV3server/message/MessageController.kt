package com.lerchenflo.schneaggchatv3server.message

import com.lerchenflo.schneaggchatv3server.repository.MessageRepository
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/messages")
@RestController
class MessageController(
    private val messageRepository: MessageRepository
) {

    @PostMapping("/send/text")
    fun sendTextMessage(){

    }


    /*
    @PostMapping("/send/image")

     */

}