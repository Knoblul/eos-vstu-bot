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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.*;
import knoblul.eosvstubot.api.BotContext;
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
 * Этот класс предназначен для управления расписанием.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 22.04.2020 12:28
 * @author Knoblul
 */
public class LessonsManager {
	/**
	 * Контекст бота
	 */
	private final BotContext context;

	/**
	 * Json-файл, в котором хранится расписание
	 */
	private final Path scheduleFile;

	/**
	 * Мапа, хранящая в себе в качесиве ключей индекс недели, а в
	 * качестве значений список предметов, которые относятся к индексу недели
	 * (предметы, которые идут на этой неделе).
	 */
	private Map<Integer, List<Lesson>> lessons = Maps.newHashMap();

	/**
	 * Смещение, которое добавляется в рассчет текущей недели.
	 * Нужно для корректировки, когда индекс недели неправильный.
	 */
	private int firstWeekOfYearIndex;

	public LessonsManager(BotContext context) {
		this.context = context;
		this.scheduleFile = Paths.get("schedule.json");
	}

	/**
	 * Десериализует все расписание из json-файла.
	 */
	public void load() {
		Log.info("Loading lesson schedule...");

		lessons.clear();
		if (Files.exists(scheduleFile)) {
			try (BufferedReader reader = Files.newBufferedReader(scheduleFile)) {
				JsonObject object = BotContext.GSON.fromJson(reader, JsonObject.class);
				if (object != null) {
					if (object.has("schedule")) {
						JsonArray array = object.get("schedule").getAsJsonArray();
						for (JsonElement element : array) {
							Lesson lesson = BotContext.GSON.fromJson(element, Lesson.class);
							getWeekLessons(lesson.getWeekIndex()).add(lesson);
						}
					}

					if (object.has("first_week_of_year_index")) {
						setFirstWeekOfYearIndex(object.get("first_week_of_year_index").getAsInt());
					}
				}
			} catch (IOException | JsonParseException e) {
				Log.warn(e, "Failed to load %s", scheduleFile);
			}
		}
	}

	/**
	 * Сериализует все расписание в json-файл.
	 */
	public void save() {
		try (BufferedWriter writer = Files.newBufferedWriter(scheduleFile)) {
			JsonObject object = new JsonObject();
			JsonArray array = new JsonArray();
			lessons.forEach((k, v) -> v.forEach(lesson -> array.add(BotContext.GSON.toJsonTree(lesson))));
			object.add("first_week_of_year_index", new JsonPrimitive(firstWeekOfYearIndex));
			object.add("schedule", array);
			BotContext.GSON.toJson(object, writer);
		} catch (IOException | JsonParseException e) {
			Log.warn(e, "Failed to save %s", scheduleFile);
		}
	}

	public BotContext getContext() {
		return context;
	}

	/**
	 * @return индекс текущей недели (индекс/номер недели в
	 * данную миллисекунду текущего времени)
	 */
	public int getCurrentWeekIndex() {
		return (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) +
				firstWeekOfYearIndex - 1) % 2;
	}

	public int getFirstWeekOfYearIndex() {
		return firstWeekOfYearIndex;
	}

	public void setFirstWeekOfYearIndex(int firstWeekOfYearIndex) {
		this.firstWeekOfYearIndex = Math.min(Math.max(firstWeekOfYearIndex, 0), 1);
	}

	/**
	 * @return предмет, который идет в данную миллисекунду
	 * текущего времени и текущей недели.
	 */
	public Lesson getCurrentLesson() {
		Calendar currentCalendar = Calendar.getInstance();
		int currentWeekIndex = getCurrentWeekIndex();
		List<Lesson> lessons = getWeekLessons(currentWeekIndex);
		for (Lesson lesson: lessons) {
			long lessonDuration = lesson.getDuration();
			Calendar lessonStartCalendar = lesson.getRelativeCalendar();

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

	/**
	 * Возвращает список предметов, которые идут на указанной неделе.
	 * @param weekIndex индекс недели
	 * @return список предметов, которые идут на указанной неделе.
	 */
	public List<Lesson> getWeekLessons(int weekIndex) {
		return lessons.computeIfAbsent(weekIndex, (k) -> Lists.newArrayList());
	}

	/**
	 * Возвращает предмет, который идет на указаной неделе по
	 * указанному индексу в списке предметов
	 * @param weekIndex индекс недели, по которому получать список недели
	 * @param lessonIndex индекс в списке предметов
	 * @return предмет, который идет на указаной неделе по указанному
	 * индексу в списке предметов
	 */
	public Lesson getLesson(int weekIndex, int lessonIndex) {
		List<Lesson> lessons = getWeekLessons(weekIndex);
		return lessonIndex >= 0 && lessonIndex < lessons.size() ? lessons.get(lessonIndex) : null;
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
