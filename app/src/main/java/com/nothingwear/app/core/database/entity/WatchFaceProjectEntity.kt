package com.nothingwear.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_face_projects")
data class WatchFaceProjectEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "category")
    val category: String,
    @ColumnInfo(name = "dial_style")
    val dialStyle: String,
    @ColumnInfo(name = "ring_style")
    val ringStyle: String,
    @ColumnInfo(name = "font_style")
    val fontStyle: String,
    @ColumnInfo(name = "background_color_argb")
    val backgroundColorArgb: Long,
    @ColumnInfo(name = "accent_color_argb")
    val accentColorArgb: Long,
    @ColumnInfo(name = "text_color_argb")
    val textColorArgb: Long,
    @ColumnInfo(name = "is_24_hour")
    val is24Hour: Boolean,
    @ColumnInfo(name = "show_seconds")
    val showSeconds: Boolean,
    @ColumnInfo(name = "time_scale")
    val timeScale: Float,
    @ColumnInfo(name = "top_complication")
    val topComplication: String,
    @ColumnInfo(name = "bottom_complication")
    val bottomComplication: String,
    @ColumnInfo(name = "left_complication")
    val leftComplication: String,
    @ColumnInfo(name = "right_complication")
    val rightComplication: String,
    @ColumnInfo(name = "updated_at_millis")
    val updatedAtMillis: Long,
)
