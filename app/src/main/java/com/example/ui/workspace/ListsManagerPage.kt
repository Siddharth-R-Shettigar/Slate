package com.example.ui.workspace

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.local.SlateListWithCounts
import com.example.ui.components.SlateIcons
import com.example.ui.components.containedTapGlow
import com.example.ui.components.stickyPressScale
import com.example.ui.theme.ListColorTags
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.math.abs

@Composable
fun ListsManagerPage(
  lists: List<SlateListWithCounts>,
  defaultLaunchListId: Long,
  activeListId: Long,
  dismissSignal: Int = 0,
  onHasActiveSubStateChanged: (Boolean) -> Unit = {},
  onSelectAndOpenList: (Long) -> Unit,
  onSetDefaultLaunchList: (Long) -> Unit,
  onCreateList: (name: String, colorHex: Long) -> Unit,
  onUpdateList: (listId: Long, name: String, colorHex: Long) -> Unit,
  onRequestDeleteList: (SlateListWithCounts) -> Unit,
  onPersistListOrder: (List<Long>) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val density = LocalDensity.current
  val scope = rememberCoroutineScope()
  val lazyListState = rememberLazyListState()

  var editingListId by remember { mutableStateOf<Long?>(null) }
  var isCreatingInline by remember { mutableStateOf(false) }

  LaunchedEffect(editingListId, isCreatingInline) {
    onHasActiveSubStateChanged(editingListId != null || isCreatingInline)
  }

  LaunchedEffect(dismissSignal) {
    if (dismissSignal > 0) {
      editingListId = null
      isCreatingInline = false
    }
  }

  // Drag-and-drop reorder state for list cards
  var orderedLists by remember { mutableStateOf(lists) }
  var draggingListId by remember { mutableStateOf<Long?>(null) }
  var dragOffsetY by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(lists, draggingListId) {
    if (draggingListId == null) {
      orderedLists = lists
    }
  }

  val reorderSlopPx = with(density) { 10.dp.toPx() }

  val listContainerModifier = remember {
    Modifier
      .fillMaxSize()
      .widthIn(max = 640.dp)
      .imePadding()
  }
  val staticCardModifier = remember { Modifier.fillMaxWidth() }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      state = lazyListState,
      modifier = listContainerModifier,
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 148.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(
        items = orderedLists,
        key = { it.id },
        contentType = { "list_card" },
      ) { list ->
        val isEditingThis = editingListId == list.id
        val isDraggingThis = draggingListId == list.id

        val dragScaleState = animateFloatAsState(
          targetValue = if (isDraggingThis) 1.035f else 1f,
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
          ),
          label = "listDragScale",
        )

        val dragModifier = remember(list.id, isEditingThis) {
          if (!isEditingThis) {
            Modifier.pointerInput(list.id) {
              val touchSlop = viewConfiguration.touchSlop
              awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var isLongPressActive = false
                var totalDx = 0f
                var totalDy = 0f

                try {
                  withTimeout(380L) {
                    while (true) {
                      val event = awaitPointerEvent(PointerEventPass.Main)
                      val change = event.changes.firstOrNull { it.id == down.id } ?: break
                      if (!change.pressed) break
                      val delta = change.positionChange()
                      totalDx += delta.x
                      totalDy += delta.y
                      if (abs(totalDy) > touchSlop || abs(totalDx) > touchSlop) {
                        break
                      }
                    }
                  }
                } catch (_: TimeoutCancellationException) {
                  isLongPressActive = true
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  draggingListId = list.id
                  dragOffsetY = 0f
                }

                if (isLongPressActive) {
                  var accY = 0f
                  var startedMoving = false
                  while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                      draggingListId = null
                      dragOffsetY = 0f
                      onPersistListOrder(orderedLists.map { it.id })
                      break
                    }
                    val dy = change.positionChange().y
                    if (!startedMoving) {
                      accY += dy
                      if (abs(accY) > reorderSlopPx) {
                        startedMoving = true
                        change.consume()
                        dragOffsetY += accY
                      }
                    } else {
                      change.consume()
                      dragOffsetY += dy
                    }

                    val currentIndex = orderedLists.indexOfFirst { it.id == list.id }
                    if (currentIndex >= 0) {
                      val itemInfo = lazyListState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.key == list.id }
                      val stepHeight = (itemInfo?.size ?: 220) + 28
                      if (dragOffsetY > stepHeight * 0.65f && currentIndex < orderedLists.lastIndex) {
                        val mutable = orderedLists.toMutableList()
                        val moved = mutable.removeAt(currentIndex)
                        mutable.add(currentIndex + 1, moved)
                        orderedLists = mutable
                        dragOffsetY -= stepHeight
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      } else if (dragOffsetY < -stepHeight * 0.65f && currentIndex > 0) {
                        val mutable = orderedLists.toMutableList()
                        val moved = mutable.removeAt(currentIndex)
                        mutable.add(currentIndex - 1, moved)
                        orderedLists = mutable
                        dragOffsetY += stepHeight
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      }
                    }
                  }
                }
              }
            }
          } else {
            Modifier
          }
        }

        ExpandableListCard(
          list = list,
          isDefault = list.id == defaultLaunchListId,
          isActive = list.id == activeListId,
          isEditing = isEditingThis,
          isDragging = isDraggingThis,
          canDelete = orderedLists.size > 1,
          onSelectAndOpen = { onSelectAndOpenList(list.id) },
          onSetDefault = { onSetDefaultLaunchList(list.id) },
          onStartEdit = {
            isCreatingInline = false
            editingListId = list.id
          },
          onCancelEdit = { editingListId = null },
          onSaveEdit = { newName, newColorHex ->
            onUpdateList(list.id, newName, newColorHex)
            editingListId = null
          },
          onDelete = { onRequestDeleteList(list) },
          modifier = staticCardModifier
            .zIndex(if (isDraggingThis) 10f else 0f)
            .graphicsLayer {
              translationY = if (draggingListId == list.id) dragOffsetY else 0f
              val s = dragScaleState.value
              scaleX = s
              scaleY = s
            }
            .then(dragModifier),
        )
      }

      // Inline Create New List Card (no popup!)
      item(key = "inline_create_new_list_card", contentType = "create_list_card") {
        InlineCreateListCard(
          isExpanded = isCreatingInline,
          onExpand = {
            editingListId = null
            isCreatingInline = true
            scope.launch {
              delay(120)
              lazyListState.animateScrollToItem(orderedLists.size)
            }
          },
          onCancel = { isCreatingInline = false },
          onCreate = { name, colorHex ->
            onCreateList(name, colorHex)
            isCreatingInline = false
          },
        )
      }
    }
  }
}

@Composable
private fun ExpandableListCard(
  list: SlateListWithCounts,
  isDefault: Boolean,
  isActive: Boolean,
  isEditing: Boolean,
  isDragging: Boolean,
  canDelete: Boolean,
  onSelectAndOpen: () -> Unit,
  onSetDefault: () -> Unit,
  onStartEdit: () -> Unit,
  onCancelEdit: () -> Unit,
  onSaveEdit: (String, Long) -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accent = Color(list.colorTagHex)
  var draftName by remember(list.id, isEditing) { mutableStateOf(list.name) }
  var draftColor by remember(list.id, isEditing) { mutableLongStateOf(list.colorTagHex) }
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(isEditing) {
    if (isEditing) {
      delay(80)
      focusRequester.requestFocus()
      keyboardController?.show()
    }
  }

  val borderColor = when {
    isDragging -> accent.copy(alpha = 0.9f)
    isEditing || isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 84.dp)
      .clip(RoundedCornerShape(24.dp))
      .containedTapGlow(accent, maxAlpha = 0.36f)
      .animateContentSize(
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMediumLow,
        )
      )
      .then(
        if (!isEditing) {
          Modifier.clickable(onClick = onSelectAndOpen)
        } else {
          Modifier
        }
      )
      .testTag("workspace_list_card_${list.id}"),
    shape = RoundedCornerShape(24.dp),
    color = when {
      isEditing -> MaterialTheme.colorScheme.surfaceContainerHighest
      isActive -> MaterialTheme.colorScheme.surfaceContainerHigh
      else -> MaterialTheme.colorScheme.surfaceContainer
    },
    border = BorderStroke(if (isDragging || isEditing || isActive) 1.5.dp else 1.dp, borderColor),
    tonalElevation = if (isActive || isEditing) 4.dp else if (isDragging) 10.dp else 1.dp,
    shadowElevation = if (isDragging) 12.dp else 0.dp,
  ) {
    if (!isEditing) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .width(6.dp)
            .height(44.dp)
            .clip(CircleShape)
            .background(accent)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = list.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          val countText = buildString {
            append("${list.activeCount} active")
            if (list.totalCount > list.activeCount) {
              append(" • ${list.totalCount} total")
            }
          }
          Text(
            text = countText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        // Default Launch Radio Badge
        Surface(
          modifier = Modifier
            .clip(CircleShape)
            .containedTapGlow(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onSetDefault)
            .defaultMinSize(minHeight = 48.dp)
            .testTag("set_default_list_${list.id}"),
          shape = CircleShape,
          color = if (isDefault) {
            MaterialTheme.colorScheme.primaryContainer
          } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
          },
          border = BorderStroke(
            width = 1.dp,
            color = if (isDefault) {
              MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            } else {
              MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            },
          ),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = if (isDefault) SlateIcons.CheckCircle else SlateIcons.RadioButtonUnchecked,
              contentDescription = if (isDefault) "Default launch list" else "Set as default launch list",
              tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = if (isDefault) "Default" else "Launch",
              style = MaterialTheme.typography.labelMedium,
              color = if (isDefault) {
                MaterialTheme.colorScheme.onPrimaryContainer
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
            )
          }
        }

        IconButton(
          onClick = onStartEdit,
          modifier = Modifier
            .size(48.dp)
            .testTag("edit_list_${list.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = "Edit ${list.name}",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        if (canDelete) {
          IconButton(
            onClick = onDelete,
            modifier = Modifier
              .size(48.dp)
              .testTag("delete_list_${list.id}"),
          ) {
            Icon(
              imageVector = SlateIcons.DeleteOutline,
              contentDescription = "Delete ${list.name}",
              tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
            )
          }
        }
      }
    } else {
      // Seamless In-Place Expanded Edit Mode
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Box(
              modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(draftColor))
            )
            Text(
              text = "Edit list",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          IconButton(
            onClick = onCancelEdit,
            modifier = Modifier.size(40.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Cancel editing",
            )
          }
        }

        OutlinedTextField(
          value = draftName,
          onValueChange = { draftName = it },
          label = { Text("List name") },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
          ),
          keyboardActions = KeyboardActions(
            onDone = { onSaveEdit(draftName.trim().ifEmpty { list.name }, draftColor) },
          ),
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .testTag("inline_edit_list_name_${list.id}"),
        )

        ColorTagPickerRow(
          selectedColorHex = draftColor,
          onSelectColor = { draftColor = it },
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onCancelEdit,
            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
          ) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { onSaveEdit(draftName.trim().ifEmpty { list.name }, draftColor) },
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .testTag("save_inline_edit_list_${list.id}"),
          ) {
            Text("Save changes")
          }
        }
      }
    }
  }
}

@Composable
private fun InlineCreateListCard(
  isExpanded: Boolean,
  onExpand: () -> Unit,
  onCancel: () -> Unit,
  onCreate: (name: String, colorHex: Long) -> Unit,
) {
  var name by remember(isExpanded) { mutableStateOf("") }
  var selectedColor by remember(isExpanded) { mutableLongStateOf(ListColorTags.Sky) }
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(isExpanded) {
    if (isExpanded) {
      delay(80)
      focusRequester.requestFocus()
      keyboardController?.show()
    }
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 64.dp)
      .clip(RoundedCornerShape(24.dp))
      .then(if (!isExpanded) Modifier.stickyPressScale(pressedScale = 0.98f) else Modifier)
      .containedTapGlow(MaterialTheme.colorScheme.primary)
      .animateContentSize(
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMediumLow,
        )
      )
      .then(if (!isExpanded) Modifier.clickable(onClick = onExpand) else Modifier)
      .testTag("create_new_list_button"),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    border = BorderStroke(
      width = if (isExpanded) 1.5.dp else 1.dp,
      color = MaterialTheme.colorScheme.primary.copy(alpha = if (isExpanded) 0.6f else 0.38f),
    ),
    tonalElevation = if (isExpanded) 4.dp else 1.dp,
  ) {
    AnimatedContent(
      targetState = isExpanded,
      transitionSpec = { fadeIn() togetherWith fadeOut() },
      label = "inlineCreateListAnim",
    ) { expanded ->
      if (!expanded) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Create custom list",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      } else {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "New custom list",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            IconButton(
              onClick = onCancel,
              modifier = Modifier.size(40.dp),
            ) {
              Icon(Icons.Rounded.Close, contentDescription = "Cancel")
            }
          }

          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("List name") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
              capitalization = KeyboardCapitalization.Words,
              imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
              onDone = {
                if (name.isNotBlank()) {
                  onCreate(name.trim(), selectedColor)
                }
              },
            ),
            modifier = Modifier
              .fillMaxWidth()
              .focusRequester(focusRequester)
              .testTag("inline_new_list_name_input"),
          )

          ColorTagPickerRow(
            selectedColorHex = selectedColor,
            onSelectColor = { selectedColor = it },
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
          ) {
            TextButton(
              onClick = onCancel,
              modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
              Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = { onCreate(name.trim().ifEmpty { "List" }, selectedColor) },
              modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag("inline_create_list_save_button"),
            ) {
              Text("Create list")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ColorTagPickerRow(
  selectedColorHex: Long,
  onSelectColor: (Long) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ListColorTags.all.forEach { (hex, label) ->
      val isSelected = selectedColorHex == hex
      val scaleState = animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "colorTagScale",
      )
      Box(
        modifier = Modifier
          .size(42.dp)
          .graphicsLayer {
            val s = scaleState.value
            scaleX = s
            scaleY = s
          }
          .clip(CircleShape)
          .background(Color(hex))
          .then(
            if (isSelected) {
              Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
            } else {
              Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), CircleShape)
            }
          )
          .clickable { onSelectColor(hex) }
          .testTag("color_tag_$label"),
        contentAlignment = Alignment.Center,
      ) {
        if (isSelected) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = "$label selected",
            tint = Color.White,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}
