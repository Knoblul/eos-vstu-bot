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
package knoblul.eosvstubot;

import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import knoblul.eosvstubot.api.BotConstants;
import knoblul.eosvstubot.api.chat.action.ChatAction;
import knoblul.eosvstubot.api.chat.action.ChatMessage;
import knoblul.eosvstubot.api.chat.action.ChatUserInformation;
import knoblul.eosvstubot.utils.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 25.04.2020 17:48
 * @author Knoblul
 */
public class RuntimeTests {
	public static void main(String[] args) throws IOException {
		Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();
		try (BufferedReader reader = Files.newBufferedReader(Paths.get("moz-captures/update_chat_json_response.json"))) {
			JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
			ChatAction action = new ChatAction(jsonObject);

			Log.info("Messages: ");
			for (ChatMessage message: action.getNewMessages()) {
				Log.info("text='%s'", message.getText());
				Log.info("user='%s'", message.getUser());
				Log.info("userId='%s'", message.getUserId());
				Log.info("systemMessage='%s'", message.isSystemMessage());
			}

			if (action.getUsers() != null) {
				Log.info("Users: ");
				for (ChatUserInformation user : action.getUsers()) {
					Log.info("name='%s'", user.getName());
					Log.info("url='%s'", user.getUrl());
					Log.info("picture='%s'", user.getPicture());
					Log.info("id='%s'", user.getId());
				}
			}
		}

		while (true) {
			Stopwatch sw = Stopwatch.createStarted();
			boolean reachable = false;
			try {
				reachable = InetAddress.getByName(BotConstants.SITE_DOMAIN).isReachable(15000);
			} catch (Throwable ignored) {}
			Log.info("Reachable: %s", reachable);
			sw.stop();

			long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
			if (5000 - elapsed > 0) {
				try {
					Thread.sleep(5000 - elapsed);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}
}
