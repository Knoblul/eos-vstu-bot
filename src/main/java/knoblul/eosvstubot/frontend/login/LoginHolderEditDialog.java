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
package knoblul.eosvstubot.frontend.login;

import knoblul.eosvstubot.backend.login.LoginHolder;
import knoblul.eosvstubot.backend.login.LoginManager;
import knoblul.eosvstubot.frontend.misc.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 22.04.2020 10:47
 * @author Knoblul
 */
public class LoginHolderEditDialog extends JComponent {
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JTextField chatPhrasesField;

	public LoginHolderEditDialog() {
		fill();
	}

	private void fill() {
		setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets.set(2, 2, 2, 2);
		gbc.gridy = 0;
		add(new JLabel("Логин"), gbc);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(usernameField = new JTextField(20), gbc);
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridy++;
		add(new JLabel("Пароль"), gbc);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(passwordField = new JPasswordField("", 20), gbc);

		gbc.fill = GridBagConstraints.NONE;
		gbc.gridy++;
		add(new JLabel("Фразы"), gbc);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(chatPhrasesField = new JTextField(20), gbc);
//		((AbstractDocument) chatPhrasesField.getDocument()).setDocumentFilter(new SimpleDocumentFilter()
//				.filterChar(LoginHolder.CHAT_PHRASES_DELIMITER));
		chatPhrasesField.setToolTipText("Рандомные фразы, которые будет говорить бот" +
				" в чате. Разделяются символом '" + LoginHolder.CHAT_PHRASES_DELIMITER + "'. ");
	}

	public String getUsername() {
		return usernameField.getText().trim();
	}

	public String getPassword() {
		return new String(passwordField.getPassword());
	}

	public String getChatPhrases() {
		return chatPhrasesField.getText().trim();
	}

	public boolean showDialog(LoginManager loginManager, LoginHolder editingHolder) {
		if (editingHolder != null) {
			usernameField.setText(editingHolder.getUsername());
			passwordField.setText(editingHolder.getPassword());
			chatPhrasesField.setText(editingHolder.getChatPhrasesAsString());
		} else {
			usernameField.setText("");
			passwordField.setText("");
			chatPhrasesField.setText(LoginHolder.DEFAULT_CHAT_PHRASES);
		}

		String title = editingHolder == null ? "Создать пользователя" : "Изменить данные пользователя";
		while (true) {
			if (JOptionPane.showConfirmDialog(this, this, title,
					JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
				return false;
			}

			String username = getUsername();
			String password = getPassword();
			String chatPhrases = getChatPhrases();
			if (!username.isEmpty() && !password.isEmpty()) {
				if (loginManager.getLoginHolder(username) != editingHolder) {
					DialogUtils.showWarning("Пользователь с таким именем уже существует.");
					continue;
				}

				boolean needsRelogin;
				LoginHolder holder;
				if (editingHolder != null) {
					holder = editingHolder;
					// не перелогиниваем пользователя если логин и пароль не были изменены
					needsRelogin = !editingHolder.getUsername().equals(username) ||
							!editingHolder.getPassword().equals(password);
					editingHolder.setChatPhrasesFromString(chatPhrases);
					if (needsRelogin) {
						editingHolder.setCredentials(username, password);
					}
				} else {
					holder = loginManager.createLoginHolder(username, password);
					holder.setChatPhrasesFromString(chatPhrases);
					needsRelogin = true;
				}

				if (needsRelogin) {
					try {
						holder.login();
					} catch (IOException e) {
						DialogUtils.showError("Ошибка входа. Пожалуйста, повторите попытку.", e);
					}
				}

				return true;
			}
		}
	}
}
