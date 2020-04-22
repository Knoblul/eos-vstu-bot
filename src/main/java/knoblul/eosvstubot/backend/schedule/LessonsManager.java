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
package knoblul.eosvstubot.backend.schedule;

import com.google.common.collect.Sets;
import knoblul.eosvstubot.backend.BotContext;
import knoblul.eosvstubot.backend.login.LoginHolder;
import knoblul.eosvstubot.utils.Log;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Set;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 22.04.2020 12:28
 * @author Knoblul
 */
public class LessonsManager {
	private final BotContext context;
	private final Path workDir;

	private Set<Lesson> lessons = Sets.newHashSet();

	public LessonsManager(BotContext context) {
		this.context = context;
		this.workDir = Paths.get("Lessons");
		if (!Files.exists(workDir)) {
			try {
				Files.createDirectories(workDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void create() {
		Log.info("Loading lessons from folder...");
		lessons.clear();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(workDir, "*.lesson")) {
			for (Path file: ds) {
				lessons.add(new Lesson(file));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void update() {
		Calendar calendar = Calendar.getInstance();

	}
}
