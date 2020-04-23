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
package knoblul.eosvstubot;

import knoblul.eosvstubot.backend.BotContext;
import knoblul.eosvstubot.frontend.BotWindow;
import knoblul.eosvstubot.utils.BotConfig;
import knoblul.eosvstubot.utils.Log;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 12:17
 * @author Knoblul
 */
public class EosVstuBot {
	public static final String NAME = "EosVstuBot";
	public static final String VERSION = "0.1.1";
	public static final String SITE_DOMAIN = "eos.vstu.ru";

	public static void main(String[] args) {
		Log.info("%s v%s", NAME, VERSION);
		BotConfig.instance.load();

		BotContext context = new BotContext();
		try {
			context.create();
			context.loadManagers();
			new BotWindow(context);
			context.processMainThreadCommands();
		} finally {
			context.destroy();
		}
	}
}
