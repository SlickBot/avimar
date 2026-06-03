package eu.slickbot.airnav.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun PhotoViewer(
  urls: List<String>,
  initialIndex: Int,
  contentDescription: String?,
  onDismiss: () -> Unit,
) {
  if (urls.isEmpty()) return
  BackHandler(onBack = onDismiss)
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black),
  ) {
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, urls.size - 1)) { urls.size }
    HorizontalPager(
      state = pagerState,
      modifier = Modifier.fillMaxSize(),
    ) { page ->
      ZoomableImage(
        url = urls[page],
        contentDescription = contentDescription,
      )
    }
    IconButton(
      onClick = onDismiss,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .statusBarsPadding()
        .padding(8.dp),
    ) {
      Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
    }
  }
}

@Composable
private fun ZoomableImage(url: String, contentDescription: String?) {
  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .pointerInput(Unit) {
        detectTapGestures(
          onDoubleTap = {
            if (scale > 1f) {
              scale = 1f
              offset = Offset.Zero
            } else {
              scale = 2f
            }
          },
        )
      }
      .pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
          val newScale = (scale * zoom).coerceIn(1f, MAX_SCALE)
          scale = newScale
          if (newScale > 1f) {
            offset += pan
          } else {
            offset = Offset.Zero
          }
        }
      },
    contentAlignment = Alignment.Center,
  ) {
    AsyncImage(
      model = url,
      contentDescription = contentDescription,
      contentScale = ContentScale.Fit,
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer(
          scaleX = scale,
          scaleY = scale,
          translationX = offset.x,
          translationY = offset.y,
        ),
    )
  }
}

private const val MAX_SCALE = 5f
