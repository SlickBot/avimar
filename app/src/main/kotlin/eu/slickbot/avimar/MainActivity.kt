package eu.slickbot.avimar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import eu.slickbot.avimar.ui.map.MapScreen
import eu.slickbot.avimar.ui.theme.AvimarTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AvimarTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          MapScreen()
        }
      }
    }
  }
}
