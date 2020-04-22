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
package knoblul.eosvstubot.backend.login;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import knoblul.eosvstubot.EosVstuBot;
import knoblul.eosvstubot.backend.BotContext;
import knoblul.eosvstubot.utils.Log;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.jetbrains.annotations.NotNull;
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
import java.util.Properties;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 13:40
 * @author Knoblul
 */
public class LoginHolder {
	public static final char CHAT_PHRASES_DELIMITER = '|';
	public static final String DEFAULT_CHAT_PHRASES = "+";

	static final String LOGIN_FILE_EXT = ".login";

	private static final String COOKIE_MID_NAME = "MOODLEID1_";
	private static final String COOKIE_SESSION_NAME = "MoodleSession";
	private static final String P_PROFILE_NAME = "ProfileName";
	private static final String P_PROFILE_LINK = "ProfileLink";
	private static final String P_USERNAME = "Username";
	private static final String P_PASSWORD = "Password";
	private static final String P_CHAT_PHRASES = "ChatPhrases";

	/**
	 * Менеджер логинов, который хранит в себе экземпляр
	 * данного холдера.
	 */
	private final LoginManager manager;

	/**
	 * Пропертиес, который хранит в себе все необходимые данные для
	 * сериализации/десериализации холдера.
	 */
	private final Properties properties;

	/**
	 * Файл в который сохраняется {@link #properties}
	 */
	private Path propertiesFile;

	/**
	 * Логин профиля eos.vstu.ru
	 */
	private String username;

	/**
	 * Пароль профиля eos.vstu.ru
	 */
	private String password;

	/**
	 * Фразы, которые бот должен говорить в чате от лица этого профиля.
	 */
	private List<String> chatPhrases;

	/**
	 * Флаг, значение которого <code>true</code> тогда, когда
	 * вход в профиль был выполнен успешно
	 */
	private boolean valid;

	public LoginHolder(LoginManager manager, Path propertiesFile) {
		this.manager = manager;
		this.properties = new Properties();
		this.propertiesFile = propertiesFile;
		this.chatPhrases = Lists.newArrayList(DEFAULT_CHAT_PHRASES);
		this.username = this.password = "";
		load();
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	/**
	 * @return имя профиля eos.vstu.ru
	 */
	public String getProfileName() {
		return properties.getProperty("ProfileName", "");
	}

	/**
	 * @return ссылку на профиль eos.vstu.ru
	 */
	public String getProfileLink() {
		return properties.getProperty("ProfileLink", "");
	}

	public List<String> getChatPhrases() {
		return chatPhrases;
	}

	/**
	 * @return <code>true</code>, если сессия на момент вызова метода действительна.
	 */
	public boolean isValid() {
		return valid;
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
		// удаляем предыдущие данные
		invalidate();

		this.username = username;
		this.password = password;
		this.propertiesFile = Paths.get(manager.getWorkDir().toString(), username+LOGIN_FILE_EXT);
	}

	/**
	 * Десериализует себя из файла
	 */
	private void load() {
		properties.clear();
		if (Files.exists(propertiesFile)) {
			try (BufferedReader reader = Files.newBufferedReader(propertiesFile)) {
				properties.load(reader);
				username = properties.getProperty(P_USERNAME, "");
				password = properties.getProperty(P_PASSWORD, "");
				setChatPhrasesFromString(properties.getProperty(P_CHAT_PHRASES, DEFAULT_CHAT_PHRASES));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Серализует себя в файл
	 */
	private void save() {
		try (BufferedWriter writer = Files.newBufferedWriter(propertiesFile)) {
			properties.setProperty(P_USERNAME, username);
			properties.setProperty(P_PASSWORD, password);
			properties.setProperty(P_CHAT_PHRASES, getChatPhrasesAsString());
			properties.store(writer, EosVstuBot.NAME + " Login File");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * "Выбирает" данный профиль. После выбора, <b>все действия,
	 * проходящие через контекст будут выполнятся от именеи
	 * данного профиля.</b>
	 */
	public void select() {
		BotContext context = manager.getContext();
		context.clearCookies();
		context.setCookie(COOKIE_MID_NAME, properties.getProperty(COOKIE_MID_NAME), "eos.vstu.ru", "/");
		context.setCookie(COOKIE_SESSION_NAME, properties.getProperty(COOKIE_SESSION_NAME), "eos.vstu.ru", "/");
	}

	/**
	 * Парсит данные профиля с главной страницы. Проверяет залогинен ли пользователь.
	 * Если залогинен, то парсит и устанавливает настоящее имя пользователя и ссылку на профиль в
	 * {@link #P_PROFILE_NAME} и {@link #P_PROFILE_LINK} у {@link #properties}
	 * @param index страница главной
	 * @throws IOException если произошла неизвестная ошибка, например
	 * код index-страницы был изменен и парсинг невозможен.
	 */
	private void parseProfile(@NotNull Document index) throws IOException {
		Elements usermenu = index.select(".navbar .navbar-inner .container-fluid .usermenu");
		if (!usermenu.select(".login").isEmpty()) {
			throw new IOException("Invalid login");
		} else {
			Elements profileNameElement = usermenu.select(".menubar li a .userbutton .usertext");
			if (profileNameElement.isEmpty()) {
				throw new IOException("Something went wrong - failed to parse profile name");
			}

			Elements profileLinkElement = usermenu.select(".menu li [aria-labelledby=actionmenuaction-2]");
			if (profileLinkElement.isEmpty()) {
				throw new IOException("Something went wrong - failed to parse profile link");
			}

			String profileName = profileNameElement.first().text();
			properties.setProperty(P_PROFILE_NAME, profileName);

			String profileLink = profileLinkElement.first().attr("href");
			properties.setProperty(P_PROFILE_LINK, profileLink);
		}
	}

	/**
	 * Инвалидирует данный профиль. Все данные, которые он хранил будут удалены.
	 * Так же файл сериализации будет удален.
	 */
	public void invalidate() {
		valid = false;
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
			select();
			BotContext context = manager.getContext();
			HttpGet request = context.buildGetRequest(EosVstuBot.SITE + "/index.php", null);
			Document document = context.executeRequestAndParseResponse(request);
			parseProfile(document);
			Log.info("%s check success", this);
			valid = true;
		} catch (IOException e) {
			// фоллбек стратегия - логинемся на сайте заново.
			Log.error(e.getMessage().equalsIgnoreCase("Invalid login") ? null : e,
					"%s check failed. Logging in...", this);
			try {
				login();
				valid = true;
			} catch (IOException x) {
				Log.error(x,"%s login failed. Holder is invalid.", this);
				valid = false;
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
		if (isValid()) {
			invalidate();
		}

		try {
			BotContext context = manager.getContext();

			context.clearCookies();

			Map<String, String> params = Maps.newHashMap();
			params.put("username", username);
			params.put("password", password);
			params.put("rememberusername", "1");
			params.put("anchor", "");
			HttpPost request = context.buildPostRequest(EosVstuBot.SITE + "/login/index.php", params);
			Document document = context.executeRequestAndParseResponse(request);

			String notice = document.select("#page #page-content #region-main #notice p").text();
			if (!Strings.isNullOrEmpty(notice)) {
				throw new IOException(notice);
			}

			String loginErrorMessage =
					document.select("#page #page-content #region-main div.loginpanel div.loginerrors a")
							.text();
			if (!Strings.isNullOrEmpty(loginErrorMessage)) {
				throw new IOException(loginErrorMessage);
			}

			parseProfile(document);

			for (Cookie cookie : context.getCookies()) {
				String cookieName = cookie.getName();
				if (cookieName.equals(COOKIE_MID_NAME) || cookieName.equals(COOKIE_SESSION_NAME)) {
					properties.setProperty(cookieName, cookie.getValue());
				}
			}

			Log.info("%s successfully logged in", this);
			valid = true;
		} finally {
			save();
		}
	}
}
