package com.lerchenflo.schneaggchatv3server.notifications

import com.lerchenflo.schneaggchatv3server.notifications.firebase.FirebaseService
import com.lerchenflo.schneaggchatv3server.notifications.websocket.SocketConnectionHandler
import org.springframework.stereotype.Service

@Service
class NotificationService(
    private val socketConnectionHandler: SocketConnectionHandler,
    private val firebaseMessaging: FirebaseService
) {

}