package com.karthik.palatelog.core.model

import java.util.UUID

enum class Channel(val displayName: String) {
    WALK_IN("Walk-in / Dine-in"),
    HIGHWAY_STOP("Highway Stop"),
    ZOMATO("Zomato"),
    SWIGGY("Swiggy"),
    OWNLY("Ownly"),
    EATCLUB("EatClub"),
    MAGICPIN("Magicpin"),
    TAKEAWAY("Takeaway"),
    DRIVE_THRU("Drive-Thru");

    companion object {
        val DEFAULT_CHANNELS: List<String> = values().map { it.displayName }

        fun getChannelDisplayName(channelStr: String): String {
            if (channelStr.isBlank()) return WALK_IN.displayName
            val enumMatch = values().firstOrNull {
                it.name.equals(channelStr, ignoreCase = true) || it.displayName.equals(channelStr, ignoreCase = true)
            }
            if (enumMatch != null) return enumMatch.displayName
            return channelStr
        }
    }
}

data class VisitEntry(
    val id: String = UUID.randomUUID().toString(),
    val restaurantId: String,
    val visitDate: Long = System.currentTimeMillis(),
    val channel: String = Channel.WALK_IN.displayName,
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
