package eu.slickbot.airnav.ui.map

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Category codes shown in the on-map legend (aircraft only; ground/obstacles omitted). */
private val LEGEND_CODES = setOf("A1", "A2", "A3", "A4", "A5", "A6", "A7", "B1", "B2", "B6", OTHER_CATEGORY_CODE)

@Composable
fun MapLegend(modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    color = Color(0xE6121218),
    contentColor = Color(0xFFC8C8D4),
    shape = RoundedCornerShape(8.dp),
    tonalElevation = 3.dp,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Text("LEGEND", fontSize = 9.sp, color = Color(0xFF8A8A99))
      ADSB_CATEGORIES.filter { it.code in LEGEND_CODES }.forEach { category ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Box(
            modifier = Modifier
              .size(9.dp)
              .clip(CircleShape)
              .background(Color(AndroidColor.parseColor(category.color))),
          ) {}
          Text(category.label, fontSize = 10.sp)
        }
      }
    }
  }
}
