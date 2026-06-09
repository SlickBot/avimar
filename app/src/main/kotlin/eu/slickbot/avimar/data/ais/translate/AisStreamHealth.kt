package eu.slickbot.avimar.data.ais.translate

import java.time.Duration
import java.time.Instant

data class AisStreamHealth(
  val vesselCount: Int,
  val translatedCount: Long,
  val lastRawMessageAt: Instant?,
  val lastTranslatedAt: Instant?,
  val snapshotAt: Instant?,
  val rawMessageAge: Duration?,
  val translatedAge: Duration?,
)
