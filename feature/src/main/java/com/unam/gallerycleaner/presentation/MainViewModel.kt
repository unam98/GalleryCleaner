package com.unam.gallerycleaner.presentation

import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unam.gallerycleaner.data.local.AppPreferences
import com.unam.gallerycleaner.data.local.MediaStoreDataSource
import com.unam.gallerycleaner.data.local.db.FavoritePhotoDao
import com.unam.gallerycleaner.data.local.db.FavoritePhotoEntity
import com.unam.gallerycleaner.domain.model.MediaType
import com.unam.gallerycleaner.domain.model.Photo
import com.unam.gallerycleaner.domain.model.PhotoGroup
import com.unam.gallerycleaner.domain.model.ScanFilter
import com.unam.gallerycleaner.domain.usecase.GroupPhotosUseCase
import com.unam.gallerycleaner.domain.usecase.GroupVideosUseCase
import com.unam.gallerycleaner.util.MlKitLabelExtractor
import com.unam.gallerycleaner.work.ScanForegroundService
import com.unam.gallerycleaner.work.ScreenshotDetectorJob
import com.unam.gallerycleaner.domain.ScanNotifier
import com.unam.gallerycleaner.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val mediaStore: MediaStoreDataSource,
    private val groupPhotos: GroupPhotosUseCase,
    private val groupVideos: GroupVideosUseCase,
    private val labelExtractor: MlKitLabelExtractor,
    private val favoriteDao: FavoritePhotoDao,
    private val workScheduler: WorkScheduler,
    private val appPreferences: AppPreferences,
    private val scanNotifier: ScanNotifier,
    private val scanResultsCache: ScanResultsCache,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState>(
        // Application-scope singleton cache survives Activity recreation (config changes)
        scanResultsCache.cachedGroups?.let { groups ->
            UiState.Done(groups = groups, totalSavingBytes = scanResultsCache.cachedTotalSavingBytes)
        } ?: when (val saved = savedStateHandle.get<UiState>(KEY_STATE)) {
            is UiState.Done -> saved
            else -> UiState.Idle
        }
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _filter = MutableStateFlow(savedStateHandle.get<ScanFilter>(KEY_FILTER) ?: ScanFilter())
    val filter: StateFlow<ScanFilter> = _filter.asStateFlow()

    private val _selectedGroup = MutableStateFlow<PhotoGroup?>(
        savedStateHandle.get<String>(KEY_SELECTED_GROUP_ID)?.let { id ->
            (_state.value as? UiState.Done)?.groups?.find { it.id == id }
        }
    )
    val selectedGroup: StateFlow<PhotoGroup?> = _selectedGroup.asStateFlow()

    private val _events = MutableSharedFlow<MainEvent>()
    val events: SharedFlow<MainEvent> = _events.asSharedFlow()

    private val _photoLabels = MutableStateFlow<Map<Long, List<String>>>(emptyMap())

    private val _selectedCategories = MutableStateFlow<Set<String>>(
        savedStateHandle.get<String>(KEY_CATEGORIES)
            ?.split("|")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
    )
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories.asStateFlow()

    val favoriteIds: StateFlow<Set<Long>> = favoriteDao.observeAll()
        .map { it.toHashSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _periodicNotification = MutableStateFlow(appPreferences.periodicScanNotification)
    val periodicNotification: StateFlow<Boolean> = _periodicNotification.asStateFlow()

    private val _screenshotNotification = MutableStateFlow(appPreferences.screenshotNotification)
    val screenshotNotification: StateFlow<Boolean> = _screenshotNotification.asStateFlow()

    // 스캔 결과에서 실제로 매칭된 카테고리만 (CATEGORY_LABELS 순서 유지)
    val availableCategories: StateFlow<Set<String>> = combine(
        _state, _photoLabels,
    ) { state, labels ->
        val groups = (state as? UiState.Done)?.groups ?: return@combine emptySet()
        CATEGORY_LABELS.keys.filterTo(LinkedHashSet()) { category ->
            groups.any { group ->
                group.photos.any { photo ->
                    val photoLabels = labels[photo.id] ?: emptyList()
                    CATEGORY_LABELS[category]?.any { l ->
                        photoLabels.any { it.equals(l, ignoreCase = true) }
                    } == true
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    // 선택된 카테고리들의 AND 필터
    val filteredGroups: StateFlow<List<PhotoGroup>> = combine(
        _state, _photoLabels, _selectedCategories,
    ) { state, labels, categories ->
        val groups = (state as? UiState.Done)?.groups ?: return@combine emptyList()
        if (categories.isEmpty()) return@combine groups
        groups.filter { group ->
            categories.all { category ->
                group.photos.any { photo ->
                    val photoLabels = labels[photo.id] ?: emptyList()
                    CATEGORY_LABELS[category]?.any { l ->
                        photoLabels.any { it.equals(l, ignoreCase = true) }
                    } == true
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _tournamentState = MutableStateFlow<TournamentState?>(null)
    val tournamentState: StateFlow<TournamentState?> = _tournamentState.asStateFlow()

    private var pendingDeleteIds: List<Long> = emptyList()

    init {
        // 프로세스 재시작 후 Done 상태 복원 시 캐시 기반 라벨 복원
        (_state.value as? UiState.Done)?.let { done ->
            viewModelScope.launch(Dispatchers.IO) {
                val allPhotos = done.groups.flatMap { it.photos }.distinctBy { it.id }
                _photoLabels.value = labelExtractor.getLabelsForPhotos(allPhotos)
            }
        }

        viewModelScope.launch {
            _state.collect { state ->
                if (state !is UiState.Scanning) {
                    try {
                        savedStateHandle[KEY_STATE] = state
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // ── 스캔 ──────────────────────────────────────────────────────────────────

    fun updateFilter(filter: ScanFilter) {
        _filter.value = filter
        savedStateHandle[KEY_FILTER] = filter
    }

    fun toggleCategory(category: String) {
        val current = _selectedCategories.value
        _selectedCategories.value = if (category in current) current - category else current + category
        savedStateHandle[KEY_CATEGORIES] = _selectedCategories.value.joinToString("|")
    }

    fun clearCategories() {
        _selectedCategories.value = emptySet()
        savedStateHandle.remove<String>(KEY_CATEGORIES)
    }

    fun scan(overrideSinceMs: Long? = null) {
        val f = if (overrideSinceMs != null) ScanFilter(customSinceMs = overrideSinceMs) else _filter.value
        _selectedCategories.value = emptySet()
        _photoLabels.value = emptyMap()
        savedStateHandle.remove<String>(KEY_CATEGORIES)

        val startedAtMs = System.currentTimeMillis()

        viewModelScope.launch {
            _state.value = UiState.Scanning(startedAtMs = startedAtMs)
            runCatching {
                val sinceMs = f.sinceTimestampMs()
                val photos = if (f.mediaType != MediaType.VIDEO_ONLY)
                    mediaStore.getAllPhotos(sinceMs = sinceMs, minSizeBytes = f.minSizeBytes, maxCount = f.maxPhotoCount)
                else emptyList()
                val videos = if (f.mediaType != MediaType.PHOTO_ONLY)
                    mediaStore.getAllVideos(sinceMs = sinceMs, minSizeBytes = f.minSizeBytes, maxCount = f.maxPhotoCount)
                else emptyList()

                // 포그라운드 서비스 시작 (백그라운드 진행 보장)
                ScanForegroundService.start(context, photos.size)

                // 슬라이딩 윈도우 ETA: 최근 30개 아이템 처리 시각만으로 속도 추정
                val recentTimestamps = ArrayDeque<Long>(ETA_WINDOW)
                fun computeEta(current: Int, total: Int): Long {
                    val now = System.currentTimeMillis()
                    recentTimestamps.addLast(now)
                    if (recentTimestamps.size > ETA_WINDOW) recentTimestamps.removeFirst()
                    if (recentTimestamps.size < 2) return 0L
                    val windowElapsed = now - recentTimestamps.first()
                    val windowItems = (recentTimestamps.size - 1).coerceAtLeast(1)
                    val msPerItem = windowElapsed.toDouble() / windowItems
                    return ((total - current) * msPerItem).toLong()
                }

                _state.value = UiState.Scanning(current = 0, total = photos.size, startedAtMs = startedAtMs)
                val photoGroups = groupPhotos.execute(photos) { current, total, label ->
                    _state.value = UiState.Scanning(
                        current = current,
                        total = total,
                        label = label,
                        startedAtMs = startedAtMs,
                        etaMs = computeEta(current, total),
                    )
                }
                val videoGroups = groupVideos.execute(videos)
                val groups = (photoGroups + videoGroups).sortedByDescending { it.potentialSavingBytes }

                if (groups.isNotEmpty()) {
                    _state.value = UiState.Scanning(isLabelingPhase = true, startedAtMs = startedAtMs)
                    val allPhotos = groups.flatMap { it.photos }.distinctBy { it.id }
                    _photoLabels.value = withContext(Dispatchers.IO) {
                        labelExtractor.getLabelsForPhotos(allPhotos)
                    }
                }
                groups
            }.onSuccess { groups ->
                ScanForegroundService.stop(context)
                val totalSaving = groups.sumOf { it.potentialSavingBytes }
                scanResultsCache.save(groups, totalSaving)
                _state.value = UiState.Done(groups = groups, totalSavingBytes = totalSaving)
                scanNotifier.notifyScanDone(groups.size, totalSaving)
            }.onFailure { e ->
                ScanForegroundService.stop(context)
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun reset() {
        scanResultsCache.clear()
        _state.value = UiState.Idle
        _selectedCategories.value = emptySet()
        _photoLabels.value = emptyMap()
    }

    // ── 그룹/삭제 ────────────────────────────────────────────────────────────

    fun selectGroup(group: PhotoGroup) {
        _selectedGroup.value = group
        savedStateHandle[KEY_SELECTED_GROUP_ID] = group.id
    }

    fun clearGroupSelection() {
        _selectedGroup.value = null
        savedStateHandle.remove<String>(KEY_SELECTED_GROUP_ID)
    }

    fun requestDelete(photos: List<Photo>) {
        val ids = photos.map { it.id }
        val uris = photos.map { it.uri }
        viewModelScope.launch {
            pendingDeleteIds = ids
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intentSender = mediaStore.createDeleteRequest(uris)
                _events.emit(MainEvent.RequestSystemDelete(intentSender))
            } else {
                mediaStore.deleteMedia(uris)
                applyDeletion(ids)
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
        if (_selectedGroup.value == null) savedStateHandle.remove<String>(KEY_SELECTED_GROUP_ID)
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

    // ── 이상형 월드컵 ─────────────────────────────────────────────────────────

    fun startTournament(group: PhotoGroup) {
        val shuffled = group.photos.shuffled()
        _tournamentState.value = TournamentState(
            groupId = group.id,
            bracket = shuffled,
            nextRound = emptyList(),
            pairIndex = 0,
            roundNumber = 1,
            totalPhotos = shuffled.size,
        )
    }

    fun pickInTournament(photo: Photo) {
        val current = _tournamentState.value ?: return
        if (current.isComplete) return

        var newNextRound = current.nextRound + photo
        var newPairIndex = current.pairIndex + 1
        var bracket = current.bracket
        var roundNumber = current.roundNumber

        // 홀수 남은 사진 자동 진출
        while (newPairIndex * 2 < bracket.size) {
            val nextLeft = bracket.getOrNull(newPairIndex * 2)
            val nextRight = bracket.getOrNull(newPairIndex * 2 + 1)
            if (nextRight == null && nextLeft != null) {
                newNextRound = newNextRound + nextLeft
                newPairIndex++
            } else break
        }

        // 라운드 완료 시 다음 라운드로
        if (newPairIndex * 2 >= bracket.size) {
            bracket = newNextRound
            newNextRound = emptyList()
            newPairIndex = 0
            roundNumber++
        }

        _tournamentState.value = current.copy(
            bracket = bracket,
            nextRound = newNextRound,
            pairIndex = newPairIndex,
            roundNumber = roundNumber,
        )
    }

    fun closeTournament() {
        _tournamentState.value = null
    }

    fun keepTournamentWinner(winnerPhoto: Photo) {
        val tournament = _tournamentState.value ?: return
        val group = (_state.value as? UiState.Done)?.groups?.find { it.id == tournament.groupId } ?: return
        val toDelete = group.photos.filter { it.id != winnerPhoto.id }
        if (toDelete.isNotEmpty()) requestDelete(toDelete)
        _tournamentState.value = null
    }

    // ── 디버그 (DEBUG 빌드 전용) ──────────────────────────────────────────────

    fun debugTriggerPeriodicScan() {
        workScheduler.triggerNow()
    }

    fun debugTriggerScreenshotNotif() {
        scanNotifier.notifyScreenshotFavorite(
            photoId = -1L,
            displayName = "테스트_스크린샷.png",
        )
    }

    fun debugTriggerScanDoneNotif() {
        scanNotifier.notifyScanDone(groupCount = 3, savingBytes = 52_428_800L)
    }

    // ── 카테고리 정의 ────────────────────────────────────────────────────────

    companion object {
        private const val KEY_STATE = "ui_state"
        private const val KEY_FILTER = "scan_filter"
        private const val KEY_CATEGORIES = "categories"
        private const val KEY_SELECTED_GROUP_ID = "selected_group_id"
        private const val ETA_WINDOW = 30

        internal fun applyFilter(
            groups: List<PhotoGroup>,
            photoLabels: Map<Long, List<String>>,
            selectedCategories: Set<String>,
        ): List<PhotoGroup> {
            if (selectedCategories.isEmpty()) return groups
            return groups.filter { group ->
                selectedCategories.all { category ->
                    group.photos.any { photo ->
                        val labels = photoLabels[photo.id] ?: emptyList()
                        CATEGORY_LABELS[category]?.any { l ->
                            labels.any { it.equals(l, ignoreCase = true) }
                        } == true
                    }
                }
            }
        }

        val CATEGORY_LABELS = linkedMapOf(
            "인물" to setOf("Person", "Face", "Human", "People", "Man", "Woman", "Child", "Forehead", "Smile", "Selfie", "Hair"),
            "음식" to setOf("Food", "Dish", "Meal", "Cuisine", "Drink", "Ingredient", "Fast food", "Recipe", "Baking", "Vegetable", "Fruit", "Snack"),
            "풍경" to setOf("Sky", "Mountain", "Landscape", "Nature", "Beach", "Ocean", "Sea", "Forest", "Tree", "Flower", "River", "Lake", "Sunrise", "Sunset", "Cloud", "Field"),
            "동물" to setOf("Animal", "Dog", "Cat", "Bird", "Fish", "Pet", "Wildlife", "Mammal", "Insect", "Reptile"),
            "스크린샷" to setOf("Screenshot", "Font", "Software", "Display device", "Text", "Multimedia", "Technology"),
            "건물·실내" to setOf("Building", "Architecture", "Interior design", "Room", "House", "Furniture", "Urban area", "Street"),
        )
    }
}

sealed class UiState : Parcelable {
    @Parcelize
    data object Idle : UiState()

    @Parcelize
    data class Scanning(
        val current: Int = 0,
        val total: Int = 0,
        val label: String = "",
        val isLabelingPhase: Boolean = false,
        val startedAtMs: Long = 0L,
        val etaMs: Long = 0L,
    ) : UiState()

    @Parcelize
    data class Done(val groups: List<PhotoGroup>, val totalSavingBytes: Long) : UiState()

    @Parcelize
    data class Error(val message: String) : UiState()
}

sealed interface MainEvent {
    data class RequestSystemDelete(val intentSender: IntentSender) : MainEvent
}
