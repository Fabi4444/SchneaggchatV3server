package com.lerchenflo.schneaggchatv3server.message.messagemodel

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.time.Instant

data class PollMessage(
    val creatorId: String,
    val title: String,
    val description: String?,

    val allowCustomAnswers: Boolean,
    val maxAnswers: Int?,

    val allowMultipleAnswers: Boolean,
    val maxCustomAnswers: Int?,

    val visibility: PollVisibility,


    val expiresAt: Instant?,

    val voteOptions: List<PollVoteOption> = emptyList(),
) {
    private val objectMapper = ObjectMapper()

    override fun toString(): String {
        return objectMapper.writeValueAsString(this)
    }
}

data class PollVoteOption(
    val id: String,
    val text: String,
    val custom: Boolean,
    val creatorId: String,
    val voters : List<PollVoter>
)

data class PollVoter(
    val userId: String,
    val votedAt: Instant,

)

enum class PollVisibility{
    PUBLIC,
    PRIVATE,
    ANONYMOUS
}