package storage

import discord4j.common.util.Snowflake
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant
import kotlin.system.exitProcess

object Database {
    private val postgres = Postgres()
    val logger: Logger = LoggerFactory.getLogger(Database::class.java)

    init {
        doMigration(
            "CREATE TABLE IF NOT EXISTS questions (id SERIAL PRIMARY KEY, author int8, question varchar(255), answered boolean default false)",
            "Created questions table."
        )
        doMigration(
            "CREATE TABLE IF NOT EXISTS suggestions (id SERIAL PRIMARY KEY, author int8, question varchar(255))",
            "Created suggestions table."
        )
        doMigration(
            "ALTER TABLE questions ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP NOT NULL DEFAULT now()",
            "Added approved_at column to questions table."
        )
    }

    private fun doMigration(@Language("SQL") sql: String, infoString: String) {
        if (!postgres.execute(sql)) {
            logger.error("Failed to execute everything in setup. Exiting.")
            exitProcess(1)
        }
        logger.info(infoString)
    }

    fun addSuggestion(author: Snowflake, question: String?): Int {
        return postgres.insert(
            "INSERT INTO public.suggestions (author, question) VALUES (?, ?)",
            SqlConsumer { statement: PreparedStatement ->
                statement.setLong(1, author.asLong())
                statement.setString(2, question)
            }
        )
    }

    fun moveSuggestionToQuestion(suggestionId: Int): Int {
        try {
            postgres.connection.use { connection ->
                val author: Long
                val question: String
                val querySuggestion = connection.prepareStatement("SELECT * FROM public.suggestions WHERE id = ?")
                querySuggestion.setInt(1, suggestionId)
                querySuggestion.executeQuery().use { resultSet ->
                    if (!resultSet.next()) {
                        logger.error("Attempted reading non-existant suggestions id {}.", suggestionId)
                        return -1
                    }
                    author = resultSet.getLong("author")
                    question = resultSet.getString("question")
                }

                connection.autoCommit = false

                val insertStatement = connection.prepareStatement(
                    "INSERT INTO public.questions (author, question, approved_at) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS,
                )
                insertStatement.setLong(1, author)
                insertStatement.setString(2, question)
                insertStatement.setTimestamp(3, Timestamp.from(Instant.now()))
                insertStatement.executeUpdate()

                val deleteStatement = connection.prepareStatement("DELETE FROM public.suggestions WHERE id = ?")
                deleteStatement.setInt(1, suggestionId)
                deleteStatement.executeUpdate()

                connection.commit()
                connection.autoCommit = true

                insertStatement.generatedKeys.use { generatedKeys ->
                    val result = generatedKeys.next()
                    if (result) return generatedKeys.getInt(1)
                }
                logger.error("Unexpectedly got no question id from result set.")
                return -1
            }
        } catch (e: SQLException) {
            logger.error("Failed to approve suggestion id {}.", suggestionId, e)
            return -1
        }
    }

    fun deleteSuggestion(suggestionId: Int): Boolean {
        return postgres.execute(
            "DELETE FROM public.suggestions WHERE id = ?",
            SqlConsumer { statement: PreparedStatement ->
                statement.setInt(1, suggestionId)
            }
        )
    }

    fun getRandomQuestion(): Question? {
        return try {
            postgres.connection.use { connection ->
                val statement =
                    connection.prepareStatement("SELECT * FROM public.questions WHERE answered = false ORDER BY random() LIMIT 1")
                val result = statement.executeQuery()

                if (!result.next()) {
                    // Currently no question
                    return null
                }
                Question(
                    id = result.getInt("id"),
                    author = result.getLong("author"),
                    question = result.getString("question"),
                    approvedAt = result.getTimestamp("approved_at").toInstant(),
                )
            }
        } catch (_: SQLException) {
            null
        }
    }

    fun setQuestionAnswered(questionId: Int): Boolean {
        return postgres.execute(
            "UPDATE public.questions SET answered = true WHERE id = ?",
            SqlConsumer { statement: PreparedStatement ->
                statement.setInt(1, questionId)
            }
        )
    }

    fun getQueueLength(): Int {
        return try {
            postgres.connection.use { connection ->
                val result = connection
                    .prepareStatement("SELECT count(*) FROM public.questions WHERE answered = false")
                    .executeQuery()
                if (result.next()) result.getInt(1) else -1
            }
        } catch (_: SQLException) {
            -1
        }
    }

    // companion object {
    //     @JvmField
    //     val INSTANCE: Database = Database()
    // }
}
