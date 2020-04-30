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
package knoblul.eosvstubot.gui;

import knoblul.eosvstubot.api.BotConstants;
import knoblul.eosvstubot.api.BotContext;
import knoblul.eosvstubot.api.BotHandler;
import knoblul.eosvstubot.api.schedule.ScheduledConnectionsHandler;
import knoblul.eosvstubot.api.profile.Profile;
import knoblul.eosvstubot.gui.chat.ChatComponent;
import knoblul.eosvstubot.gui.profile.ProfileTable;
import knoblul.eosvstubot.gui.schedule.ScheduleManagerComponent;
import knoblul.eosvstubot.utils.swing.DialogUtils;
import knoblul.eosvstubot.utils.swing.TextPaneAppender;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Основное окно бота.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 20:53
 * @author Knoblul
 */
public class BotMainWindow extends JFrame implements BotHandler {
	public static BotMainWindow instance;
	private final BotContext context;
	private final ScheduledConnectionsHandler scheduledConnectionsHandler;

	private JTabbedPane tabs;
	private ScheduleManagerComponent scheduleManagerComponent;
	private JComponent consoleComponent;
	private JComponent chatComponent;
	private ProfileTable profileTable;

	@SuppressWarnings("unused")
	public BotMainWindow(@NotNull BotContext context) {
		instance = this;
		this.context = context;
		this.scheduledConnectionsHandler = context.registerHandler(ScheduledConnectionsHandler.class);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable ignored) { }

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setTitle(BotConstants.NAME + " v" + BotConstants.VERSION);
		setSize(800, 600);
		setLocationRelativeTo(null);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				context.stopMainThreadCommandsProcessing();
			}
		});

		fill();

		// если в списке профилей хотя бы один не смог зайти на сайт,
		// то выводим предупреждение
		if (!context.getProfileManager().getProfiles().stream().allMatch(Profile::isValid)) {
			DialogUtils.showWarning("Один или несколько пользователей не были загружены." +
					" Смотрите консоль, чтобы узнать детали.");
		}
	}

	private void fill() {
		setLayout(new BorderLayout());
		tabs = new JTabbedPane();
		tabs.addTab("Пользователи", profileTable = new ProfileTable(context.getProfileManager()));
		tabs.addTab("Расписание", scheduleManagerComponent = new ScheduleManagerComponent(context.getLessonsManager()));
		tabs.addTab("Чаты", chatComponent = new ChatComponent(scheduledConnectionsHandler));
		tabs.addTab("Консоль", consoleComponent = new JScrollPane(TextPaneAppender.consoleComponent,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,  JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));

		int consoleIndex = tabs.indexOfComponent(consoleComponent);
		Color originalConsoleColor = tabs.getForegroundAt(consoleIndex);
		tabs.addChangeListener(cl -> {
			if (tabs.getSelectedIndex() == consoleIndex) {
				tabs.setForegroundAt(consoleIndex, originalConsoleColor);
			}
		});

		int chatIndex = tabs.indexOfComponent(chatComponent);
		Color originalChatColor = tabs.getForegroundAt(chatIndex);
		tabs.addChangeListener(cl -> {
			if (tabs.getSelectedIndex() == chatIndex) {
				tabs.setForegroundAt(chatIndex, originalChatColor);
			}
		});

		if (TextPaneAppender.wasConsoleErrors) {
			tabs.setForegroundAt(consoleIndex, Color.RED.brighter().brighter());
		}

		add(tabs, BorderLayout.CENTER);
	}

	public void markConsoleErrors() {
		SwingUtilities.invokeLater(() -> {
			try {
				int consoleIndex = tabs.indexOfComponent(consoleComponent);
				if (tabs.getSelectedIndex() != consoleIndex) {
					tabs.setForegroundAt(consoleIndex, new Color(190, 0, 0));
				}
			} catch (Throwable ignored) { }
		});
	}

	public void markChatNotifies() {
		SwingUtilities.invokeLater(() -> {
			try {
				int chatIndex = tabs.indexOfComponent(chatComponent);
				if (tabs.getSelectedIndex() != chatIndex) {
					tabs.setForegroundAt(chatIndex, new Color(190, 150, 20));
				}
			} catch (Throwable ignored) { }
		});
	}

	public void updateProfileTable() {
		if (profileTable != null) {
			profileTable.refresh();
		}
	}

	@Override
	public void update() {
		scheduleManagerComponent.update();
	}
}

