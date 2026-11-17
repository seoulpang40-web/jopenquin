package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.DailyHealthLog
import com.example.data.DailyHealthLogState
import com.example.data.DailyHealthRepository
import com.example.data.MedicationSlot
import com.example.ui.theme.CoralWarm
import com.example.ui.theme.CoralWarmSoft
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.ErrorSoft
import com.example.ui.theme.IvoryBackground
import com.example.ui.theme.IvoryBorder
import com.example.ui.theme.IvorySurface
import com.example.ui.theme.IvorySurfaceWarm
import com.example.ui.theme.LeafGreen
import com.example.ui.theme.LeafGreenDark
import com.example.ui.theme.LeafGreenSoft
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.OceanBlueBorder
import com.example.ui.theme.OceanBlueSurface
import com.example.ui.theme.TwilightIndigo
import com.example.ui.theme.TwilightIndigoSoft
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberSoft
import com.example.ui.theme.WarmSlate

@Composable
fun DailyHealthScreen(
  viewModel: DailyHealthViewModel,
  userEmail: String?,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val formattedDate by viewModel.formattedDate.collectAsStateWithLifecycle()
  val actionError by viewModel.actionError.collectAsStateWithLifecycle()

  var showAccountDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  Scaffold(
    containerColor = IvoryBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    bottomBar = {
      if (uiState is UiState.Success) {
        val log = (uiState as UiState.Success<DailyHealthLogState>).data.log
        WaterBottomActionBar(
          canAddMore = log.waterCups < DailyHealthRepository.MAX_WATER_CUPS,
          onAddWaterClick = viewModel::addWaterCup,
        )
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 600.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        // 1. Prominent Today's Date Header + Account button
        DateHeaderCard(
          formattedDate = formattedDate,
          uiState = uiState,
          onAccountClick = { showAccountDialog = true },
        )

        if (actionError != null) {
          Surface(
            color = ErrorSoft,
            border = BorderStroke(1.5.dp, ErrorCrimson),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = actionError!!,
                fontSize = 22.sp,
                lineHeight = 30.sp,
                color = ErrorCrimson,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
              )
              Spacer(modifier = Modifier.width(12.dp))
              TextButton(
                onClick = viewModel::clearActionError,
                modifier = Modifier.heightIn(min = 48.dp),
              ) {
                Text(
                  text = stringResource(R.string.dialog_close),
                  fontSize = 22.sp,
                  fontWeight = FontWeight.Bold,
                  color = ErrorCrimson,
                )
              }
            }
          }
        }

        when (val state = uiState) {
          is UiState.Loading -> {
            LoadingCard()
          }
          is UiState.Error -> {
            ErrorCard(message = state.message, onRetry = viewModel::retry)
          }
          is UiState.Success -> {
            val log = state.data.log

            // 2. Vertical Big Medication Buttons ('아침 약', '점심 약', '저녁 약')
            Text(
              text = stringResource(R.string.med_section_title),
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              color = DeepCharcoal,
              modifier = Modifier.padding(top = 4.dp),
            )

            Column(
              verticalArrangement = Arrangement.spacedBy(18.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              MedicationCheckButton(
                title = stringResource(R.string.med_morning),
                isTaken = log.morningTaken,
                icon = Icons.Rounded.WbSunny,
                badgeColor = WarmAmberSoft,
                badgeTint = WarmAmber,
                testTag = "med_button_morning",
                onClick = { viewModel.toggleMedication(MedicationSlot.MORNING) },
              )
              MedicationCheckButton(
                title = stringResource(R.string.med_lunch),
                isTaken = log.lunchTaken,
                icon = Icons.Rounded.LightMode,
                badgeColor = CoralWarmSoft,
                badgeTint = CoralWarm,
                testTag = "med_button_lunch",
                onClick = { viewModel.toggleMedication(MedicationSlot.LUNCH) },
              )
              MedicationCheckButton(
                title = stringResource(R.string.med_dinner),
                isTaken = log.dinnerTaken,
                icon = Icons.Rounded.NightsStay,
                badgeColor = TwilightIndigoSoft,
                badgeTint = TwilightIndigo,
                testTag = "med_button_dinner",
                onClick = { viewModel.toggleMedication(MedicationSlot.DINNER) },
              )
            }

            // 3. Water Intake Counter & Visual Cups Card
            WaterIntakeCard(
              waterCups = log.waterCups.toInt(),
              onUndoClick = viewModel::removeWaterCup,
            )

            Spacer(modifier = Modifier.height(8.dp))
          }
        }
      }
    }
  }

  if (showAccountDialog) {
    AlertDialog(
      onDismissRequest = { showAccountDialog = false },
      containerColor = IvorySurface,
      title = {
        Text(
          text = stringResource(R.string.account_dialog_title),
          fontSize = 26.sp,
          fontWeight = FontWeight.Bold,
          color = DeepCharcoal,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = userEmail?.takeIf { it.isNotBlank() } ?: "Google 계정 연결됨",
            fontSize = 22.sp,
            lineHeight = 30.sp,
            color = WarmSlate,
          )
          Text(
            text = "오늘 체크한 약과 물 기록은 안전하게 저장됩니다.",
            fontSize = 22.sp,
            lineHeight = 30.sp,
            color = DeepCharcoal,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showAccountDialog = false
            signOut(credentialManager = credentialManager, scope = scope)
          },
          colors = ButtonDefaults.buttonColors(containerColor = ErrorCrimson, contentColor = Color.White),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.heightIn(min = 52.dp).testTag("sign_out_button"),
        ) {
          Text(
            text = stringResource(R.string.account_sign_out),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showAccountDialog = false },
          border = BorderStroke(1.5.dp, IvoryBorder),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.heightIn(min = 52.dp),
        ) {
          Text(
            text = stringResource(R.string.dialog_close),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepCharcoal,
          )
        }
      },
    )
  }
}

@Composable
private fun DateHeaderCard(
  formattedDate: String,
  uiState: UiState<DailyHealthLogState>,
  onAccountClick: () -> Unit,
) {
  val log = (uiState as? UiState.Success)?.data?.log
  val takenCount =
    if (log != null) {
      listOf(log.morningTaken, log.lunchTaken, log.dinnerTaken).count { it }
    } else {
      0
    }
  val allMedsDone = log != null && takenCount == 3

  Surface(
    color = IvorySurface,
    shape = RoundedCornerShape(26.dp),
    border = BorderStroke(2.dp, IvoryBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = formattedDate,
          fontSize = 30.sp,
          lineHeight = 38.sp,
          fontWeight = FontWeight.Bold,
          color = DeepCharcoal,
          modifier = Modifier.weight(1f).testTag("today_date_text"),
        )
        IconButton(
          onClick = onAccountClick,
          modifier = Modifier.size(52.dp).testTag("account_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.AccountCircle,
            contentDescription = stringResource(R.string.cd_account_menu),
            tint = WarmSlate,
            modifier = Modifier.size(38.dp),
          )
        }
      }

      if (log != null) {
        Surface(
          color = if (allMedsDone) LeafGreenSoft else IvorySurfaceWarm,
          shape = RoundedCornerShape(14.dp),
        ) {
          Text(
            text =
              if (allMedsDone) {
                "오늘 약 3개를 모두 복용했어요!"
              } else {
                "오늘 약 $takenCount / 3개 복용"
              },
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (allMedsDone) LeafGreenDark else DeepCharcoal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun MedicationCheckButton(
  title: String,
  isTaken: Boolean,
  icon: ImageVector,
  badgeColor: Color,
  badgeTint: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  val containerColor by
    animateColorAsState(
      targetValue = if (isTaken) LeafGreen else IvorySurface,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "med_bg",
    )
  val borderColor by
    animateColorAsState(
      targetValue = if (isTaken) LeafGreenDark else IvoryBorder,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "med_border",
    )
  val scale by
    animateFloatAsState(
      targetValue = if (isTaken) 1.0f else 0.99f,
      animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
      label = "med_scale",
    )

  val statusText =
    if (isTaken) stringResource(R.string.med_status_done)
    else stringResource(R.string.med_status_pending)
  val stateDesc =
    if (isTaken) "$title, ${stringResource(R.string.cd_med_done)}"
    else "$title, ${stringResource(R.string.cd_med_pending)}"

  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(24.dp),
    color = containerColor,
    border = BorderStroke(2.5.dp, borderColor),
    shadowElevation = if (isTaken) 4.dp else 2.dp,
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 102.dp)
        .scale(scale)
        .testTag(testTag)
        .semantics {
          role = Role.Button
          contentDescription = stateDesc
        },
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      // Time-of-day icon badge
      Box(
        modifier =
          Modifier.size(58.dp)
            .background(
              color = if (isTaken) Color.White.copy(alpha = 0.22f) else badgeColor,
              shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isTaken) Color.White else badgeTint,
          modifier = Modifier.size(34.dp),
        )
      }

      // Large clear label + status
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = title,
          fontSize = 28.sp,
          lineHeight = 34.sp,
          fontWeight = FontWeight.Bold,
          color = if (isTaken) Color.White else DeepCharcoal,
        )
        Text(
          text = statusText,
          fontSize = 22.sp,
          lineHeight = 28.sp,
          fontWeight = if (isTaken) FontWeight.Bold else FontWeight.Medium,
          color = if (isTaken) Color.White.copy(alpha = 0.95f) else WarmSlate,
        )
      }

      // Checkmark indicator
      if (isTaken) {
        Icon(
          imageVector = Icons.Rounded.CheckCircle,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(46.dp),
        )
      } else {
        Box(
          modifier =
            Modifier.size(42.dp)
              .border(width = 3.dp, color = IvoryBorder, shape = CircleShape)
        )
      }
    }
  }
}

@Composable
private fun WaterIntakeCard(
  waterCups: Int,
  onUndoClick: () -> Unit,
) {
  Surface(
    color = OceanBlueSurface,
    shape = RoundedCornerShape(26.dp),
    border = BorderStroke(2.dp, OceanBlueBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 22.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.WaterDrop,
            contentDescription = null,
            tint = OceanBlue,
            modifier = Modifier.size(32.dp),
          )
          Text(
            text = stringResource(R.string.water_section_title),
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = DeepCharcoal,
          )
        }

        Surface(
          color = Color.White,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.5.dp, OceanBlueBorder),
        ) {
          Text(
            text = stringResource(R.string.water_count_format, waterCups),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = OceanBlue,
            modifier =
              Modifier.padding(horizontal = 18.dp, vertical = 8.dp).testTag("water_count_text"),
          )
        }
      }

      // Visual filling water cups
      WaterCupsGrid(cupsCount = waterCups, goalCups = 8)

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        val goalLabel =
          if (waterCups > 8) {
            stringResource(R.string.water_extra_format, waterCups - 8)
          } else {
            stringResource(R.string.water_goal_hint)
          }

        Text(
          text = goalLabel,
          fontSize = 22.sp,
          fontWeight = FontWeight.SemiBold,
          color = if (waterCups >= 8) LeafGreenDark else WarmSlate,
          modifier = Modifier.weight(1f),
        )

        if (waterCups > 0) {
          Spacer(modifier = Modifier.width(12.dp))
          OutlinedButton(
            onClick = onUndoClick,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, OceanBlueBorder),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            modifier = Modifier.heightIn(min = 52.dp).testTag("undo_water_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Remove,
              contentDescription = null,
              tint = OceanBlue,
              modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.water_undo_button),
              fontSize = 22.sp,
              fontWeight = FontWeight.SemiBold,
              color = OceanBlue,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WaterBottomActionBar(
  canAddMore: Boolean,
  onAddWaterClick: () -> Unit,
) {
  Surface(
    color = IvoryBackground,
    shadowElevation = 8.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
      HorizontalDivider(thickness = 1.5.dp, color = IvoryBorder)
      Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
      ) {
        Button(
          onClick = onAddWaterClick,
          enabled = canAddMore,
          shape = RoundedCornerShape(24.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = OceanBlue,
              contentColor = Color.White,
            ),
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
          modifier =
            Modifier.fillMaxWidth()
              .widthIn(max = 600.dp)
              .heightIn(min = 78.dp)
              .testTag("add_water_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.WaterDrop,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = stringResource(R.string.water_add_button),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

@Composable
private fun LoadingCard() {
  Surface(
    color = IvorySurface,
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(2.dp, IvoryBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      CircularProgressIndicator(color = LeafGreen, modifier = Modifier.size(48.dp))
      Text(
        text = stringResource(R.string.loading_records),
        fontSize = 24.sp,
        fontWeight = FontWeight.Medium,
        color = DeepCharcoal,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun ErrorCard(
  message: String,
  onRetry: () -> Unit,
) {
  Surface(
    color = ErrorSoft,
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(2.dp, ErrorCrimson),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Text(
        text = message,
        fontSize = 22.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Medium,
        color = ErrorCrimson,
        textAlign = TextAlign.Center,
      )
      Button(
        onClick = onRetry,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LeafGreen, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Refresh,
          contentDescription = null,
          modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = stringResource(R.string.retry_button),
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}
