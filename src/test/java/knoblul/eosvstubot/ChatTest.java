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

import com.google.common.base.Charsets;
import knoblul.eosvstubot.api.BotContext;
import knoblul.eosvstubot.api.chat.ChatConnection;
import knoblul.eosvstubot.api.chat.ChatSession;
import knoblul.eosvstubot.api.chat.listening.ChatConnectionListener;
import knoblul.eosvstubot.api.profile.Profile;
import knoblul.eosvstubot.utils.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 24.04.2020 11:42
 * @author Knoblul
 */
public class ChatTest {
	public static void main(String[] args) throws IOException {
		BotContext context = new BotContext();
		try {
			context.create();

			String chatLink = "http://eos.vstu.ru/mod/chat/gui_ajax/index.php?id=1946";
			ChatSession session = context.createChatSession(chatLink);

			session.addChatActionListener(action -> {
				if (action.getUsers() != null) {
					action.getUsers().forEach(user ->
							Log.info("New user: name='%s', url='%s', picture='%s', id='%s'",
									user.getName(), user.getUrl(), user.getPicture(), user.getId())
					);
				}

				action.getMessages().forEach(message ->
					Log.info("New message: user='%s', userId='%s', text='%s'",
							message.getUser(), message.getUserId(), message.getText())
				);
			});

			session.addChatConnectionListener(new ChatConnectionListener() {
				@Override
				public void completed(ChatConnection connection) {
					// отправляем приветсвенное сообщение в чат при успешном подключении
					connection.sendMessage("test");
				}

				@Override
				public void failed(ChatConnection connection, Throwable error) {
					Log.error(error, "Hello listener! Connection failed!");
				}
			});

			Profile profile = new Profile();
			profile.setCredentials("user", "pass");
			context.getProfileManager().loginProfile(profile);

			ChatConnection connection = session.createConnection(profile);

			Thread consoleReadThread = new Thread(() -> {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, Charsets.UTF_8))) {
					String ln;
					while ((ln = reader.readLine()) != null) {
						connection.sendMessage(ln);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			consoleReadThread.setName("Console Read Thread");
			consoleReadThread.setDaemon(true);
			consoleReadThread.start();

			context.occupyMainThread();
			context.destroyChatSession(session);
		} finally {
			context.destroy();
		}
	}
}
