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
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 26.04.2020 1:04
 *
 * @author Knoblul
 */
public class ConnectionProblemsDetector implements Runnable {
	private static final int PING_PERIOD_TIME = 2000; // каждые 2 секунды пингуем хост
	private static final int PING_TIMEOUT = 10000; // 10 секунд достаточно для таймаута

	private final String hostToPing;
	private final BiConsumer<Integer, Boolean> updateCallback;
	private final Thread thread;
	private volatile boolean running;

	private int badAttempts;

	public ConnectionProblemsDetector(String hostToPing, BiConsumer<Integer, Boolean> updateCallback) {
		this.hostToPing = hostToPing;
		this.updateCallback = updateCallback;
		thread = new Thread(this);
		thread.setName("Internet Issues Detector");
		thread.start();
	}

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
				badAttempts++;
			}

			updateCallback.accept(badAttempts, reachable);

			if (reachable) {
				badAttempts = 0;
			}

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
