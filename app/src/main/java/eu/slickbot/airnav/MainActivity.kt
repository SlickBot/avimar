package eu.slickbot.airnav

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.slickbot.airnav.ui.map.MapScreen
import eu.slickbot.airnav.ui.theme.AirnavTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AirnavApp()
    }
  }
}

@Composable
private fun AirnavApp() {
  AirnavTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
      MapScreen()
    }
  }
}
