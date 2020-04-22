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
package knoblul.eosvstubot.utils;

import knoblul.eosvstubot.EosVstuBot;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintStream;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 14:00
 * @author Knoblul
 */
public class Log {
	private static final Logger LOGGER = LogManager.getLogger(EosVstuBot.NAME);

	private static class LoggingPrintStream extends PrintStream {
		private final Logger logger;
		private final Level level;

		public LoggingPrintStream(PrintStream original, Logger logger, Level level) {
			super(original);
			this.logger = logger;
			this.level = level;
		}

		@Override
		public void println(Object o) {
			logger.log(level, o);
		}

		@Override
		public void println(String s) {
			logger.log(level, s);
		}
	}

	static {
		Logger logger = LogManager.getLogger("SYSTEM");
		System.setErr(new LoggingPrintStream(System.err, logger, Level.ERROR));
		System.setOut(new LoggingPrintStream(System.out, logger, Level.INFO));
	}

	public static void log(Level level, Throwable t, String msg, Object... args) {
		if (t != null) {
			LOGGER.log(level, () -> args.length > 0 ? String.format(msg, args) : msg, t);
		} else {
			LOGGER.log(level, () -> args.length > 0 ? String.format(msg, args) : msg);
		}
	}

	public static void error(Throwable t, String msg, Object... args) {
		log(Level.ERROR, t, msg, args);
	}

	public static void error(String msg, Object... args) {
		error(null, msg, args);
	}

	public static void warn(Throwable t, String msg, Object... args) {
		log(Level.WARN, t, msg, args);
	}

	public static void warn(String msg, Object... args) {
		warn(null, msg, args);
	}

	public static void info(String msg, Object... args) {
		log(Level.INFO, null, msg, args);
	}

	public static void trace(String msg, Object... args) {
		log(Level.TRACE, null, msg, args);
	}
}
