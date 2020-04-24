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
package knoblul.eosvstubot.api.profile;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import knoblul.eosvstubot.api.BotConstants;
import knoblul.eosvstubot.api.BotContext;
import knoblul.eosvstubot.utils.Log;
import org.apache.http.client.methods.HttpUriRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Этот класс предназначен для управления списком профилей.
 * Так же содержит в себе ключевые поля, без которых не сможет
 * работать ни один профиль.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 16:49
 * @author Knoblul
 */
public class ProfileManager {
	// куки, хранящие moodle session
	private static final String COOKIE_MID_NAME = "MOODLEID1_";
	private static final String COOKIE_SESSION_NAME = "MoodleSession";

	/**
	 * Контекст бота
	 */
	private final BotContext context;

	/**
	 * Json-файл, в котором хранятся профили
	 */
	private final Path profilesFile;

	/**
	 * Список всех профилей, зарегестрированых менеджером.
	 */
	private List<Profile> profiles = Lists.newArrayList();

	public ProfileManager(BotContext context) {
		this.context = context;
		this.profilesFile = Paths.get("profiles.json");
	}

	public BotContext getContext() {
		return context;
	}

	public List<Profile> getProfiles() {
		return profiles;
	}

	/**
	 * Десериализует все профили из json-файла, затем
	 * проверяет их запросами на сайт.
	 */
	public void load() {
		Log.info("Loading profiles...");
		profiles.clear();
		if (Files.exists(profilesFile)) {
			try (BufferedReader reader = Files.newBufferedReader(profilesFile)) {
				JsonArray array = BotContext.GSON.fromJson(reader, JsonArray.class);
				if (array != null) {
					for (JsonElement element : array) {
						profiles.add(BotContext.GSON.fromJson(element, Profile.class));
					}
				}
			} catch (IOException | JsonParseException e) {
				Log.warn(e, "Failed to load %s", profilesFile);
			}
		}
		Log.info("Checking profiles...");
		profiles.forEach(this::checkProfile);
		save();
	}

	/**
	 * Сериализует все профили в json-файл.
	 */
	public void save() {
		try (BufferedWriter writer = Files.newBufferedWriter(profilesFile)) {
			JsonArray array = new JsonArray();
			profiles.forEach(profile -> array.add(BotContext.GSON.toJsonTree(profile)));
			BotContext.GSON.toJson(array, writer);
		} catch (IOException | JsonParseException e) {
			Log.warn(e, "Failed to save %s", profilesFile);
		}
	}

	/**
	 * Парсит данные профиля с главной страницы. Проверяет залогинен ли пользователь.
	 * Если залогинен, то парсит и устанавливает настоящее имя пользователя и ссылку на профиль.
	 * @param index страница главной
	 * @throws IOException если произошла неизвестная ошибка, например
	 * код index-страницы был изменен и парсинг невозможен.
	 */
	private static void parseIndexProfileInfo(@NotNull Profile profile, @NotNull Document index) throws IOException {
		Elements userMenu = index.select(".navbar .navbar-inner .container-fluid .usermenu");
		if (!userMenu.select(".login").isEmpty()) {
			throw new IOException("Invalid login");
		} else {
			Elements profileNameElement = userMenu.select(".menubar li a .userbutton .usertext");
			if (profileNameElement.isEmpty()) {
				throw new IOException("Something went wrong - failed to parse profile name");
			}

			Elements profileLinkElement = userMenu.select(".menu li [aria-labelledby=actionmenuaction-2]");
			if (profileLinkElement.isEmpty()) {
				throw new IOException("Something went wrong - failed to parse profile link");
			}

			profile.setProfileName(profileNameElement.first().text().trim());
			profile.setProfileLink(profileLinkElement.first().attr("href").trim());
		}
	}

	/**
	 * Инвалидирует указанный профиль. Все данные сессии, которые он хранил
	 * будут удалены.
	 * @param profile профиль, который инвалидировать.
	 */
	public void logoutProfile(@NotNull Profile profile) {
		profile.invalidate();
	}

	/**
	 * "Выбирает" указанный профиль. После выбора, <b>все действия,
	 * проходящие через контекст будут выполнятся от именеи
	 * данного профиля.</b>
	 * @param profile профиль, который выбрать
	 */
	public void selectProfile(@Nullable Profile profile) {
		context.clearCookies();
		if (profile != null) {
			String[] cookies = profile.getCookies();
			context.setCookie(COOKIE_MID_NAME, cookies[0], BotConstants.SITE_DOMAIN, "/");
			context.setCookie(COOKIE_SESSION_NAME, cookies[1], BotConstants.SITE_DOMAIN, "/");
		}
	}

	/**
	 * Проверяет, действителен ли профиль простым GET запросом на index.
	 * Если не действителен, то пытается войти с помощью {@link #loginProfile(Profile)}
	 */
	public void checkProfile(@NotNull Profile profile) {
		try {
			// выбираем этот профиль для проверки
			selectProfile(profile);
			// отправляем гет запрос на главную страницу
			String checkURI = "http://" + BotConstants.SITE_DOMAIN + "/index.php";
			HttpUriRequest request = context.buildGetRequest(checkURI, null);
			Document document = context.executeRequest(request, Document.class);
			parseIndexProfileInfo(profile, document); // парсим главную страницу
			Log.info("%s check success", profile.getUsername());
			profile.setValid(true);
		} catch (IOException e) {
			// фоллбек стратегия - логинемся на сайте заново.
			Log.warn(e.getMessage().equalsIgnoreCase("Invalid login") ? null : e,
					"%s check failed. Logging in...", profile.getUsername());
			try {
				loginProfile(profile);
			} catch (IOException x) {
				Log.error(x,"%s login failed. Profile is invalid.", profile.getUsername());
			}
		}
	}

	/**
	 * Отправляет на сайт запрос о создании сесси используя пароль.
	 *
	 * @throws IOException если произошла ошибка
	 */
	public void loginProfile(@NotNull Profile profile) throws IOException {
		if (profile.isValid()) {
			// удаляем сессионные данные профиля
			logoutProfile(profile);
		}

		// очищаем все куки перед входом
		context.clearCookies();

		String loginURI = "http://" + BotConstants.SITE_DOMAIN + "/login/index.php";
		Map<String, String> params = Maps.newHashMap();
		params.put("username", profile.getUsername());
		params.put("password", profile.getPassword());
		params.put("rememberusername", "1");
		params.put("anchor", "");
		HttpUriRequest request = context.buildPostRequest(loginURI, params);
		Document document = context.executeRequest(request, Document.class);

		// парсим примечание (обычно отображается если пользователь
		// уже авторизирован)
		String notice = document.select("#page #page-content #region-main #notice p").text();
		if (!Strings.isNullOrEmpty(notice)) {
			throw new IOException(notice);
		}

		// парсим ошибку входа (обычно отображается если
		// сайтом выявлена ошибка входа, напр. неверный логин или пароль)
		String loginErrorMessage =
				document.select("#page #page-content #region-main div.loginpanel div.loginerrors a")
						.text();
		if (!Strings.isNullOrEmpty(loginErrorMessage)) {
			throw new IOException(loginErrorMessage);
		}

		// после успешного входа происходит редирект на главную.
		// парсим имя профиля и ссылку профиля с главной страницы
		parseIndexProfileInfo(profile, document);

		// сохраняем значение сессионных куки, которые возвратил сайт
		String[] cookies = profile.getCookies();
		cookies[0] = context.getCookieValue(COOKIE_MID_NAME);
		cookies[1] = context.getCookieValue(COOKIE_SESSION_NAME);

		Log.info("%s (%s) successfully logged in", profile.getUsername(), profile.getProfileName());
		profile.setValid(true);
	}

	/**
	 * Находит экземпляр профиля в списке по логину пользователя.
	 * @param username логин пользователя
	 * @return профиль из списка по логину, либо <code>null</code>
	 */
	@Nullable
	public Profile getProfile(@NotNull String username) {
		// сейвовая итерация, чтобы избежать ConcurrentModificationException
		for (int i = 0; i < profiles.size(); i++) {
			Profile profile = getProfile(i);
			if (profile != null && profile.getUsername().equals(username)) {
				return profile;
			}
		}
		return null;
	}

	/**
	 * Находит экземпляр профиля в списке по его индексу.
	 * @param index индекс профиля в списке
	 * @return профиль из списка по индексу, либо <code>null</code>
	 */
	@Nullable
	public Profile getProfile(int index) {
		return index >= 0 && index < profiles.size() ? profiles.get(index) : null;
	}

	/**
	 * Создает новый профиль с указанными данными и записывает в список.
	 * @param username логин пользователя
	 * @param password пароль пользователя
	 * @throws IllegalArgumentException если username уже используется
	 * @return новосозданный профиль
	 */
	@NotNull
	public Profile createProfile(@NotNull String username, @NotNull String password) {
		if (getProfile(username) != null) {
			throw new IllegalArgumentException("User with that username already exists");
		}

		Profile profile = new Profile();
		profile.setCredentials(username, password);
		profiles.add(profile);
		return profile;
	}

	/**
	 * Удаляет экземпляр профиля из списка а так же данные о нем.
	 * @param profile профиля, который необходимо удалить
	 */
	public void removeProfile(@NotNull Profile profile) {
		profiles.remove(profile);
	}
}
