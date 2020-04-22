package knoblul.eosvstubot.frontend;

import knoblul.eosvstubot.EosVstuBot;
import knoblul.eosvstubot.backend.BotContext;
import knoblul.eosvstubot.frontend.login.LoginManagerTable;
import knoblul.eosvstubot.frontend.misc.DialogUtils;
import knoblul.eosvstubot.frontend.misc.TextPaneAppender;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Knoblul
 * Created: 21.04.2020 20:53
 */
public class BotUI extends JFrame {
	private final BotContext context;

	public BotUI(BotContext context) {
		this.context = context;

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable ignored) { }

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

		if (context.getLoginManager().isSomeHoldersAreInvalid()) {
			DialogUtils.showWarning("Один или несколько пользователей не были загружены." +
					" Смотрите консоль, чтобы узнать детали.");
		}
	}

	private void fill() {
		setLayout(new BorderLayout());
		JTabbedPane tabs = new JTabbedPane();

		tabs.addTab("Пользователи", new LoginManagerTable(context.getLoginManager()));

		JTextPane console = TextPaneAppender.consoleComponent;
		console.setFont(new Font("Consolas", Font.PLAIN, 12));
		tabs.addTab("Консоль", new JScrollPane(console, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		add(tabs, BorderLayout.CENTER);
	}
}

