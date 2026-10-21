package app.royalreader.domain.model

/**
 * Wall-clock time spent in reader screen, per calendar day.
 * - [dateEpochDay] is the [java.time.LocalDate.toEpochDay] of the session
 **/
data class ReadingSession(
    val id: Long = 0L,
    val novelID: Long,
    val dateEpochDay: Long,
    val durationMs: Long
)

/**
 * Total reading time for one day.
 * - [day] is an epoch day
 **/
data class DailyReadingTime(
    val day: Long,
    val totalMs: Long
)