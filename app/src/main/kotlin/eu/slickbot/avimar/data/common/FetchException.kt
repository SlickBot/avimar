package eu.slickbot.avimar.data.common

class FetchException(
  message: String,
  val statusCode: Int? = null,
  cause: Throwable? = null,
) : RuntimeException(message, cause)
