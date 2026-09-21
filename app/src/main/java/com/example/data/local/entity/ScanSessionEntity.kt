package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "scan_sessions")
data class ScanSessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val documentType: String = "DOCUMENT",
    val suggestedTitle: String = "وثيقة ممسوحة جديدة"
)

@Entity(
    tableName = "scan_session_pages",
    indices = [Index(value = ["sessionId", "pageNumber"])]
)
data class ScanSessionPageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val pageNumber: Int,
    val imagePath: String,
    val thumbnailPath: String? = null,
    val rotationDegrees: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
