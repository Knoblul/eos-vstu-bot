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

/**
 * Листенер событий, связанных с чат-подключениями.
 * Отлавливает момент успешного входа в чат и
 * все ошибки, возникшие внутри чат-подключения.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 25.04.2020 20:24
 * @author Knoblul
 */
public interface ChatConnectionListener {
	/**
	 * Вызывается при успешном входе в чат.
	 * @param connection чат-подключение, от которого произошел вызов метода.
	 */
	void connected(ChatConnection connection);

	/**
	 * Вызывается при исключении, возникшем во время попытки входа в чат,
	 * либо на момент обработки чат-подключения.
	 * @param connection чат-подключение, от которого произошел вызов метода.
	 * @param error исключение, которое возникло внутри чат-подключения.
	 */
	void error(ChatConnection connection, Throwable error);
}
