package xyz.elspeth.handlers;

import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;
import storage.Database;

import java.sql.SQLException;
import java.util.Collections;

public class SuggestionHandler {
	
	private static Mono<Message> updateEmbedColor(Message message, Color color) {
		
		var embed = message.getEmbeds()
						   .getFirst();
		
		var builder = EmbedCreateSpec
			.builder()
			.color(color);
		
		for (var field : embed.getFields()) {
			builder.addField(field.getName(), field.getValue(), field.isInline());
		}
		
		return message
			.edit(MessageEditSpec
					  .builder()
					  .addEmbed(builder.build())
					  .addAllComponents(Collections.emptyList())
					  .build());
	}
	
	public static Mono<Message> approveSuggestion(Message message, int suggestionId) {
		
		return Mono.just(Database.INSTANCE.moveSuggestionToQuestion(suggestionId))
				   .filter(value -> value != -1)
				   .switchIfEmpty(Mono.error(new SQLException()))
				   .flatMap(ignored -> updateEmbedColor(message, Color.SEA_GREEN));
		
	}
	
	public static Mono<Message> rejectSuggestion(Message message, int suggestionId) {
		
		return Mono.just(Database.INSTANCE.deleteSuggestion(suggestionId))
				   .filter(value -> value)
				   .switchIfEmpty(Mono.error(new SQLException()))
				   .flatMap(ignored -> updateEmbedColor(message, Color.JAZZBERRY_JAM));
	}
	
}
