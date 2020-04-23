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
package knoblul.eosvstubot.backend.schedule;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 22.04.2020 12:28
 *
 * @author Knoblul
 */
public class Lesson {
	public static final int DEFAULT_LESSON_DURATION = (60 + 30)*60*1000; // один час тридцать минут (в мс)

	private String name = "";
	private String teacher = "";
	private long scheduleTime = 0;
	private int weekIndex = 0;
	private long duration = DEFAULT_LESSON_DURATION;
	private String chatId = "";

	public long getScheduleTime() {
		return scheduleTime;
	}

	public int getWeekIndex() {
		return weekIndex;
	}

	public long getDuration() {
		return duration;
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

	public void setSchedule(long scheduleTime, int scheduleWeekIndex, long lessonDuration) {
		this.scheduleTime = scheduleTime;
		this.weekIndex = scheduleWeekIndex;
		this.duration = lessonDuration;
	}
}
