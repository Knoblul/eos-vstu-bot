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

	private final ChatSession chatSession;
	private final Profile profile;
	private ChatConnectionConfiguration configuration;

	private volatile boolean invalid;
	private volatile boolean configurationCompleted;
	private AtomicInteger reconnectAttempts = new AtomicInteger();

	private long lastPingTime;
	private long lastPongTime;

	private String chatLastTime = "";
	private String chatLastRow = "0";

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
					profile.getAlias(), t.getCause() != null ? t.getCause().toString() : t.toString(),
					reconnectAttempts.get()+1, chatSession.getMaximumReconnectAttempts());
			chatSession.getContext().invokeMainThreadCommand(this::reconnect);
			return;
		}

		invalid = true;
		Log.error(t, "Connection error");
		chatSession.onConnectionFailed(this, t);
	}

	private boolean processUpdateResponse(JsonElement json) {
		lastPongTime = System.currentTimeMillis();

		if (json == null || !json.isJsonObject()) {
			onErrorCaused(new IOException("Wrong response"));
			return false;
		}

		JsonObject jsonObject = json.getAsJsonObject();
		if (jsonObject.has("error")) {
			onErrorCaused(new IOException("Response error: (" + jsonObject.get("errorcode").getAsJsonObject() + ") "
					+ jsonObject.get("error").getAsString()));
			return false;
		}

		chatLastTime = jsonObject.has("lasttime") ? jsonObject.get("lasttime").getAsString() : "";
		chatLastRow = jsonObject.has("lastrow") ? jsonObject.get("lastrow").getAsString() : "0";

		if (jsonObject.has("users") || jsonObject.has("msgs")) {
			ChatAction action = new ChatAction(jsonObject);

			// отмечаем ботов
			if (action.getUsers() != null) {
				for (ChatUserInformation user: action.getUsers()) {
					if (user.getId().equals(profile.getProfileId())) {
						user.setIsBot(true);
					}
				}
			}

			chatSession.onChatAction(action);
		}

		return true;
	}

	private void doConfiguration(Document page) {
		try {
			// парсим конфигурацию из ответа на запрос главной чата
			configuration.parse(page, chatSession.getChatIndexLink());
		} catch (IOException e) {
			onErrorCaused(new IOException("Failed to configure chat connection", e));
			return;
		}

		// отправляем пост-запрос на ajax-скрипт чата с параметром action=init
		// когда он придет, вызвается метод #doInit
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
				if (processUpdateResponse(json)) {
					completeConnection();
				}
			}, this::onErrorCaused)));
		});
	}

	private void completeConnection() {
		configurationCompleted = true;
		chatSession.onConnectionCompleted(this);
		Log.info("%s connected to chat '%s'", profile.getAlias(), configuration.getTitle());
		reconnectAttempts.set(0);
	}

	private void connect() {
		BotContext context = chatSession.getContext();

		// отправляем асинхронный запрос на главную страницу чата
		// чтобы получить настройки и ключевые данные для "входа"

		HttpUriRequest request = context.buildGetRequest(chatSession.getChatIndexLink(), null);

		// выбираем нужный профиль перед отпракой запроса
		context.getProfileManager().selectProfile(profile);
		requestFutures.add(context.executeRequestAsync(request, Document.class,
				HttpCallbacks.onEither(this::doConfiguration, this::onErrorCaused)));
	}

	private void reconnect() {
		cancelRequests();
		configurationCompleted = false;
		reconnectAttempts.getAndIncrement();
		connect();
	}

	private void ping() {
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
				HttpCallbacks.onEither(this::processUpdateResponse, this::onErrorCaused)));

		lastPingTime = System.currentTimeMillis();
	}

	boolean update() {
		if (invalid) {
			close();
			return false;
		}

		// если конфигурация прошла успешно, то пингуем скрипт
		// каждые N миллисекунд (указано в конфигурации чата)
		if (configurationCompleted) {
			if (System.currentTimeMillis() - lastPongTime > CONNECTION_RESET_TIME) {
				Log.error("%s connection reset. Reconnecting... (attempt %d/%d)", profile.getAlias(),
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

	public void sendMessage(String message) {
		BotContext context = chatSession.getContext();
		context.invokeMainThreadCommand(() -> {
			if (Strings.isNullOrEmpty(message)) {
				return;
			}

			if (invalid || !configurationCompleted) {
				Log.warn("Could not send chat message as %s, because connection is not configured yet",
						profile.getUsername());
				return;
			}

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

			Log.info("%s sended message '%s' to chat", profile.getAlias(), message);
		});
	}

	private void cancelRequests() {
		chatSession.getContext().invokeMainThreadCommand(() -> {
			for (Future<HttpResponse> future: requestFutures) {
				if (!future.isDone() && !future.isCancelled()) {
					future.cancel(true);
				}
			}
			requestFutures.clear();
		});
	}

	public void close() {
		Log.info("%s connection to chat '%s' closed", profile.getAlias(), configuration.getTitle());

		// запрещаем отправку сообщений после закрытия подключения
		invalid = true;
		configurationCompleted = false;
		cancelRequests();
	}

	public Profile getProfile() {
		return profile;
	}

	public boolean isInvalid() {
		return invalid;
	}
}
