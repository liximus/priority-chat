package com.listraind.prioritychat;

import net.fabricmc.api.ClientModInitializer;

public class PriorityChatClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PriorityChatCommands.register();
		PriorityChatConfig.getInstance();
	}
}