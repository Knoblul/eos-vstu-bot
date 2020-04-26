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

import knoblul.eosvstubot.api.BotConstants;
import knoblul.eosvstubot.api.BotContext;
import knoblul.eosvstubot.gui.BotMainWindow;
import knoblul.eosvstubot.utils.Log;

/**
 * Точка входа в программу. Этот класс создает
 * основной функционал бота и его пользовательский графический интерфейс.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 12:17
 * @author Knoblul
 */
public class BotGUI {
	public static void main(String[] args) {
		// пишет в консоль текущую версию
		Log.info("Bot version: v%s", BotConstants.VERSION);

		BotContext context = new BotContext();
		try {
			// создаем контекст и грузим менеджеры
			context.create();
			context.loadManagers();

			// создаем и открываем гуи компоненты
			BotMainWindow window = context.registerHandler(BotMainWindow.class);
			window.setVisible(true);

			// кормим поток контексту
			context.occupyMainThread();
		} finally {
			// после того, как контекст отпустил поток - удаляем окно
			if (BotMainWindow.instance != null) {
				BotMainWindow.instance.setVisible(false);
				BotMainWindow.instance.dispose();
			}

			context.destroy();
		}
	}
}
