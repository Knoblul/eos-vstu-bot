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
package knoblul.eosvstubot.backend.schedule;

import knoblul.eosvstubot.EosVstuBot;
import knoblul.eosvstubot.utils.Log;
import knoblul.eosvstubot.utils.PropertiesHelper;
import knoblul.eosvstubot.utils.PropertyField;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 22.04.2020 12:28
 *
 * @author Knoblul
 */
public class Lesson {
	private static final String DEFAULT_LESSON_DURATION = "5400000"; // один час тридцать минут (в мс)

	static final String LESSON_FILE_EXT = ".lesson";

	private final Properties properties;
	private Path propertiesFile;

	@PropertyField(defaultValue = "0")
	private long scheduleTime;

	@PropertyField(defaultValue = "0")
	private int scheduleWeekIndex;

	@PropertyField(defaultValue = DEFAULT_LESSON_DURATION)
	private long lessonDuration;

	@PropertyField
	private String name;

	@PropertyField
	private String teacher;

	public Lesson() {
		properties = new Properties();
	}

	public Lesson(Path propertiesFile) {
		this();
		this.propertiesFile = propertiesFile;
		load();
	}

	/**
	 * Десериализует себя из файла
	 */
	public void load() {
		properties.clear();
		if (Files.exists(propertiesFile)) {
			try (BufferedReader reader = Files.newBufferedReader(propertiesFile)) {
				properties.load(reader);
				PropertiesHelper.load(Lesson.class, this, properties);
			} catch (Throwable e) {
				Log.warn(e, "Failed to load properties file %s", propertiesFile);
			}
		}
	}

	/**
	 * Серализует себя в файл
	 */
	public void save() {
		try (BufferedWriter writer = Files.newBufferedWriter(propertiesFile)) {
			PropertiesHelper.save(Lesson.class, this, properties);
			properties.store(writer, EosVstuBot.NAME + " Lesson File");
		} catch (Throwable e) {
			Log.warn(e, "Failed to save properties file %s", propertiesFile);
		}
	}

	public long getScheduleTime() {
		return scheduleTime;
	}

	public int getScheduleWeekIndex() {
		return scheduleWeekIndex;
	}

	public void setSchedule(long scheduleTime, int scheduleWeekIndex, long lessonDuration) {
		this.scheduleTime = scheduleTime;
		this.scheduleWeekIndex = scheduleWeekIndex;
		this.lessonDuration = lessonDuration;
	}

	public long getLessonDuration() {
		return lessonDuration;
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
}
