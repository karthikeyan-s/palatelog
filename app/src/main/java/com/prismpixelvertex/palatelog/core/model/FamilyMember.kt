package com.prismpixelvertex.palatelog.core.model

import java.util.UUID

data class FamilyMember(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val avatarColorHex: String = "#10B981",
    val isPrimaryUser: Boolean = false
)
