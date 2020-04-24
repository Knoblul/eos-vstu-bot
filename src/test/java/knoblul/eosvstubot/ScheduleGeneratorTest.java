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

import com.google.common.base.Stopwatch;
import knoblul.eosvstubot.api.BotContext;
import knoblul.eosvstubot.api.profile.Profile;
import knoblul.eosvstubot.utils.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 23.04.2020 19:50
 *
 * @author Knoblul
 */
public class ScheduleGeneratorTest {
	public static void main(String[] args) throws IOException {
		Log.info("Starting schedule generation...");
		Stopwatch sw = Stopwatch.createStarted();
		BotContext context = new BotContext();
		context.create();

		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("lessons.json"))) {
			Profile profile = new Profile();
			profile.setCredentials("user", "pass");
			context.getProfileManager().loginProfile(profile);
			VolgasuScheduleGenerator.generateScheduleJson(context, "2%3B3%3B61", writer);
		} finally {
			context.destroy();
		}
		sw.stop();
		Log.info("Schedule generated. Total time elapsed: %d sec", sw.elapsed(TimeUnit.SECONDS));
	}
}
