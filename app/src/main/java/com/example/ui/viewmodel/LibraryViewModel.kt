package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.repository.RoomLibraryRepository
import com.example.domain.repository.LibraryRepository

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    val repository: LibraryRepository = RoomLibraryRepository(application)
}
