package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DailyHealthLog
import com.example.data.DailyHealthLogState
import com.example.data.DailyHealthRepository
import com.example.data.MedicationSlot
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class DailyHealthViewModel(
  private val repository: DailyHealthRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val _dateKey = MutableStateFlow(computeTodayDateKey())
  private val _retryTrigger = MutableStateFlow(0)

  private val _formattedDate = MutableStateFlow(computeFormattedTodayDate())
  val formattedDate: StateFlow<String> = _formattedDate.asStateFlow()

  private val _actionError = MutableStateFlow<String?>(null)
  val actionError: StateFlow<String?> = _actionError.asStateFlow()

  val uiState: StateFlow<UiState<DailyHealthLogState>> =
    combine(_dateKey, _retryTrigger) { dateKey, _ -> dateKey }
      .flatMapLatest { dateKey ->
        repository
          .observeDailyLog(currentUserId, dateKey)
          .map<DailyHealthLogState, UiState<DailyHealthLogState>> { UiState.Success(it) }
          .catch { error ->
            val firestoreError =
              generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing daily log", rootError)
            emit(UiState.Error("기록을 불러오지 못했어요. 인터넷 연결을 확인한 뒤 다시 시도해 주세요."))
          }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  fun refreshDateIfNeeded() {
    val latestKey = computeTodayDateKey()
    if (latestKey != _dateKey.value) {
      _dateKey.value = latestKey
    }
    _formattedDate.value = computeFormattedTodayDate()
  }

  fun retry() {
    _actionError.value = null
    _retryTrigger.value += 1
  }

  fun clearActionError() {
    _actionError.value = null
  }

  fun toggleMedication(slot: MedicationSlot) {
    val current = (uiState.value as? UiState.Success)?.data ?: return
    val dateKey = _dateKey.value
    viewModelScope.launch {
      val result =
        repository.toggleMedication(
          userId = currentUserId,
          dateKey = dateKey,
          slot = slot,
          currentLog = current.log,
          existsInCloud = current.existsInCloud,
        )
      if (result.isFailure) {
        _actionError.value = "저장에 실패했어요. 잠시 후 다시 눌러 주세요."
      }
    }
  }

  fun addWaterCup() {
    val current = (uiState.value as? UiState.Success)?.data ?: return
    if (current.log.waterCups >= DailyHealthRepository.MAX_WATER_CUPS) return
    val dateKey = _dateKey.value
    val nextCups = current.log.waterCups + 1L
    viewModelScope.launch {
      val result =
        repository.updateWaterCups(
          userId = currentUserId,
          dateKey = dateKey,
          targetCups = nextCups,
          currentLog = current.log,
          existsInCloud = current.existsInCloud,
        )
      if (result.isFailure) {
        _actionError.value = "물 기록 저장에 실패했어요. 잠시 후 다시 눌러 주세요."
      }
    }
  }

  fun removeWaterCup() {
    val current = (uiState.value as? UiState.Success)?.data ?: return
    if (current.log.waterCups <= 0L) return
    val dateKey = _dateKey.value
    val nextCups = (current.log.waterCups - 1L).coerceAtLeast(0L)
    viewModelScope.launch {
      val result =
        repository.updateWaterCups(
          userId = currentUserId,
          dateKey = dateKey,
          targetCups = nextCups,
          currentLog = current.log,
          existsInCloud = current.existsInCloud,
        )
      if (result.isFailure) {
        _actionError.value = "물 기록 수정에 실패했어요. 잠시 후 다시 눌러 주세요."
      }
    }
  }

  companion object {
    private const val TAG = "DailyHealthVM"
    private const val STOP_TIMEOUT_MILLIS = 5000L

    fun computeTodayDateKey(now: Date = Date()): String =
      SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)

    fun computeFormattedTodayDate(now: Date = Date()): String =
      SimpleDateFormat("yyyy년 M월 d일 (E)", Locale.KOREA).format(now)
  }
}
