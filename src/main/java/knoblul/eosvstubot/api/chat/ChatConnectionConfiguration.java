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

import com.google.gson.JsonObject;
import knoblul.eosvstubot.api.BotContext;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Вспомогательный класс - конфигурация чат-подключения.
 * Основная задача - хранит все данные, необходимые для
 * отправки чат-подключением нужных запростов.
 * Так же хранит имя чата.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 24.04.2020 18:34
 * @author Knoblul
 */
public class ChatConnectionConfiguration {
	private static final Pattern chatModuleSettingsPattern = Pattern.compile("(?s)M\\.mod_chat_ajax\\.init\\(Y,(.+?)\\);");
	private static final Pattern apiConfigPattern = Pattern.compile("(?s)M.cfg\\s*=\\s*(.+?);");

	/**
	 * Ссылка на ajax-модуль чата, к которой далее обращаться
	 */
	private String chatModuleLink;

	/**
	 * Имя чат-комнаты
	 */
	private String title; // chatroom_name

	private String sessionId; // sid => chat_sid
	private String theme; // обязательный параметр
	private long pingPeriod;

	ChatConnectionConfiguration() {

	}

	void parse(@NotNull Document chatPage, @NotNull String chatPageLink) throws IOException {
		String scriptsContent = chatPage.select("script").html();

		// парсим json настроек чат-модуля
		Matcher m = chatModuleSettingsPattern.matcher(scriptsContent);
		if (!m.find()) {
			throw new IOException("Invalid response");
		}

		// из настроек moodle чат-модуля достаем имя чата,
		// частоту пингования, айди чат-сессии и тему чата
		// (которая почему-то является обязательным параметром)
		JsonObject json = BotContext.GSON.fromJson(StringEscapeUtils.unescapeJava(m.group(1)), JsonObject.class);
		title = json.get("chatroom_name").getAsString();
		pingPeriod = json.get("timer").getAsInt();
		sessionId = json.get("sid").getAsString();
		theme = json.get("theme").getAsString();

		// парсим json настроек moodle api
		m = apiConfigPattern.matcher(scriptsContent);
		if (!m.find()) {
			throw new IOException("Invalid response");
		}
		json = BotContext.GSON.fromJson(StringEscapeUtils.unescapeJava(m.group(1)), JsonObject.class);
		String sessionKey = json.get("sesskey").getAsString();

		URL url = URI.create(chatPageLink).toURL();
		chatModuleLink = url.getProtocol() + "://" + url.getHost() + "/mod/chat/chat_ajax.php?sesskey=" + sessionKey;
	}

	public String getChatModuleLink() {
		return chatModuleLink;
	}

	public String getTitle() {
		return title;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getTheme() {
		return theme;
	}

	public long getPingPeriod() {
		return pingPeriod;
	}
}
