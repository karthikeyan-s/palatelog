package com.karthik.palatelog.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.karthik.palatelog.core.model.FamilyMember

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val avatarColorHex: String,
    val isPrimaryUser: Boolean
) {
    fun toDomain(): FamilyMember = FamilyMember(
        id = id,
        displayName = displayName,
        avatarColorHex = avatarColorHex,
        isPrimaryUser = isPrimaryUser
    )

    companion object {
        fun fromDomain(model: FamilyMember): FamilyMemberEntity = FamilyMemberEntity(
            id = model.id,
            displayName = model.displayName,
            avatarColorHex = model.avatarColorHex,
            isPrimaryUser = model.isPrimaryUser
        )
    }
}
