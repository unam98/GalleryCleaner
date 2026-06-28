package com.unam.photocleaner.presentation

import android.content.IntentSender
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unam.photocleaner.data.local.MediaStoreDataSource
import com.unam.photocleaner.domain.model.PhotoGroup
import com.unam.photocleaner.domain.model.ScanFilter
import com.unam.photocleaner.domain.usecase.GroupPhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _filter = MutableStateFlow(ScanFilter())
    val filter: StateFlow<ScanFilter> = _filter.asStateFlow()

    private val _selectedGroup = MutableStateFlow<PhotoGroup?>(null)
    val selectedGroup: StateFlow<PhotoGroup?> = _selectedGroup.asStateFlow()

    private val _events = MutableSharedFlow<MainEvent>()
    val events: SharedFlow<MainEvent> = _events.asSharedFlow()

    private var pendingDeleteIds: List<Long> = emptyList()

    fun updateFilter(filter: ScanFilter) { _filter.value = filter }

    fun scan() {
        val f = _filter.value
        viewModelScope.launch {
            _state.value = UiState.Scanning()
            runCatching {
                val photos = mediaStore.getAllPhotos(
                    sinceMs = f.sinceTimestampMs(),
                    minSizeBytes = f.minSizeBytes,
                )
                _state.value = UiState.Scanning(current = 0, total = photos.size)
                groupPhotos.execute(photos) { current, total, label ->
                    _state.value = UiState.Scanning(current = current, total = total, label = label)
                }
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

    fun selectGroup(group: PhotoGroup) {
        _selectedGroup.value = group
    }

    fun clearGroupSelection() {
        _selectedGroup.value = null
    }

    fun requestDelete(photoIds: List<Long>) {
        viewModelScope.launch {
            pendingDeleteIds = photoIds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intentSender = mediaStore.createDeleteRequest(photoIds)
                _events.emit(MainEvent.RequestSystemDelete(intentSender))
            } else {
                mediaStore.deletePhotos(photoIds)
                applyDeletion(photoIds)
            }
        }
    }

    // Android 10+ 시스템 다이얼로그에서 사용자가 OK 누른 후 호출
    fun onSystemDeleteConfirmed() {
        applyDeletion(pendingDeleteIds)
        pendingDeleteIds = emptyList()
    }

    private fun applyDeletion(deletedIds: List<Long>) {
        val current = _state.value as? UiState.Done ?: return
        val deletedSet = deletedIds.toHashSet()

        val updatedGroups = current.groups.mapNotNull { group ->
            val remaining = group.photos.filterNot { it.id in deletedSet }
            if (remaining.size < 2) return@mapNotNull null
            val newBestId = if (group.bestPhotoId !in deletedSet) group.bestPhotoId
                            else remaining.first().id
            val newSaving = remaining.sumOf { it.size } - (remaining.find { it.id == newBestId }?.size ?: 0L)
            group.copy(photos = remaining, bestPhotoId = newBestId, potentialSavingBytes = newSaving)
        }

        _state.value = current.copy(
            groups = updatedGroups,
            totalSavingBytes = updatedGroups.sumOf { it.potentialSavingBytes },
        )
        // 현재 열려있는 그룹도 업데이트 (삭제 후 남은 사진 반영)
        val groupId = _selectedGroup.value?.id
        _selectedGroup.value = updatedGroups.find { it.id == groupId }
    }
}

sealed interface UiState {
    data object Idle : UiState
    data class Scanning(val current: Int = 0, val total: Int = 0, val label: String = "") : UiState
    data class Done(val groups: List<PhotoGroup>, val totalSavingBytes: Long) : UiState
    data class Error(val message: String) : UiState
}

sealed interface MainEvent {
    data class RequestSystemDelete(val intentSender: IntentSender) : MainEvent
}
