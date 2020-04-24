/*
 * Copyright 2020 Knoblul
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package knoblul.eosvstubot.api.chat;

import com.google.common.collect.Maps;
import knoblul.eosvstubot.api.BotContext;
import knoblul.eosvstubot.api.profile.Profile;

import java.util.Map;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 24.04.2020 18:13
 * @author Knoblul
 */
public class ChatSession {
	private final BotContext context;
	private final String chatIndexLink;

	private Map<Profile, ChatConnection> connections = Maps.newHashMap();

	public ChatSession(BotContext context, String chatIndexLink) {
		this.context = context;
		this.chatIndexLink = chatIndexLink;
	}

	public BotContext getContext() {
		return context;
	}

	public String getChatIndexLink() {
		return chatIndexLink;
	}

	public void update() {
		if (context.mainThread != Thread.currentThread()) {
			throw new IllegalStateException("This function must only be called from main thread");
		}

		connections.values().removeIf(chatConnection -> !chatConnection.update());
	}

	public ChatConnection createConnection(Profile profile) {
		if (context.mainThread != Thread.currentThread()) {
			throw new IllegalStateException("This function must only be called from main thread");
		}

		return connections.computeIfAbsent(profile, (k) -> new ChatConnection(this, profile));
	}

	public void destroyConnection(ChatConnection connection) {
		if (context.mainThread != Thread.currentThread()) {
			throw new IllegalStateException("This function must only be called from main thread");
		}

		connections.values().remove(connection);
	}

	public void close() {
		if (context.mainThread != Thread.currentThread()) {
			throw new IllegalStateException("This function must only be called from main thread");
		}

		connections.clear();
	}
}
