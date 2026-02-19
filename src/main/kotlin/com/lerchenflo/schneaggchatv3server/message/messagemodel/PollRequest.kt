package com.lerchenflo.schneaggchatv3server.message.messagemodel

import com.lerchenflo.schneaggchatv3server.message.MessageService
import org.bson.types.ObjectId
import kotlin.time.Clock
import kotlin.time.Instant


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


fun PollCreateRequest.toPoll(creatorId: ObjectId) : PollMessage {
    return PollMessage(
        creatorId = creatorId,
        title = this.title.trim(),
        description = this.description?.trim(),
        maxAnswers = this.maxAnswers,
        customAnswersEnabled = this.customAnswersEnabled,
        maxAllowedCustomAnswers = this.maxAllowedCustomAnswers,
        visibility = this.visibility,
        closeDate = if (this.closeDate != null) Instant.fromEpochMilliseconds(this.closeDate) else null,
        voteOptions = this.voteOptions.map {
            PollVoteOption(
                id = ObjectId.get().toHexString(),
                text = it.text,
                custom = false,
                creatorId = creatorId,
                voters = emptyList(),
            )
        }
    )
}




/**
 * Vote in a poll
 */
data class PollVoteRequest(
    val messageId: String,
    val id: String?, //Pass if available, else this is a new custom option
    val text: String?, //Pass if the id is null (New custom option with this text)
    val selected: Boolean, //Did the user select or unselect this item
)




