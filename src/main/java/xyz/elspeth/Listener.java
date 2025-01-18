package xyz.elspeth;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.TextInput;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;
import storage.Database;
import xyz.elspeth.handlers.ModalHandler;
import xyz.elspeth.handlers.SuggestionHandler;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Listener {
	
	private static final Logger logger = LoggerFactory.getLogger(Listener.class);
	
	public void onCommand(ApplicationCommandInteractionEvent event) {
		
		var user = event.getInteraction()
						.getUser();
		
		if (event.getCommandName()
				 .equals(DiscordIds.COMMAND)) {
			event
				.presentModal(
					ModalHandler.buildSuggestionModal()
				)
				.subscribe();
		}
		
	}
	
	private Mono<Message> sendModMessage(Snowflake author, String question, int questionId) {
		
		return Main.gateway.getChannelById(Constants.MOD_CHANNEL)
						   .cast(TextChannel.class)
						   .flatMap(channel -> {
							   var embed = EmbedCreateSpec
								   .builder()
								   .addField("Author", "<@!" + author.asString() + ">", false)
								   .addField("Question", question, false)
								   .color(Color.GRAY)
								   .build();
							   
							   var message = MessageCreateSpec
								   .builder()
								   .addEmbed(embed)
								   .addComponent(
									   ActionRow.of(
										   Button.success(DiscordIds.ACCEPT_BUTTON + questionId, "Approve"),
										   Button.danger(DiscordIds.DENY_BUTTON + questionId, "Deny")
									   )
								   )
								   .build();
							   
							   return channel.createMessage(message);
						   });
	}
	
	public void onModal(ModalSubmitInteractionEvent event) {
		
		var author = event.getInteraction()
						  .getUser();
		
		if (event.getCustomId()
				 .equals(DiscordIds.MODAL)) {
			String question = "";
			
			for (TextInput component : event.getComponents(TextInput.class)) {
				if (DiscordIds.QUESTION_AREA.equals(component.getCustomId())) {
					if (component.getValue()
								 .isEmpty()) {
						event.reply("You must provide a question")
							 .subscribe();
						return;
					}
					question = component.getValue()
										.get();
					break;
				}
			}
			
			final String fQuestion = question;
			
			
			Mono
				.just(Database.INSTANCE.addSuggestion(author.getId(), fQuestion))
				.filter(id -> id >= 0)
				.switchIfEmpty(Mono.error(new SQLException("Failed to add suggestion")))
				.flatMap(id -> sendModMessage(author.getId(), fQuestion, id))
				.flatMap(ignored -> event.reply("Submitted question \n>>> " + fQuestion)
										 .withEphemeral(true))
				.onErrorResume(error -> {
					if (!(error instanceof SQLException)) {
						logger.error("", error);
					}
					return event.reply("There was an error submitting your question.")
								.withEphemeral(true);
				})
				.subscribe();
		}
	}
	
	public void onComponent(ComponentInteractionEvent event) {
		
		if (event.getCustomId()
				 .startsWith(DiscordIds.ACCEPT_BUTTON)) {
			
			if (event.getMessage()
					 .isEmpty()) {
				event.reply("Component isn't from a message. " + event.getCustomId())
					 .withEphemeral(true)
					 .subscribe();
				return;
			}
			
			try {
				SuggestionHandler.approveSuggestion(
									 event.getMessage()
										  .get(), Integer.parseInt(event.getCustomId()
																		.substring(DiscordIds.ACCEPT_BUTTON.length()))
								 )
								 .flatMap(ignored -> event.reply("Approved")
														  .withEphemeral(true))
								 .onErrorResume(error -> {
									 if (!(error instanceof SQLException)) {
										 logger.error("", error);
									 }
									 return event.reply("Failed to approve suggestion")
												 .withEphemeral(true);
								 })
								 .subscribe();
			} catch (NumberFormatException e) {
				event.reply("Custom id is invalid: " + event.getCustomId())
					 .withEphemeral(true)
					 .subscribe();
			}
		} else if (event.getCustomId()
						.startsWith(DiscordIds.DENY_BUTTON)) {
			
			if (event.getMessage()
					 .isEmpty()) {
				event.reply("Component isn't from a message. " + event.getCustomId())
					 .withEphemeral(true)
					 .subscribe();
				return;
			}
			
			try {
				SuggestionHandler.rejectSuggestion(
									 event.getMessage()
										  .get(), Integer.parseInt(event.getCustomId()
																		.substring(DiscordIds.DENY_BUTTON.length()))
								 )
								 .flatMap(ignored -> event.reply("Denied")
														  .withEphemeral(true))
								 .onErrorResume(error -> {
									 if (!(error instanceof SQLException)) {
										 logger.error("", error);
									 }
									 return event.reply("Failed to deny suggestion")
												 .withEphemeral(true);
								 })
								 .subscribe();
			} catch (NumberFormatException e) {
				event.reply("Custom id is invalid: " + event.getCustomId())
					 .withEphemeral(true)
					 .subscribe();
			}
		} else if (event.getCustomId()
						.equals(DiscordIds.OPEN_MODAL_BUTTON)) {
			event
				.presentModal(ModalHandler.buildSuggestionModal())
				.subscribe();
		}
	}
}
