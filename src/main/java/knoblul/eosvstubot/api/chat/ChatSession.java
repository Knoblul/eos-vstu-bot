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
import com.google.common.collect.Sets;
import knoblul.eosvstubot.api.BotContext;
import knoblul.eosvstubot.api.chat.action.ChatAction;
import knoblul.eosvstubot.api.chat.listening.ChatActionListener;
import knoblul.eosvstubot.api.chat.listening.ChatConnectionListener;
import knoblul.eosvstubot.api.profile.Profile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Класс, который представляет собой чат-сессию.
 * Основное предназначение - управление подключениями к чату.
 * Чтобы создать новое подключение, достаточно вызова {@link #createConnection(Profile)}.
 * <p>Чтобы обрабатывать подключения, например отлавливать ошибки или момент успешного входа в чат,
 * нужно добавить листенер подключений с помощью {@link #addChatConnectionListener(ChatConnectionListener)}.</p>
 * <p>Чтобы обрабатывать чат-события, которые приходят от созданных подключений,
 * нужно добавить листенер событий с помощью {@link #addChatActionListener(ChatActionListener)}.</p>
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 24.04.2020 18:13
 *
 * @author Knoblul
 */
public class ChatSession {
	/**
	 * Контекст бота
	 */
	private final BotContext context;

	/**
	 * Ссылка на index.php чата
	 */
	private final String chatIndexLink;

	/**
	 * Список всех созданных и действительных чат-подключений.
	 * Удаленые чат-подключения будут автоматически чистится из этого
	 * списка.
	 */
	private List<ChatConnection> connections = Lists.newArrayList();

	/**
	 * Список всех листенеров чат-подключений
	 */
	private Set<ChatConnectionListener> chatConnectionListeners = Sets.newHashSet();

	/**
	 * Сприсок всех листенеров чат-событий
	 */
	private Set<ChatActionListener> chatActionListeners = Sets.newHashSet();

	/**
	 * Максимальное количество попыток реконнекта чат-подключений.
	 */
	private int maximumReconnectAttempts;

	/**
	 * Флаг, сигнализирующий контексту о том, что данная чат-сессия
	 * является недействительной и подлежит удалению.
	 */
	private boolean destroyed;

	public ChatSession(@NotNull BotContext context, @NotNull String chatIndexLink) {
		this.context = context;
		this.chatIndexLink = chatIndexLink;
		setMaximumReconnectAttempts(3);
	}

	public BotContext getContext() {
		return context;
	}

	public String getChatIndexLink() {
		return chatIndexLink;
	}

	public int getMaximumReconnectAttempts() {
		return maximumReconnectAttempts;
	}

	public void setMaximumReconnectAttempts(int maximumReconnectAttempts) {
		this.maximumReconnectAttempts = maximumReconnectAttempts;
	}

	/**
	 * Обновляет чат-сессию, обновляя все созданные чат-подключения.
	 *
	 * @return true, если {@link #destroyed} == <code>true</code>
	 */
	public boolean update() {
		context.requireMainThread();

		// обновляем все подключения, удаляем те что недействительны
		connections.removeIf(chatConnection -> !chatConnection.update());
		return destroyed;
	}

	/**
	 * Добавляет листенер чат-подключений в список всех листенеров чат-подключений
	 *
	 * @param listener листенер чат-подключений
	 */
	public void addChatConnectionListener(@NotNull ChatConnectionListener listener) {
		chatConnectionListeners.add(listener);
	}

	/**
	 * Добавляет листенер <b>успешных</b> чат-подключений в список всех листенеров чат-подключений
	 *
	 * @param listener коллбек вызова {@link ChatConnectionListener#connected(ChatConnection)}
	 */
	public void addChatConnectionCompletedListener(@NotNull Consumer<ChatConnection> listener) {
		chatConnectionListeners.add(new ChatConnectionListener() {
			@Override
			public void connected(ChatConnection connection) {
				listener.accept(connection);
			}

			@Override
			public void error(ChatConnection connection, Throwable error) {

			}
		});
	}

	/**
	 * Добавляет листенер чат-событий в список всех листенеров чат-событий
	 *
	 * @param listener листенер чат-событий
	 */
	public void addChatActionListener(@NotNull ChatActionListener listener) {
		chatActionListeners.add(listener);
	}

	/**
	 * Вызывается из BotConnection при исключении, возникшем на момент попытки входа
	 * в чат или на момент обработки чат-подключения.
	 *
	 * @param connection чат-подключение, из которого вызвался метод
	 * @param error      исключение, произошедшее внутри чат-подключения
	 */
	void onConnectionError(ChatConnection connection, Throwable error) {
		context.invokeMainThreadCommand(() ->
				chatConnectionListeners.forEach(listener -> listener.error(connection, error)));
	}

	/**
	 * Вызывается из BotConnection при успешном входе в чат.
	 *
	 * @param connection чат-подключение, из которого вызвался метод
	 */
	void onConnectionCompleted(ChatConnection connection) {
		context.invokeMainThreadCommand(() ->
				chatConnectionListeners.forEach(listener -> listener.connected(connection)));
	}

	/**
	 * Вызывается из BotConnection при поступлении новго чат-события
	 *
	 * @param connection чат-подключение, из которого вызвался метод
	 * @param action новое чат-событие
	 */
	void onChatAction(ChatConnection connection, ChatAction action) {
		context.invokeMainThreadCommand(() ->
				chatActionListeners.forEach(listener -> listener.action(connection, action)));
	}

	/**
	 * Если чат-сессия уже хранит чат-подключение, которое представляется указанным профилем,
	 * то возвращает созданное ранее чат-подключение из списка.
	 * Иначе создает новое чат-подключение, которое сразу же пытается выполнить
	 * вход в чат.
	 *
	 * @param profile профиль, которым будет представляться
	 *                новосозданное подключение
	 * @return новосозданное/существующее подключение, которое
	 * представляется указанным профилем.
	 */
	public ChatConnection createConnection(@NotNull Profile profile) {
		context.requireMainThread();

		for (ChatConnection chatConnection : connections) {
			if (chatConnection.getProfile() == profile) {
				throw new IllegalArgumentException(String.format("Connection to chat from profile '%s' already exists!",
						profile.toString()));
			}
		}

		ChatConnection chatConnection = new ChatConnection(this, profile);
		connections.add(chatConnection);
		return chatConnection;
	}

	/**
	 * Удаляет данную чат-сессию, закрывая все подключения
	 * и высвобождая ресурсы.
	 */
	public void destroy() {
		destroyed = true;
		context.requireMainThread();
		connections.forEach(ChatConnection::destroy);
		connections.clear();
	}
}
