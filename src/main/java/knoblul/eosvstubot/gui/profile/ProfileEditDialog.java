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
package knoblul.eosvstubot.gui.profile;

import knoblul.eosvstubot.api.profile.Profile;
import knoblul.eosvstubot.api.profile.ProfileManager;
import knoblul.eosvstubot.gui.BotMainWindow;
import knoblul.eosvstubot.utils.swing.DialogUtils;
import knoblul.eosvstubot.utils.swing.TimeChooser;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * Диалог для редактирования данных {@link Profile}.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 22.04.2020 10:47
 * @author Knoblul
 */
class ProfileEditDialog extends JComponent {
	private JTextField usernameField;
	private JPasswordField passwordField;
	private JTextField chatPhrasesField;
	private TimeChooser lateTimeChooser;

	ProfileEditDialog() {
		fill();
	}

	private void fill() {
		setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets.set(2, 2, 2, 2);
		gbc.gridy = 0;

		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		add(new JLabel("Логин"), gbc);
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		add(usernameField = new JTextField(20), gbc);
		usernameField.setToolTipText("Логин профиля eos.vstu.ru");
		gbc.weightx = 0;
		gbc.gridy++;

		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		add(new JLabel("Пароль"), gbc);
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		add(passwordField = new JPasswordField("", 20), gbc);
		passwordField.setToolTipText("Пароль профиля eos.vstu.ru");
		gbc.weightx = 0;
		gbc.gridy++;

		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		add(new JLabel("Фразы"), gbc);
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		add(chatPhrasesField = new JTextField(20), gbc);
		chatPhrasesField.setToolTipText("Рандомные фразы, которые будет говорить бот" +
				" в чате. Разделяются с помощью '" + Profile.CHAT_PHRASES_DELIMITER + "'. ");
		gbc.weightx = 0;
		gbc.gridy++;

		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		add(new JLabel("Макс. опоздание"), gbc);
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		add(lateTimeChooser = new TimeChooser(), gbc);
		lateTimeChooser.setToolTipText("Максимальное время, на которое может опоздать бот (от нуля до указанного)");
		gbc.weightx = 0;
		gbc.gridy++;
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

	void showDialog(ProfileManager profileManager, Profile editingProfile, Runnable swingUpdateCallback) {
		if (editingProfile != null) {
			usernameField.setText(editingProfile.getUsername());
			passwordField.setText(editingProfile.getPassword());
			chatPhrasesField.setText(editingProfile.getChatPhrasesAsString());
			lateTimeChooser.setTimeMillis(editingProfile.getMaximumLateTime());
		} else {
			usernameField.setText("");
			passwordField.setText("");
			chatPhrasesField.setText(String.join(Profile.CHAT_PHRASES_DELIMITER, Profile.DEFAULT_CHAT_PHRASES));
			lateTimeChooser.setTimeMillis(Profile.DEFAULT_MAXIMUM_LATE_TIME);
		}

		String title = editingProfile == null ? "Создать пользователя" : "Изменить данные пользователя";
		while (JOptionPane.showConfirmDialog(BotMainWindow.instance, this, title,
				JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {

			String username = getUsername();
			String password = getPassword();
			String chatPhrases = getChatPhrases();
			long lateTime = lateTimeChooser.getTimeMillis();
			if (!username.isEmpty() && !password.isEmpty()) {
				Profile listedProfile = profileManager.getProfile(username);
				if (listedProfile != null && listedProfile != editingProfile) {
					DialogUtils.showWarning("Пользователь с таким именем уже существует.");
					continue;
				}

				profileManager.getContext().invokeMainThreadCommand(() -> {
					boolean needsLogin;
					Profile profile;
					if (editingProfile != null) {
						profile = editingProfile;
						// не перелогиниваем пользователя если логин и пароль не были изменены
						needsLogin = !editingProfile.getUsername().equals(username) ||
								!editingProfile.getPassword().equals(password);
						if (needsLogin) {
							profileManager.logoutProfile(profile);
							editingProfile.setCredentials(username, password);
						}
					} else {
						profile = profileManager.createProfile(username, password);
						needsLogin = true;
					}

					profile.setChatPhrasesFromString(chatPhrases);
					profile.setMaximumLateTime(lateTime);

					if (needsLogin) {
						try {
							profileManager.loginProfile(profile);
						} catch (IOException e) {
							DialogUtils.showError("Ошибка входа. Пожалуйста, повторите попытку.", e, true);
						}
					}

					profileManager.save();
					SwingUtilities.invokeLater(swingUpdateCallback);
				});

				break;
			}
		}
	}
}
