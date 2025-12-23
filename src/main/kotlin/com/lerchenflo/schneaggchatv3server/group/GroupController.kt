package com.lerchenflo.schneaggchatv3server.group

import com.lerchenflo.schneaggchatv3server.group.model.GroupResponse
import com.lerchenflo.schneaggchatv3server.user.UserController.IdTimeStamp
import com.lerchenflo.schneaggchatv3server.user.usermodel.UserService
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

@Component
@RequestMapping("/groups")
class GroupController(
    private val groupService: GroupService
) {

    @PostMapping("/create", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun register(
        @RequestParam("name") groupname: String,
        @RequestParam("memberlist") members: List<String>,
        @RequestParam("description") description: String,
        @RequestParam("profilepic") profilePic: MultipartFile
    ) : GroupResponse {
        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String ?: throw ResponseStatusException(
                /* status = */ HttpStatus.FORBIDDEN,
                /* reason = */ "Not logged in"
            )

        val group = groupService.createGroup(
            groupName = groupname,
            members = members.map { ObjectId(it) },
            creatorId = ObjectId(requestingUserId),
            profilePic = profilePic,
            description = description
        )

        return groupService.getGroupAsGroupResponse(group.id)
    }

    @PostMapping("/sync")
    fun syncGroups(
        @RequestBody requestBody: List<IdTimeStamp>
    ) : GroupService.GroupSyncResponse {
        val requestingUserId =
            SecurityContextHolder.getContext().authentication?.principal as? String ?: throw ResponseStatusException(
                /* status = */ HttpStatus.FORBIDDEN,
                /* reason = */ "Not logged in"
            )

        return groupService.syncGroups(
            userId = requestingUserId,
            ids = requestBody
        )
    }

}