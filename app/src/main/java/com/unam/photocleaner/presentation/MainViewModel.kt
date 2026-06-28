package com.unam.photocleaner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unam.photocleaner.data.local.MediaStoreDataSource
import com.unam.photocleaner.domain.model.PhotoGroup
import com.unam.photocleaner.domain.usecase.GroupPhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val mediaStore: MediaStoreDataSource,
    private val groupPhotos: GroupPhotosUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun scan() {
        viewModelScope.launch {
            _state.value = UiState.Scanning
            runCatching {
                val photos = mediaStore.getAllPhotos()
                groupPhotos.execute(photos)
            }.onSuccess { groups ->
                _state.value = UiState.Done(
                    groups = groups,
                    totalSavingBytes = groups.sumOf { it.potentialSavingBytes },
                )
            }.onFailure { e ->
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun reset() {
        _state.value = UiState.Idle
    }
}

sealed interface UiState {
    data object Idle : UiState
    data object Scanning : UiState
    data class Done(val groups: List<PhotoGroup>, val totalSavingBytes: Long) : UiState
    data class Error(val message: String) : UiState
}
