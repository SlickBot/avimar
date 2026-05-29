package eu.slickbot.airnav.data.common

class RateLimitException(
  message: String,
  cause: Throwable? = null,
) : RuntimeException(message, cause)
