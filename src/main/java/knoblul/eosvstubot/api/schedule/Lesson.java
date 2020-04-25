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
package knoblul.eosvstubot.api.schedule;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 22.04.2020 12:28
 *
 * @author Knoblul
 */
public class Lesson {
	public static final int DEFAULT_LESSON_DURATION = (60 + 30)*60*1000; // один час тридцать минут (в мс)

	/**
	 * Название предмета.
	 */
	private String name = "";

	/**
	 * Имя преподавателя, который ведет данный предмет.
	 */
	private String teacher = "";

	/**
	 * Время старта предмета, от начала недели, в миллисекундах.
	 */
	private long scheduleTime = 0;

	/**
	 * Индекс/номер недели, на которой данный предмет должен преподаваться.
	 */
	private int weekIndex = 0;

	/**
	 * Продолжительность предмета, в миллисекундах.
	 */
	private long duration = DEFAULT_LESSON_DURATION;

	/**
	 * ID чата, в котором идет онлайн-конференция.
	 */
	private String chatId = "";

	public long getScheduleTime() {
		return scheduleTime;
	}

	public void setScheduleTime(long scheduleTime) {
		this.scheduleTime = scheduleTime;
	}

	public int getWeekIndex() {
		return weekIndex;
	}

	public void setWeekIndex(int weekIndex) {
		this.weekIndex = weekIndex;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTeacher() {
		return teacher;
	}

	public void setTeacher(String teacher) {
		this.teacher = teacher;
	}

	public String getChatId() {
		return chatId;
	}

	public void setChatId(String chatId) {
		this.chatId = chatId;
	}

	public Calendar getRelativeCalendar() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.clear(Calendar.MINUTE);
		calendar.clear(Calendar.SECOND);
		calendar.clear(Calendar.MILLISECOND);
		calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
		calendar.setTimeInMillis(calendar.getTimeInMillis() + scheduleTime);
		return calendar;
	}
}
