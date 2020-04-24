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
package knoblul.eosvstubot;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import knoblul.eosvstubot.backend.BotContext;
import knoblul.eosvstubot.backend.schedule.Lesson;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpUriRequest;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Генерирует по айдишнику ПОЛНОЕ расписание в формате JSON.
 * В контексте обязательно должен быть залогинен пользователь - иначе,
 * запросы на получение айди чата не пройдут.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 23.04.2020 20:34
 * @author Knoblul
 */
public class VolgasuScheduleGenerator {
	private static Map<String, String> cachedCourseLinks = Maps.newHashMap();
	private static final Pattern CHAT_ID_PATTERN = Pattern.compile("(?s)\\?id=(.+?)$");

	private static String fetchChatId(BotContext context, Document index, String lessonName) throws IOException {
		if (cachedCourseLinks.containsKey(lessonName)) {
			return cachedCourseLinks.get(lessonName);
		}

		// теперь самая сложная часть - парсинг айдишника чата.
		// для этого бот переходит по нескольким ссылкам

		// парсит список всех курсов, находит курс с нужным именем.
		Elements courses = index.select("#frontpage-course-list .courses .coursebox .coursename a");
		String targetCourseLink = null;
		for (Element course : courses) {
			String courseName = course.text();
			String courseLink = course.attr("href");
			if (StringUtils.containsIgnoreCase(courseName, lessonName)) {
				targetCourseLink = courseLink;
				break;
			}
		}

		if (targetCourseLink != null) {
			// идем по ссылке на курс
			HttpUriRequest request = context.buildGetRequest(targetCourseLink, null);
			Document coursePage = context.executeRequest(request, Document.class);
			// парсим ссылку на консультацию из страницы курса
			Elements activityInstance = coursePage.select(".activity.chat.modtype_chat .activityinstance a");
			String consultationLink = activityInstance.attr("href");
			if (!consultationLink.isEmpty()) {
				// идем по ссылке на консультацию в режиме онлайн
				request = context.buildGetRequest(consultationLink, null);
				Document consultationPage = context.executeRequest(request, Document.class);
				// получаем ссылку на ajax чат.
				String joinChatAjax = consultationPage.select("#enterlink [href*=/gui_ajax/]")
						.attr("href");
				// парсим id из ссылки на чат
				if (!joinChatAjax.isEmpty()) {
					Matcher m = CHAT_ID_PATTERN.matcher(joinChatAjax);
					if (m.find()) {
						String chatId = m.group(1);
						cachedCourseLinks.put(lessonName, chatId);
						return chatId;
					}
				}
			}
		}
		cachedCourseLinks.put(lessonName, "");
		return "";
	}

	public static void generateScheduleJson(@NotNull BotContext context, @NotNull String scheduleParameter,
											@NotNull BufferedWriter writer) throws IOException {
		// получаем экземпляр главной эиоса
		HttpUriRequest request = context.buildGetRequest("http://eos.vstu.ru/index.php", null);
		Document eosIndex = context.executeRequest(request, Document.class);

		// получаем экземпляр таблицы расписания
		Map<String, String> params = Maps.newHashMap();
		params.put("params", scheduleParameter);
		request = context.buildPostRequest("http://vgasu.ru/contents/select.shedule.php", params);
		Document document = context.executeRequest(request, Document.class);
		Element table = document.select("body table tbody").first();

		// парсим таблицу с расписанием
		String currentWeek = null;
		String currentDayOfWeek = null;
		List<Lesson> lessons = Lists.newArrayList();
		for (Element e : table.children()) {
			if (e.tagName().equals("tr") && e.className().isEmpty()) {
				if (!e.select("th").isEmpty()) {
					currentWeek = e.select("th h4").text();
				} else if (currentWeek != null) {
					Elements elements = e.select("td");
					if (elements.size() == 5) {
						currentDayOfWeek = elements.remove(0).text();
					}

					if (currentDayOfWeek != null && elements.size() == 4) {
						int day = -1;
						String[] shortWeekDays = new DateFormatSymbols(new Locale("RU")).getShortWeekdays();
						for (int i = 0; i < shortWeekDays.length; i++) {
							if (shortWeekDays[i].equalsIgnoreCase(currentDayOfWeek)) {
								day = i;
							}
						}

						if (day != -1) {
							String time = elements.get(0).text();
							String title = elements.get(1).text();
							String type = elements.get(2).text();
							String teacher = elements.get(3).text();
							// получаем и вычисляем айди чата
							String chatId = fetchChatId(context, eosIndex, title.toLowerCase().trim());

							Calendar calendar = Calendar.getInstance();
							calendar.setTimeInMillis(0);
							calendar.set(Calendar.DAY_OF_WEEK, day);
							calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.split(":")[0]));
							calendar.set(Calendar.MINUTE, Integer.parseInt(time.split(":")[1]));
							calendar.set(Calendar.SECOND, 0);

							Lesson lesson = new Lesson();
							lesson.setName(title + ", " + type);
							lesson.setTeacher(teacher);
							lesson.setSchedule(calendar.getTimeInMillis(), currentWeek.contains("II") ? 1 : 0,
									Lesson.DEFAULT_LESSON_DURATION);
							lesson.setChatId(chatId);
							lessons.add(lesson);
						}
					}
				}
			}
		}

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonArray array = new JsonArray();
		lessons.forEach(lesson -> array.add(gson.toJsonTree(lesson)));
		gson.toJson(array, writer);
	}
}
