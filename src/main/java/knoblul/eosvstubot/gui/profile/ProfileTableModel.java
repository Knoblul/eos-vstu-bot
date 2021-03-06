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
package knoblul.eosvstubot.gui.profile;

import knoblul.eosvstubot.api.profile.Profile;
import knoblul.eosvstubot.api.profile.ProfileManager;

import javax.swing.table.AbstractTableModel;

/**
 * Модель компонента-таблицы менеджера профилей.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 22:19
 * @author Knoblul
 */
class ProfileTableModel extends AbstractTableModel {
	public static final int COLUMN_USERNAME = 0;
	public static final int COLUMN_PROFILE_NAME = 1;
	public static final int COLUMN_PROFILE_LINK = 2;
	public static final int COLUMN_STATUS = 3;

	private static final String[] COLUMNS = new String[] { "Логин", "Имя", "Ссылка на профиль", "Статус" };
	private final ProfileManager profileManager;

	ProfileTableModel(ProfileManager profileManager) {
		this.profileManager = profileManager;
	}

	@Override
	public int getRowCount() {
		return profileManager.getProfiles().size();
	}

	@Override
	public int getColumnCount() {
		return COLUMNS.length;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return columnIndex >= 0 && columnIndex < COLUMNS.length ? COLUMNS[columnIndex] : null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex != COLUMN_STATUS;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (getColumnName(columnIndex) != null) {
			Profile holder = profileManager.getProfile(rowIndex);
			if (holder != null) {
				switch (columnIndex) {
					case COLUMN_USERNAME:
						return holder.getUsername();
					case COLUMN_PROFILE_NAME:
						return holder.getProfileName();
					case COLUMN_PROFILE_LINK:
						return holder.getProfileLink();
					case COLUMN_STATUS:
						return holder.isValid() ? "Действителен" : "Ошибка входа";
				}
			}
		}
		return null;
	}
}
