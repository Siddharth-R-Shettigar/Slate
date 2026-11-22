package com.example.ui.workspace

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.calendar.DeviceCalendarEvent
import com.example.data.local.SlateTaskEntity
import com.example.ui.CalendarViewMode
import com.example.ui.SearchFilter
import com.example.ui.SlateUiState
import com.example.ui.components.SlateIcons
import com.example.ui.components.aiModeGlowPillBorder
import com.example.ui.components.containedTapGlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private val AiModeGlowColors = listOf(
  Color(0xFF4285F4), // Google Blue
  Color(0xFF9B72CB), // Gemini Violet
  Color(0xFFEA4335), // Coral Red
  Color(0xFFFBBC05), // Warm Amber
  Color(0xFF34A853), // Mint Green
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WorkspaceScreen(
  uiState: SlateUiState,
  isLayer2Visible: Boolean,
  onWorkspacePageChanged: (Int) -> Unit,
  onOpenSettings: () -> Unit,
  onPullUpActiveSheet: () -> Unit,
  onSelectAndOpenList: (Long) -> Unit,
  onSetDefaultLaunchList: (Long) -> Unit,
  onCreateList: (String, Long) -> Unit,
  onUpdateList: (Long, String, Long) -> Unit,
  onDeleteList: (Long) -> Unit,
  onPersistListOrder: (List<Long>) -> Unit,
  onCalendarPermissionsResult: () -> Unit,
  onSelectDate: (Long) -> Unit,
  onChangeCalendarViewMode: (CalendarViewMode) -> Unit,
  onSyncCalendarNow: () -> Unit,
  onAddCalendarTask: (String, Long) -> Unit,
  onImportCalendarEvent: (DeviceCalendarEvent) -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onSelectSearchFilter: (SearchFilter) -> Unit,
  onToggleTaskComplete: (SlateTaskEntity) -> Unit,
  onToggleTaskStar: (SlateTaskEntity) -> Unit,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()
  val keyboardController = LocalSoftwareKeyboardController.current
  val pagerState = rememberPagerState(
    initialPage = uiState.workspacePage,
    pageCount = { 3 },
  )

  LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.currentPage }
      .distinctUntilChanged()
      .collect { page ->
        onWorkspacePageChanged(page)
        if (page != 2) {
          keyboardController?.hide()
        }
      }
  }

  LaunchedEffect(uiState.workspacePage) {
    if (pagerState.currentPage != uiState.workspacePage) {
      pagerState.animateScrollToPage(
        page = uiState.workspacePage,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioNoBouncy,
          stiffness = Spring.StiffnessMediumLow,
        ),
      )
    }
  }

  val listsTitle = stringResource(R.string.workspace_lists)
  val calendarTitle = stringResource(R.string.workspace_calendar)
  val searchTitle = stringResource(R.string.workspace_search)
  val workspaceTitles = remember(listsTitle, calendarTitle, searchTitle) {
    listOf(listsTitle, calendarTitle, searchTitle)
  }

  val topBg = MaterialTheme.colorScheme.surfaceContainerLowest
  val midBg = MaterialTheme.colorScheme.background
  val botBg = MaterialTheme.colorScheme.surfaceContainerLow
  val backgroundBrush = remember(topBg, midBg, botBg) {
    Brush.verticalGradient(colors = listOf(topBg, midBg, botBg))
  }

  val scheduledTasks = remember(uiState.allTasks) {
    uiState.allTasks.filter { it.scheduledStartMillis != null }
  }

  val activePage = pagerState.currentPage
  val isSearchMode = activePage == 2 && isLayer2Visible && !uiState.isSettingsOpen

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(backgroundBrush)
      .statusBarsPadding(),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Layer 2 Top Bar: Expressive Workspace Title + Circular Settings IconButton
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 22.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        AnimatedContent(
          targetState = workspaceTitles[activePage],
          transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
          label = "workspaceTitleAnim",
        ) { title ->
          Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag("workspace_top_title"),
          )
        }

        Surface(
          onClick = onOpenSettings,
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          tonalElevation = 2.dp,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .containedTapGlow(MaterialTheme.colorScheme.primary)
            .testTag("workspace_settings_button"),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Outlined.Settings,
              contentDescription = stringResource(R.string.cd_settings),
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }

      // 3-Page HorizontalPager with beyondViewportPageCount = 0 so off-screen tabs don't compose during sheet drags
      HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 0,
        key = { it },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .testTag("workspace_horizontal_pager"),
      ) { page ->
        when (page) {
          0 -> ListsManagerPage(
            lists = uiState.lists,
            defaultLaunchListId = uiState.settings.defaultLaunchListId,
            activeListId = uiState.activeListId,
            onSelectAndOpenList = onSelectAndOpenList,
            onSetDefaultLaunchList = onSetDefaultLaunchList,
            onCreateList = onCreateList,
            onUpdateList = onUpdateList,
            onDeleteList = onDeleteList,
            onPersistListOrder = onPersistListOrder,
          )
          1 -> CalendarSyncPage(
            hasCalendarPermissions = uiState.hasCalendarPermissions,
            syncedEvents = uiState.syncedEvents,
            scheduledTasks = scheduledTasks,
            selectedDateMillis = uiState.selectedDateMillis,
            viewMode = uiState.calendarViewMode,
            isSyncing = uiState.isSyncingCalendar,
            lastSyncedMillis = uiState.lastSyncedMillis,
            onPermissionsResult = onCalendarPermissionsResult,
            onSelectDate = onSelectDate,
            onChangeViewMode = onChangeCalendarViewMode,
            onSyncNow = onSyncCalendarNow,
            onAddCalendarTask = onAddCalendarTask,
            onImportEventToActiveList = onImportCalendarEvent,
          )
          else -> SearchFilterPage(
            searchQuery = uiState.searchQuery,
            selectedFilter = uiState.searchFilter,
            allTasks = uiState.allTasks,
            lists = uiState.lists,
            syncedEvents = uiState.syncedEvents,
            onSelectFilter = onSelectSearchFilter,
            onToggleTaskComplete = onToggleTaskComplete,
            onToggleTaskStar = onToggleTaskStar,
            onJumpToList = onSelectAndOpenList,
            onImportEvent = onImportCalendarEvent,
          )
        }
      }
    }

    // Isolated Bottom Dock so pagerState.currentPageOffsetFraction NEVER recomposes WorkspaceScreen!
    WorkspaceBottomDock(
      pagerState = pagerState,
      isSearchMode = isSearchMode,
      searchQuery = uiState.searchQuery,
      activeSheetLabel = if (uiState.settings.useTodaysCalendarAsLaunchList) {
        "Today's Events"
      } else {
        uiState.activeList?.name ?: "List"
      },
      onPullUpActiveSheet = onPullUpActiveSheet,
      onSearchQueryChange = onSearchQueryChange,
      onSelectPage = { page ->
        scope.launch { pagerState.animateScrollToPage(page) }
      },
      modifier = Modifier.align(Alignment.BottomCenter),
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WorkspaceBottomDock(
  pagerState: PagerState,
  isSearchMode: Boolean,
  searchQuery: String,
  activeSheetLabel: String,
  onPullUpActiveSheet: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onSelectPage: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val keyboardController = LocalSoftwareKeyboardController.current

  Box(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .imePadding()
      .padding(horizontal = 20.dp, vertical = 14.dp),
    contentAlignment = Alignment.Center,
  ) {
    AnimatedContent(
      targetState = isSearchMode,
      transitionSpec = {
        (fadeIn(tween(200)) + scaleIn(initialScale = 0.94f, animationSpec = tween(220, easing = FastOutSlowInEasing))) togetherWith
          (fadeOut(tween(140)) + scaleOut(targetScale = 0.94f))
      },
      label = "bottomBarSearchMorph",
    ) { searchActive ->
      if (searchActive) {
        BottomGlowingSearchPill(
          query = searchQuery,
          onQueryChange = onSearchQueryChange,
          onBackToNav = {
            keyboardController?.hide()
            onSelectPage(0)
          },
        )
      } else {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // Separate Circular Open-List Button to the LEFT of the Navigation Bar
          Surface(
            onClick = onPullUpActiveSheet,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            tonalElevation = 5.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
              .size(62.dp)
              .clip(CircleShape)
              .containedTapGlow(MaterialTheme.colorScheme.primary)
              .testTag("pull_up_active_sheet_pill"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = SlateIcons.ArrowUpward,
                contentDescription = "Open $activeSheetLabel",
                modifier = Modifier.size(27.dp),
              )
            }
          }

          // Pill Navigation Bar with layout-phase sliding highlight (0 recompositions during swipe!)
          HorizontalFloatingToolbar(
            expanded = true,
            shape = CircleShape,
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
              toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            expandedShadowElevation = 10.dp,
            modifier = Modifier
              .clip(CircleShape)
              .containedTapGlow(MaterialTheme.colorScheme.primary, maxAlpha = 0.34f)
              .testTag("workspace_floating_toolbar"),
          ) {
            val buttonSize = 54.dp
            val buttonGap = 8.dp
            val stepPx = with(density) { (buttonSize + buttonGap).toPx() }
            val currentPage = pagerState.currentPage

            Box(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
              // Deferred offset lambda reads pagerState ONLY in layout placement -> 0 recompositions while swiping!
              Box(
                modifier = Modifier
                  .offset {
                    val pos = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 2f)
                    IntOffset((pos * stepPx).roundToInt(), 0)
                  }
                  .size(buttonSize)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary)
              )

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(buttonGap),
              ) {
                val onPrimary = MaterialTheme.colorScheme.onPrimary
                val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

                val listsTintState = animateColorAsState(
                  targetValue = if (currentPage == 0) onPrimary else onSurfaceVariant,
                  animationSpec = tween(240, easing = FastOutSlowInEasing),
                  label = "listsTint",
                )
                val calendarTintState = animateColorAsState(
                  targetValue = if (currentPage == 1) onPrimary else onSurfaceVariant,
                  animationSpec = tween(240, easing = FastOutSlowInEasing),
                  label = "calendarTint",
                )
                val searchTintState = animateColorAsState(
                  targetValue = if (currentPage == 2) onPrimary else onSurfaceVariant,
                  animationSpec = tween(240, easing = FastOutSlowInEasing),
                  label = "searchTint",
                )

                ToolbarNavIconSlot(
                  testTag = "toolbar_tab_lists",
                  glowColor = MaterialTheme.colorScheme.primary,
                  onClick = { onSelectPage(0) },
                ) {
                  AnimatedStackedLayersIcon(
                    selected = currentPage == 0,
                    tintState = listsTintState,
                    modifier = Modifier.size(26.dp),
                  )
                }

                ToolbarNavIconSlot(
                  testTag = "toolbar_tab_calendar",
                  glowColor = MaterialTheme.colorScheme.primary,
                  onClick = { onSelectPage(1) },
                ) {
                  AnimatedCalendarIcon(
                    selected = currentPage == 1,
                    tintState = calendarTintState,
                    modifier = Modifier.size(26.dp),
                  )
                }

                ToolbarNavIconSlot(
                  testTag = "toolbar_tab_search",
                  glowColor = MaterialTheme.colorScheme.primary,
                  onClick = { onSelectPage(2) },
                ) {
                  AnimatedCoinFlipSearchIcon(
                    selected = currentPage == 2,
                    tintState = searchTintState,
                    modifier = Modifier.size(26.dp),
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

@Composable
private fun BottomGlowingSearchPill(
  query: String,
  onQueryChange: (String) -> Unit,
  onBackToNav: () -> Unit,
) {
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(Unit) {
    delay(80)
    focusRequester.requestFocus()
    keyboardController?.show()
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = 580.dp)
      .clip(CircleShape)
      .aiModeGlowPillBorder(
        colors = AiModeGlowColors,
        cornerRadius = 32.dp,
        strokeWidth = 2.6.dp,
      )
      .containedTapGlow(MaterialTheme.colorScheme.primary, maxAlpha = 0.42f)
      .testTag("bottom_ai_search_pill"),
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 8.dp,
    shadowElevation = 14.dp,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = onBackToNav,
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .containedTapGlow(MaterialTheme.colorScheme.primary)
          .testTag("search_pill_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Back to workspace navigation",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart,
      ) {
        if (query.isEmpty()) {
          Text(
            text = "Search tasks, lists & calendar...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            maxLines = 1,
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
          ),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(
            onSearch = { keyboardController?.hide() },
          ),
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .testTag("unified_search_textfield"),
        )
      }

      if (query.isNotEmpty()) {
        IconButton(
          onClick = { onQueryChange("") },
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .containedTapGlow(MaterialTheme.colorScheme.primary)
            .testTag("clear_search_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Clear search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun ToolbarNavIconSlot(
  testTag: String,
  glowColor: Color,
  onClick: () -> Unit,
  content: @Composable () -> Unit,
) {
  Box(
    modifier = Modifier
      .size(54.dp)
      .clip(CircleShape)
      .containedTapGlow(glowColor = glowColor)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
      )
      .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}

@Composable
private fun AnimatedStackedLayersIcon(
  selected: Boolean,
  tintState: State<Color>,
  modifier: Modifier = Modifier,
) {
  val animProgress = animateFloatAsState(
    targetValue = if (selected) 1f else 0f,
    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
    label = "layersAnimProgress",
  )

  Canvas(
    modifier = modifier
      .semantics { contentDescription = "Lists" }
      .graphicsLayer {
        val s = 0.96f + 0.12f * animProgress.value
        scaleX = s
        scaleY = s
      }
  ) {
    val p = animProgress.value
    val tint = tintState.value
    val w = size.width
    val h = size.height
    val cardW = w * 0.78f
    val cardH = h * 0.29f
    val left = (w - cardW) / 2f
    val splitPx = h * 0.16f * sin(PI * p).toFloat()
    val radius = CornerRadius(w * 0.14f, w * 0.14f)
    val strokeWidth = w * 0.085f

    val bottomTop = h * 0.56f + splitPx
    val topTop = h * 0.15f - splitPx

    if (p > 0.5f) {
      drawRoundRect(
        color = tint.copy(alpha = 0.68f),
        topLeft = Offset(left, bottomTop),
        size = Size(cardW, cardH),
        cornerRadius = radius,
      )
      drawRoundRect(
        color = tint,
        topLeft = Offset(left, topTop),
        size = Size(cardW, cardH),
        cornerRadius = radius,
      )
    } else {
      drawRoundRect(
        color = tint,
        topLeft = Offset(left, bottomTop),
        size = Size(cardW, cardH),
        cornerRadius = radius,
        style = Stroke(width = strokeWidth),
      )
      drawRoundRect(
        color = tint,
        topLeft = Offset(left, topTop),
        size = Size(cardW, cardH),
        cornerRadius = radius,
        style = Stroke(width = strokeWidth),
      )
    }
  }
}

@Composable
private fun AnimatedCalendarIcon(
  selected: Boolean,
  tintState: State<Color>,
  modifier: Modifier = Modifier,
) {
  val animProgress = animateFloatAsState(
    targetValue = if (selected) 1f else 0f,
    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
    label = "calendarAnimProgress",
  )
  val roundedPainter = rememberVectorPainter(SlateIcons.CalendarMonthRounded)
  val outlinedPainter = rememberVectorPainter(SlateIcons.CalendarMonthOutlined)

  Canvas(
    modifier = modifier
      .semantics { contentDescription = "Calendar" }
      .graphicsLayer {
        val p = animProgress.value
        val s = 0.96f + 0.12f * p
        scaleX = s
        scaleY = s
        rotationZ = sin(PI * p).toFloat() * -10f
      },
  ) {
    val painter = if (animProgress.value > 0.5f) roundedPainter else outlinedPainter
    with(painter) {
      draw(size = size, colorFilter = ColorFilter.tint(tintState.value))
    }
  }
}

@Composable
private fun AnimatedCoinFlipSearchIcon(
  selected: Boolean,
  tintState: State<Color>,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current.density
  val animProgress = animateFloatAsState(
    targetValue = if (selected) 1f else 0f,
    animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
    label = "searchCoinFlipProgress",
  )
  val roundedPainter = rememberVectorPainter(Icons.Rounded.Search)
  val outlinedPainter = rememberVectorPainter(Icons.Outlined.Search)

  Canvas(
    modifier = modifier
      .semantics { contentDescription = "Search" }
      .graphicsLayer {
        rotationY = 360f * animProgress.value
        cameraDistance = 14f * density
      },
  ) {
    val painter = if (animProgress.value > 0.5f) roundedPainter else outlinedPainter
    with(painter) {
      draw(size = size, colorFilter = ColorFilter.tint(tintState.value))
    }
  }
}
