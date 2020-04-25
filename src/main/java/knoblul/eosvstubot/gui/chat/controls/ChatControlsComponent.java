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
package knoblul.eosvstubot.gui.chat.controls;

import knoblul.eosvstubot.api.handlers.ScheduledConnectionsHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 25.04.2020 20:19
 *
 * @author Knoblul
 */
public class ChatControlsComponent extends JComponent {
	private final ScheduledConnectionsHandler scheduledConnectionsHandler;

	private UserSelectionComponent userSelection;
	private JTextField chatMessageField;

	public ChatControlsComponent(ScheduledConnectionsHandler scheduledConnectionsHandler) {
		this.scheduledConnectionsHandler = scheduledConnectionsHandler;
		fill();
	}

	private void fill() {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridy = 0;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.insets.set(2, 4, 2, 4);

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		add(chatMessageField = new JTextField(), gbc);
		chatMessageField.setPreferredSize(new Dimension(10, 30));
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;

		add(userSelection = new UserSelectionComponent(scheduledConnectionsHandler), gbc);
		userSelection.setEnabled(false);

		JButton sendButton = new JButton("Отправить");
		sendButton.setPreferredSize(new Dimension(sendButton.getPreferredSize().width, 30));
		sendButton.addActionListener(this::sendChatMessage);
		add(sendButton, gbc);

		getInsets().set(4, 4, 4, 4);
		setBorder(BorderFactory.createTitledBorder("Отправить сообщение"));

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == KeyEvent.VK_ENTER) {
					sendChatMessage(null);
				}
			}
		});
	}

	private void sendChatMessage(ActionEvent actionEvent) {
		ScheduledConnectionsHandler.ScheduledConnection sc
				= (ScheduledConnectionsHandler.ScheduledConnection) userSelection.getSelectedItem();
		if (sc != null && sc.getConnection() != null && !chatMessageField.getText().isEmpty()) {
			String msg = chatMessageField.getText();
			scheduledConnectionsHandler.getContext().invokeMainThreadCommand(() ->
				sc.getConnection().sendMessage(msg));
			chatMessageField.setText("");
		}
	}

	public void fireUsersUpdated() {
		Object prevSelectedItem = userSelection.getSelectedItem();
		userSelection.fireUpdate();
		userSelection.setEnabled(!scheduledConnectionsHandler.getScheduledConnections().isEmpty());
		if (prevSelectedItem != null) {
			userSelection.setSelectedItem(prevSelectedItem);
		} else if (userSelection.getModel().getSize() > 0) {
			userSelection.setSelectedIndex(0);
		} else {
			userSelection.setSelectedItem(null);
		}
	}
}
