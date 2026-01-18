package com.lerchenflo.schneaggchatv3server.notifications.websocket.model

import org.bson.types.ObjectId
import org.springframework.web.socket.WebSocketSession
import kotlin.time.Clock
import kotlin.time.Instant

data class SocketConnection (
    val sessionId: String,
    val userId: ObjectId,
    val session: WebSocketSession,

    val startedAt: Instant = Clock.System.now(),
)