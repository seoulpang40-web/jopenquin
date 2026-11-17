package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class DailyHealthLog(
  @DocumentId val id: String = "",
  val userId: String = "",
  val dateKey: String = "",
  val morningTaken: Boolean = false,
  val lunchTaken: Boolean = false,
  val dinnerTaken: Boolean = false,
  val waterCups: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class DailyHealthLogState(
  val log: DailyHealthLog,
  val existsInCloud: Boolean,
)

enum class MedicationSlot(val firestoreField: String) {
  MORNING("morningTaken"),
  LUNCH("lunchTaken"),
  DINNER("dinnerTaken"),
}
