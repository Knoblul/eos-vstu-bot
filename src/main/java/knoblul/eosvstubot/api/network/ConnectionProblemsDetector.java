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
package knoblul.eosvstubot.api.network;

import com.google.common.base.Stopwatch;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Простой детектор падений сети/сайта.
 * В основной конструктор передеается имя хоста и коллбек.
 * Каждые {@link #PING_PERIOD_TIME} миллисекунд в отдельном потоке этот
 * класс пингует хост и передает результат в коллбек вместе с {@link #badAttempts}.
 * Если пинг неудачен - счетчик {@link #badAttempts} увеличивается,
 * а если удачен - сбрасывается.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 26.04.2020 1:04
 * @author Knoblul
 */
public class ConnectionProblemsDetector implements Runnable {
	/**
	 * Частота выполнения пинг-запроса.
	 * Если пинг-запрос исполнялся дольше этого значения,
	 * то "спячка" потока пропускается и выполняется следующий пинг-запрос.
	 */
	private static final int PING_PERIOD_TIME = 3000;

	/**
	 * Таймаут для отправки пинг-запроса на хост.
	 * По истечению этого времени если хост не отправил
	 * ответ, то попытка будет считаться неудачной.
	 */
	private static final int PING_TIMEOUT = 10000;

	/**
	 * Хост, который пинговать.
	 */
	private final String hostToPing;

	/**
	 * Коллбек, который вызывается после завершения
	 * попытки пинг-запроса.
	 */
	private final BiConsumer<Integer, Boolean> updateCallback;

	/**
	 * Поток, в котором происходит цикл попыток
	 */
	private final Thread thread;

	/**
	 * Флаг, который
	 */
	private volatile boolean running;

	/**
	 * Счетчик "плохих" попыток
	 */
	private int badAttempts;

	public ConnectionProblemsDetector(String hostToPing, BiConsumer<Integer, Boolean> updateCallback) {
		this.hostToPing = hostToPing;
		this.updateCallback = updateCallback;
		thread = new Thread(this);
		thread.setName("Internet Issues Detector");
		thread.start();
	}

	/**
	 * Останавливает поток
	 */
	public void destroy() {
		running = false;
		thread.interrupt();
	}

	@Override
	public void run() {
		running = true;
		while (running) {
			Stopwatch sw = Stopwatch.createStarted();
			boolean reachable = false;
			try {
				reachable = InetAddress.getByName(hostToPing).isReachable(PING_TIMEOUT);
			} catch (Throwable ignored) { }
			sw.stop();

			if (!reachable) {
				// накапливаем неудачные попытки при неудаче
				badAttempts++;
			}

			// отправляем результаты в коллбек
			updateCallback.accept(badAttempts, reachable);

			if (reachable) {
				// сбрасываем неудачные попытки при удаче
				badAttempts = 0;
			}

			// консервативный алгоритм для экономии времени между запросами.
			long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
			if (PING_PERIOD_TIME - elapsed > 0) {
				try {
					Thread.sleep(PING_PERIOD_TIME - elapsed);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}
}
