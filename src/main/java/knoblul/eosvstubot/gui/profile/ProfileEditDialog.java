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
import knoblul.eosvstubot.gui.scripting.ScriptEditorDialog;
import knoblul.eosvstubot.utils.swing.DialogUtils;
import knoblul.eosvstubot.utils.swing.TimeChooser;

import javax.script.ScriptException;
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
	private static ScriptEditorDialog scriptEditorDialog = new ScriptEditorDialog();

	private JTextField usernameField;
	private JPasswordField passwordField;
	private TimeChooser lateTimeChooser;
	private String chatJoinScriptContent;

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
		add(new JLabel("Чат-скрипт"), gbc);
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		JButton scriptEditButton;
		add(scriptEditButton = new JButton("Редактировать..."), gbc);
		scriptEditButton.addActionListener((e) -> {
			scriptEditorDialog.setEditingScript(chatJoinScriptContent);
			scriptEditorDialog.setVisible(true);
			chatJoinScriptContent = scriptEditorDialog.getEditingScript();
		});
		scriptEditButton.setToolTipText("Чат скрипт нужен для детальной настройки поведения бота в чате от имени профиля.");
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

	void showDialog(ProfileManager profileManager, Profile editingProfile, Runnable swingUpdateCallback) {
		if (editingProfile != null) {
			usernameField.setText(editingProfile.getUsername());
			passwordField.setText(editingProfile.getPassword());
			chatJoinScriptContent = editingProfile.getChatScript().getContent();
			lateTimeChooser.setTimeMillis(editingProfile.getMaximumLateTime());
		} else {
			usernameField.setText("");
			passwordField.setText("");
			chatJoinScriptContent = ProfileTable.getDefaultChatJoinScriptContent();
			lateTimeChooser.setTimeMillis(Profile.DEFAULT_MAXIMUM_LATE_TIME);
		}

		String title = editingProfile == null ? "Создать пользователя" : "Изменить данные пользователя";
		while (JOptionPane.showConfirmDialog(BotMainWindow.instance, this, title,
				JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {

			String username = usernameField.getText().trim();
			String password = new String(passwordField.getPassword());
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

					String prevScriptContent = profile.getChatScript().getContent();
					profile.getChatScript().setContent(chatJoinScriptContent);
					if (!prevScriptContent.equals(chatJoinScriptContent)) {
						try {
							profile.getChatScript().recompile();
						} catch (ScriptException e) {
							e.printStackTrace();
						}
					}

					profile.setMaximumLateTime(lateTime);

					if (needsLogin) {
						try {
							profileManager.loginProfile(profile);
						} catch (IOException e) {
							DialogUtils.showError("Ошибка входа. Пожалуйста, повторите попытку.",
									e, true);
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
