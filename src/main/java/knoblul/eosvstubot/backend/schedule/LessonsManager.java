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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import knoblul.eosvstubot.backend.BotContext;
import knoblul.eosvstubot.utils.BotConfig;
import knoblul.eosvstubot.utils.Log;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 22.04.2020 12:28
 * @author Knoblul
 */
public class LessonsManager {
	private final BotContext context;
	private final Path scheduleFile;

	private Map<Integer, List<Lesson>> lessons = Maps.newHashMap();

	public LessonsManager(BotContext context) {
		this.context = context;
		this.scheduleFile = Paths.get("schedule.json");
	}

	public void load() {
		Log.info("Loading lesson schedule...");

		lessons.clear();
		if (Files.exists(scheduleFile)) {
			try (BufferedReader reader = Files.newBufferedReader(scheduleFile)) {
				JsonArray array = BotContext.GSON.fromJson(reader, JsonArray.class);
				if (array != null) {
					for (JsonElement element : array) {
						Lesson lesson = BotContext.GSON.fromJson(element, Lesson.class);
						getWeekLessons(lesson.getWeekIndex()).add(lesson);
					}
				}
			} catch (IOException | JsonParseException e) {
				Log.warn(e, "Failed to load %s", scheduleFile);
			}
		}
	}

	public void save() {
		try (BufferedWriter writer = Files.newBufferedWriter(scheduleFile)) {
			JsonArray array = new JsonArray();
			lessons.forEach((k, v) -> v.forEach(lesson -> array.add(BotContext.GSON.toJsonTree(lesson))));
			BotContext.GSON.toJson(array, writer);
		} catch (IOException | JsonParseException e) {
			Log.warn(e, "Failed to save %s", scheduleFile);
		}
	}

	public BotContext getContext() {
		return context;
	}

	public Lesson getCurrentLesson() {
		Calendar currentCalendar = Calendar.getInstance();
		int currentWeekIndex = getCurrentWeekIndex();
		List<Lesson> lessons = getWeekLessons(currentWeekIndex);
		for (Lesson lesson: lessons) {
			Calendar lessonScheduleCalendar = Calendar.getInstance();
			lessonScheduleCalendar.setTimeInMillis(lesson.getScheduleTime());
			int lessonWeekIndex = lesson.getWeekIndex();
			long lessonDuration = lesson.getDuration();

			Calendar lessonStartCalendar = Calendar.getInstance();
			lessonStartCalendar.set(Calendar.DAY_OF_WEEK, lessonScheduleCalendar.get(Calendar.DAY_OF_WEEK));
			lessonStartCalendar.set(Calendar.HOUR, lessonScheduleCalendar.get(Calendar.HOUR_OF_DAY));
			lessonStartCalendar.set(Calendar.MINUTE, lessonScheduleCalendar.get(Calendar.MINUTE));
			lessonStartCalendar.set(Calendar.SECOND, lessonScheduleCalendar.get(Calendar.SECOND));
			if (currentWeekIndex != lessonWeekIndex) {
				// если сейчас номер недели отличен от номера недели урока,
				// то "переносим" урок на некст неделю
				lessonStartCalendar.add(Calendar.WEEK_OF_MONTH, 1);
			}

			// находим время, которое прошло с момента конца урока.
			// если время меньше lessonDuration, то урок идет. Если больше, то
			// timeDifference-lessonDuration это время, которое остается до начала урока.
			// если timeDifference меньше нуля, то урок уже прошел.
			long timeDifference = lessonStartCalendar.getTimeInMillis() + lessonDuration
					- currentCalendar.getTimeInMillis();
			if (timeDifference > 0 && timeDifference < lessonDuration) {
				return lesson;
			}
		}
		return null;
	}

	public List<Lesson> getWeekLessons(int weekIndex) {
		return lessons.computeIfAbsent(weekIndex, (k) -> Lists.newArrayList());
	}

	public Lesson getLesson(int weekIndex, int lessonIndex) {
		List<Lesson> lessons = getWeekLessons(weekIndex);
		return lessonIndex >= 0 && lessonIndex < lessons.size() ? lessons.get(lessonIndex) : null;
	}

	public int getCurrentWeekIndex() {
		return (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) +
				BotConfig.instance.getFirstWeekOfYearIndex() - 1) % 2;
	}

	public void removeLesson(@NotNull Lesson lesson) {
		getWeekLessons(lesson.getWeekIndex()).remove(lesson);
	}

	public Lesson createLesson(int weekIndex) {
		Lesson lesson = new Lesson();
		getWeekLessons(weekIndex).add(lesson);
		return lesson;
	}
}
