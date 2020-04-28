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
package knoblul.eosvstubot.tests.passing;

import com.google.gson.JsonObject;
import knoblul.eosvstubot.api.chat.action.ChatAction;
import knoblul.eosvstubot.api.chat.action.ChatMessage;
import knoblul.eosvstubot.api.chat.action.ChatUserInformation;
import knoblul.eosvstubot.utils.Log;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static knoblul.eosvstubot.api.BotContext.GSON;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 28.04.2020 19:44
 * @author Knoblul
 */
public class ChatActionParsingTest extends Assert {
	@Test
	public void testChatActionParsingResult() throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(Paths.get("moz-captures/message_dialogue.json"))) {
			JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
			ChatAction action = new ChatAction(jsonObject);

			Log.info("Messages: ");
			for (ChatMessage message: action.getNewMessages()) {
				Log.info("messageDocument='%s'", message.getMessageDocument());
				Log.info("text='%s'", message.getText());
				Log.info("user='%s'", message.getUser());
				Log.info("userId='%s'", message.getUserId());
				Log.info("messageType='%s'", message.getMessageType());
			}

			if (action.getUsers() != null) {
				Log.info("\nUsers: ");
				for (ChatUserInformation user : action.getUsers()) {
					Log.info("name='%s'", user.getName());
					Log.info("url='%s'", user.getUrl());
					Log.info("picture='%s'", user.getPicture());
					Log.info("id='%s'", user.getId());
				}
			}
		}
	}
}
