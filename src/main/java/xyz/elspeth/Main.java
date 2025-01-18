package xyz.elspeth;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import xyz.elspeth.handlers.QuestionHandler;
import xyz.elspeth.schedule.Scheduler;

import java.io.IOException;

public class Main {
	
	public static GatewayDiscordClient gateway;
	
	public static void main(String[] args) {
		
		try {
			Constants.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		System.out.println(Constants.TOKEN);
		
		System.exit(1);
		
		DiscordClient client = DiscordClient.create(Constants.TOKEN);
		
		GatewayDiscordClient gateway = client.gateway()
											 .setEnabledIntents(
												 IntentSet.of(
													 Intent.GUILD_MESSAGES
												 )
											 )
											 .login()
											 .block();
		
		if (gateway == null) {
			// Never happens
			throw new RuntimeException("Discord client not connected");
		}
		
		Main.gateway = gateway;
		
		Listener listener = new Listener();
		
		Commands.registerCommands(gateway);
		
		gateway.on(ApplicationCommandInteractionEvent.class)
			   .subscribe(listener :: onCommand);
		gateway.on(ModalSubmitInteractionEvent.class)
			   .subscribe(listener :: onModal);
		gateway.on(ComponentInteractionEvent.class)
			   .subscribe(listener :: onComponent);
		
		Scheduler.schedule(QuestionHandler.postNextQuestion(gateway));
		//		Scheduler.schedule(() -> System.out.println("Scheduler working."));
		
		gateway.onDisconnect()
			   .block();
	}
	
}
