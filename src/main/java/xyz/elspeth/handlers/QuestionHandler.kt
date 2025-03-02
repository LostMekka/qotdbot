package xyz.elspeth.handlers;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;
import storage.Database;
import storage.Question;
import xyz.elspeth.Constants;
import xyz.elspeth.DiscordIds;

import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestionHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(QuestionHandler.class);
	
	private static Mono<MessageCreateSpec> createQuestionMessage(GatewayDiscordClient client, Question question) {
		
		return client.getUserById(Snowflake.of(question.author()))
					 .map(User :: getUsername)
					 .onErrorReturn("Missing user")
					 .map(username -> {
						 var embed = EmbedCreateSpec
							 .builder()
							 .title("Babas question of the day!")
							 .description(question.question())
							 .footer("Author: %s -- %d Cards Remaining".formatted(username, Database.INSTANCE.getQueueLength() - 1), "")
							 .color(Color.DEEP_LILAC)
							 .build();
						 
						 return MessageCreateSpec
							 .builder()
							 .addEmbed(embed)
							 .addComponent(
								 ActionRow.of(
									 Button.secondary(DiscordIds.OPEN_MODAL_BUTTON, "Suggest a question.")
								 )
							 )
							 .build();
					 });
	}
	
	private static MessageCreateSpec createNoQuestionMessage() {
		
		var embed = EmbedCreateSpec
			.builder()
			.description("""
							 This channel has no more cards to post. Please add more cards (or reset decks) if you'd like to continue posting.
							 
							 If no cards are available on the next post, the channel will **not** be automatically paused."""
			)
			.color(Color.MOON_YELLOW)
			.build();
		
		return MessageCreateSpec
			.builder()
			.addEmbed(embed)
			.addComponent(
				ActionRow.of(
					Button.primary(DiscordIds.OPEN_MODAL_BUTTON, "Suggest a question.")
				)
			)
			.build();
	}
	
	public static Runnable postNextQuestion(GatewayDiscordClient client) {
		
		return () -> {
			logger.info("Posting next question.");
			Mono.justOrEmpty(Database.INSTANCE.getRandomQuestion())
				.filter(Objects :: nonNull)
				.switchIfEmpty(Mono.error(new SQLException()))
				.flatMap(question -> client
					.getChannelById(Constants.DAILY_CHANNEL)
					.cast(TextChannel.class)
					.zipWith(createQuestionMessage(client, question))
					.flatMap(tuple -> {
						return tuple.getT1()
									.createMessage(tuple.getT2());
					})
					.map(ignored -> Database.INSTANCE.setQuestionAnswered(question.id()))
					.filter(value -> value)
					.switchIfEmpty(Mono.error(new SQLException()))
					.onErrorResume(error -> {
						if (!(error instanceof SQLException)) {
							logger.error("", error);
						}
						return client
							.getChannelById(Constants.MOD_CHANNEL)
							.cast(TextChannel.class)
							.flatMap(channel -> channel.createMessage("<@!261538420952662016> Error during question post."))
							.thenReturn(true);
					})
				)
				.onErrorResume(error -> {
								   return client.getChannelById(Constants.DAILY_CHANNEL)
												.cast(TextChannel.class)
												.flatMap(channel -> channel.createMessage(createNoQuestionMessage()))
												.then(Mono.empty());
							   }
				)
				.subscribe();
		};
	}
	
}
