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
package knoblul.eosvstubot.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 23.04.2020 14:34
 * @author Knoblul
 */
public class BotConfig {
	public static final BotConfig instance = new BotConfig();

	private final Path propertiesFile;
	private final Properties properties;

	@PropertyField(defaultValue = "1")
	private int firstWeekOfYearIndex;

	public BotConfig() {
		propertiesFile = Paths.get("config.cfg");
		properties = new Properties();
	}

	public int getFirstWeekOfYearIndex() {
		// clamp(firstWeekOfYearIndex, 1, 2);
		return firstWeekOfYearIndex = Math.max(Math.min(firstWeekOfYearIndex, 2), 1);
	}

	public void setFirstWeekOfYearIndex(int firstWeekOfYearIndex) {
		this.firstWeekOfYearIndex = Math.max(Math.min(firstWeekOfYearIndex, 2), 1);
	}

	public void load() {
		PropertiesHelper.load(BotConfig.class, this, propertiesFile);
	}

	public void save() {
		PropertiesHelper.save(BotConfig.class, this, propertiesFile);
	}
}
