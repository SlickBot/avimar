package eu.slickbot.avimar.data.common

import java.time.Duration

data class RateLimit(
  val maxUnits: Int,
  val window: Duration,
)
