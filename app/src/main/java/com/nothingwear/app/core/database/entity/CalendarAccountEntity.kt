package com.nothingwear.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_accounts")
data class CalendarAccountEntity(
    @PrimaryKey
    @ColumnInfo(name = "calendar_id")
    val calendarId: Long,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "account_name")
    val accountName: String,
    @ColumnInfo(name = "account_type")
    val accountType: String,
    @ColumnInfo(name = "color")
    val color: Int,
    @ColumnInfo(name = "is_selected")
    val isSelected: Boolean,
    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean,
)
