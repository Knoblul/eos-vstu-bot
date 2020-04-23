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
package knoblul.eosvstubot.frontend;

import knoblul.eosvstubot.EosVstuBot;
import knoblul.eosvstubot.backend.BotContext;
import knoblul.eosvstubot.frontend.profile.ProfileManagerTable;
import knoblul.eosvstubot.utils.swing.DialogUtils;
import knoblul.eosvstubot.utils.swing.TextPaneAppender;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 20:53
 * @author Knoblul
 */
public class BotWindow extends JFrame {
	public static BotWindow instance;
	private final BotContext context;

	public BotWindow(BotContext context) {
		instance = this;
		this.context = context;

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable ignored) { }

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setTitle(EosVstuBot.NAME + " v" + EosVstuBot.VERSION);
		setSize(800, 600);
		setLocationRelativeTo(null);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				context.stopMainThreadCommandsProcessing();
			}
		});

		fill();
		setVisible(true);

		if (context.getProfileManager().isSomeHoldersAreInvalid()) {
			DialogUtils.showWarning("Один или несколько пользователей не были загружены." +
					" Смотрите консоль, чтобы узнать детали.");
		}
	}

	private void fill() {
		setLayout(new BorderLayout());
		JTabbedPane tabs = new JTabbedPane();

		tabs.addTab("Пользователи", new ProfileManagerTable(context.getProfileManager()));

		JTextPane console = TextPaneAppender.consoleComponent;
		console.setFont(new Font("Consolas", Font.PLAIN, 12));
		tabs.addTab("Консоль", new JScrollPane(console, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		add(tabs, BorderLayout.CENTER);
	}
}

