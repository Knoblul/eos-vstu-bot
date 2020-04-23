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

import javax.swing.table.AbstractTableModel;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 23.04.2020 13:22
 *
 * @author Knoblul
 */
class ScheduleTableModel extends AbstractTableModel {
	static final int COLUMN_DAY_OF_WEEK = 0;
	static final int COLUMN_LESSON = 1;
	static final int COLUMN_TEACHER = 2;
	static final int COLUMN_TIME = 3;
	static final int COLUMN_DURATION = 4;
	static final int COLUMN_CHAT_ID = 5;

	static final String[] COLUMNS = new String[] {
			"День недели",
			"Предмет",
			"Препод",
			"Время",
			"Продолжительность",
			"ID чата"
	};

	private final ScheduleTable table;

	ScheduleTableModel(ScheduleTable table) {
		this.table = table;
	}

	@Override
	public int getRowCount() {
		return table.getLessonsManager().getWeekLessons(table.getWeekIndex()).size();
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
		switch (columnIndex) {
			case COLUMN_DAY_OF_WEEK:
				return int.class;
			case COLUMN_TIME:
				return long.class;
			default:
				return Object.class;
		}
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (getColumnName(columnIndex) != null) {
			Lesson lesson = table.getLessonsManager().getLesson(table.getWeekIndex(), rowIndex);
			if (lesson != null) {
				Calendar scheduleCalendar = Calendar.getInstance();
				scheduleCalendar.setTimeInMillis(lesson.getScheduleTime());
				switch (columnIndex) {
					case COLUMN_DAY_OF_WEEK:
						// нормализация дня недели для сортера
						int day = scheduleCalendar.get(Calendar.DAY_OF_WEEK);
						return day == Calendar.SUNDAY ? Calendar.SATURDAY+1 : day;
					case COLUMN_LESSON:
						return lesson.getName();
					case COLUMN_TEACHER:
						return lesson.getTeacher();
					case COLUMN_TIME:
						scheduleCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
						long timeWithoutDays = scheduleCalendar.getTimeInMillis();
						scheduleCalendar.setTimeInMillis(lesson.getScheduleTime());
						return timeWithoutDays;
					case COLUMN_DURATION:
						return String.format("%02d:%02d:%02d",
								TimeUnit.MILLISECONDS.toHours(lesson.getDuration())%24,
								TimeUnit.MILLISECONDS.toMinutes(lesson.getDuration())%60,
								TimeUnit.MILLISECONDS.toSeconds(lesson.getDuration())%60
						);
					case COLUMN_CHAT_ID:
						return lesson.getChatId();
				}
			}
		}
		return null;
	}
}
