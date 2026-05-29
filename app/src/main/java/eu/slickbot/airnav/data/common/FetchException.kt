package eu.slickbot.airnav.data.common

/**
 * Thrown by [HttpFetcher] implementations when an outbound request fails or
 * returns a non-2xx status. [statusCode] is null for transport-level failures
 * (network, timeout, interrupt).
 */
class FetchException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
