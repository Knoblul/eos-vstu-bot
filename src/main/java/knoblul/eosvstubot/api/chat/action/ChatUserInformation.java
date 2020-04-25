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
package knoblul.eosvstubot.api.chat.action;

import java.util.Objects;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 25.04.2020 17:18
 *
 * @author Knoblul
 */
@SuppressWarnings("unused") // json serialization
public class ChatUserInformation {
	private String name;
	private String url;
	private String picture;
	private String id;
	private transient boolean isBot;

	ChatUserInformation() {
	}

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	public String getPicture() {
		return picture;
	}

	public String getId() {
		return id;
	}

	public boolean isBot() {
		return isBot;
	}

	public void setIsBot(boolean isBot) {
		this.isBot = isBot;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ChatUserInformation that = (ChatUserInformation) o;
		return name.equals(that.name) &&
				url.equals(that.url) &&
				picture.equals(that.picture) &&
				id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, url, picture, id);
	}
}
