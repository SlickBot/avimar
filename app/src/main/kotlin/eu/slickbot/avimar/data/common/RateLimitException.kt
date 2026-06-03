package eu.slickbot.avimar.data.common

class RateLimitException(
  message: String,
  cause: Throwable? = null,
) : RuntimeException(message, cause)
