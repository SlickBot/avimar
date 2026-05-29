package eu.slickbot.airnav.data.common

/**
 * Process-wide, runtime-adjustable network knobs read by [BaseHttpFetcher] on
 * each call. Lets the UI change the request timeout without rebuilding the
 * OkHttp client or the provider graph.
 */
object NetworkTuning {

  /** Per-call timeout in milliseconds; <= 0 disables the per-call override. */
  @Volatile
  var callTimeoutMs: Long = 15_000L
}
