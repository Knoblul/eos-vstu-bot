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
package knoblul.eosvstubot.frontend.schedule;

import com.google.common.collect.Lists;
import knoblul.eosvstubot.backend.schedule.Lesson;
import knoblul.eosvstubot.backend.schedule.LessonsManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;


/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 23.04.2020 13:18
 * @author Knoblul
 */
public class ScheduleTable extends JComponent {
	private final LessonsManager lessonsManager;
	private final int weekIndex;

	private JTable table;
	private ScheduleTableModel tableModel;

	private JButton editButton;
	private JButton removeButton;
	private LessonEditDialog editDialog;

	public ScheduleTable(LessonsManager lessonsManager, int weekIndex) {
		this.lessonsManager = lessonsManager;
		this.weekIndex = weekIndex;
		fill();
	}

	private void onLessonSelected(@NotNull ListSelectionEvent event) {
		int[] rows = table.getSelectedRows();
		removeButton.setEnabled(rows.length > 0);
		editButton.setEnabled(rows.length == 1);
	}

	private void addLesson(ActionEvent event) {
		if (editDialog.showDialog(lessonsManager, weekIndex, null)) {
			int row = tableModel.getRowCount()-1;
			tableModel.fireTableRowsInserted(row, row);
			table.revalidate();

			row = table.convertRowIndexToView(row);
			table.setRowSelectionInterval(row, row);
		}
	}

	private void editLesson(ActionEvent event) {
		Lesson lesson = lessonsManager.getLesson(weekIndex, table.convertRowIndexToModel(table.getSelectedRow()));
		if (lesson == null) {
			return;
		}

		if (editDialog.showDialog(lessonsManager, weekIndex, lesson)) {
			int row = tableModel.getRowCount()-1;
			tableModel.fireTableRowsUpdated(row, row);
			table.revalidate();

			row = table.convertRowIndexToView(row);
			table.setRowSelectionInterval(row, row);
		}
	}

	private void removeLessons(ActionEvent event) {
		// чтобы избежать десинхрона, нужно вызывать удаление в основном потоке.
		int[] rows = Arrays.copyOf(table.getSelectedRows(), table.getSelectedRows().length);
		lessonsManager.getContext().invokeMainThreadCommand(() -> {
			List<Lesson> toRemove = Lists.newArrayList();
			for (int row : rows) {
				Lesson lesson = lessonsManager.getLesson(weekIndex, table.convertRowIndexToModel(row));
				if (lesson != null) {
					toRemove.add(lesson);
				}
			}
			toRemove.forEach(lessonsManager::removeLesson);
			SwingUtilities.invokeLater(() -> {
				lessonsManager.save();
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

	private void fill() {
		table = new JTable();
		ScheduleTableRenderer renderer = new ScheduleTableRenderer(this);
		table.setDefaultRenderer(int.class, renderer);
		table.setDefaultRenderer(long.class, renderer);
		table.setDefaultRenderer(Object.class, renderer);
		table.getSelectionModel().addListSelectionListener(this::onLessonSelected);
		table.setModel(tableModel = new ScheduleTableModel(this));
		table.setFont(table.getFont().deriveFont(10F));
		table.setGridColor(new Color(200, 200, 200));
		table.setRowHeight(25);
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == KeyEvent.VK_DELETE) {
					removeLessons(null);
				}
			}
		});

		TableRowSorter<ScheduleTableModel> tableSorter = new TableRowSorter<>(tableModel);
		List<RowSorter.SortKey> sortKeys = Lists.newArrayList();
		sortKeys.add(new RowSorter.SortKey(ScheduleTableModel.COLUMN_DAY_OF_WEEK, SortOrder.ASCENDING));
		sortKeys.add(new RowSorter.SortKey(ScheduleTableModel.COLUMN_TIME, SortOrder.ASCENDING));
		for (int i = 0; i < tableModel.getColumnCount(); i++) {
			tableSorter.setSortable(i, false);
		}
		tableSorter.setSortKeys(sortKeys);
		tableSorter.setSortsOnUpdates(true);
		table.setRowSorter(tableSorter);
		tableSorter.sort();

		setBorder(BorderFactory.createTitledBorder("Неделя " + (weekIndex+1)));
		setLayout(new BorderLayout());
		add(new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		JButton addButton;
		buttonsPanel.add(addButton = new JButton("Добавить"));
		buttonsPanel.add(editButton = new JButton("Изменить"));
		buttonsPanel.add(removeButton = new JButton("Удалить"));
		addButton.addActionListener(this::addLesson);
		editButton.addActionListener(this::editLesson);
		removeButton.addActionListener(this::removeLessons);
		add(buttonsPanel, BorderLayout.SOUTH);

		editDialog = new LessonEditDialog();
		onLessonSelected(new ListSelectionEvent(table, -1, -1, false));
	}

	int getWeekIndex() {
		return weekIndex;
	}

	LessonsManager getLessonsManager() {
		return lessonsManager;
	}
}
