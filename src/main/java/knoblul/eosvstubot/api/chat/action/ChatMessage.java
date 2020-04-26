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
	private Document messageDocument;

	public enum MessageType {
		SYSTEM,
		MESSAGE,
		BEEP,
		DIALOGUE,
	}

	private MessageType messageType = MessageType.MESSAGE;

	ChatMessage(JsonObject jsonObject) {
		parse(jsonObject);
	}

	private void parse(JsonObject jsonObject) {
//		systemMessage = jsonObject.has("system") && jsonObject.get("system").getAsString().equals("1");
//		if (jsonObject.has("type")) {
//			systemMessage |= jsonObject.get("type").getAsString().equalsIgnoreCase("system");
//		}

		if (jsonObject.has("system") && jsonObject.get("system").getAsString().equals("1")) {
			messageType = MessageType.SYSTEM;
		} else if (jsonObject.has("type")) {
			messageType = MessageType.valueOf(jsonObject.get("type").getAsString().toUpperCase());
		}

		// нужно для получения верной хешсумы сообщения
		// и правильного сравнения сообщений
		id = jsonObject.get("id").getAsString();

		userId = jsonObject.get("userid").getAsString();

		String messageContent = StringEscapeUtils.unescapeJson(jsonObject.get("message").getAsString());
		messageDocument = Jsoup.parse(messageContent, "");

		Elements elements;
		switch (messageType) {
			case SYSTEM:
				elements = messageDocument.select(".chat-event");
				time = elements.select(".time").text();
				user = "";
				text = elements.select(".event").text();
				break;
			case MESSAGE:
			case BEEP:
			case DIALOGUE:
				elements = messageDocument.select(".chat-message");
				time = elements.select(".chat-message-meta .time").text();
				user = elements.select(".chat-message-meta .user a").text();
				text = elements.select(".text").text();
				break;
		}
	}

	public String getTime() {
		return time;
	}

	public MessageType getMessageType() {
		return messageType;
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

	public Document getMessageDocument() {
		return messageDocument;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ChatMessage message = (ChatMessage) o;
		return Objects.equals(id, message.id) &&
				Objects.equals(time, message.time) &&
				Objects.equals(user, message.user) &&
				Objects.equals(userId, message.userId) &&
				Objects.equals(text, message.text) &&
				messageType == message.messageType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, time, user, userId, text, messageType);
	}
}
