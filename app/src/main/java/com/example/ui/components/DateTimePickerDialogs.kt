package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.SlateListWithCounts
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
