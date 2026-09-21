package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // DOCUMENT, MAGAZINE, BOOK, COLLECTION
    val author: String? = null,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val pageCount: Int = 0,
    val thumbnailPath: String? = null,
    val isFavorite: Boolean = false,
    val lastReadPage: Int = 1,
    val ocrStatus: String = "NOT_PROCESSED",
    val fileSizeFormatted: String = "0 بايت",
    val subjectCategory: String? = null
)
