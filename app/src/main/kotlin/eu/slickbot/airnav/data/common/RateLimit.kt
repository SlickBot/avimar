package eu.slickbot.airnav.data.common

import java.time.Duration

data class RateLimit(
  val maxUnits: Int,
  val window: Duration,
)
