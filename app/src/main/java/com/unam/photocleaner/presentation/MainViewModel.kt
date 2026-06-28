package com.unam.photocleaner.presentation

import android.content.Context
import android.content.IntentSender
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unam.photocleaner.data.local.AppPreferences
import com.unam.photocleaner.data.local.MediaStoreDataSource
import com.unam.photocleaner.data.local.db.FavoritePhotoDao
import com.unam.photocleaner.data.local.db.FavoritePhotoEntity
import com.unam.photocleaner.domain.model.PhotoGroup
import com.unam.photocleaner.domain.model.ScanFilter
import com.unam.photocleaner.domain.usecase.GroupPhotosUseCase
import com.unam.photocleaner.util.MlKitLabelExtractor
import com.unam.photocleaner.work.ScreenshotDetectorJob
import com.unam.photocleaner.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaStore: MediaStoreDataSource,
    private val groupPhotos: GroupPhotosUseCase,
    private val labelExtractor: MlKitLabelExtractor,
    private val favoriteDao: FavoritePhotoDao,
    private val workScheduler: WorkScheduler,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _filter = MutableStateFlow(ScanFilter())
    val filter: StateFlow<ScanFilter> = _filter.asStateFlow()

    private val _selectedGroup = MutableStateFlow<PhotoGroup?>(null)
    val selectedGroup: StateFlow<PhotoGroup?> = _selectedGroup.asStateFlow()

    private val _events = MutableSharedFlow<MainEvent>()
    val events: SharedFlow<MainEvent> = _events.asSharedFlow()

    // ML Kit 레이블 캐시 (photoId → labels)
    private val _photoLabels = MutableStateFlow<Map<Long, List<String>>>(emptyMap())

    private val _isLabeling = MutableStateFlow(false)
    val isLabeling: StateFlow<Boolean> = _isLabeling.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _keyword = MutableStateFlow("")
    val keyword: StateFlow<String> = _keyword.asStateFlow()

    // 즐겨찾기 (Room Flow)
    val favoriteIds: StateFlow<Set<Long>> = favoriteDao.observeAll()
        .map { it.toHashSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    // 설정
    private val _periodicNotification = MutableStateFlow(appPreferences.periodicScanNotification)
    val periodicNotification: StateFlow<Boolean> = _periodicNotification.asStateFlow()

    private val _screenshotNotification = MutableStateFlow(appPreferences.screenshotNotification)
    val screenshotNotification: StateFlow<Boolean> = _screenshotNotification.asStateFlow()

    // 필터 적용된 그룹 목록
    val filteredGroups: StateFlow<List<PhotoGroup>> = combine(
        _state, _photoLabels, _selectedCategory, _keyword,
    ) { state, labels, category, keyword ->
        val groups = (state as? UiState.Done)?.groups ?: return@combine emptyList()
        if (category == null && keyword.isBlank()) return@combine groups
        groups.filter { group ->
            group.photos.any { photo ->
                val photoLabels = labels[photo.id] ?: emptyList()
                val matchesCategory = category == null ||
                    CATEGORY_LABELS[category]?.any { l -> photoLabels.any { it.equals(l, ignoreCase = true) } } == true
                val matchesKeyword = keyword.isBlank() || run {
                    val enKeyword = KO_EN_MAP[keyword.trim().lowercase()] ?: keyword.trim()
                    photoLabels.any { it.contains(enKeyword, ignoreCase = true) } ||
                        photo.displayName.contains(keyword.trim(), ignoreCase = true)
                }
                matchesCategory && matchesKeyword
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var pendingDeleteIds: List<Long> = emptyList()

    // ── 스캔 ──────────────────────────────────────────────────────────────────

    fun updateFilter(filter: ScanFilter) { _filter.value = filter }
    fun setCategory(category: String?) { _selectedCategory.value = category }
    fun setKeyword(keyword: String) { _keyword.value = keyword }

    fun scan(overrideSinceMs: Long? = null) {
        val f = if (overrideSinceMs != null) ScanFilter(customSinceMs = overrideSinceMs) else _filter.value
        _selectedCategory.value = null
        _keyword.value = ""
        _photoLabels.value = emptyMap()
        _isLabeling.value = false

        viewModelScope.launch {
            _state.value = UiState.Scanning()
            runCatching {
                val photos = mediaStore.getAllPhotos(
                    sinceMs = f.sinceTimestampMs(),
                    minSizeBytes = f.minSizeBytes,
                    maxCount = f.maxPhotoCount,
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
                startLabeling(groups)
            }.onFailure { e ->
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun startLabeling(groups: List<PhotoGroup>) {
        if (groups.isEmpty()) return
        _isLabeling.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val allPhotos = groups.flatMap { it.photos }.distinctBy { it.id }
            val labels = mutableMapOf<Long, List<String>>()
            allPhotos.forEach { photo ->
                labels[photo.id] = labelExtractor.getLabels(photo.uri)
                _photoLabels.value = labels.toMap()
            }
            _isLabeling.value = false
        }
    }

    fun reset() {
        _state.value = UiState.Idle
        _selectedCategory.value = null
        _keyword.value = ""
        _photoLabels.value = emptyMap()
        _isLabeling.value = false
    }

    // ── 그룹/삭제 ────────────────────────────────────────────────────────────

    fun selectGroup(group: PhotoGroup) { _selectedGroup.value = group }
    fun clearGroupSelection() { _selectedGroup.value = null }

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
        val groupId = _selectedGroup.value?.id
        _selectedGroup.value = updatedGroups.find { it.id == groupId }
    }

    // ── 즐겨찾기 ─────────────────────────────────────────────────────────────

    fun toggleFavorite(photoId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (photoId in favoriteIds.value) {
                favoriteDao.delete(photoId)
            } else {
                favoriteDao.insert(FavoritePhotoEntity(photoId))
            }
        }
    }

    // ── 설정 ─────────────────────────────────────────────────────────────────

    fun setPeriodicNotification(enabled: Boolean) {
        appPreferences.periodicScanNotification = enabled
        _periodicNotification.value = enabled
        if (enabled) workScheduler.schedulePeriodicScan() else workScheduler.cancelScan()
    }

    fun setScreenshotNotification(enabled: Boolean) {
        appPreferences.screenshotNotification = enabled
        _screenshotNotification.value = enabled
        if (enabled) ScreenshotDetectorJob.schedule(context)
        else ScreenshotDetectorJob.cancel(context)
    }

    // ── 카테고리 정의 ────────────────────────────────────────────────────────

    companion object {
        val CATEGORY_LABELS = linkedMapOf(
            "인물" to setOf("Person", "Face", "Human", "People", "Man", "Woman", "Child", "Forehead", "Smile", "Selfie", "Hair"),
            "음식" to setOf("Food", "Dish", "Meal", "Cuisine", "Drink", "Ingredient", "Fast food", "Recipe", "Baking", "Vegetable", "Fruit", "Snack"),
            "풍경" to setOf("Sky", "Mountain", "Landscape", "Nature", "Beach", "Ocean", "Sea", "Forest", "Tree", "Flower", "River", "Lake", "Sunrise", "Sunset", "Cloud", "Field"),
            "동물" to setOf("Animal", "Dog", "Cat", "Bird", "Fish", "Pet", "Wildlife", "Mammal", "Insect", "Reptile"),
            "스크린샷" to setOf("Screenshot", "Font", "Software", "Display device", "Text", "Multimedia", "Technology"),
            "건물·실내" to setOf("Building", "Architecture", "Interior design", "Room", "House", "Furniture", "Urban area", "Street"),
        )

        private val KO_EN_MAP = mapOf(
            "인물" to "person", "사람" to "person", "얼굴" to "face",
            "음식" to "food", "밥" to "food", "요리" to "cuisine",
            "풍경" to "landscape", "하늘" to "sky", "산" to "mountain", "바다" to "ocean", "꽃" to "flower", "나무" to "tree",
            "동물" to "animal", "강아지" to "dog", "개" to "dog", "고양이" to "cat", "새" to "bird",
            "스크린샷" to "screenshot", "화면" to "screenshot",
            "건물" to "building", "집" to "house", "실내" to "interior",
            "자동차" to "vehicle", "차" to "vehicle",
        )
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
