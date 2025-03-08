package xyz.elspeth.handlers

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.TextChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.MessageCreateSpec
import discord4j.rest.util.Color
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import storage.Database
import storage.Question
import xyz.elspeth.Constants
import xyz.elspeth.DiscordIds
import java.sql.SQLException
import java.time.Instant
import java.time.temporal.ChronoUnit

object QuestionHandler {
    private val logger: Logger = LoggerFactory.getLogger(QuestionHandler::class.java)

    suspend fun createQuestionMessage(client: GatewayDiscordClient, question: Question): MessageCreateSpec {
        val username = runCatching {
            client.getUserById(Snowflake.of(question.author))
                .awaitSingleOrNull()
                ?.username
        }.getOrDefault("Missing user")

        val cardsRemaining = Database.getQueueLength() - 1
        val questionAge = ChronoUnit.DAYS.between(question.approvedAt, Instant.now())
        val embed = EmbedCreateSpec.builder()
            .title("Babas question of the day!")
            .description(question.question)
            .footer("Author: $username -- added $questionAge days ago -- $cardsRemaining Cards Remaining", "")
            .color(Color.DEEP_LILAC)
            .build()

        return MessageCreateSpec.builder()
            .addEmbed(embed)
            .addComponent(ActionRow.of(Button.secondary(DiscordIds.OPEN_MODAL_BUTTON, "Suggest a question.")))
            .build()
    }

    private fun createNoQuestionMessage(): MessageCreateSpec {
        val embed: EmbedCreateSpec = EmbedCreateSpec
            .builder()
            .description(
                """
                This channel has no more cards to post. Please add more cards (or reset decks) if you'd like to continue posting.
                
                If no cards are available on the next post, the channel will **not** be automatically paused.
                """.trimIndent()
            )
            .color(Color.MOON_YELLOW)
            .build()

        return MessageCreateSpec
            .builder()
            .addEmbed(embed)
            .addComponent(ActionRow.of(Button.primary(DiscordIds.OPEN_MODAL_BUTTON, "Suggest a question.")))
            .build()
    }

    @JvmStatic
    fun postNextQuestion(client: GatewayDiscordClient): suspend () -> Unit = {
        try {
            logger.info("Posting next question.")
            val question = Database.getRandomQuestion()
            if (question == null) runCatching {
                client.sendMessage(Constants.DAILY_CHANNEL, createNoQuestionMessage())
            } else {
                client.sendMessage(Constants.DAILY_CHANNEL, createQuestionMessage(client, question))

                val success = Database.setQuestionAnswered(question.id)
                if (!success) throw SQLException("Failed to mark question as answered.")
            }
        } catch (e: Exception) {
            if (e !is SQLException) logger.error("Error during question post.", e)
            runCatching {
                client.sendMessage(Constants.MOD_CHANNEL, "<@!261538420952662016> Error during question post.")
            }
        }
    }

    private suspend fun GatewayDiscordClient.sendMessage(
        channelSnowflake: Snowflake,
        messageCreateSpec: MessageCreateSpec,
    ): Message =
        this.getTextChannel(channelSnowflake)
            .createMessage(messageCreateSpec)
            .awaitSingle()

    private suspend fun GatewayDiscordClient.sendMessage(
        channelSnowflake: Snowflake,
        plainMessage: String,
    ): Message =
        this.getTextChannel(channelSnowflake)
            .createMessage(plainMessage)
            .awaitSingle()

    private suspend fun GatewayDiscordClient.getTextChannel(channelSnowflake: Snowflake): TextChannel =
        getChannelById(channelSnowflake).awaitSingle() as TextChannel
}
