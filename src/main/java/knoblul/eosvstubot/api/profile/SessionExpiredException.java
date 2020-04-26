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
package knoblul.eosvstubot.api.profile;

import knoblul.eosvstubot.api.profile.Profile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 26.04.2020 11:29
 * @author Knoblul
 */
public class SessionExpiredException extends IOException {
	public SessionExpiredException(@NotNull Profile profile) {
		super("Session for " + profile.getAlias() + " expired");
	}
}
