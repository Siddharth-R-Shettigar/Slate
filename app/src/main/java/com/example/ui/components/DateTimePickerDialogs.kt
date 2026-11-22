package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.SlateListWithCounts
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDateTimeDialog(
  initialMillis: Long?,
  title: String = "Schedule & Sync",
  onDismiss: () -> Unit,
  onConfirm: (Long) -> Unit,
) {
  val baseCal = remember(initialMillis) {
    Calendar.getInstance().apply {
      timeInMillis = initialMillis ?: (System.currentTimeMillis() + 3_600_000L)
    }
  }
  var showTimeStep by remember { mutableStateOf(false) }

  val initialUtcMidnight = remember(baseCal) {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      clear()
      set(
        baseCal.get(Calendar.YEAR),
        baseCal.get(Calendar.MONTH),
        baseCal.get(Calendar.DAY_OF_MONTH),
      )
    }
    utcCal.timeInMillis
  }

  val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMidnight)
  val timePickerState = rememberTimePickerState(
    initialHour = baseCal.get(Calendar.HOUR_OF_DAY),
    initialMinute = baseCal.get(Calendar.MINUTE),
    is24Hour = false,
  )

  if (!showTimeStep) {
    DatePickerDialog(
      onDismissRequest = onDismiss,
      confirmButton = {
        TextButton(
          onClick = { showTimeStep = true },
          modifier = Modifier.testTag("date_picker_next_button"),
        ) {
          Text("Pick Time")
        }
      },
      dismissButton = {
        TextButton(onClick = onDismiss) {
          Text("Cancel")
        }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  } else {
    Dialog(onDismissRequest = onDismiss) {
      Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        tonalElevation = 6.dp,
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = SlateIcons.Schedule,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = title,
              style = MaterialTheme.typography.titleLarge,
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          TimePicker(state = timePickerState)
          Spacer(modifier = Modifier.height(16.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
          ) {
            TextButton(onClick = { showTimeStep = false }) {
              Text("Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
              onClick = {
                val selectedUtc = datePickerState.selectedDateMillis ?: initialUtcMidnight
                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                  timeInMillis = selectedUtc
                }
                val localCal = Calendar.getInstance().apply {
                  set(
                    utcCal.get(Calendar.YEAR),
                    utcCal.get(Calendar.MONTH),
                    utcCal.get(Calendar.DAY_OF_MONTH),
                    timePickerState.hour,
                    timePickerState.minute,
                    0,
                  )
                  set(Calendar.MILLISECOND, 0)
                }
                onConfirm(localCal.timeInMillis)
              },
              modifier = Modifier.testTag("time_picker_confirm_button"),
            ) {
              Text("Save & Sync")
            }
          }
        }
      }
    }
  }
}

@Composable
fun MoveToListDialog(
  lists: List<SlateListWithCounts>,
  currentListId: Long,
  onDismiss: () -> Unit,
  onSelectList: (Long) -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Move to list",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      val cardShape = remember { RoundedCornerShape(16.dp) }
      val cardModifier = remember(cardShape) {
        Modifier
          .fillMaxWidth()
          .clip(cardShape)
      }
      val rowModifier = remember {
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 14.dp)
      }
      val dotModifier = remember {
        Modifier
          .size(12.dp)
          .clip(CircleShape)
      }
      val checkIconModifier = remember { Modifier.size(18.dp) }
      val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
      val primaryBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)

      LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(
          items = lists,
          key = { it.id },
          contentType = { "move_list_row" },
        ) { list ->
          val isCurrent = list.id == currentListId
          Surface(
            modifier = cardModifier
              .clickable { onSelectList(list.id) }
              .testTag("move_target_list_${list.id}"),
            color = if (isCurrent) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceContainerLow
            },
            border = BorderStroke(
              width = if (isCurrent) 1.5.dp else 1.dp,
              color = if (isCurrent) primaryBorderColor else outlineColor,
            ),
            shape = cardShape,
          ) {
            Row(
              modifier = rowModifier,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(modifier = dotModifier.background(Color(list.colorTagHex)))
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = list.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
              )
              if (isCurrent) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = "Current list",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = checkIconModifier,
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    },
  )
}

@Composable
fun EditTaskDialog(
  initialTitle: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var value by remember(initialTitle) {
    mutableStateOf(
      TextFieldValue(
        text = initialTitle,
        selection = TextRange(initialTitle.length),
      )
    )
  }
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  LaunchedEffect(Unit) {
    delay(80)
    focusRequester.requestFocus()
    keyboardController?.show()
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = SlateIcons.Edit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Edit task",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        OutlinedTextField(
          value = value,
          onValueChange = { value = it },
          label = { Text("Task title") },
          singleLine = false,
          maxLines = 4,
          shape = RoundedCornerShape(16.dp),
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
          ),
          keyboardActions = KeyboardActions(
            onDone = {
              val trimmed = value.text.trim()
              if (trimmed.isNotEmpty()) {
                onConfirm(trimmed)
              }
            },
          ),
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .testTag("edit_task_textfield"),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
          ) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              val trimmed = value.text.trim()
              if (trimmed.isNotEmpty()) {
                onConfirm(trimmed)
              }
            },
            enabled = value.text.trim().isNotEmpty(),
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .testTag("edit_task_save_button"),
          ) {
            Text("Save")
          }
        }
      }
    }
  }
}

@Composable
fun DeleteListConfirmationDialog(
  list: SlateListWithCounts,
  onDismiss: () -> Unit,
  onConfirmDelete: () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
      tonalElevation = 6.dp,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = SlateIcons.DeleteOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
          )
          Text(
            text = "Delete \"${list.name}\"?",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        val itemSummary = when {
          list.totalCount == 0 -> "This empty list will be permanently removed."
          list.totalCount == 1 -> "This will permanently delete 1 item inside \"${list.name}\"."
          else -> "This will permanently delete all ${list.totalCount} items inside \"${list.name}\"."
        }
        Text(
          text = itemSummary,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .testTag("cancel_delete_list_button"),
          ) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = onConfirmDelete,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .testTag("confirm_delete_list_button"),
          ) {
            Text("Delete list", fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}
