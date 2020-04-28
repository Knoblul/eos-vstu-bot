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

import com.google.common.base.Splitter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Представление пользователя, используемое
 * для подключения к сайту или чату.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 13:40
 * @author Knoblul
 */
public class Profile {
	public static final String CHAT_PHRASES_DELIMITER = "|";
	public static final String[] DEFAULT_CHAT_PHRASES = new String[] { "+" };
	public static final int DEFAULT_MAXIMUM_LATE_TIME = 15*60*1000; // 15 минут

	/**
	 * Логин профиля eos.vstu.ru
	 */
	private String username = "";

	/**
	 * Пароль профиля eos.vstu.ru
	 */
	private String password = "";

	/**
	 * Фразы, которые бот должен говорить в чате от лица этого профиля.
	 */
	private String[] chatPhrases = DEFAULT_CHAT_PHRASES;

	/**
	 * Максимальное время, на которое бот может "опоздать"
	 */
	private long maximumLateTime = DEFAULT_MAXIMUM_LATE_TIME;

	/**
	 * Имя профиля eos.vstu.ru
	 * Значение этого поля должно устанавливаться только
	 * при успешном входе в аккаунт.
	 */
	private transient String profileName = "";

	/**
	 * Ссылка профиля eos.vstu.ru
	 * Значение этого поля должно устанавливаться только
	 * при успешном входе в аккаунт.
	 */
	private transient String profileLink = "";

	/**
	 * Айди профиля eos.vstu.ru.
	 * Достается из параметра id в {@link #profileLink}
	 * Значение этого поля должно устанавливаться только
	 * при успешном входе в аккаунт.
	 */
	private transient String profileId = "";

	/**
	 * Куки сессии eos.vstu.ru
	 */
	private String[] cookies = new String[] { "", "" };

	/**
	 * Флаг, значение которого <code>true</code> тогда, когда
	 * вход в аккаунт был выполнен успешно
	 */
	private transient boolean valid;

	public Profile() { }

	public Profile(String username, String password) {
		setCredentials(username, password);
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

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public String getProfileLink() {
		return profileLink;
	}

	public void setProfileLink(String profileLink) {
		this.profileLink = profileLink;
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	/**
	 * @return масив, хранящий фразы, которые бот должен
	 * говорить в чате от лица этого профиля.
	 */
	public String[] getChatPhrases() {
		return chatPhrases;
	}

	public long getMaximumLateTime() {
		return maximumLateTime;
	}

	public void setMaximumLateTime(long maximumLateTime) {
		this.maximumLateTime = maximumLateTime;
	}

	public String[] getCookies() {
		return cookies;
	}

	/**
	 * @see #getChatPhrases()
	 * @return фразы в формате строки, разделеные {@link #CHAT_PHRASES_DELIMITER}
	 */
	public String getChatPhrasesAsString() {
		return String.join(CHAT_PHRASES_DELIMITER, chatPhrases);
	}

	/**
	 * Создает массив фраз из указанной строки
	 * @param chatPhrases фразы в формате строки, разделеные {@link #CHAT_PHRASES_DELIMITER}
	 */
	public void setChatPhrasesFromString(String chatPhrases) {
		this.chatPhrases = Splitter.on(CHAT_PHRASES_DELIMITER).trimResults()
				.splitToList(chatPhrases).toArray(new String[0]);
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
		this.username = username;
		this.password = password;
	}

	/**
	 * @return <code>true</code>, если сессия на момент вызова метода действительна.
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * @param valid если <code>true</code>, то сессия помечена действительной
	 */
	public void setValid(boolean valid) {
		this.valid = valid;
	}

	/**
	 * Инвалидирует данный профиль. Все данные сессии, которые он хранил
	 * (куки, имя профиля, ссылку профиля) будут удалены.
	 */
	public void invalidate() {
		// отмечаем что сессия отсутсвует
		valid = false;
		// обнуляем имя профиля, ссылку профиля и куки
		profileName = "";
		profileLink = "";
		profileId = "";
		Arrays.fill(cookies, "");
	}

	@Override
	public String toString() {
		if (profileName == null || profileId == null) {
			return username + " (N/A)";
		}
		return username + " (" + profileName + "#" + profileId + ")";
	}
}
