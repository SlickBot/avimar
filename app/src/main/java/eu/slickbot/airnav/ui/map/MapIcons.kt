package eu.slickbot.airnav.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import org.maplibre.android.maps.Style

/**
 * Generates white-silhouette icon bitmaps (one per [AircraftIconCategory] /
 * [VesselIconCategory]) and registers them with a MapLibre [Style] as SDF
 * images. Registering as SDF lets the SymbolLayer tint each instance with the
 * data-driven `icon-color` property while a single image is shared across all
 * markers of that category.
 *
 * Shapes are authored in a [-13, 13] design space (nose / bow pointing up so
 * `icon-rotate` from track/heading degrees works directly) and rasterized to a
 * small bitmap.
 */
object MapIcons {

  private const val SIZE = 48
  private val SCALE = (SIZE / 2f - 2f) / 13f

  private fun paint(): Paint {
    return Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = 0xFFFFFFFF.toInt()
      style = Paint.Style.FILL
    }
  }

  private fun tx(x: Float): Float {
    return SIZE / 2f + x * SCALE
  }

  private fun ty(y: Float): Float {
    return SIZE / 2f + y * SCALE
  }

  private fun polygonBitmap(points: List<Pair<Float, Float>>): Bitmap {
    val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val path = Path()
    points.forEachIndexed { i, (x, y) ->
      if (i == 0) path.moveTo(tx(x), ty(y)) else path.lineTo(tx(x), ty(y))
    }
    path.close()
    canvas.drawPath(path, paint())
    return bmp
  }

  private fun helicopterBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val p = paint()
    // Cabin (rounded teardrop approximated by an oval).
    canvas.drawOval(tx(-3.5f), ty(-6f), tx(3.5f), ty(4f), p)
    // Tail boom + fin.
    canvas.drawRect(tx(-1f), ty(3f), tx(1f), ty(10f), p)
    canvas.drawRect(tx(-3f), ty(9f), tx(3f), ty(11f), p)
    // Main rotor: two thin crossed bars.
    canvas.drawRect(tx(-12f), ty(-2.6f), tx(12f), ty(-1.4f), p)
    canvas.drawRect(tx(-0.6f), ty(-11f), tx(0.6f), ty(7f), p)
    return bmp
  }

  private fun droneBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val p = paint()
    val stroke = paint().apply {
      style = Paint.Style.STROKE
      strokeWidth = 1.4f * SCALE
    }
    // Diagonal arms.
    canvas.drawLine(tx(-8f), ty(-8f), tx(8f), ty(8f), stroke)
    canvas.drawLine(tx(-8f), ty(8f), tx(8f), ty(-8f), stroke)
    // Central body.
    canvas.drawRect(tx(-2.5f), ty(-2.5f), tx(2.5f), ty(2.5f), p)
    // Rotor rings at arm ends.
    val ringR = 3f * SCALE
    listOf(-8f to -8f, 8f to -8f, -8f to 8f, 8f to 8f).forEach { (x, y) ->
      canvas.drawCircle(tx(x), ty(y), ringR, stroke)
    }
    return bmp
  }

  // ---------- Aircraft silhouettes (nose up) ----------

  private val genericPlane = listOf(
    0f to -10f, 2f to -3f, 10f to 0f, 10f to 2f, 2f to 1f, 1f to 8f, 4f to 10f,
    4f to 11f, 0f to 9.5f, -4f to 11f, -4f to 10f, -1f to 8f, -2f to 1f,
    -10f to 2f, -10f to 0f, -2f to -3f,
  )

  private val jet = listOf(
    0f to -11f, 1.5f to -4f, 11f to 3f, 11f to 5f, 1.5f to 1.5f, 1f to 8f,
    3.5f to 10.5f, 3.5f to 11.5f, 0f to 10f, -3.5f to 11.5f, -3.5f to 10.5f,
    -1f to 8f, -1.5f to 1.5f, -11f to 5f, -11f to 3f, -1.5f to -4f,
  )

  private val turboprop = listOf(
    0f to -9f, 2f to -4f, 11f to -1f, 11f to 1f, 2f to 0f, 1.5f to 7f, 4f to 9f,
    4f to 10f, 0f to 8.5f, -4f to 10f, -4f to 9f, -1.5f to 7f, -2f to 0f,
    -11f to 1f, -11f to -1f, -2f to -4f,
  )

  private val piston = listOf(
    0f to -8f, 1.5f to -3f, 9f to 0f, 9f to 1.5f, 1.5f to 0.5f, 1f to 6f,
    3f to 8f, 3f to 9f, 0f to 7.5f, -3f to 9f, -3f to 8f, -1f to 6f,
    -1.5f to 0.5f, -9f to 1.5f, -9f to 0f, -1.5f to -3f,
  )

  private val military = listOf(
    0f to -11f, 2f to 2f, 9f to 7f, 9f to 8.5f, 0f to 5f, -9f to 8.5f, -9f to 7f, -2f to 2f,
  )

  private val glider = listOf(
    0f to -10f, 1f to -2f, 13f to -0.3f, 13f to 0.7f, 1f to 1f, 0.8f to 8f,
    2.5f to 10f, 2.5f to 10.8f, 0f to 9.5f, -2.5f to 10.8f, -2.5f to 10f,
    -0.8f to 8f, -1f to 1f, -13f to 0.7f, -13f to -0.3f, -1f to -2f,
  )

  // ---------- Vessel silhouettes (bow up) ----------

  private val genericShip = listOf(0f to -9f, 5f to -2f, 5f to 8f, -5f to 8f, -5f to -2f)
  private val cargo = listOf(0f to -11f, 3.5f to -5f, 3.5f to 10f, -3.5f to 10f, -3.5f to -5f)
  private val tanker = listOf(0f to -10f, 5f to -3f, 5f to 9f, -5f to 9f, -5f to -3f)
  private val fishing = listOf(0f to -7f, 3f to -1f, 3f to 6f, -3f to 6f, -3f to -1f)
  private val pleasure = listOf(0f to -10f, 3f to 7f, -3f to 7f)
  private val tug = listOf(0f to -6f, 4f to -1f, 4f to 5f, -4f to 5f, -4f to -1f)

  // Stepped outline: narrow tall bow rising to a broad deck → reads as a large
  // passenger/cruise ship. One polygon so it works as an SDF (alpha-only) image.
  private val passenger = listOf(
    0f to -10f, 2.2f to -4f, 2.2f to -1f, 4.5f to -1f, 4.5f to 8f,
    -4.5f to 8f, -4.5f to -1f, -2.2f to -1f, -2.2f to -4f,
  )

  /** All category images keyed by the [AircraftIconCategory.image] / [VesselIconCategory.image] name. */
  fun all(): Map<String, Bitmap> {
    return mapOf(
      AircraftIconCategory.DEFAULT.image to polygonBitmap(genericPlane),
      AircraftIconCategory.JET.image to polygonBitmap(jet),
      AircraftIconCategory.TURBOPROP.image to polygonBitmap(turboprop),
      AircraftIconCategory.PISTON.image to polygonBitmap(piston),
      AircraftIconCategory.MILITARY.image to polygonBitmap(military),
      AircraftIconCategory.GLIDER.image to polygonBitmap(glider),
      AircraftIconCategory.HELICOPTER.image to helicopterBitmap(),
      AircraftIconCategory.DRONE.image to droneBitmap(),
      VesselIconCategory.DEFAULT.image to polygonBitmap(genericShip),
      VesselIconCategory.CARGO.image to polygonBitmap(cargo),
      VesselIconCategory.TANKER.image to polygonBitmap(tanker),
      VesselIconCategory.FISHING.image to polygonBitmap(fishing),
      VesselIconCategory.PLEASURE.image to polygonBitmap(pleasure),
      VesselIconCategory.TUG.image to polygonBitmap(tug),
      VesselIconCategory.PASSENGER.image to polygonBitmap(passenger),
    )
  }

  /** Registers every category image with [style] as an SDF image (tintable). */
  fun register(style: Style) {
    all().forEach { (name, bitmap) -> style.addImage(name, bitmap, true) }
  }
}
