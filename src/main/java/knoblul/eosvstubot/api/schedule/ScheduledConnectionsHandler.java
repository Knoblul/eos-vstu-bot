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
package knoblul.eosvstubot.api.schedule;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import knoblul.eosvstubot.api.BotConstants;
import knoblul.eosvstubot.api.BotContext;
import knoblul.eosvstubot.api.BotHandler;
import knoblul.eosvstubot.api.chat.ChatConnection;
import knoblul.eosvstubot.api.chat.ChatSession;
import knoblul.eosvstubot.api.profile.Profile;
import knoblul.eosvstubot.api.profile.ProfileManager;
import knoblul.eosvstubot.api.scripting.Script;
import knoblul.eosvstubot.utils.Log;
import knoblul.eosvstubot.utils.swing.DialogUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.function.Consumer;

/**
 * Написанный мной на коленке менеджер подключений по расписанию.
 * Основное назначение - подключает профили по
 * расписанию и отправляет от их имени "привественные" сообщения.
 * Состояние этого менеджера сериализуется, это нужно за тем,
 * чтобы избежать такие моменты как:
 * "выключил программу/выключили интернет когда профили были подключены к чату,
 * потом включил/включили и они опять написали свои фразы"
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 25.04.2020 12:15
 *
 * @author Knoblul
 */
public class ScheduledConnectionsHandler implements BotHandler {
	private final BotContext context;
	private final Path chatFile;

	private List<ScheduledConnection> scheduledConnections = Lists.newArrayList();
	private ChatSession currentChatSession;
	private Consumer<ChatSession> onSessionChangedCallback;

	@SuppressWarnings("unused")
	public ScheduledConnectionsHandler(BotContext context) {
		this.context = context;
		this.chatFile = Paths.get("chat.json");
		load();
	}

	private static void message(String msg, Object... args) {
		Log.info("[ScheduledConnections] " + msg, args);
	}

	private void load() {
		if (Files.exists(chatFile)) {
			try (BufferedReader reader = Files.newBufferedReader(chatFile)) {
				JsonArray array = BotContext.GSON.fromJson(reader, JsonArray.class);
				if (array != null) {
					for (JsonElement element : array) {
						ScheduledConnection acc = BotContext.GSON.fromJson(element, ScheduledConnection.class);
						if (acc != null) {
							acc.setHandler(this);
							scheduledConnections.add(acc);
							message("Loaded scheduled connection for %s. Planned time: %s", acc.username,
									new SimpleDateFormat("dd.MM.YYYY HH:mm:ss").format(acc.scheduledJoinTime));
						}
					}
				}
			} catch (IOException | JsonParseException e) {
				Log.warn(e, "Failed to load %s", chatFile);
			}
		}
	}

	private void save() {
		try (BufferedWriter writer = Files.newBufferedWriter(chatFile)) {
			JsonArray array = new JsonArray();
			scheduledConnections.forEach(acc -> array.add(BotContext.GSON.toJsonTree(acc)));
			BotContext.GSON.toJson(array, writer);
		} catch (IOException | JsonParseException e) {
			Log.warn(e, "Failed to save %s", chatFile);
		}
	}

	public BotContext getContext() {
		return context;
	}

	public void setOnSessionChangedCallback(Consumer<ChatSession> onSessionChangedCallback) {
		this.onSessionChangedCallback = onSessionChangedCallback;
	}

	@NotNull
	private ScheduledConnectionsHandler.ScheduledConnection createAutomaticConnection(@NotNull Lesson currentLesson,
																					  @NotNull Profile profile) {
		ScheduledConnection acc = new ScheduledConnection();
		acc.username = profile.getUsername();
		acc.profile = profile;
		acc.chatLink = currentChatSession.getChatIndexLink();
		long lessonStartTime = currentLesson.getRelativeCalendar().getTimeInMillis();
		acc.scheduledJoinTime = lessonStartTime + RandomUtils.nextLong(0, profile.getMaximumLateTime() + 1);
		acc.setHandler(this);
		message("Created scheduled connection for %s. Planned time: %s", profile.getUsername(),
				new SimpleDateFormat("dd.MM.YYYY HH:mm:ss").format(acc.scheduledJoinTime));
		save();
		return acc;
	}

	private void setLesson(Lesson lesson) {
		if (lesson == null) {
			if (currentChatSession != null) {
				currentChatSession.destroy();
			}

			if (!scheduledConnections.isEmpty()) {
				scheduledConnections.forEach(ScheduledConnection::destroy);
				scheduledConnections.clear();
				save();
			}

			if (currentChatSession != null) {
				if (onSessionChangedCallback != null) {
					onSessionChangedCallback.accept(null);
				}
				currentChatSession = null;
			}
			return;
		}

		String chatLink = "http://" + BotConstants.SITE_DOMAIN + "/mod/chat/gui_ajax/index.php?id="
				+ lesson.getChatId();
		if (currentChatSession == null || !currentChatSession.getChatIndexLink().equals(chatLink)) {
			if (currentChatSession != null && !currentChatSession.getChatIndexLink().equals(chatLink)) {
				scheduledConnections.forEach(ScheduledConnection::destroy);
				scheduledConnections.clear();
				save();
			}

			if (currentChatSession != null) {
				currentChatSession.destroy();
			}
			currentChatSession = context.createChatSession(chatLink);
			currentChatSession.setMessageSendingDisabled(lesson.isSilentMode());
			currentChatSession.addChatActionListener((connection, action) -> {
				for (ScheduledConnection sc : scheduledConnections) {
					if (sc.connection == connection) {
						// выполняем onChatAction на чат-скрипте пользователя
						Profile profile = sc.profile;
						Script script = profile.getChatScript();
						try {
							script.invokeFunction("onChatAction", connection, action);
						} catch (NoSuchMethodException ignored) {
							// noop
						} catch (Throwable t) {
							t.printStackTrace();
						}
					}
				}
			});
			currentChatSession.addChatConnectionCompletedListener(connection -> {
				for (ScheduledConnection sc : scheduledConnections) {
					if (sc.connection == connection && !sc.scriptExecuted) {
						sc.scriptExecuted = true;
						// выполняем onConnected на чат-скрипте пользователя
						Profile profile = sc.profile;
						Script script = profile.getChatScript();
						try {
							script.clearBindings();
							script.putBinding("_context", context);
							script.putBinding("_chatConnection", connection);
							script.recompile();
							try {
								script.invokeFunction("onConnected", connection);
							} catch (Throwable t) {
								DialogUtils.showError("Не могу выполнить скрипт-метод onConnected у "
										+ profile, t, true);
							}
						} catch (Throwable t) {
							DialogUtils.showError("Не могу выполнить скрипт у " + profile,
									t, true);
						}
						save();
					}
				}
			});

			if (onSessionChangedCallback != null) {
				onSessionChangedCallback.accept(currentChatSession);
			}
		}
	}

	@Override
	public void update() {
		ProfileManager profileManager = context.getProfileManager();
		LessonsManager lessonsManager = context.getLessonsManager();
		Lesson currentLesson = lessonsManager.getCurrentLesson();
		if (currentLesson != null) {
			setLesson(currentLesson);
			List<Profile> profiles = profileManager.getProfiles();
			for (Profile profile : profiles) {
				boolean found = false;
				for (ScheduledConnection sc : scheduledConnections) {
					if (sc.username.equals(profile.getUsername())) {
						found = true;
						break;
					}
				}

				if (!found) {
					scheduledConnections.add(createAutomaticConnection(currentLesson, profile));
					save();
				}
			}

			if (scheduledConnections.removeIf(ScheduledConnection::update)) {
				save();
			}
		} else {
			setLesson(null);
		}
	}

	@Override
	public void reconnect() {
		// грубо обнуляем инстансы подключений, чтобы реконнектнуть всех ботов
		scheduledConnections.forEach(sc -> sc.connection = null);
	}

	public List<ScheduledConnection> getScheduledConnections() {
		return scheduledConnections;
	}

	/**
	 * Экземпляр подключения по расписанию.
	 * Хранит экземпляр самого чат-подключения,
	 * профиль и сериализуемые переменные.
	 */
	public static class ScheduledConnection {
		private transient ScheduledConnectionsHandler handler;
		private transient ChatConnection connection;

		private String username = "";
		private transient Profile profile;
		private long scheduledJoinTime;
		private String chatLink = "";
		private boolean scriptExecuted = false;

		public void setHandler(ScheduledConnectionsHandler handler) {
			this.handler = handler;
		}

		private void connect() {
			message("Connecting %s...", username);
			connection = handler.currentChatSession.createConnection(profile);
		}

		private boolean update() {
			if (handler == null) {
				return true;
			}

			BotContext context = handler.context;
			ChatSession session = handler.currentChatSession;

			profile = context.getProfileManager().getProfile(username);
			if (profile == null || chatLink.isEmpty() || (session != null &&
					!session.getChatIndexLink().equals(chatLink))) {
				destroy();
				return true;
			}

			if (connection == null && System.currentTimeMillis() > scheduledJoinTime
					&& session != null && profile.isValid()) {
				connect();
			}

			return false;
		}

		private void destroy() {
			message("Removed scheduled connection for %s", username);

			handler = null;
			profile = null;
			username = "";
			chatLink = "";
			scheduledJoinTime = 0;
			if (connection != null) {
				connection.destroy();
				connection = null;
			}
		}

		public ChatConnection getConnection() {
			return connection;
		}
	}
}
