package storage

import java.time.Instant

@JvmRecord
data class Question(
    val id: Int,
    val author: Long,
    val question: String,
    val approvedAt: Instant,
)
