package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.SlateRootScreen
import com.example.ui.SlateViewModel
import com.example.ui.theme.SlateTheme

class MainActivity : ComponentActivity() {
  private val viewModel: SlateViewModel by viewModels {
    SlateViewModel.factory(application)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val themePrefs by viewModel.themePrefsFlow.collectAsStateWithLifecycle()
      SlateTheme(
        oledDarkMode = themePrefs.first,
        appColorTheme = themePrefs.second,
      ) {
        SlateRootScreen(viewModel = viewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshCalendarPermissionsAndSync()
  }
}
