package eu.slickbot.airnav.data.common

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Manually-advanced [Clock] used across data/common unit tests. */
internal class FakeClock(start: Instant) : Clock() {
    private var now: Instant = start
    fun advance(d: Duration) { now = now.plus(d) }
    override fun instant(): Instant = now
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
}
