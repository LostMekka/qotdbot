package xyz.elspeth.schedule

import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import kotlin.time.toKotlinDuration

object Scheduler {
    private val logger: Logger = LoggerFactory.getLogger(Scheduler::class.java)
    private val scope = Executors
        // daemon threads get killed when main() exits and there are no non-daemon threads left
        .newSingleThreadExecutor { Thread(it, "scheduler-thread").apply { isDaemon = true } }
        .asCoroutineDispatcher()
        .let { CoroutineScope(it) }

    @JvmStatic
    fun schedule(job: suspend () -> Unit) {
        var nextRun = now.truncatedTo(ChronoUnit.DAYS).plusDays(1)

        scope.launch {
            while (isActive) {
                val delay = timeUntil(nextRun)
                if (delay.isPositive()) delay(delay)
                nextRun = nextRun.plusDays(1)
                job()
            }
        }

        logger.info("Started schedule. Next run in: {}", timeUntil(nextRun))
    }

    private fun timeUntil(nextRun: ZonedDateTime?) = Duration.between(now, nextRun).toKotlinDuration()
    private val now: ZonedDateTime get() = ZonedDateTime.now(ZoneId.of("UTC"))
}
