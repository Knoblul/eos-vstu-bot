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
package knoblul.eosvstubot.tests.special;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import knoblul.eosvstubot.api.chat.action.ChatAction;
import knoblul.eosvstubot.api.chat.action.ChatMessage;
import knoblul.eosvstubot.utils.Log;
import org.apache.logging.log4j.Level;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 25.04.2020 17:48
 * @author Knoblul
 */
public class ChatGuiTest extends Assert {
	private static Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();
	private static JTextPane chatPane = new JTextPane();

	static {
		chatPane.setEditable(false);
		chatPane.setFont(new Font("Helvetica", Font.PLAIN, 14));
	}

	private static void appendChatString(String text) {
		chatPane.setEditable(true);
		chatPane.setCaretPosition(chatPane.getDocument().getLength());
		chatPane.replaceSelection(text);
		chatPane.setCaretPosition(chatPane.getDocument().getLength());
		chatPane.setEditable(false);
	}

	private static void setChatAttribute(Object name, Object value) {
		StyleContext styleContext = StyleContext.getDefaultStyleContext();
		chatPane.setCharacterAttributes(styleContext.addAttribute(SimpleAttributeSet.EMPTY, name, value), false);
	}

	private static boolean isPokeMessage(ChatMessage message) {
		if (message.getMessageType() == ChatMessage.MessageType.BEEP) {
			return true;
		} else if (message.getMessageType() == ChatMessage.MessageType.DIALOGUE) {
			Elements textElements = message.getMessageDocument().select(".chat-message .text");
			String to = textElements.select("i").text();
			// to equals ...
			return true;
		}
		return false;
	}

	private static void appendMessage(ChatMessage message) {
		String sender = message.getMessageType() == ChatMessage.MessageType.SYSTEM ? "(СИСТЕМА)" :
				String.format("(%s#%s)", message.getUser(), message.getUserId());

		boolean poke = isPokeMessage(message);
		setChatAttribute(StyleConstants.Background, poke ? new Color(230, 180, 180) : Color.WHITE);
		setChatAttribute(StyleConstants.Foreground, Color.BLACK);
		// временная метка
		appendChatString("[" + message.getTime() + "] ");

		// отправитель
		setChatAttribute(StyleConstants.Foreground, message.getMessageType() == ChatMessage.MessageType.SYSTEM
				? new Color(150, 50, 50)
				: new Color(50, 50, 150)
		);
		appendChatString(sender);
		setChatAttribute(StyleConstants.Foreground, Color.BLACK);
		appendChatString(": ");

		String consoleText;
		if (message.getMessageType() == ChatMessage.MessageType.DIALOGUE) {
			Elements textElements = message.getMessageDocument().select(".chat-message .text");
			String to = textElements.select("i").text();
			String text = textElements.select("p").text();

			setChatAttribute(StyleConstants.Bold, Boolean.TRUE);
			setChatAttribute(StyleConstants.Italic, Boolean.TRUE);
			appendChatString("@(" + to + ")");
			setChatAttribute(StyleConstants.Italic, Boolean.FALSE);
			setChatAttribute(StyleConstants.Bold, Boolean.FALSE);
			appendChatString(" " + text);

			consoleText = "@(" + to + ") " + text;
		} else {
			appendChatString(consoleText = message.getText());
		}

		Log.log(poke ? Level.WARN : Level.INFO, null, sender + ": " + consoleText);

		setChatAttribute(StyleConstants.Background, Color.WHITE);
		setChatAttribute(StyleConstants.Foreground, Color.BLACK);
		appendChatString("\n");
	}

	private static void readJsonResponseFile(String name) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(Paths.get("moz-captures/" + name))) {
			JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);
			ChatAction action = new ChatAction(jsonObject);
			action.getNewMessages().forEach(ChatGuiTest::appendMessage);
		}
	}

	@Test
	public void testChatGUI() throws IOException {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable ignored) { }

		readJsonResponseFile("message_dialogue.json");
		readJsonResponseFile("message_system.json");
		readJsonResponseFile("message_beep.json");
		readJsonResponseFile("message_user.json");

		JTabbedPane tabs = new JTabbedPane();

		tabs.add("Чат", new JScrollPane(chatPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
		tabs.add("Тест", new JPanel());

		tabs.setForegroundAt(0, new Color(190, 150, 20));
		tabs.setForegroundAt(1, new Color(190, 0, 0));

		JFrame frame = new JFrame();
		frame.setLayout(new BorderLayout());
		frame.add(tabs, BorderLayout.CENTER);
		frame.setSize(800, 600);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		while (frame.isVisible()) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
}
