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

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import knoblul.eosvstubot.api.BotContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 25.04.2020 17:04
 * @author Knoblul
 */
public class ChatAction {
	private List<ChatMessage> messages = Lists.newArrayList();
	private List<ChatUserInformation> users;

	public ChatAction(@NotNull JsonObject jsonObject) {
		parse(jsonObject);
	}

	private void parse(@NotNull JsonObject jsonObject) {
		if (jsonObject.has("msgs")) {
			JsonObject messagesJson = jsonObject.get("msgs").getAsJsonObject();
			for (String elementName: messagesJson.keySet()) {
				ChatMessage message = new ChatMessage(messagesJson.get(elementName).getAsJsonObject());
				messages.add(message);
			}
		}

		if (jsonObject.has("users")) {
			users = Lists.newArrayList();
			JsonArray array = jsonObject.get("users").getAsJsonArray();
			for (JsonElement element: array) {
				users.add(BotContext.GSON.fromJson(element, ChatUserInformation.class));
			}
		}
	}

	@NotNull
	public List<ChatMessage> getMessages() {
		return messages;
	}

	@Nullable
	public List<ChatUserInformation> getUsers() {
		return users;
	}
}
