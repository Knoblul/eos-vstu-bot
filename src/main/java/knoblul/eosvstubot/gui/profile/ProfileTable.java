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

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import knoblul.eosvstubot.api.profile.Profile;
import knoblul.eosvstubot.api.profile.ProfileManager;
import knoblul.eosvstubot.utils.swing.DialogUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 * Компонент-таблица менеджера профилей.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 22:34
 * @author Knoblul
 */
public class ProfileTable extends JComponent {
	private static String defaultChatJoinScriptContent;

	private final ProfileManager profileManager;

	private JTable table;
	private ProfileTableModel tableModel;

	private JButton editButton;
	private JButton removeButton;
	private ProfileEditDialog editDialog;

	public ProfileTable(ProfileManager profileManager) {
		this.profileManager = profileManager;
		fill();
	}

	private void onUserSelected(@NotNull ListSelectionEvent event) {
		int[] rows = table.getSelectedRows();
		removeButton.setEnabled(rows.length > 0);
		editButton.setEnabled(rows.length == 1);
	}

	private void addProfile(ActionEvent event) {
		editDialog.showDialog(profileManager, null, () -> {
			int row = tableModel.getRowCount() - 1;
			tableModel.fireTableRowsInserted(row, row);
			table.revalidate();
			row = table.convertRowIndexToView(row);
			table.setRowSelectionInterval(row, row);
		});
	}

	/**
	 * Открывает окно редактирования пользователя
	 */
	private void editProfile(ActionEvent event) {
		Profile profile = profileManager.getProfile(table.convertRowIndexToModel(table.getSelectedRow()));
		if (profile == null) {
			return;
		}

		editDialog.showDialog(profileManager, profile, () -> {
			int row = tableModel.getRowCount() - 1;
			tableModel.fireTableRowsUpdated(row, row);
			table.revalidate();
			row = table.convertRowIndexToView(row);
			table.setRowSelectionInterval(row, row);
		});
	}

	/**
	 * Вызывается при удалении выбранных пользователей.
	 * Выбранных пользователей может быть несколько.
	 */
	private void removeProfiles(ActionEvent event) {
		if (DialogUtils.showConfirmation("Вы уверены что хотите удалить выбранные профили? " +
				"Они будут удалены НАВСЕГДА.")) {
			// чтобы избежать десинхрона, нужно вызывать удаление в основном потоке.
			int[] rows = Arrays.copyOf(table.getSelectedRows(), table.getSelectedRows().length);
			profileManager.getContext().invokeMainThreadCommand(() -> {
				List<Profile> toRemove = Lists.newArrayList();
				for (int row : rows) {
					Profile profile = profileManager.getProfile(table.convertRowIndexToModel(row));
					if (profile != null) {
						toRemove.add(profile);
					}
				}
				toRemove.forEach(profileManager::removeProfile);
				profileManager.save();
				SwingUtilities.invokeLater(() -> {
					if (rows.length == 1) {
						int row = table.convertRowIndexToModel(rows[0]);
						tableModel.fireTableRowsDeleted(row, row);
					} else {
						tableModel.fireTableStructureChanged();
					}
					table.revalidate();
					table.clearSelection();
				});
			});
		}
	}

	public void refresh() {
		tableModel.fireTableStructureChanged();
		table.revalidate();
		table.clearSelection();
	}

	private void fill() {
		editDialog = new ProfileEditDialog();
		table = new JTable();
		table.setModel(tableModel = new ProfileTableModel(profileManager));
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value,
														   boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected,
						hasFocus, row, column);

				// текст статуса, зеленый или черный
				if (column == ProfileTableModel.COLUMN_STATUS) {
					Profile profile = profileManager.getProfile(row);
					if (profile != null) {
						label.setForeground(profile.isValid() ? Color.GREEN.darker()
								: Color.RED.darker());
					}
				} else {
					label.setForeground(Color.BLACK);
				}

				// каждая вторая строка в таблице темнее чем каждая первая
				if (!isSelected) {
					Color color1 = new Color(255, 255, 255);
					Color color2 = new Color(230, 230, 230);
					label.setBackground(row % 2 == 0 ? color1 : color2);
				}
				label.setToolTipText(label.getText());
				return label;
			}
		});
		table.getSelectionModel().addListSelectionListener(this::onUserSelected);
		table.setFont(table.getFont().deriveFont(12F));
		table.setGridColor(new Color(200, 200, 200));
		table.setRowHeight(30);

		JTextField tf = new JTextField();
		tf.setEditable(false);
		table.setDefaultEditor(Object.class, new DefaultCellEditor(tf));

		setLayout(new BorderLayout());
		add(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JButton addButton;
		buttonsPanel.add(addButton = new JButton("Добавить"));
		buttonsPanel.add(editButton = new JButton("Изменить"));
		buttonsPanel.add(removeButton = new JButton("Удалить"));
		add(buttonsPanel, BorderLayout.SOUTH);

		addButton.addActionListener(this::addProfile);
		editButton.addActionListener(this::editProfile);
		removeButton.addActionListener(this::removeProfiles);

		onUserSelected(new ListSelectionEvent(table, -1, -1, false));
	}

	public static String getDefaultChatJoinScriptContent() {
		if (defaultChatJoinScriptContent == null) {
			InputStream in = ProfileEditDialog.class.getResourceAsStream("/default_chat_join.js");
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8))) {
				StringBuilder sb = new StringBuilder();
				String ln;
				while ((ln = reader.readLine()) != null) {
					sb.append(ln).append("\n");
				}
				defaultChatJoinScriptContent = sb.toString();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return defaultChatJoinScriptContent;
	}
}
