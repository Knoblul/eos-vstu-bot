package knoblul.eosvstubot;

import knoblul.eosvstubot.backend.BotContext;
import knoblul.eosvstubot.backend.login.LoginHolder;
import knoblul.eosvstubot.frontend.BotUI;
import knoblul.eosvstubot.utils.Log;

import java.io.IOException;

/**
 * @author Knoblul
 * Created: 21.04.2020 12:17
 */
public class EosVstuBot {
	public static final String NAME = "EosVstuBot";
	public static final String VERSION = "0.1.1";
	public static final String SITE = "http://eos.vstu.ru";

	public static void main(String[] args) {
		Log.info("%s v%s", NAME, VERSION);
		BotContext context = new BotContext();
		try {
			context.create();
			new BotUI(context);
			context.processMainThreadCommands();
		} finally {
			context.destroy();
		}
	}
}
