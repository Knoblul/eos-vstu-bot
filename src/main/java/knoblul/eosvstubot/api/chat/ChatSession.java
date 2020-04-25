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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import knoblul.eosvstubot.api.BotContext;
import knoblul.eosvstubot.api.chat.action.ChatAction;
import knoblul.eosvstubot.api.chat.listening.ChatActionListener;
import knoblul.eosvstubot.api.chat.listening.ChatConnectionListener;
import knoblul.eosvstubot.api.profile.Profile;
import knoblul.eosvstubot.gui.chat.ChatComponent;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 24.04.2020 18:13
 * @author Knoblul
 */
public class ChatSession {
	private final BotContext context;
	private final String chatIndexLink;

	private Map<Profile, ChatConnection> connections = Maps.newHashMap();

	private Set<ChatActionListener> chatActionListeners = Sets.newHashSet();
	private Set<ChatConnectionListener> chatConnectionListeners = Sets.newHashSet();

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

	public void addChatActionListener(ChatActionListener listener) {
		chatActionListeners.add(listener);
	}

	public void addChatConnectionListener(ChatConnectionListener listener) {
		chatConnectionListeners.add(listener);
	}

	public void addChatConnectionCompletedListener(Consumer<ChatConnection> listener) {
		chatConnectionListeners.add(new ChatConnectionListener() {
			@Override
			public void completed(ChatConnection connection) {
				listener.accept(connection);
			}

			@Override
			public void failed(ChatConnection connection, Throwable error) {

			}
		});
	}

	void onChatAction(ChatAction action) {
		context.invokeMainThreadCommand(() ->
			chatActionListeners.forEach(listener -> listener.action(action)));
	}

	void onConnectionFailed(ChatConnection connection, Throwable error) {
		context.invokeMainThreadCommand(() ->
				chatConnectionListeners.forEach(listener -> listener.failed(connection, error)));
	}

	void onConnectionCompleted(ChatConnection connection) {
		context.invokeMainThreadCommand(() ->
				chatConnectionListeners.forEach(listener -> listener.completed(connection)));
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
		connection.close();
		connections.values().remove(connection);
	}

	public void close() {
		if (context.mainThread != Thread.currentThread()) {
			throw new IllegalStateException("This function must only be called from main thread");
		}

		connections.values().forEach(ChatConnection::close);
		connections.clear();
	}
}
