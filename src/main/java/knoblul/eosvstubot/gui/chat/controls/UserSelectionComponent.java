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

import knoblul.eosvstubot.api.chat.ChatConnection;
import knoblul.eosvstubot.api.schedule.ScheduledConnectionsHandler;

import javax.accessibility.Accessible;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboPopup;
import java.awt.*;

/**
 * Компонент выбора профиля, от которого
 * будет оптравлено сообщение.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 25.04.2020 20:15
 * @author Knoblul
 */
public class UserSelectionComponent extends JComboBox<ScheduledConnectionsHandler.ScheduledConnection> {
	private UserSelectionModel model;
	private boolean expanded;

	UserSelectionComponent(ScheduledConnectionsHandler scheduledConnectionsHandler) {
		setModel(model = new UserSelectionModel(scheduledConnectionsHandler));
		setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
														  boolean isSelected, boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				label.setText("N/A");
				if (value != null) {
					ChatConnection connection = ((ScheduledConnectionsHandler.ScheduledConnection) value)
							.getConnection();
					if (connection != null) {
						label.setText(connection.getProfile().toString());
					}
				}
				return label;
			}
		});
		setPreferredSize(new Dimension(100, 30));

		// пришлось сделать костыль (когда-нибудь я забуду о Java Swing),
		// чтобы сделать размер попапа комбобокса независимым
		// от размера самого комбобокса
		addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				expanded = true;
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				expanded = false;
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {

			}
		});
	}

	@Override
	public Dimension getSize() {
		if (expanded) {
			Accessible child = getAccessibleContext().getAccessibleChild(0);
			if (child instanceof BasicComboPopup) {
				Dimension dims = super.getSize();
				BasicComboPopup popup = (BasicComboPopup) child;
				@SuppressWarnings("unchecked")
				JList<ScheduledConnectionsHandler.ScheduledConnection> list = popup.getList();
				for (int i = 0; i < model.getSize(); i++) {
					Component c =
							renderer.getListCellRendererComponent(list, model.getElementAt(i), i,
									getSelectedIndex() == i, false);
					dims.width = Math.max(c.getPreferredSize().width, dims.width);
				}
				return dims;
			}
		}
		return super.getSize();
	}

	void fireUpdate() {
		model.fireUpdate();
		revalidate();
	}
}
