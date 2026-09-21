package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
    indices = [Index(value = ["documentId", "pageNumber"])]
)
data class PageEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val pageNumber: Int,
    val imagePath: String,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val rotation: Int = 0,
    val processingState: String = "READY", // RAW, PROCESSING, READY, FAILED
    val ocrText: String = "",
    val notes: String = ""
)
