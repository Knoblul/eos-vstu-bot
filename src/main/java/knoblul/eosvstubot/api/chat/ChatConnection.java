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

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import knoblul.eosvstubot.api.BotContext;
import knoblul.eosvstubot.api.chat.action.ChatAction;
import knoblul.eosvstubot.api.chat.action.ChatUserInformation;
import knoblul.eosvstubot.api.profile.Profile;
import knoblul.eosvstubot.utils.HttpCallbacks;
import knoblul.eosvstubot.utils.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 24.04.2020 18:13
 * @author Knoblul
 */
public class ChatConnection {
	/**
	 * Среднее значение, после которого подключение
	 * считается "разрованным".
	 */
	private static final int CONNECTION_RESET_TIME = 15000;

	/**
	 * Чат-сессия, к которой принадлежит данное чат-подключение.
	 */
	private final ChatSession chatSession;

	/**
	 * Профиль, от "имени" которого "представляется" данное чат-подключение.
	 */
	private final Profile profile;

	private ChatConnectionConfiguration configuration;

	/**
	 * Этот флаг принимает значение <code>true</code> тогда,
	 * когда чат-подключение было удалено, либо произошло необратимое исключение.
	 */
	private volatile boolean invalid;

	/**
	 * Этот флаг принимает значение <code>true</code> тогда, когда
	 * вход в чат прошел успешен и конфигурация чата получена.
	 * Принимает значение <code>false</code> тогда, когда
	 * произошло необратимое/обратимое исключение.
	 */
	private volatile boolean configurationCompleted;

	/**
	 * Количество попыток переподключится к чату.
	 */
	private AtomicInteger reconnectAttempts = new AtomicInteger();

	/**
	 * Последнее время отправки ping-запроса к ajax-скрипту.
	 */
	private long lastPingTime;

	/**
	 * Последнее время принятия ответа на ping-запрос к ajax-скрипту.
	 */
	private long lastPongTime;

	private String chatLastTime = "";
	private String chatLastRow = "0";

	/**
	 * Хранит результаты выполнения асинхронных http-запросов
	 */
	private Set<Future<HttpResponse>> requestFutures = Sets.newHashSet();

	ChatConnection(ChatSession chatSession, Profile profile) {
		this.chatSession = chatSession;
		this.profile = profile;
		this.configuration = new ChatConnectionConfiguration();
		connect();
	}

	public ChatConnectionConfiguration getConfiguration() {
		return configuration;
	}

	private void onErrorCaused(Throwable t) {
		if (t instanceof CancellationException) {
			return;
		}

		if (!configurationCompleted && reconnectAttempts.get() < chatSession.getMaximumReconnectAttempts()) {
			Log.error("%s connection failed: %s. Reconnecting... (attempt %d/%d)",
					profile, t.getCause() != null ? t.getCause().toString() : t.toString(),
					reconnectAttempts.get()+1, chatSession.getMaximumReconnectAttempts());
			chatSession.getContext().invokeMainThreadCommand(this::reconnect);
			return;
		}

		invalid = true;
		Log.error(t, "Connection error");
		chatSession.onConnectionError(this, t);
	}

	/**
	 * Обрабатывает ответ от ajax-скрипта чата, независимо от того
	 * какой запрос на скрипт был отправлен.
	 * @param json ответ от скрипта в виде json-документа
	 * @return <code>true</code> если не произошло никаких
	 * ошибок при обработке запроса
	 */
	private boolean processAjaxResponse(JsonElement json) {
		lastPongTime = System.currentTimeMillis();

		if (json == null || !json.isJsonObject()) {
			onErrorCaused(new IOException("Wrong response"));
			return false;
		}

		JsonObject jsonObject = json.getAsJsonObject();
		if (jsonObject.has("error")) {
			onErrorCaused(new IOException("Response error: (" + jsonObject.get("errorcode").getAsString() + ") "
					+ jsonObject.get("error").getAsString()));
			return false;
		}

		// два не совсем понятных мне значения, которые нужно отправлять
		// в запросе после получения от сервера ответа на init или update
		chatLastTime = jsonObject.has("lasttime") ? jsonObject.get("lasttime").getAsString() : "";
		chatLastRow = jsonObject.has("lastrow") ? jsonObject.get("lastrow").getAsString() : "0";

		// если ответ содержит users или msgs, то парсим их
		// и отправляем на листенеры в виде ChatAction
		if (jsonObject.has("users") || jsonObject.has("msgs")) {
			ChatAction action = new ChatAction(jsonObject);

			// "помечаем" наших ботов
			if (action.getUsers() != null) {
				for (ChatUserInformation user: action.getUsers()) {
					if (user.getId().equals(profile.getProfileId())) {
						user.setIsBot(true);
					}
				}
			}

			chatSession.onChatAction(this, action);
		}

		return true;
	}

	/**
	 * Завершает процесс подключения, отмечая
	 * данное чат-подключение настроенным и готовым.
	 */
	private void completeConnection() {
		configurationCompleted = true;
		chatSession.onConnectionCompleted(this);
		Log.info("%s connected to chat '%s'", profile, configuration.getTitle());
		reconnectAttempts.set(0);
	}

	/**
	 * Обрабатывает ответ от сервера на запрос о подключении к чату.
	 * Парсит ответ, и отправляет еще один запрос, но уже на ajax-скрипт чата,
	 * в параметрах этого запроса передается action=init.
	 * При положительном овтете от сервера вызывается метод {@link #completeConnection()}
	 * @param page html-страница с ответом, содержащая критические данные о
	 *             конфигурации чата.
	 */
	private void doConfiguration(Document page) {
		try {
			// парсим конфигурацию из ответа на запрос к index.php чата
			configuration.parse(page, chatSession.getChatIndexLink());
		} catch (IOException e) {
			onErrorCaused(new IOException("Failed to configure chat connection", e));
			return;
		}

		// отправляем пост-запрос на ajax-скрипт чата с параметром action=init
		BotContext context = chatSession.getContext();
		context.invokeMainThreadCommand(() -> {
			Map<String, String> params = Maps.newHashMap();
			params.put("action", "init");
			params.put("chat_init", "1");
			params.put("chat_sid", configuration.getSessionId());
			params.put("theme", configuration.getTheme());
			HttpUriRequest request = context.buildPostRequest(configuration.getChatModuleLink(), params);
			// выбираем нужный профиль перед отпракой запроса
			context.getProfileManager().selectProfile(profile);
			requestFutures.add(context.executeRequestAsync(request, JsonElement.class, HttpCallbacks.onEither(json -> {
				if (processAjaxResponse(json)) {
					completeConnection();
				}
			}, this::onErrorCaused)));
		});
	}

	/**
	 * Начинает процесс подключения к чату, отправляя запрос на index.php чата.
	 * Ответ от сервера обрабатывается в {@link #doConfiguration(Document)}
	 */
	private void connect() {
		BotContext context = chatSession.getContext();

		// отправляем асинхронный запрос на главную страницу чата
		// чтобы получить настройки и ключевые данные для "входа"

		HttpUriRequest request = context.buildGetRequest(chatSession.getChatIndexLink(), null);

		// логинем + выбираем нужный профиль перед отпракой запроса на вход
		try {
			context.getProfileManager().loginProfile(profile);
			requestFutures.add(context.executeRequestAsync(request, Document.class,
					HttpCallbacks.onEither(this::doConfiguration, this::onErrorCaused)));
		} catch (IOException e) {
			onErrorCaused(new IOException("Failed to login", e));
		}
	}

	/**
	 * Отмечает это чат-подключение ненастроенным и
	 * пытается переподключится к чату.
	 */
	private void reconnect() {
		cancelHttpRequests();
		configurationCompleted = false;
		reconnectAttempts.getAndIncrement();
		connect();
	}

	/**
	 * Метод для отправки асинхронного пинг-запроса на ajax-скрипт чата.
	 * Ответ от сервера обрабатывается в {@link #processAjaxResponse(JsonElement)}
	 */
	private void ping() {
		lastPingTime = System.currentTimeMillis();

		BotContext context = chatSession.getContext();

		// отправляем асинхронный запрос на ajax-скрипт чата
		Map<String, String> params = Maps.newHashMap();
		params.put("action", "update");
		params.put("chat_lastrow", chatLastRow);
		params.put("chat_lasttime", chatLastTime);
		params.put("chat_sid", configuration.getSessionId());
		params.put("theme", configuration.getTheme());
		HttpUriRequest request = context.buildPostRequest(configuration.getChatModuleLink(), params);

		// выбираем нужный профиль перед отпракой запроса
		context.getProfileManager().selectProfile(profile);
		requestFutures.add(context.executeRequestAsync(request, JsonElement.class,
				HttpCallbacks.onEither(this::processAjaxResponse, this::onErrorCaused)));
	}

	/**
	 * Обновляет логику чат-подключения.
	 * После успешной конфигурации отправляет пинг-запрос на
	 * ajax-скрипт чата, чтобы получить обновления от сервера.
	 * Если ответ не был получен более чем {@link #CONNECTION_RESET_TIME},
	 * то пытается переподключится к чату.
	 * @return <code>true</code>, если произошло необратимое исключение
	 * или это чат-подключение следует удалить.
	 */
	boolean update() {
		if (invalid) {
			destroy();
			return false;
		}

		// если конфигурация прошла успешно, то пингуем скрипт
		// каждые N миллисекунд (указано в конфигурации чата)
		if (configurationCompleted) {
			if (System.currentTimeMillis() - lastPongTime > CONNECTION_RESET_TIME) {
				Log.error("%s connection reset. Reconnecting... (attempt %d/%d)", profile,
						reconnectAttempts.get()+1, chatSession.getMaximumReconnectAttempts());
				reconnect();
				return true;
			}

			long time = System.currentTimeMillis();
			if (time > lastPingTime + configuration.getPingPeriod()) {
				ping();
			}
		}

		// удаляем завершеные запросы
		requestFutures.removeIf(Future::isDone);
		return true;
	}

	private void processSendMessageResponse(@NotNull String string) {
		if (!string.equalsIgnoreCase("true")) {
			Log.warn("Chat message not sended: %s", string);
		}
	}

	/**
	 * Отправляет сообщение в чат от данного подключения
	 * @param message сообщение
	 */
	public void sendMessage(@Nullable String message) {
		if (Strings.isNullOrEmpty(message)) {
			return;
		}

		if (invalid || !configurationCompleted) {
			Log.warn("Could not send chat message as %s, because connection is not configured yet",
					profile.getUsername());
			return;
		}

		Log.info("%s sended message '%s' to chat", profile, message);

		// пропускаем отправку сообщения если чат-сессия
		// запрещает отправлять сообщения в чат,
		if (chatSession.isMessageSendingDisabled()) {
			return;
		}

		BotContext context = chatSession.getContext();
		context.invokeMainThreadCommand(() -> {
			// отправляем асинхронный запрос на ajax-скрипт чата
			Map<String, String> params = Maps.newHashMap();
			params.put("action", "chat");
			params.put("chat_message", message);
			params.put("chat_sid", configuration.getSessionId());
			params.put("theme", configuration.getTheme());
			HttpUriRequest request = context.buildPostRequest(configuration.getChatModuleLink(), params);

			// выбираем нужный профиль перед отпракой запроса
			context.getProfileManager().selectProfile(profile);
			requestFutures.add(context.executeRequestAsync(request, String.class,
					HttpCallbacks.onEither(this::processSendMessageResponse, this::onErrorCaused)));

			Log.info("%s sended message '%s' to chat", profile, message);
		});
	}

	private void cancelHttpRequests() {
		chatSession.getContext().invokeMainThreadCommand(() -> {
			for (Future<HttpResponse> future: requestFutures) {
				if (!future.isDone() && !future.isCancelled()) {
					future.cancel(true);
				}
			}
			requestFutures.clear();
		});
	}

	/**
	 * Удаляет данное подключение, отменяя все незавершенные
	 * http-запросы и освобождая ресурсы.
	 */
	public void destroy() {
		if (!invalid) {
			Log.info("%s connection to chat '%s' closed", profile, configuration.getTitle());
		}

		// отмечаем подключение недействительным и ненастроенным
		invalid = true;
		configurationCompleted = false;
		// отменяем все отправленные и ожидающие http-запросы
		cancelHttpRequests();
	}

	public Profile getProfile() {
		return profile;
	}

	public boolean isInvalid() {
		return invalid;
	}
}
