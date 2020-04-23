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

import knoblul.eosvstubot.backend.schedule.Lesson;
import knoblul.eosvstubot.backend.schedule.LessonsManager;
import knoblul.eosvstubot.frontend.BotWindow;
import knoblul.eosvstubot.utils.swing.TimeChooser;

import javax.swing.*;
import java.awt.*;
import java.util.Calendar;

import static knoblul.eosvstubot.frontend.schedule.ScheduleTableModel.*;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 23.04.2020 15:03
 * @author Knoblul
 */
public class LessonEditDialog extends JComponent {
	private static final Integer[] COMBO_WEEK_DAYS = new Integer[] {
			Calendar.MONDAY,
			Calendar.TUESDAY,
			Calendar.WEDNESDAY,
			Calendar.THURSDAY,
			Calendar.FRIDAY,
			Calendar.SATURDAY,
			Calendar.SUNDAY,
	};

	private JTextField nameField;
	private JTextField teacherField;
	private JComboBox<Integer> dayOfWeekComboBox;
	private TimeChooser timeSpinner;
	private TimeChooser durationSpinner;
	private JTextField chatIdField;

	LessonEditDialog() {
		fill();
	}

	private void fill() {
		setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets.set(4, 4, 4, 4);
		gbc.gridy = 0;

		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		add(new JLabel(COLUMNS[COLUMN_LESSON]), gbc);
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		add(nameField = new JTextField(20), gbc);
		gbc.weightx = 0;
		gbc.gridy++;

		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		add(new JLabel(COLUMNS[COLUMN_TEACHER]), gbc);
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		add(teacherField = new JTextField(20), gbc);
		gbc.weightx = 0;
		gbc.gridy++;

		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		add(new JLabel(COLUMNS[COLUMN_DAY_OF_WEEK]), gbc);
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		add(dayOfWeekComboBox = new JComboBox<>(COMBO_WEEK_DAYS), gbc);
		dayOfWeekComboBox.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
														  boolean isSelected, boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				label.setText(ScheduleComponent.WEEKDAY_NAMES[(int) value]);
				return label;
			}
		});
		gbc.weightx = 0;
		gbc.gridy++;

		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		add(new JLabel(COLUMNS[COLUMN_TIME]), gbc);
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		add(timeSpinner = new TimeChooser(), gbc);
		gbc.weightx = 0;
		gbc.gridy++;

		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		add(new JLabel(COLUMNS[COLUMN_DURATION]), gbc);
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		add(durationSpinner = new TimeChooser(), gbc);
		gbc.weightx = 0;
		gbc.gridy++;

		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		add(new JLabel(COLUMNS[COLUMN_CHAT_ID]), gbc);
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		add(chatIdField = new JTextField(20), gbc);
		gbc.weightx = 0;
		gbc.gridy++;

		setPreferredSize(new Dimension(300, getPreferredSize().height));
	}

	boolean showDialog(LessonsManager lessonsManager, int weekIndex, Lesson editingLesson) {
		Calendar calendar = Calendar.getInstance();
		if (editingLesson != null) {
			calendar.setTimeInMillis(editingLesson.getScheduleTime());
		}

		nameField.setText(editingLesson != null ? editingLesson.getName() : "");
		teacherField.setText(editingLesson != null ? editingLesson.getTeacher() : "");
		dayOfWeekComboBox.setSelectedItem(calendar.get(Calendar.DAY_OF_WEEK));
		timeSpinner.set(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
		durationSpinner.setTimeMillis(editingLesson != null ? editingLesson.getDuration() : Lesson.DEFAULT_LESSON_DURATION);
		chatIdField.setText(editingLesson != null ? editingLesson.getChatId() : "");

		String title = editingLesson == null ? "Создать предмет" : "Изменить данные предмета";
		if (JOptionPane.showConfirmDialog(BotWindow.instance, this, title,
				JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
			return false;
		}

		Lesson lesson = editingLesson != null ? editingLesson : lessonsManager.createLesson(weekIndex);
		Calendar scheduleCalendar = Calendar.getInstance();
		scheduleCalendar.setTimeInMillis(0);
		scheduleCalendar.set(Calendar.DAY_OF_WEEK, (int) dayOfWeekComboBox.getSelectedItem());
		scheduleCalendar.set(Calendar.HOUR, timeSpinner.getHour());
		scheduleCalendar.set(Calendar.MINUTE, timeSpinner.getMinute());
		scheduleCalendar.set(Calendar.SECOND, timeSpinner.getSecond());
		lesson.setName(nameField.getText().trim());
		lesson.setTeacher(teacherField.getText().trim());
		lesson.setSchedule(scheduleCalendar.getTimeInMillis(), weekIndex, durationSpinner.getTimeMillis());
		lesson.setChatId(chatIdField.getText().trim());
		lessonsManager.save();
		return true;
	}
}