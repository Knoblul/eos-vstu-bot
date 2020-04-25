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
package knoblul.eosvstubot.gui.schedule;

import knoblul.eosvstubot.api.schedule.Lesson;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Calendar;

/**
 * Рендер клеток недельной таблицы с предметами.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 23.04.2020 17:34
 * @author Knoblul
 */
class ScheduleTableRenderer extends DefaultTableCellRenderer {
	private final ScheduleTable table;

	ScheduleTableRenderer(ScheduleTable table) {
		this.table = table;
	}

	@Override
	public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
												   boolean hasFocus, int row, int column) {
		JLabel label = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
		int modelColumn = t.convertColumnIndexToModel(column);
		int modelRow = t.convertRowIndexToModel(row);
		Lesson lesson = table.getLessonsManager().getLesson(table.getWeekIndex(), modelRow);
		if (lesson != null) {
			Calendar calendar = lesson.getRelativeCalendar();
			switch (modelColumn) {
				case ScheduleTableModel.COLUMN_DAY_OF_WEEK:
					label.setText(StringUtils.capitalize(ScheduleManagerComponent.WEEKDAY_NAMES[calendar.get(Calendar.DAY_OF_WEEK)]));
					break;
				case ScheduleTableModel.COLUMN_TIME:
					label.setText(String.format("%02d:%02d:%02d",
							calendar.get(Calendar.HOUR_OF_DAY),
							calendar.get(Calendar.MINUTE),
							calendar.get(Calendar.SECOND)
					));
					break;
			}

			if (!isSelected) {
				Color color1 = new Color(255, 255, 255);
				Color color2 = new Color(230, 230, 230);
				label.setBackground(calendar.get(Calendar.DAY_OF_WEEK) % 2 == 0 ? color1 : color2);
			}
		}

		label.setToolTipText(label.getText());
		return label;
	}
}
