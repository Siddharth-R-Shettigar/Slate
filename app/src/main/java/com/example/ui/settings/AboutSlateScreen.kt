package com.example.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.R
import com.example.ui.components.SlateIcons
import com.example.ui.components.containedTapGlow
import com.example.ui.components.stickyPressScale
import kotlin.coroutines.cancellation.CancellationException

private const val GITHUB_URL = "https://github.com/Siddharth-R-Shettigar/Slate"
private const val SPONSOR_URL = "https://github.com/Siddharth-R-Shettigar/Slate"
private const val FEEDBACK_MAILTO = "mailto:siddharthrshettigar@gmail.com?subject=Slate%20v1.0%20Feedback"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutSlateScreen(
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var backProgress by remember { mutableFloatStateOf(0f) }

  PredictiveBackHandler(enabled = true) { progress ->
    try {
      progress.collect { backEvent ->
        backProgress = backEvent.progress
      }
      backProgress = 0f
      onClose()
    } catch (e: CancellationException) {
      backProgress = 0f
      throw e
    }
  }

  val appIconBitmap = remember(context) {
    runCatching {
      context.packageManager
        .getApplicationIcon(context.applicationInfo)
        .toBitmap(width = 192, height = 192)
        .asImageBitmap()
    }.getOrNull()
  }

  val cardBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
  val backSheetShape = remember { RoundedCornerShape(28.dp) }

  Surface(
    modifier = modifier
      .fillMaxSize()
      .graphicsLayer {
        translationX = size.width * backProgress * 0.72f
        val isSwiping = backProgress > 0f
        shadowElevation = if (isSwiping) 24f else 0f
        shape = backSheetShape
        clip = isSwiping
      }
      .testTag("about_slate_screen"),
    color = MaterialTheme.colorScheme.background,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Floating Top Action Bar aligned with SettingsScreen style
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 640.dp)
          .height(64.dp)
          .padding(horizontal = 16.dp),
      ) {
        Surface(
          onClick = onClose,
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          border = BorderStroke(1.dp, cardBorderColor),
          tonalElevation = 2.dp,
          shadowElevation = 4.dp,
          modifier = Modifier
            .align(Alignment.CenterStart)
            .size(48.dp)
            .stickyPressScale(0.92f)
            .clip(CircleShape)
            .containedTapGlow(MaterialTheme.colorScheme.primary)
            .testTag("about_back_button"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(22.dp),
            )
          }
        }

        Text(
          text = "About",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.align(Alignment.Center),
        )
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .widthIn(max = 640.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Hero App Identity Card
        item(key = "about_hero_card", contentType = "hero_card") {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, cardBorderColor),
            tonalElevation = 2.dp,
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                tonalElevation = 3.dp,
                modifier = Modifier.size(72.dp),
              ) {
                if (appIconBitmap != null) {
                  Image(
                    bitmap = appIconBitmap,
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                      .fillMaxSize()
                      .clip(RoundedCornerShape(22.dp)),
                  )
                } else {
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = SlateIcons.Layers,
                      contentDescription = stringResource(R.string.app_name),
                      tint = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.size(34.dp),
                    )
                  }
                }
              }

              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("about_version_badge"),
                  ) {
                    Text(
                      text = "v1.0",
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                  }
                }
                Text(
                  text = "Expressive sheet-first tasks & two-way device calendar sync.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        // Offline-First & Zero-Ads Privacy Card
        item(key = "about_privacy_card", contentType = "privacy_card") {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, cardBorderColor),
            tonalElevation = 1.dp,
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.secondaryContainer,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                  modifier = Modifier.size(38.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = SlateIcons.Shield,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSecondaryContainer,
                      modifier = Modifier.size(20.dp),
                    )
                  }
                }
                Column {
                  Text(
                    text = "Offline-First & Private",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = "Your tasks never leave your phone",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }

              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                AboutTrustChip(label = "100% On-Device", borderColor = cardBorderColor)
                AboutTrustChip(label = "Zero Ads", borderColor = cardBorderColor)
                AboutTrustChip(label = "Zero Trackers", borderColor = cardBorderColor)
              }

              Text(
                text = "Slate stores your lists and tasks locally on your device without accounts, telemetry, or third-party analytics. Calendar access is strictly optional and syncs directly with your chosen Android calendar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        // Developer & Links Card
        item(key = "about_developer_card", contentType = "developer_card") {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, cardBorderColor),
            tonalElevation = 1.dp,
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                Text(
                  text = "Developer",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = "Built by Siddharth R Shettigar",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }

              Spacer(modifier = Modifier.height(6.dp))
              HorizontalDivider(color = cardBorderColor)

              AboutLinkRow(
                icon = SlateIcons.Code,
                title = "Source Code on GitHub",
                subtitle = "github.com/siddharthrshettigar/slate",
                testTag = "about_github_row",
                onClick = {
                  launchExternalIntent(
                    context = context,
                    intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)),
                    fallbackMessage = "No browser app found",
                  )
                },
              )

              HorizontalDivider(
                color = cardBorderColor,
                modifier = Modifier.padding(horizontal = 20.dp),
              )

              AboutLinkRow(
                icon = SlateIcons.Email,
                title = "Send Feedback",
                subtitle = "siddharthrshettigar@gmail.com",
                testTag = "about_feedback_row",
                onClick = {
                  launchExternalIntent(
                    context = context,
                    intent = Intent(Intent.ACTION_SENDTO, Uri.parse(FEEDBACK_MAILTO)),
                    fallbackMessage = "No email app found",
                  )
                },
              )
            }
          }
        }

        // Support Development CTA Card
        item(key = "about_support_card", contentType = "support_card") {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, cardBorderColor),
            tonalElevation = 1.dp,
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Text(
                text = "Slate is free, open, and completely ad-free. If it helps keep your day calm and organized, consider supporting future updates.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              Button(
                onClick = {
                  launchExternalIntent(
                    context = context,
                    intent = Intent(Intent.ACTION_VIEW, Uri.parse(SPONSOR_URL)),
                    fallbackMessage = "No browser app found",
                  )
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .defaultMinSize(minHeight = 54.dp)
                  .stickyPressScale(0.975f)
                  .clip(RoundedCornerShape(20.dp))
                  .containedTapGlow(MaterialTheme.colorScheme.onPrimary, maxAlpha = 0.25f)
                  .testTag("about_support_development_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Favorite,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Support Development",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AboutTrustChip(
  label: String,
  borderColor: androidx.compose.ui.graphics.Color,
) {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    border = BorderStroke(1.dp, borderColor),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
  }
}

@Composable
private fun AboutLinkRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  testTag: String,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 56.dp)
      .clickable(onClick = onClick)
      .padding(horizontal = 20.dp, vertical = 12.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(22.dp),
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Icon(
      imageVector = SlateIcons.OpenInNew,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(18.dp),
    )
  }
}

private fun launchExternalIntent(
  context: Context,
  intent: Intent,
  fallbackMessage: String,
) {
  try {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  } catch (_: ActivityNotFoundException) {
    Toast.makeText(context, fallbackMessage, Toast.LENGTH_SHORT).show()
  }
}
