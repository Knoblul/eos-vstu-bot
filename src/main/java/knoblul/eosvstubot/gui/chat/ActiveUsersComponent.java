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
import knoblul.eosvstubot.api.chat.action.ChatUserInformation;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Компонент, содержащий список активны пользователей в чате
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 25.04.2020 19:30
 * @author Knoblul
 */
class ActiveUsersComponent extends JComponent {
	private final List<ChatUserInformation> users = Lists.newArrayList();

	private JList<ChatUserInformation> list;
	private ActiveUsersModel listModel;

	ActiveUsersComponent() {
		fill();
	}

	private void fill() {
		setLayout(new BorderLayout());
		list = new JList<>(listModel = new ActiveUsersModel(this));
		add(new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
		list.setFont(list.getFont().deriveFont(Font.PLAIN, 14));
		list.setSelectionModel(new DefaultListSelectionModel() {
			@Override
			public void setAnchorSelectionIndex(final int anchorIndex) {}

			@Override
			public void setLeadAnchorNotificationEnabled(final boolean flag) {}

			@Override
			public void setLeadSelectionIndex(final int leadIndex) {}

			@Override
			public void setSelectionInterval(final int index0, final int index1) { }
		});
		list.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
														  boolean isSelected, boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
				ChatUserInformation user = (ChatUserInformation) value;
				label.setText("N/A");
				if (user != null) {
					label.setForeground(user.isBot() ? new Color(100, 0, 0) : Color.BLACK);
					label.setText(user.getName() + "#" + user.getId());
				}
				return label;
			}
		});

		JLabel title = new JLabel("Пользователи в чате");
		title.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
		title.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		title.setHorizontalAlignment(JLabel.CENTER);
		add(title, BorderLayout.NORTH);
	}

	public void onUsersChanged(List<ChatUserInformation> newUsers) {
		List<String> botIds = Lists.newArrayList();

		for (ChatUserInformation user: users) {
			if (user.isBot()) {
				botIds.add(user.getId());
			}
		}

		users.clear();
		users.addAll(newUsers);

		for (ChatUserInformation user: users) {
			if (botIds.contains(user.getId())) {
				user.setIsBot(true);
			}
		}

		listModel.fireUpdate();
		list.revalidate();
	}

	List<ChatUserInformation> getUsers() {
		return users;
	}
}
