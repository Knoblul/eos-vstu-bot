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
package knoblul.eosvstubot.api.chat.action;

import com.google.gson.JsonObject;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.Objects;

/**
 * Содержит данные о сообщении, которое пришло в обновлении
 * от сервера.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 25.04.2020 17:14
 * @author Knoblul
 */
public class ChatMessage {
	private String id;
	private String time;
	private String user;
	private String userId;

	private String text;
	private boolean systemMessage;

	ChatMessage(JsonObject jsonObject) {
		parse(jsonObject);
	}

	private void parse(JsonObject jsonObject) {
		systemMessage = jsonObject.has("system") && jsonObject.get("system").getAsString().equals("1");
		if (jsonObject.has("type")) {
			systemMessage |= jsonObject.get("type").getAsString().equalsIgnoreCase("system");
		}

		// нужно для получения верной хешсумы сообщения
		// и правильного сравнения сообщений
		id = jsonObject.get("id").getAsString();

		userId = jsonObject.get("userid").getAsString();

		String messageContent = StringEscapeUtils.unescapeJson(jsonObject.get("message").getAsString());
		Document messageDocument = Jsoup.parse(messageContent, "");

		if (systemMessage) {
			Elements chatEventElements = messageDocument.select(".chat-event");
			user = "";
			text = chatEventElements.select(".event").text();
			time = chatEventElements.select(".time").text();
		} else {
			Elements chatMessageElements = messageDocument.select(".chat-message");
			user = chatMessageElements.select(".user a").text();
			text = chatMessageElements.select(".text").text();
			time = chatMessageElements.select(".time").text();
		}
	}

	public String getTime() {
		return time;
	}

	public boolean isSystemMessage() {
		return systemMessage;
	}

	public String getUser() {
		return user;
	}

	public String getUserId() {
		return userId;
	}

	public String getText() {
		return text;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ChatMessage message = (ChatMessage) o;
		return systemMessage == message.systemMessage &&
				user.equals(message.user) &&
				userId.equals(message.userId) &&
				text.equals(message.text) &&
				time.equals(message.time) &&
				id.equals(message.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(time, user, userId, text, systemMessage);
	}
}
