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
 * Этот класс предназначен для управления списком холдеров.
 * Так же содержит в себе ключевые поля, без которых не сможет
 * работать ни один холдер.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 16:49
 * @author Knoblul
 */
public class LoginManager {
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
	private List<LoginHolder> loginHolders = Lists.newArrayList();

	/**
	 * Флаг, который сигнализирует о том, что некоторые холдеры не
	 * были залогинены на сайт при чтении с диска.
	 */
	private boolean someHoldersAreInvalid;

	public LoginManager(BotContext context) {
		this.context = context;
		this.workDir = Paths.get("Logins");
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

	public boolean isSomeHoldersAreInvalid() {
		return someHoldersAreInvalid;
	}

	public List<LoginHolder> getLoginHolders() {
		return loginHolders;
	}

	/**
	 * Инициализирует менеджер, читает все холдеры с диска и записывает их
	 * в список {@link #loginHolders}.
	 */
	public void create() {
		Log.info("Loading logins from folder...");

		loginHolders.clear();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(workDir, "*.login")) {
			for (Path file: ds) {
				loginHolders.add(new LoginHolder(this, file));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		Log.info("Checking logins...");
		loginHolders.forEach(LoginHolder::check);

		someHoldersAreInvalid = false;
		loginHolders.forEach(holder -> someHoldersAreInvalid |= !holder.isValid());
	}

	/**
	 * Находит экземпляр холдера в списке по логину пользователя.
	 * @param username логин пользователя
	 * @return холдер из списка по логину, либо <code>null</code>
	 */
	@Nullable
	public LoginHolder getLoginHolder(@NotNull String username) {
		for (LoginHolder holder: loginHolders) {
			if (holder.getUsername().equals(username)) {
				return holder;
			}
		}
		return null;
	}

	/**
	 * Находит экземпляр холдера в списке по его индексу.
	 * @param index индекс холдера в списке
	 * @return холдер из списка по индексу, либо <code>null</code>
	 */
	@Nullable
	public LoginHolder getLoginHolder(int index) {
		return index >= 0 && index < loginHolders.size() ? loginHolders.get(index) : null;
	}

	/**
	 * Создает новый холдер с указанными данными и записывает в список.
	 * @param username логин пользователя
	 * @param password пароль пользователя
	 * @throws IllegalArgumentException если username уже используется
	 * @return новосозданный холдер
	 */
	@NotNull
	public LoginHolder createLoginHolder(@NotNull String username, @NotNull String password) {
		if (getLoginHolder(username) != null) {
			throw new IllegalArgumentException("User with that username already exists");
		}

		Path propertiesFile = Paths.get(workDir.toString(), username + LoginHolder.LOGIN_FILE_EXT);
		LoginHolder holder = new LoginHolder(this, propertiesFile);
		holder.setCredentials(username, password);
		loginHolders.add(holder);
		return holder;
	}

	/**
	 * Удаляет экземпляр холдера из списка а так же данные о нем.
	 * @param holder холдер, который необходимо удалить
	 */
	public void removeLoginHolder(@NotNull LoginHolder holder) {
		if (loginHolders.remove(holder)) {
			holder.invalidate();
		}
	}
}
