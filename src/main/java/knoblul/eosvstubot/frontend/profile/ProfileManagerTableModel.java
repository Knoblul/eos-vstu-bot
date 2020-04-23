/*
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
package knoblul.eosvstubot.frontend.profile;

import knoblul.eosvstubot.backend.profile.Profile;
import knoblul.eosvstubot.backend.profile.ProfileManager;

import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 22:19
 * @author Knoblul
 */
public class ProfileManagerTableModel implements TableModel {
	public static final int COLUMN_USERNAME = 0;
	public static final int COLUMN_PROFILE_NAME = 1;
	public static final int COLUMN_STATUS = 2;

	private static final String[] COLUMNS = new String[] { "Логин", "Имя", "Статус" };
	private final ProfileManager profileManager;

	private EventListenerList listenerList = new EventListenerList();

	public ProfileManagerTableModel(ProfileManager profileManager) {
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
	public Class<?> getColumnClass(int columnIndex) {
		return Object.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (getColumnName(columnIndex) != null) {
			Profile holder = profileManager.getLoginHolder(rowIndex);
			if (holder != null) {
				switch (columnIndex) {
					case COLUMN_USERNAME:
						return holder.getUsername();
					case COLUMN_PROFILE_NAME:
						return holder.getProfileName();
					case COLUMN_STATUS:
						return holder.isOnline() ? "Действителен" : "Ошибка входа";
				}
			}
		}
		return null;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

	}

	@Override
	public void addTableModelListener(TableModelListener l) {
		listenerList.add(TableModelListener.class, l);
	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		listenerList.remove(TableModelListener.class, l);
	}

	public void fireChangeEvent(int firstRow, int lastRow, int eventType) {
		TableModelListener[] listeners = listenerList.getListeners(TableModelListener.class);
		for (TableModelListener listener: listeners) {
			listener.tableChanged(new TableModelEvent(this, 0, getRowCount(),
					TableModelEvent.ALL_COLUMNS, eventType));
		}
	}

	public void fireInsertEvent(int row) {
		fireChangeEvent(row, row, TableModelEvent.INSERT);
	}

	public void fireDeleteEvent(int row) {
		fireChangeEvent(row, row, TableModelEvent.DELETE);
	}

	public void fireUpdateEvent(int row) {
		fireChangeEvent(row, row, TableModelEvent.UPDATE);
	}

	public void fireUpdateEvent() {
		fireChangeEvent(0, getRowCount(), TableModelEvent.UPDATE);
	}
}