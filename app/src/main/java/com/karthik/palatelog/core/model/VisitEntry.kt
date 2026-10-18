package com.karthik.palatelog.core.model

import java.util.UUID

enum class Channel(val displayName: String) {
    WALK_IN("Walk-in / Dine-in"),
    HIGHWAY_STOP("Highway Stop"),
    ZOMATO("Zomato"),
    SWIGGY("Swiggy"),
    TAKEAWAY("Takeaway"),
    DRIVE_THRU("Drive-Thru")
}

data class VisitEntry(
    val id: String = UUID.randomUUID().toString(),
    val restaurantId: String,
    val visitDate: Long = System.currentTimeMillis(),
    val channel: Channel = Channel.WALK_IN,
    val visitVerdict: Verdict = Verdict.GO,
    val billAmount: Double = 0.0,
    val billReceiptPhotoUri: String? = null,
    val generalNotes: String = "",
    val createdByMemberId: String = "",
    val createdByMemberName: String = "",
    val isShared: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
