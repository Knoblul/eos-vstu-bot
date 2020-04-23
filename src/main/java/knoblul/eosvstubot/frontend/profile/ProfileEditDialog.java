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
package knoblul.eosvstubot.frontend.profile;

import knoblul.eosvstubot.backend.profile.Profile;
import knoblul.eosvstubot.backend.profile.ProfileManager;
import knoblul.eosvstubot.frontend.BotWindow;
import knoblul.eosvstubot.utils.swing.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 22.04.2020 10:47
 * @author Knoblul
 */
class ProfileEditDialog extends JComponent {
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JTextField chatPhrasesField;

	ProfileEditDialog() {
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
				" в чате. Разделяются символом '" + Profile.CHAT_PHRASES_DELIMITER + "'. ");
	}

	String getUsername() {
		return usernameField.getText().trim();
	}

	String getPassword() {
		return new String(passwordField.getPassword());
	}

	String getChatPhrases() {
		return chatPhrasesField.getText().trim();
	}

	boolean showDialog(ProfileManager profileManager, Profile editingProfile) {
		if (editingProfile != null) {
			usernameField.setText(editingProfile.getUsername());
			passwordField.setText(editingProfile.getPassword());
			chatPhrasesField.setText(editingProfile.getChatPhrasesAsString());
		} else {
			usernameField.setText("");
			passwordField.setText("");
			chatPhrasesField.setText(String.join(""+Profile.CHAT_PHRASES_DELIMITER,
					Profile.DEFAULT_CHAT_PHRASES));
		}

		String title = editingProfile == null ? "Создать пользователя" : "Изменить данные пользователя";
		while (true) {
			if (JOptionPane.showConfirmDialog(BotWindow.instance, this, title,
					JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
				return false;
			}

			String username = getUsername();
			String password = getPassword();
			String chatPhrases = getChatPhrases();
			if (!username.isEmpty() && !password.isEmpty()) {
				Profile listedProfile = profileManager.getProfile(username);
				if (listedProfile != null && listedProfile != editingProfile) {
					DialogUtils.showWarning("Пользователь с таким именем уже существует.");
					continue;
				}

				boolean needsLogin;
				Profile profile;
				if (editingProfile != null) {
					profile = editingProfile;
					// не перелогиниваем пользователя если логин и пароль не были изменены
					needsLogin = !editingProfile.getUsername().equals(username) ||
							!editingProfile.getPassword().equals(password);
					editingProfile.setChatPhrasesFromString(chatPhrases);
					if (needsLogin) {
						profileManager.logoutProfile(profile);
						editingProfile.setCredentials(username, password);
					}
				} else {
					profile = profileManager.createProfile(username, password);
					profile.setChatPhrasesFromString(chatPhrases);
					needsLogin = true;
				}

				if (needsLogin) {
					try {
						profileManager.loginProfile(profile);
					} catch (IOException e) {
						DialogUtils.showError("Ошибка входа. Пожалуйста, повторите попытку.", e);
					}
				}
				profileManager.save();
				return true;
			}
		}
	}
}
