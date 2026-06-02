package eu.slickbot.airnav.ui.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt

/**
 * One legend row's data: a filter [code], a [label], a [glyph], and an optional
 * short [codeLabel] shown beside the glyph (the ADS-B emitter code for aircraft;
 * null for vessels/status where there is no meaningful short code).
 */
data class CategoryRow(
  val code: String,
  val label: String,
  val glyph: CategoryGlyph,
  val codeLabel: String? = null,
)

sealed interface CategoryGlyph {
  /** ADS-B silhouette by category code. */
  data class Aircraft(val categoryCode: String) : CategoryGlyph
  /** Vessel silhouette by icon category. */
  data class VesselSilhouette(val iconCategory: VesselIconCategory) : CategoryGlyph
  /** Colored dot (navigational status). */
  data class Dot(val colorHex: String) : CategoryGlyph
}

/**
 * Legend + filter list shared by the on-map legend panel and the settings tabs.
 * Each row is the real marker glyph + label + checkbox; checking shows the
 * category, unchecking hides it. "Hide all / Show all" toggles the whole group.
 */
@Composable
fun CategoryFilterList(
  title: String,
  rows: List<CategoryRow>,
  hidden: Set<String>,
  onToggle: (String) -> Unit,
  onToggleAll: (allVisibleNow: Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val allVisible = rows.none { it.code in hidden }
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = if (allVisible) "Hide all" else "Show all",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
          .clip(MaterialTheme.shapes.small)
          .clickable { onToggleAll(allVisible) }
          .padding(horizontal = 6.dp, vertical = 2.dp),
      )
    }
    rows.forEach { row ->
      val visible = row.code !in hidden
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onToggle(row.code) }
          .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = if (visible) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
          contentDescription = null,
          tint = if (visible) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
          modifier = Modifier.size(20.dp),
        )
        if (row.codeLabel != null) {
          Text(
            text = row.codeLabel,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(22.dp),
          )
        }
        CategoryGlyphIcon(row.glyph)
        Text(
          text = row.label,
          style = MaterialTheme.typography.bodyMedium,
          color = if (visible) {
            MaterialTheme.colorScheme.onSurface
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
        )
      }
    }
  }
}

@Composable
private fun CategoryGlyphIcon(glyph: CategoryGlyph) {
  when (glyph) {
    is CategoryGlyph.Aircraft -> {
      val bmp = remember(glyph.categoryCode) {
        MapIcons.categoryBitmap(adsbCategoryByCode(glyph.categoryCode)).asImageBitmap()
      }
      Image(bitmap = bmp, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(20.dp))
    }
    is CategoryGlyph.VesselSilhouette -> {
      val bmp = remember(glyph.iconCategory) {
        MapIcons.vesselBitmap(glyph.iconCategory).asImageBitmap()
      }
      Image(bitmap = bmp, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(20.dp))
    }
    is CategoryGlyph.Dot -> {
      Box(
        modifier = Modifier
          .size(12.dp)
          .clip(CircleShape)
          .background(Color(glyph.colorHex.toColorInt())),
      )
    }
  }
}

// ---------- Row builders (single source of truth for both call sites) ----------

fun aircraftCategoryRows(): List<CategoryRow> {
  return ADSB_CATEGORIES.map {
    val codeLabel = if (it.code == OTHER_CATEGORY_CODE) null else it.code
    CategoryRow(it.code, it.label, CategoryGlyph.Aircraft(it.code), codeLabel = codeLabel)
  }
}

fun vesselCategoryRows(): List<CategoryRow> {
  return VESSEL_CATEGORIES.map {
    CategoryRow(it.code, it.label, CategoryGlyph.VesselSilhouette(it.iconCategory))
  }
}

fun vesselStatusRows(): List<CategoryRow> {
  return AIS_STATUS_CATEGORIES.map { CategoryRow(it.code, it.label, CategoryGlyph.Dot(it.color)) }
}
