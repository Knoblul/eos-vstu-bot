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
package knoblul.eosvstubot.gui.chat;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import knoblul.eosvstubot.api.chat.ChatConnection;
import knoblul.eosvstubot.api.chat.ChatSession;
import knoblul.eosvstubot.api.chat.action.ChatMessage;
import knoblul.eosvstubot.api.chat.listening.ChatConnectionListener;
import knoblul.eosvstubot.api.profile.Profile;
import knoblul.eosvstubot.api.profile.ProfileManager;
import knoblul.eosvstubot.api.schedule.Lesson;
import knoblul.eosvstubot.api.schedule.ScheduledConnectionsHandler;
import knoblul.eosvstubot.gui.BotMainWindow;
import knoblul.eosvstubot.gui.chat.controls.ChatControlsComponent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jsoup.select.Elements;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.List;
import java.util.Set;


/**
 * Основной компонент чата.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 25.04.2020 16:02
 * @author Knoblul
 */
public class ChatComponent extends JComponent {
	private static final Set<ChatMessage> receivedMessages = Sets.newHashSet();
	private static final Logger CHAT_LOGGER = LogManager.getLogger("CHAT");
	private final ScheduledConnectionsHandler scheduledConnectionsHandler;

	private JLabel disabledLabel;

	private JTextPane chatPane;
	private JLabel chatTitle;
	private ActiveUsersComponent activeUsers;
	private ChatControlsComponent chatControls;

	public ChatComponent(@NotNull ScheduledConnectionsHandler scheduledConnectionsHandler) {
		this.scheduledConnectionsHandler = scheduledConnectionsHandler;
		scheduledConnectionsHandler.setOnSessionChangedCallback(this::onSessionChanged);
		fill();
	}

	private void onSessionChanged(ChatSession session) {
		receivedMessages.clear();

		if (session == null) {
			// при обнулении сессии закрываем чат и лог
			CHAT_LOGGER.log(Level.INFO, "**** КОНЕЦ ЧАТА ****");
			if (activeUsers != null) {
				activeUsers.onUsersChanged(Lists.newArrayList());
			}

			if (chatControls != null) {
				chatControls.fireUsersUpdated();
			}

			setEnabled(false);
			return;
		}

		// при создании новой чат-сессии открываем лог
		Lesson lesson = scheduledConnectionsHandler.getContext().getLessonsManager().getCurrentLesson();
		String title = lesson != null ? lesson.getName() + " (" + lesson.getTeacher() + ")" : "???";
		CHAT_LOGGER.log(Level.INFO, "**** НАЧАЛО ЧАТА, ПРЕДМЕТ: " + title + " ****");
		session.addChatConnectionListener(new ChatConnectionListener() {
			@Override
			public void connected(ChatConnection connection) {
				SwingUtilities.invokeLater(() -> {
					// при создании новго успешного подключения
					// к чату, открываем чат
					setEnabled(true);
					chatControls.fireUsersUpdated();
					if (chatControls != null) {
						chatControls.fireUsersUpdated();
					}
					chatTitle.setText(connection.getConfiguration().getTitle());
					BotMainWindow.instance.updateProfileTable();
				});
			}

			@Override
			public void error(ChatConnection connection, Throwable error) {
//				DialogUtils.showError(String.format("%s: не могу подключится к чату",
//						connection.getProfile().getAlias()), error, false);
				SwingUtilities.invokeLater(() -> {
					if (chatControls != null) {
						chatControls.fireUsersUpdated();
					}

					boolean stillConnected = false;
					List<ScheduledConnectionsHandler.ScheduledConnection> connections
							= scheduledConnectionsHandler.getScheduledConnections();
					//noinspection ForLoopReplaceableByForEach
					for (int i = 0; i < connections.size(); i++) {
						ScheduledConnectionsHandler.ScheduledConnection sc = connections.get(i);
						if (sc != null && sc.getConnection() != null && !sc.getConnection().isInvalid()) {
							stillConnected = true;
						}
					}
					setEnabled(stillConnected);
					BotMainWindow.instance.updateProfileTable();
				});
			}
		});

		session.addChatActionListener((connection, action) -> {
			if (action.getUsers() != null) {
				activeUsers.onUsersChanged(action.getUsers());
			}
			action.getNewMessages().forEach(message -> insertMessage(connection, message));
		});
	}

	private void appendChatString(String text) {
		chatPane.setEditable(true);
		chatPane.setCaretPosition(chatPane.getDocument().getLength());
		chatPane.replaceSelection(text);
		chatPane.setCaretPosition(chatPane.getDocument().getLength());
		chatPane.setEditable(false);
	}

	private void setChatAttribute(Object name, Object value) {
		StyleContext styleContext = StyleContext.getDefaultStyleContext();
		chatPane.setCharacterAttributes(styleContext.addAttribute(SimpleAttributeSet.EMPTY, name, value), false);
	}

	private boolean isPokeMessage(ChatMessage message) {
		ChatMessage.MessageType messageType = message.getMessageType();
		if (messageType == ChatMessage.MessageType.BEEP) {
			return true;
		} else if (messageType == ChatMessage.MessageType.DIALOGUE) {
			Elements textElements = message.getMessageDocument().select(".chat-message .text");
			String to = textElements.select("i").text();
			ProfileManager profileManager = scheduledConnectionsHandler.getContext().getProfileManager();
			List<Profile> profiles = profileManager.getProfiles();
			for (int i = 0; i < profiles.size(); i++) {
				Profile profile = profileManager.getProfile(i);
				if (profile != null && profile.isValid() && profile.getProfileName().equalsIgnoreCase(to)) {
					return true;
				}
			}
		}
		return false;
	}

	private void insertMessage(ChatConnection connection, ChatMessage message) {
		if (receivedMessages.contains(message)) {
			return;
		}
		receivedMessages.add(message);

		ChatMessage.MessageType messageType = message.getMessageType();
		boolean pokeMessage = isPokeMessage(message);
		String sender = messageType == ChatMessage.MessageType.SYSTEM ? "(СИСТЕМА)" :
				String.format("(%s#%s)", message.getUser(), message.getUserId());

		setChatAttribute(StyleConstants.Background, pokeMessage ? new Color(230, 180, 180) : Color.WHITE);
		setChatAttribute(StyleConstants.Foreground, Color.BLACK);
		// временная метка
		appendChatString("[" + message.getTime() + "] ");

		// отправитель
		setChatAttribute(StyleConstants.Foreground, messageType == ChatMessage.MessageType.SYSTEM
				? new Color(150, 50, 50)
				: new Color(50, 50, 150)
		);
		appendChatString(sender);
		setChatAttribute(StyleConstants.Foreground, Color.BLACK);
		appendChatString(": ");

		String consoleText;
		if (messageType == ChatMessage.MessageType.DIALOGUE) {
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
		} else if (messageType == ChatMessage.MessageType.BEEP) {
			appendChatString(consoleText = "Отправил сигнал к " + connection.getProfile());
		} else {
			appendChatString(consoleText = message.getText());
		}

		CHAT_LOGGER.log(Level.INFO, (pokeMessage ? "[УПОМИНАНИЕ] " : "") + sender + ": " + consoleText);
		if (pokeMessage) {
			BotMainWindow.instance.markChatNotifies();
		}

		setChatAttribute(StyleConstants.Background, Color.WHITE);
		setChatAttribute(StyleConstants.Foreground, Color.BLACK);
		appendChatString("\n");
	}

	private JComponent createDisabledComponent() {
		disabledLabel = new JLabel("Нет активных чатов");
		disabledLabel.setFont(disabledLabel.getFont().deriveFont(Font.BOLD, 32));
		disabledLabel.setAlignmentX(0.5F);
		disabledLabel.setAlignmentY(0.5F);
		disabledLabel.setVisible(false);
		return disabledLabel;
	}

	private JComponent createChatComponent() {
		JPanel panel = new JPanel(new BorderLayout());

		chatPane = new JTextPane();
		chatPane.setEditable(false);
		chatPane.setFont(new Font("Helvetica", Font.PLAIN, 14));
		panel.add(new JScrollPane(chatPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

		chatTitle = new JLabel("Чат");
		chatTitle.setMinimumSize(new Dimension(0, 0));
		chatTitle.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
		chatTitle.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		chatTitle.setHorizontalAlignment(JLabel.CENTER);
		panel.add(chatTitle, BorderLayout.NORTH);
		return panel;
	}

	private JComponent createChatActiveUsersComponent() {
		activeUsers = new ActiveUsersComponent();
		activeUsers.setMinimumSize(new Dimension(200, 0));
		return activeUsers;
	}

	private JComponent createChatControls() {
		return chatControls = new ChatControlsComponent(scheduledConnectionsHandler);
	}

	private void fill() {
		setLayout(new OverlayLayout(this));

		add(createDisabledComponent());

		JPanel windowPanel = new JPanel(new BorderLayout());

		JSplitPane chatPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		chatPanel.setLeftComponent(createChatComponent());
		chatPanel.setRightComponent(createChatActiveUsersComponent());
		chatPanel.setResizeWeight(1.0);
		chatPanel.setDividerLocation(0.5);
		windowPanel.add(chatPanel, BorderLayout.CENTER);

		windowPanel.add(createChatControls(), BorderLayout.SOUTH);
		add(windowPanel);

		setEnabled(false);
	}

	private static void recursiveSetEnabled(Component component, boolean enabled, int depth) {
		if (depth > 100) {
			throw new StackOverflowError();
		}
		if (component instanceof JComponent) {
			for (Component children : ((JComponent) component).getComponents()) {
				recursiveSetEnabled(children, enabled, depth + 1);
			}
		}
		component.setEnabled(enabled);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		for (Component component: getComponents()) {
			if (component != disabledLabel) {
				recursiveSetEnabled(component, enabled, 0);
			}
		}
		disabledLabel.setVisible(!enabled);
	}
}
