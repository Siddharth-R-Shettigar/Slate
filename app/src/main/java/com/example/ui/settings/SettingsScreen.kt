package com.example.ui.settings

import android.Manifest
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.calendar.DeviceCalendar
import com.example.data.preferences.AppColorTheme
import com.example.data.preferences.SlateSettings
import com.example.ui.components.SlateIcons
import com.example.ui.components.containedTapGlow
import com.example.ui.components.stickyPressScale
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
  settings: SlateSettings,
  hasCalendarPermissions: Boolean,
  writableCalendars: List<DeviceCalendar>,
  effectiveCalendarId: Long?,
  onClose: () -> Unit,
  onToggleOpenDirectly: (Boolean) -> Unit,
  onToggleTodaysEventsMode: (Boolean) -> Unit,
  onSelectTargetCalendar: (Long) -> Unit,
  onToggleBackdropBlur: (Boolean) -> Unit,
  onToggleOledDarkMode: (Boolean) -> Unit,
  onSelectColorTheme: (AppColorTheme) -> Unit,
  onCalendarPermissionsResult: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val haptic = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()

  var isAboutOpen by remember { mutableStateOf(false) }
  var backProgress by remember { mutableFloatStateOf(0f) }

  PredictiveBackHandler(enabled = !isAboutOpen) { progress ->
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

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
    onResult = { onCalendarPermissionsResult() },
  )

  // Material 3 Expressive + One UI Collapsible Reachability Header (0f = expanded 144dp, 1f = compact 64dp)
  val expandedHeaderHeight = 144.dp
  val collapsedHeaderHeight = 64.dp
  val expandedHeaderHeightPx = with(density) { expandedHeaderHeight.toPx() }
  val collapsedHeaderHeightPx = with(density) { collapsedHeaderHeight.toPx() }
  val collapseRangePx = (expandedHeaderHeightPx - collapsedHeaderHeightPx).coerceAtLeast(1f)
  val headerCollapseAnim = remember { Animatable(0f) }

  val scrollConnection = remember(collapseRangePx, haptic) {
    object : NestedScrollConnection {
      override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val dy = available.y
        if (dy < 0f && headerCollapseAnim.value < 1f) {
          val curPx = headerCollapseAnim.value * collapseRangePx
          val nextPx = (curPx - dy).coerceIn(0f, collapseRangePx)
          val consumedY = curPx - nextPx
          scope.launch { headerCollapseAnim.snapTo(nextPx / collapseRangePx) }
          return Offset(0f, consumedY)
        }
        return Offset.Zero
      }

      override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        val dy = available.y
        if (dy > 0f && headerCollapseAnim.value > 0f) {
          val curPx = headerCollapseAnim.value * collapseRangePx
          val nextPx = (curPx - dy).coerceIn(0f, collapseRangePx)
          val consumedY = curPx - nextPx
          scope.launch { headerCollapseAnim.snapTo(nextPx / collapseRangePx) }
          return Offset(0f, consumedY)
        }
        return Offset.Zero
      }

      override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val current = headerCollapseAnim.value
        if (current > 0.02f && current < 0.98f) {
          val snapTarget = when {
            available.y < -300f -> 1f
            available.y > 300f -> 0f
            current >= 0.45f -> 1f
            else -> 0f
          }
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          headerCollapseAnim.animateTo(
            targetValue = snapTarget,
            animationSpec = spring(
              dampingRatio = Spring.DampingRatioNoBouncy,
              stiffness = Spring.StiffnessMedium,
            ),
          )
        }
        return Velocity.Zero
      }
    }
  }

  val cardBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
  val selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
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
      .testTag("settings_screen"),
    color = MaterialTheme.colorScheme.background,
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .nestedScroll(scrollConnection),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Collapsible M3 Expressive Header with Floating Top Controls & Fading Large Title
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .layout { measurable, constraints ->
              val t = headerCollapseAnim.value
              val hPx = (expandedHeaderHeightPx - collapseRangePx * t)
                .roundToInt()
                .coerceAtLeast(1)
              val placeable = measurable.measure(
                constraints.copy(minHeight = hPx, maxHeight = hPx),
              )
              layout(placeable.width, hPx) {
                placeable.placeRelative(0, 0)
              }
            }
            .padding(horizontal = 16.dp),
        ) {
          // Expanded One UI-style Large Title & Contextual Subtitle below floating controls
          Column(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(start = 6.dp, end = 6.dp, bottom = 12.dp)
              .graphicsLayer {
                val t = headerCollapseAnim.value
                val scale = 1f - 0.14f * t
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0.5f)
                alpha = (1f - t * 1.85f).coerceIn(0f, 1f)
                translationY = -22f * t
              },
          ) {
            Text(
              text = stringResource(R.string.settings_title),
              style = MaterialTheme.typography.displaySmall,
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag("settings_large_title"),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Behavior, appearance & calendar sync",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          // Top Floating Control Row: Back (Left) • Compact Title (Center) • About (i) (Right)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(collapsedHeaderHeight)
              .align(Alignment.TopCenter),
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
                .testTag("settings_back_button"),
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
              text = stringResource(R.string.settings_title),
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 60.dp)
                .graphicsLayer {
                  val t = headerCollapseAnim.value
                  alpha = ((t - 0.48f) / 0.42f).coerceIn(0f, 1f)
                  translationY = (1f - t) * 12f
                }
                .testTag("settings_compact_title"),
            )

            Surface(
              onClick = { isAboutOpen = true },
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              border = BorderStroke(1.dp, cardBorderColor),
              tonalElevation = 2.dp,
              shadowElevation = 4.dp,
              modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(48.dp)
                .stickyPressScale(0.92f)
                .clip(CircleShape)
                .containedTapGlow(MaterialTheme.colorScheme.primary)
                .testTag("settings_about_button"),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Outlined.Info,
                  contentDescription = "About Slate",
                  tint = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.size(22.dp),
                )
              }
            }
          }
        }

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .widthIn(max = 640.dp),
          contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item(key = "setting_launch_direct", contentType = "toggle_card") {
            SettingsToggleCard(
              icon = SlateIcons.Layers,
              title = "Launch straight into active list sheet",
              subtitle = "Open Layer 1 automatically on startup. Disable to start in the Workspace.",
              checked = settings.openDirectlyIntoActiveList,
              testTag = "setting_toggle_launch_direct",
              onCheckedChange = onToggleOpenDirectly,
            )
          }

          item(key = "setting_todays_events", contentType = "toggle_card") {
            SettingsToggleCard(
              icon = SlateIcons.EventNote,
              title = "Use Today's Calendar Events as launch list",
              subtitle = "Replace Layer 1's active list with today's synced calendar events & tasks.",
              checked = settings.useTodaysCalendarAsLaunchList,
              testTag = "setting_toggle_todays_events",
              onCheckedChange = onToggleTodaysEventsMode,
            )
          }

          item(key = "setting_backdrop_blur", contentType = "toggle_card") {
            SettingsToggleCard(
              icon = SlateIcons.BlurOn,
              title = "Backdrop Blur Effects",
              subtitle = "Blur Layer 2 and behind long-press menus for depth. Disable for maximum frame pacing.",
              checked = settings.backdropBlurEnabled,
              testTag = "setting_toggle_backdrop_blur",
              onCheckedChange = onToggleBackdropBlur,
            )
          }

          item(key = "setting_oled_dark_mode", contentType = "toggle_card") {
            SettingsToggleCard(
              icon = SlateIcons.Contrast,
              title = "Pure AMOLED Black",
              subtitle = "Use pure #000000 AMOLED black surfaces in dark theme instead of tinted palette surfaces.",
              checked = settings.oledDarkModeEnabled,
              testTag = "setting_toggle_oled_dark_mode",
              onCheckedChange = onToggleOledDarkMode,
            )
          }

          // Accent Color Theme Selector Card
          item(key = "setting_color_theme", contentType = "color_palette_card") {
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 18.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = SlateIcons.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Color Palette",
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = "Choose wallpaper dynamic color or a curated Slate palette.",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }

                val swatchShape = remember { RoundedCornerShape(18.dp) }
                val swatchColumnModifier = remember {
                  Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                }
                val swatchCircleBaseModifier = remember {
                  Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                }
                val swatchIconModifier = remember { Modifier.size(18.dp) }

                LazyRow(
                  contentPadding = PaddingValues(horizontal = 18.dp),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  items(
                    items = AppColorTheme.entries,
                    key = { it.name },
                    contentType = { "color_theme_swatch" },
                  ) { theme ->
                    val isSelected = settings.appColorTheme == theme
                    val swatchColor = Color(theme.previewHex)

                    Surface(
                      onClick = { onSelectColorTheme(theme) },
                      shape = swatchShape,
                      color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                      } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                      },
                      border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) selectedBorderColor else cardBorderColor,
                      ),
                      modifier = Modifier
                        .clip(swatchShape)
                        .containedTapGlow(swatchColor)
                        .testTag("settings_color_theme_${theme.name.lowercase()}"),
                    ) {
                      Column(
                        modifier = swatchColumnModifier,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                      ) {
                        Box(
                          modifier = swatchCircleBaseModifier
                            .background(
                              if (theme == AppColorTheme.WALLPAPER) {
                                MaterialTheme.colorScheme.primary
                              } else {
                                swatchColor
                              }
                            )
                            .then(
                              if (isSelected) {
                                Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                              } else {
                                Modifier.border(1.dp, cardBorderColor, CircleShape)
                              }
                            ),
                          contentAlignment = Alignment.Center,
                        ) {
                          when {
                            isSelected -> Icon(
                              imageVector = Icons.Rounded.Check,
                              contentDescription = "${theme.label} selected",
                              tint = Color.White,
                              modifier = swatchIconModifier,
                            )
                            theme == AppColorTheme.WALLPAPER -> Icon(
                              imageVector = SlateIcons.Wallpaper,
                              contentDescription = null,
                              tint = MaterialTheme.colorScheme.onPrimary,
                              modifier = swatchIconModifier,
                            )
                          }
                        }
                        Text(
                          text = theme.label,
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                          color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                          } else {
                            MaterialTheme.colorScheme.onSurface
                          },
                          maxLines = 1,
                          softWrap = false,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          item(key = "setting_target_calendar", contentType = "target_calendar_card") {
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
                  .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = SlateIcons.CalendarMonthRounded,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Target Sync Calendar",
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = "Choose where new Slate tasks are saved (events from all visible calendars sync back).",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }

                if (!hasCalendarPermissions) {
                  Button(
                    onClick = {
                      permissionLauncher.launch(
                        arrayOf(
                          Manifest.permission.READ_CALENDAR,
                          Manifest.permission.WRITE_CALENDAR,
                        )
                      )
                    },
                    modifier = Modifier.testTag("settings_grant_calendar_permission"),
                  ) {
                    Text("Grant Calendar Permissions")
                  }
                } else if (writableCalendars.isEmpty()) {
                  Text(
                    text = "No writable local calendars found on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                } else {
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    writableCalendars.forEach { cal ->
                      val isSelected = cal.id == effectiveCalendarId
                      Surface(
                        onClick = { onSelectTargetCalendar(cal.id) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                          MaterialTheme.colorScheme.primaryContainer
                        } else {
                          MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        border = BorderStroke(
                          width = if (isSelected) 1.5.dp else 1.dp,
                          color = if (isSelected) selectedBorderColor else cardBorderColor,
                        ),
                        modifier = Modifier
                          .fillMaxWidth()
                          .clip(RoundedCornerShape(16.dp))
                          .containedTapGlow(MaterialTheme.colorScheme.primary)
                          .testTag("settings_calendar_option_${cal.id}"),
                      ) {
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                          verticalAlignment = Alignment.CenterVertically,
                        ) {
                          Box(
                            modifier = Modifier
                              .size(12.dp)
                              .clip(CircleShape)
                              .background(
                                if (cal.color != 0) Color(cal.color)
                                else MaterialTheme.colorScheme.primary
                              )
                          )
                          Spacer(modifier = Modifier.width(12.dp))
                          Column(modifier = Modifier.weight(1f)) {
                            Text(
                              text = cal.displayName,
                              style = MaterialTheme.typography.bodyLarge,
                              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                              text = buildString {
                                if (cal.isSamsungCalendar) append("Samsung / Local • ")
                                append(cal.accountName.ifBlank { cal.accountType })
                              },
                              style = MaterialTheme.typography.labelSmall,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                            )
                          }
                          if (isSelected) {
                            Icon(
                              imageVector = Icons.Rounded.Check,
                              contentDescription = "Selected calendar",
                              tint = MaterialTheme.colorScheme.primary,
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      // About Slate Sub-Screen Overlay
      AnimatedVisibility(
        visible = isAboutOpen,
        enter = slideInHorizontally(
          initialOffsetX = { it },
          animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        ),
        exit = slideOutHorizontally(
          targetOffsetX = { it },
          animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        ),
      ) {
        AboutSlateScreen(
          onClose = { isAboutOpen = false },
        )
      }
    }
  }
}

@Composable
private fun SettingsToggleCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  testTag: String,
  onCheckedChange: (Boolean) -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .containedTapGlow(MaterialTheme.colorScheme.primary),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    tonalElevation = 1.dp,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onCheckedChange(!checked) }
        .padding(horizontal = 18.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp),
      )
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.testTag(testTag),
      )
    }
  }
}
