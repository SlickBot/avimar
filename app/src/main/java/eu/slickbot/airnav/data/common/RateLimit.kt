package eu.slickbot.airnav.data.common

import java.time.Duration

/** Sliding-window rate limit: at most [maxUnits] within any [window] period. */
data class RateLimit(
  val maxUnits: Int,
  val window: Duration,
)
