package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.EmptyCupOutline
import com.example.ui.theme.IvorySurface
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.OceanBlueLight

@Composable
fun WaterCupsGrid(
  cupsCount: Int,
  goalCups: Int = 8,
  modifier: Modifier = Modifier,
) {
  val accessibilityDesc = stringResource(R.string.cd_water_cups, cupsCount)
  BoxWithConstraints(
    modifier = modifier.fillMaxWidth().semantics { contentDescription = accessibilityDesc }
  ) {
    val perRow = if (maxWidth >= 480.dp) 8 else 4
    val rows = (0 until goalCups).chunked(perRow)
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      rows.forEach { rowIndices ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          rowIndices.forEach { index ->
            WaterCupGraphic(
              isFilled = index < cupsCount,
              modifier = Modifier.size(width = 52.dp, height = 60.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
fun WaterCupGraphic(
  isFilled: Boolean,
  modifier: Modifier = Modifier,
) {
  val fillProgress by
    animateFloatAsState(
      targetValue = if (isFilled) 0.80f else 0f,
      animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
      label = "cup_fill",
    )

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    val topLeftX = w * 0.14f
    val topRightX = w * 0.86f
    val bottomLeftX = w * 0.24f
    val bottomRightX = w * 0.76f
    val topY = h * 0.10f
    val bottomY = h * 0.90f

    val cupPath =
      Path().apply {
        moveTo(topLeftX, topY)
        lineTo(topRightX, topY)
        lineTo(bottomRightX, bottomY - h * 0.05f)
        quadraticTo(bottomRightX, bottomY, bottomRightX - w * 0.06f, bottomY)
        lineTo(bottomLeftX + w * 0.06f, bottomY)
        quadraticTo(bottomLeftX, bottomY, bottomLeftX, bottomY - h * 0.05f)
        close()
      }

    // Cup interior background
    drawPath(path = cupPath, color = IvorySurface)

    // Animated water fill clipped inside cup silhouette
    if (fillProgress > 0.01f) {
      clipPath(cupPath) {
        val waterTopY = bottomY - (bottomY - topY) * fillProgress
        drawRect(
          brush =
            Brush.verticalGradient(
              colors = listOf(OceanBlueLight, OceanBlue),
              startY = waterTopY,
              endY = bottomY,
            ),
          topLeft = Offset(0f, waterTopY),
          size = Size(w, bottomY - waterTopY + 4f),
        )
        // Top water meniscus highlight
        drawLine(
          color = Color.White.copy(alpha = 0.55f),
          start = Offset(topLeftX, waterTopY + 2f),
          end = Offset(topRightX, waterTopY + 2f),
          strokeWidth = h * 0.045f,
          cap = StrokeCap.Round,
        )
        // Subtle vertical glass sheen
        drawRoundRect(
          color = Color.White.copy(alpha = 0.28f),
          topLeft = Offset(w * 0.27f, waterTopY + h * 0.12f),
          size = Size(w * 0.08f, (bottomY - waterTopY) * 0.55f),
          cornerRadius = CornerRadius(w * 0.04f, w * 0.04f),
        )
      }
    }

    // Crisp outer glass rim & wall stroke
    drawPath(
      path = cupPath,
      color = if (isFilled) OceanBlue else EmptyCupOutline,
      style = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // Top oval rim accent
    drawLine(
      color = if (isFilled) OceanBlue else EmptyCupOutline,
      start = Offset(w * 0.10f, topY),
      end = Offset(w * 0.90f, topY),
      strokeWidth = w * 0.08f,
      cap = StrokeCap.Round,
    )
  }
}
