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
package knoblul.eosvstubot.frontend.login;

import com.google.common.collect.Lists;
import knoblul.eosvstubot.backend.login.LoginHolder;
import knoblul.eosvstubot.backend.login.LoginManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 22:34
 * @author Knoblul
 */
public class LoginManagerTable extends JComponent {
	private final LoginManager loginManager;

	private JTable table;
	private LoginManagerTableModel tableModel;

	private JButton editButton;
	private JButton removeButton;
	private LoginHolderEditDialog editDialog;

	public LoginManagerTable(LoginManager loginManager) {
		this.loginManager = loginManager;
		fill();
	}

	private void onUserSelected(@NotNull ListSelectionEvent event) {
		int first = event.getFirstIndex();
		int last = event.getLastIndex();
		removeButton.setEnabled(first != -1);
		editButton.setEnabled(false);
		if (Math.abs(first - last) <= 1) {
			editButton.setEnabled(first != -1);
		}
	}

	private void addUser(ActionEvent event) {
		if (editDialog.showDialog(loginManager, null)) {
			tableModel.fireInsertEvent(tableModel.getRowCount()-1);
			table.revalidate();
		}
	}

	/**
	 * Открывает окно редактирования пользователя
	 */
	private void editUser(ActionEvent event) {
		LoginHolder holder = loginManager.getLoginHolder(table.getSelectedRow());
		if (holder == null) {
			return;
		}

		if (editDialog.showDialog(loginManager, holder)) {
			tableModel.fireUpdateEvent(table.getSelectedRow());
			table.revalidate();
		}
	}

	/**
	 * Вызывается при удалении выбранных пользователей.
	 * Выбранных пользователей может быть несколько.
	 */
	private void removeUsers(ActionEvent event) {
		List<LoginHolder> toRemove = Lists.newArrayList();
		for (int i: table.getSelectedRows()) {
			toRemove.add(loginManager.getLoginHolder(i));
		}
		toRemove.forEach(loginManager::removeLoginHolder);
		for (int i: table.getSelectedRows()) {
			tableModel.fireDeleteEvent(i);
		}
		table.revalidate();
	}

	private void fill() {
		editDialog = new LoginHolderEditDialog();
		table = new JTable();
		table.setModel(tableModel = new LoginManagerTableModel(loginManager));
		table.getSelectionModel().addListSelectionListener(this::onUserSelected);
		table.setRowHeight(30);
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value,
														   boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected,
						hasFocus, row, column);

				// текст статуса, зеленый или черный
				if (column == LoginManagerTableModel.COLUMN_STATUS) {
					LoginHolder holder = loginManager.getLoginHolder(row);
					if (holder != null) {
						label.setForeground(holder.isValid() ? Color.GREEN.darker()
								: Color.RED.darker());
					}
				} else {
					label.setForeground(Color.BLACK);
				}

				// каждая вторая строка в таблице темнее чем каждая первая
				if (!isSelected) {
					Color color1 = new Color(255, 255, 255);
					Color color2 = new Color(240, 240, 240);
					label.setBackground(row % 2 == 0 ? color1 : color2);
				}

				return label;
			}
		});
		table.setFont(table.getFont().deriveFont(12F));
		table.setGridColor(new Color(200, 200, 200));

		setLayout(new BorderLayout());
		add(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

		JButton addButton;

		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttonsPanel.add(addButton = new JButton("Добавить"));
		buttonsPanel.add(editButton = new JButton("Изменить"));
		buttonsPanel.add(removeButton = new JButton("Удалить"));
		add(buttonsPanel, BorderLayout.SOUTH);

		addButton.addActionListener(this::addUser);
		editButton.addActionListener(this::editUser);
		removeButton.addActionListener(this::removeUsers);

		onUserSelected(new ListSelectionEvent(table, -1, -1, false));
	}
}
