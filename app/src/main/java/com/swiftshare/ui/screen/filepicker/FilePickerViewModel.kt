package com.swiftshare.ui.screen.filepicker

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swiftshare.domain.model.FileItem
import com.swiftshare.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FilePickerViewModel @Inject constructor(
    private val fileRepository: FileRepository,
) : ViewModel() {

    private val _selectedFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val selectedFiles: StateFlow<List<FileItem>> = _selectedFiles.asStateFlow()

    fun addFiles(uris: List<Uri>) {
        viewModelScope.launch {
            val newFiles = fileRepository.resolveUris(uris)
            _selectedFiles.update { current ->
                val existingUris = current.map { it.uri }.toSet()
                current + newFiles.filter { it.uri !in existingUris }
            }
        }
    }

    fun removeFile(fileItem: FileItem) {
        _selectedFiles.update { current ->
            current.filter { it.uri != fileItem.uri }
        }
    }

    fun clearFiles() {
        _selectedFiles.value = emptyList()
    }
}
