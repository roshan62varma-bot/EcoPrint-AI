package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.CarbonDashboard
import com.example.ui.CarbonSplashScreen
import com.example.ui.CarbonViewModel
import com.example.ui.CarbonViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Standard ViewModel instantiation with customized Factory passing Application
    val viewModel = ViewModelProvider(
      this, 
      CarbonViewModelFactory(application)
    )[CarbonViewModel::class.java]

    setContent {
      MyApplicationTheme {
        var showSplash by remember { mutableStateOf(true) }
        Crossfade(targetState = showSplash, animationSpec = tween(800)) { isSplash ->
          if (isSplash) {
            CarbonSplashScreen(onTimeout = { showSplash = false })
          } else {
            CarbonDashboard(viewModel = viewModel)
          }
        }
      }
    }
  }
}
