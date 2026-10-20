package com.prismpixelvertex.palatelog.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.prismpixelvertex.palatelog.core.model.Channel
import com.prismpixelvertex.palatelog.core.model.Verdict
import com.prismpixelvertex.palatelog.core.model.VisitEntry

@Entity(
    tableName = "visit_entries",
    foreignKeys = [
        ForeignKey(
            entity = RestaurantEntity::class,
            parentColumns = ["id"],
            childColumns = ["restaurantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["restaurantId"]),
        Index(value = ["visitDate"]),
        Index(value = ["channel"])
    ]
)
data class VisitEntryEntity(
    @PrimaryKey
    val id: String,
    val restaurantId: String,
    val visitDate: Long,
    val channel: String,
    val visitVerdict: String,
    val billAmount: Double,
    val billReceiptPhotoUri: String?,
    val generalNotes: String,
    val createdByMemberId: String,
    val createdByMemberName: String,
    val isShared: Boolean,
    val updatedAt: Long,
    val isDeleted: Boolean
) {
    fun toDomain(): VisitEntry = VisitEntry(
        id = id,
        restaurantId = restaurantId,
        visitDate = visitDate,
        channel = Channel.getChannelDisplayName(channel),
        visitVerdict = try { Verdict.valueOf(visitVerdict) } catch (e: Exception) { Verdict.GO },
        billAmount = billAmount,
        billReceiptPhotoUri = billReceiptPhotoUri,
        generalNotes = generalNotes,
        createdByMemberId = createdByMemberId,
        createdByMemberName = createdByMemberName,
        isShared = isShared,
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    companion object {
        fun fromDomain(model: VisitEntry): VisitEntryEntity = VisitEntryEntity(
            id = model.id,
            restaurantId = model.restaurantId,
            visitDate = model.visitDate,
            channel = model.channel,
            visitVerdict = model.visitVerdict.name,
            billAmount = model.billAmount,
            billReceiptPhotoUri = model.billReceiptPhotoUri,
            generalNotes = model.generalNotes,
            createdByMemberId = model.createdByMemberId,
            createdByMemberName = model.createdByMemberName,
            isShared = model.isShared,
            updatedAt = model.updatedAt,
            isDeleted = model.isDeleted
        )
    }
}
