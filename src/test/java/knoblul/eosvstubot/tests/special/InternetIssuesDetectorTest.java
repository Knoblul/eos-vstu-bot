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
package knoblul.eosvstubot.tests.special;

import com.google.common.base.Stopwatch;
import knoblul.eosvstubot.api.BotConstants;
import knoblul.eosvstubot.utils.Log;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 28.04.2020 19:46
 *
 * @author Knoblul
 */
public class InternetIssuesDetectorTest extends Assert {
	@Test
	public void testInternetIssuesDetector() {
		while (true) {
			Stopwatch sw = Stopwatch.createStarted();
			boolean reachable = false;
			try {
				reachable = InetAddress.getByName(BotConstants.SITE_DOMAIN).isReachable(15000);
			} catch (Throwable ignored) {}
			Log.info("Reachable: %s", reachable);
			sw.stop();

			long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
			if (5000 - elapsed > 0) {
				try {
					Thread.sleep(5000 - elapsed);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}
}
