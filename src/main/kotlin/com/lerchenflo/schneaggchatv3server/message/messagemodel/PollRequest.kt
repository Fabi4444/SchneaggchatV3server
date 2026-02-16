package com.lerchenflo.schneaggchatv3server.message.messagemodel

import org.bson.types.ObjectId


/**
 * Request payload for creating a poll
 */
data class PollCreateRequest(
    val title: String,
    val description: String?,

    val maxAnswers: Int?, // null = unlimited
    val customAnswersEnabled: Boolean,
    val maxAllowedCustomAnswers: Int?, // null = unlimited

    val visibility: PollVisibility,

    val closeDate: Long?,

    val voteOptions: List<PollVoteOptionCreateRequest>,
)

/**
 * Data class needed for appending options when creating a poll
 */
data class PollVoteOptionCreateRequest(
    //Ids get assigned by the server
    val text: String,
)

/**
 * Vote in a poll
 */
data class PollVoteRequest(
    val id: String?, //Pass if available, else this is a new custom option
    val text: String?, //Pass if the id is null (New custom option with this text)
)

