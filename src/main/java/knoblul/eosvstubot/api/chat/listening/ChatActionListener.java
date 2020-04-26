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
package knoblul.eosvstubot.api.chat.listening;

import knoblul.eosvstubot.api.chat.ChatConnection;
import knoblul.eosvstubot.api.chat.action.ChatAction;

/**
 * Листенер чат-событий, пришедших от чат-подключения.
 * Чат подключение получает чат-события, которые приходят от сервера,
 * обрабатывает те, что важны для жизненного цикла, а другие,
 * такие как сообщения, информация о чаттерах - отправляет на листенеры.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 25.04.2020 20:24
 * @author Knoblul
 */
public interface ChatActionListener {
	/**
	 * Вызывается при получении чат-подключением новго события от сервера.
	 * @param connection чат-подключение, от которого произошел вызов метода.
	 * @param action чат-событие
	 */
	void action(ChatConnection connection, ChatAction action);
}
