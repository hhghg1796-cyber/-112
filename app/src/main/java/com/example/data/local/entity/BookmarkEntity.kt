package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["documentId"])]
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val pageId: String? = null,
    val pageNumber: Int,
    val title: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
