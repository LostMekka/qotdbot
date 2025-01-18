package xyz.elspeth.handlers;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;
import discord4j.core.spec.InteractionPresentModalSpec;
import xyz.elspeth.DiscordIds;

public class ModalHandler {
	
	public static InteractionPresentModalSpec buildSuggestionModal() {
		
		return InteractionPresentModalSpec
			.builder()
			.title("Suggest a question")
			.customId(DiscordIds.MODAL)
			.addComponent(
				ActionRow.of(
					TextInput.paragraph("question", "Question", 0, 255)
							 .required()
				)
			)
			.build();
	}
	
}
