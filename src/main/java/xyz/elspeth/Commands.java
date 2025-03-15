package xyz.elspeth;

import discord4j.core.GatewayDiscordClient;
import discord4j.discordjson.json.ApplicationCommandRequest;

public class Commands {
	
	public static void registerCommands(GatewayDiscordClient client) {
		
		ApplicationCommandRequest request = ApplicationCommandRequest
			.builder()
			.name("suggest")
			.description("Suggest question")
			.build();
		
		long applicationId = client
			.getRestClient()
			.getApplicationId()
			.block();
		
		client
			.getRestClient()
			.getApplicationService()
			.createGuildApplicationCommand(applicationId, Constants.BABAMOTES, request)
			.subscribe();
		
	}
	
}
