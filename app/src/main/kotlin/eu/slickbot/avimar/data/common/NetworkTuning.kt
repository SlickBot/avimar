package eu.slickbot.avimar.data.common

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object NetworkTuning {

  @Volatile
  var callTimeout: Duration = 15.seconds
}
