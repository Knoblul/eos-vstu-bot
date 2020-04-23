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

import com.google.common.collect.Lists;
import knoblul.eosvstubot.backend.BotContext;
import knoblul.eosvstubot.utils.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
	/**
	 * Контекст бота
	 */
	private final BotContext context;

	/**
	 * Папка, в которую сохраняются холдеры
	 */
	private final Path workDir;

	/**
	 * Список всех холдеров, зарегестрированых менеджером.
	 */
	private List<Profile> profiles = Lists.newArrayList();

	/**
	 * Флаг, который сигнализирует о том, что некоторые холдеры не
	 * были залогинены на сайт при чтении с диска.
	 */
	private boolean someProfilesAreInvalid;

	public ProfileManager(BotContext context) {
		this.context = context;
		this.workDir = Paths.get("Profiles");
		if (!Files.exists(workDir)) {
			try {
				Files.createDirectories(workDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public BotContext getContext() {
		return context;
	}

	public Path getWorkDir() {
		return workDir;
	}

	public boolean isSomeProfilesAreInvalid() {
		return someProfilesAreInvalid;
	}

	public List<Profile> getProfiles() {
		return profiles;
	}

	/**
	 * Инициализирует менеджер, читает все профили с диска и записывает их
	 * в список {@link #profiles}.
	 */
	public void load() {
		Log.info("Loading profiles from folder...");

		profiles.clear();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(workDir, "*"+Profile.PROFILE_FILE_EXT)) {
			for (Path file: ds) {
				profiles.add(new Profile(this, file));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		Log.info("Checking profiles...");
		profiles.forEach(Profile::check);

		someProfilesAreInvalid = false;
		profiles.forEach(profile -> someProfilesAreInvalid |= !profile.isOnline());
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

		Path propertiesFile = Paths.get(workDir.toString(), username + Profile.PROFILE_FILE_EXT);
		Profile profile = new Profile(this, propertiesFile);
		profile.setCredentials(username, password);
		profiles.add(profile);
		return profile;
	}

	/**
	 * Удаляет экземпляр профиля из списка а так же данные о нем.
	 * @param profile профиля, который необходимо удалить
	 */
	public void removeProfile(@NotNull Profile profile) {
		if (profiles.remove(profile)) {
			profile.delete();
		}
	}
}
