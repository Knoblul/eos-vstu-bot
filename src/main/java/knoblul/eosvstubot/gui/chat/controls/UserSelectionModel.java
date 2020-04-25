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
import java.util.List;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 25.04.2020 18:14
 *
 * @author Knoblul
 */
class UserSelectionModel extends AbstractListModel<ScheduledConnectionsHandler.ScheduledConnection>
		implements ComboBoxModel<ScheduledConnectionsHandler.ScheduledConnection> {

	private final ScheduledConnectionsHandler scheduledConnectionsHandler;
	private Object selectedItem;

	UserSelectionModel(ScheduledConnectionsHandler scheduledConnectionsHandler) {
		this.scheduledConnectionsHandler = scheduledConnectionsHandler;
	}

	@Override
	public int getSize() {
		List<ScheduledConnectionsHandler.ScheduledConnection> connections
				= scheduledConnectionsHandler.getScheduledConnections();

		int size = 0;
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < connections.size(); i++) {
			ScheduledConnectionsHandler.ScheduledConnection connection = connections.get(i);
			if (connection != null && connection.getConnection() != null) {
				size++;
			}
		}

		return size;
	}

	@Override
	public ScheduledConnectionsHandler.ScheduledConnection getElementAt(int index) {
		List<ScheduledConnectionsHandler.ScheduledConnection> connections
				= scheduledConnectionsHandler.getScheduledConnections();

		int j = 0;
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < connections.size(); i++) {
			ScheduledConnectionsHandler.ScheduledConnection connection = connections.get(i);
			if (connection != null && connection.getConnection() != null) {
				if (j == index) {
					return connection;
				}
				j++;
			}
		}

		return null;
	}

	public void fireUpdate() {
		fireContentsChanged(this, 0, Math.max(getSize()-1, 0));
	}

	@Override
	public void setSelectedItem(Object anItem) {
		this.selectedItem = anItem;
	}

	@Override
	public Object getSelectedItem() {
		return selectedItem;
	}
}
