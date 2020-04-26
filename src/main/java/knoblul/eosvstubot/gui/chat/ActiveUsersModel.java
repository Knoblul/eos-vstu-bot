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

import knoblul.eosvstubot.api.chat.action.ChatUserInformation;

import javax.swing.*;

/**
 * Модель компонента списка активных пользователей в чате.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 25.04.2020 19:31
 * @author Knoblul
 */
public class ActiveUsersModel extends AbstractListModel<ChatUserInformation> {
	private final ActiveUsersComponent parent;

	public ActiveUsersModel(ActiveUsersComponent parent) {
		this.parent = parent;
	}

	@Override
	public int getSize() {
		return parent.getUsers().size();
	}

	@Override
	public ChatUserInformation getElementAt(int index) {
		return index >= 0 && index < parent.getUsers().size() ? parent.getUsers().get(index) : null;
	}

	public void fireUpdate() {
		fireContentsChanged(this, 0, Math.max(getSize()-1, 0));
	}
}
