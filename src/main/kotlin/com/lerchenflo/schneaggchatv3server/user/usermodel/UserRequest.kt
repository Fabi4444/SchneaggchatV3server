package com.lerchenflo.schneaggchatv3server.user.usermodel

data class UserRequest(
    val userId: String,
    val newDescription: String?,
    val newStatus: String?,
    val newEmail: String?,
    val newBirthDate: String?,
    val newNickName: String?
)
