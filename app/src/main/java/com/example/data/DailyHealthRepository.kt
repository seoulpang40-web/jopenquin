package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DailyHealthRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(expectedUserId: String): String {
    val uid =
      auth.currentUser?.uid
        ?: throw IllegalStateException("로그인이 필요합니다.")
    check(uid == expectedUserId) { "계정 정보가 일치하지 않습니다." }
    return uid
  }

  private fun validateDateKey(dateKey: String) {
    require(DATE_KEY_REGEX.matches(dateKey)) { "올바르지 않은 날짜 형식입니다: $dateKey" }
  }

  fun observeDailyLog(userId: String, dateKey: String): Flow<DailyHealthLogState> = flow {
    val uid = requireUserId(userId)
    validateDateKey(dateKey)
    val docPath = "users/$uid/daily_logs/$dateKey"
    val docRef = db.collection("users").document(uid).collection("daily_logs").document(dateKey)
    emitAll(
      docRef
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            val log =
              snapshot.toObject(
                DailyHealthLog::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              ) ?: DailyHealthLog(userId = uid, dateKey = dateKey)
            DailyHealthLogState(log = log, existsInCloud = true)
          } else {
            DailyHealthLogState(
              log = DailyHealthLog(userId = uid, dateKey = dateKey),
              existsInCloud = false,
            )
          }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.GET, docPath)
          }
          throw error
        }
    )
  }

  suspend fun toggleMedication(
    userId: String,
    dateKey: String,
    slot: MedicationSlot,
    currentLog: DailyHealthLog,
    existsInCloud: Boolean,
  ): Result<Unit> {
    val docPath = "users/$userId/daily_logs/$dateKey"
    return try {
      val uid = requireUserId(userId)
      validateDateKey(dateKey)
      val docRef = db.collection("users").document(uid).collection("daily_logs").document(dateKey)

      val nextMorning =
        if (slot == MedicationSlot.MORNING) !currentLog.morningTaken else currentLog.morningTaken
      val nextLunch =
        if (slot == MedicationSlot.LUNCH) !currentLog.lunchTaken else currentLog.lunchTaken
      val nextDinner =
        if (slot == MedicationSlot.DINNER) !currentLog.dinnerTaken else currentLog.dinnerTaken
      val newFieldValue =
        when (slot) {
          MedicationSlot.MORNING -> nextMorning
          MedicationSlot.LUNCH -> nextLunch
          MedicationSlot.DINNER -> nextDinner
        }

      val createPayload =
        mapOf(
          "userId" to uid,
          "dateKey" to dateKey,
          "morningTaken" to nextMorning,
          "lunchTaken" to nextLunch,
          "dinnerTaken" to nextDinner,
          "waterCups" to currentLog.waterCups.coerceIn(0L, MAX_WATER_CUPS),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      val updatePayload =
        mapOf(
          slot.firestoreField to newFieldValue,
          "updatedAt" to FieldValue.serverTimestamp(),
        )

      if (!existsInCloud) {
        try {
          docRef.set(createPayload).await()
        } catch (e: FirebaseFirestoreException) {
          if (e.code == FirebaseFirestoreException.Code.ALREADY_EXISTS) {
            docRef.update(updatePayload).await()
          } else {
            throw e
          }
        }
      } else {
        try {
          docRef.update(updatePayload).await()
        } catch (e: FirebaseFirestoreException) {
          if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
            docRef.set(createPayload).await()
          } else {
            throw e
          }
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (existsInCloud) OperationType.UPDATE else OperationType.CREATE,
        docPath,
      )
      Result.failure(e)
    }
  }

  suspend fun updateWaterCups(
    userId: String,
    dateKey: String,
    targetCups: Long,
    currentLog: DailyHealthLog,
    existsInCloud: Boolean,
  ): Result<Unit> {
    val docPath = "users/$userId/daily_logs/$dateKey"
    return try {
      val uid = requireUserId(userId)
      validateDateKey(dateKey)
      val clampedCups = targetCups.coerceIn(0L, MAX_WATER_CUPS)
      val docRef = db.collection("users").document(uid).collection("daily_logs").document(dateKey)

      val createPayload =
        mapOf(
          "userId" to uid,
          "dateKey" to dateKey,
          "morningTaken" to currentLog.morningTaken,
          "lunchTaken" to currentLog.lunchTaken,
          "dinnerTaken" to currentLog.dinnerTaken,
          "waterCups" to clampedCups,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      val updatePayload =
        mapOf(
          "waterCups" to clampedCups,
          "updatedAt" to FieldValue.serverTimestamp(),
        )

      if (!existsInCloud) {
        try {
          docRef.set(createPayload).await()
        } catch (e: FirebaseFirestoreException) {
          if (e.code == FirebaseFirestoreException.Code.ALREADY_EXISTS) {
            docRef.update(updatePayload).await()
          } else {
            throw e
          }
        }
      } else {
        try {
          docRef.update(updatePayload).await()
        } catch (e: FirebaseFirestoreException) {
          if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
            docRef.set(createPayload).await()
          } else {
            throw e
          }
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (existsInCloud) OperationType.UPDATE else OperationType.CREATE,
        docPath,
      )
      Result.failure(e)
    }
  }

  companion object {
    const val MAX_WATER_CUPS = 30L
    private val DATE_KEY_REGEX = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
  }
}
