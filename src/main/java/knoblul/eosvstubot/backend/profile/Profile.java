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
package knoblul.eosvstubot.backend.profile;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import knoblul.eosvstubot.EosVstuBot;
import knoblul.eosvstubot.backend.BotContext;
import knoblul.eosvstubot.utils.Log;
import knoblul.eosvstubot.utils.PropertiesHelper;
import knoblul.eosvstubot.utils.PropertyField;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 13:40
 * @author Knoblul
 */
public class Profile {
	public static final char CHAT_PHRASES_DELIMITER = '|';
	public static final String DEFAULT_CHAT_PHRASES = "+";

	static final String PROFILE_FILE_EXT = ".profile";

	private static final String COOKIE_MID_NAME = "MOODLEID1_";
	private static final String COOKIE_SESSION_NAME = "MoodleSession";

	/**
	 * Менеджер профилей, который хранит в себе экземпляр
	 * данного профиля.
	 */
	private final ProfileManager manager;

	/**
	 * Пропертиес, который хранит в себе все необходимые данные для
	 * сериализации/десериализации профиля.
	 */
	private final Properties properties;

	/**
	 * Файл в который сохраняется {@link #properties}
	 */
	private Path propertiesFile;

	/**
	 * Логин профиля eos.vstu.ru
	 */
	@PropertyField
	private String username;

	/**
	 * Пароль профиля eos.vstu.ru
	 */
	@PropertyField
	private String password;

	/**
	 * Имя профиля eos.vstu.ru
	 */
	private String profileName;

	/**
	 * Ссылка профиля eos.vstu.ru
	 */
	private String profileLink;

	/**
	 * Куки сессии eos.vstu.ru
	 */
	@PropertyField(readMethodName = "readCookies", writeMethodName = "writeCookies")
	private String[] cookies = new String[2];

	/**
	 * Фразы, которые бот должен говорить в чате от лица этого профиля.
	 */
	@PropertyField(readMethodName = "setChatPhrasesFromString", writeMethodName = "getChatPhrasesAsString")
	private List<String> chatPhrases;

	/**
	 * Флаг, значение которого <code>true</code> тогда, когда
	 * вход в профиль был выполнен успешно
	 */
	private boolean online;

	public Profile(ProfileManager manager, Path propertiesFile) {
		this.manager = manager;
		this.properties = new Properties();
		this.propertiesFile = propertiesFile;
		this.chatPhrases = Lists.newArrayList(DEFAULT_CHAT_PHRASES);
		load();
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getProfileName() {
		return profileName;
	}

	public String getProfileLink() {
		return profileLink;
	}

	public List<String> getChatPhrases() {
		return chatPhrases;
	}

	/**
	 * @see #getChatPhrases()
	 * @return фразы в формате строки, разделеные {@link #CHAT_PHRASES_DELIMITER}
	 */
	public String getChatPhrasesAsString() {
		return String.join(Character.toString(CHAT_PHRASES_DELIMITER), chatPhrases);
	}

	public void setChatPhrasesFromString(String chatPhrases) {
		this.chatPhrases = Splitter.on(CHAT_PHRASES_DELIMITER).trimResults().splitToList(chatPhrases);
	}

	/**
	 * Устанавливает новые данные для входа, а так же
	 * удаляет старые данные и меняет путь до файла сохранения.
	 *
	 * @param username логин
	 * @param password пароль
	 */
	public void setCredentials(@NotNull String username, @NotNull String password) {
		// удаляем данные предыдущей сессии
		logout();

		this.username = username;
		this.password = password;
		this.propertiesFile = Paths.get(manager.getWorkDir().toString(), username+PROFILE_FILE_EXT);

		// удаляем предыдущий файл конфига
		try {
			Files.deleteIfExists(propertiesFile);
		} catch (IOException ignored) { }
	}

	/**
	 * Десериализует себя из конфиг файла
	 */
	public void load() {
		PropertiesHelper.load(Profile.class, this, propertiesFile);
	}

	/**
	 * Серализует себя в конфиг файл
	 */
	public void save() {
		PropertiesHelper.save(Profile.class, this, propertiesFile);
	}

	@SuppressWarnings("unused")
	private void readCookies(String value) {
		if (value.isEmpty()) {
			cookies = new String[2];
			return;
		}

		try {
			ByteArrayInputStream in = new ByteArrayInputStream(Hex.decodeHex(value));
			cookies = (String[]) new ObjectInputStream(in).readObject();
		} catch (IOException | ClassNotFoundException | DecoderException e) {
			e.printStackTrace();
		}
	}

	@NotNull
	@SuppressWarnings("unused")
	private String writeCookies() {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			new ObjectOutputStream(out).writeObject(cookies);
			return new String(Hex.encodeHex(out.toByteArray()));
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * @return <code>true</code>, если сессия на момент вызова метода действительна.
	 */
	public boolean isOnline() {
		return online;
	}

	/**
	 * "Выбирает" данный профиль. После выбора, <b>все действия,
	 * проходящие через контекст будут выполнятся от именеи
	 * данного профиля.</b>
	 */
	public void select() {
		if (isOnline()) {
			BotContext context = manager.getContext();
			context.clearCookies();
			context.setCookie(COOKIE_MID_NAME, cookies[0], EosVstuBot.SITE_DOMAIN, "/");
			context.setCookie(COOKIE_SESSION_NAME, cookies[1], EosVstuBot.SITE_DOMAIN, "/");
		}
	}

	/**
	 * Парсит данные профиля с главной страницы. Проверяет залогинен ли пользователь.
	 * Если залогинен, то парсит и устанавливает настоящее имя пользователя и ссылку на профиль.
	 * @param index страница главной
	 * @throws IOException если произошла неизвестная ошибка, например
	 * код index-страницы был изменен и парсинг невозможен.
	 */
	private void parseProfile(@NotNull Document index) throws IOException {
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

			profileName = profileNameElement.first().text();
			profileLink = profileLinkElement.first().attr("href");
		}
	}

	/**
	 * Инвалидирует данный профиль. Все данные сессии, которые он хранил
	 * будут удалены.
	 */
	public void logout() {
		// отмечаем что сессия отсутсвует
		online = false;
		// обнуляем имя профиля, ссылку профиля и куки
		profileName = null;
		profileLink = null;
		cookies = new String[2];
	}

	/**
	 * Удаляет сессионные данные профиля, файл конфига и
	 * очищает пропертиес.
	 */
	public void delete() {
		logout();

		// удаляем файл конфига и обнуляем пропертиес
		properties.clear();
		try {
			Files.deleteIfExists(propertiesFile);
		} catch (IOException ignored) { }
	}

	/**
	 * Проверяет, действителен ли профиль простым GET запросом на index.
	 * Если не действителен, то пытается войти с помощью {@link #login()}
	 */
	public void check() {
		try {
			// выбираем этот профиль для проверки
			select();
			// отправляем гет запрос на главную страницу
			BotContext context = manager.getContext();
			String checkURI = "http://" + EosVstuBot.SITE_DOMAIN + "/index.php";
			HttpGet request = context.buildGetRequest(checkURI, null);
			Document document = context.executeRequestAndParseResponse(request);
			parseProfile(document); // парсим главную страницу
			Log.info("%s check success", username);
			online = true;
		} catch (IOException e) {
			// фоллбек стратегия - логинемся на сайте заново.
			Log.warn(e.getMessage().equalsIgnoreCase("Invalid login") ? null : e,
					"%s check failed. Logging in...", username);
			try {
				login();
			} catch (IOException x) {
				Log.error(x,"%s login failed. Profile is invalid.", username);
			}
		}
	}

	/**
	 * Отправляет на сайт запрос о создании сесси используя пароль.
	 * При успехе, сохраняет все данные сессии в {@link #properties}
	 * а затем сохраняет все в файл.
	 *
	 * @throws IOException если произошла ошибка
	 */
	public void login() throws IOException {
		if (isOnline()) {
			// удаляем сессионные данные профиля
			logout();
		}

		try {
			BotContext context = manager.getContext();

			// очищаем все куки перед входом
			context.clearCookies();

			String loginURI = "http://" + EosVstuBot.SITE_DOMAIN + "/login/index.php";
			Map<String, String> params = Maps.newHashMap();
			params.put("username", username);
			params.put("password", password);
			params.put("rememberusername", "1");
			params.put("anchor", "");
			HttpPost request = context.buildPostRequest(loginURI, params);
			Document document = context.executeRequestAndParseResponse(request);

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
			parseProfile(document);

			// сохраняем значение сессионных куки, которые возвратил сайт
			cookies[0] = context.getCookieValue(COOKIE_MID_NAME);
			cookies[1] = context.getCookieValue(COOKIE_SESSION_NAME);

			Log.info("%s (%s) successfully logged in", username, profileName);
			online = true;
		} finally {
			save();
		}
	}
}
