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
package knoblul.eosvstubot.backend;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import knoblul.eosvstubot.backend.profile.ProfileManager;
import knoblul.eosvstubot.backend.schedule.LessonsManager;
import knoblul.eosvstubot.utils.Log;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Простое асбтрагирование основных низкоуровневых действий бота.
 * Так же данный класс содержит в себе все необходимые данные для
 * управления аккаунтами.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 13:41
 * @author Knoblul
 */
public class BotContext {
	private static final int MAX_REDIRECTS = 10;

	private final Thread mainThread;
	private final ProfileManager profileManager;
	private final LessonsManager lessonsManager;

	private CookieStore cookieStore;
	private CloseableHttpClient client;

	private Queue<Runnable> mainThreadCommands = Queues.newConcurrentLinkedQueue();

	public BotContext() {
		mainThread = Thread.currentThread();
		if (!mainThread.getName().equals("main")) {
			throw new IllegalStateException("Context can be created only from the main thread!");
		}

		profileManager = new ProfileManager(this);
		lessonsManager = new LessonsManager(this);
	}
	
	private void check() {
		if (client == null) {
			throw new IllegalStateException("Context not created yet");
		}
	}

	/**
	 * Вызывает команду в основном потоке.
	 * @param command команда
	 */
	public void invokeMainThreadCommand(Runnable command) {
		if (Thread.currentThread() == mainThread) {
			try {
				command.run();
			} catch (Throwable t) {
				Log.warn(t,"Failed to execute command");
			}
		} else {
			mainThreadCommands.add(command);
		}
	}

	/**
	 * Запускает бексконечный цикл для обработки
	 * команд из очереди.
	 * Основное назначение - блокирование текущего потока и ожидание команд.
	 */
	public void processMainThreadCommands() {
		Log.info("Command processing started");
		while (true) {
			Runnable command = mainThreadCommands.poll();
			if (command != null) {
				invokeMainThreadCommand(command);
			}

			lessonsManager.update();

			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				Log.info("Command processing terminated");
				break;
			}
		}
	}

	/**
	 * Говорит основному потоку перестать обрабатывать команды.
	 */
	public void stopMainThreadCommandsProcessing() {
		mainThread.interrupt();
	}

	/**
	 * @return {@link #profileManager}
	 */
	public ProfileManager getProfileManager() {
		check();
		return profileManager;
	}

	/**
	 * @return {@link #lessonsManager}
	 */
	public LessonsManager getLessonsManager() {
		check();
		return lessonsManager;
	}

	/**
	 * Создает котекст. Далее все контекстные действия могут выполнятся
	 * вплоть до вызова {@link #destroy()}.
	 */
	public void create() {
		cookieStore = new BasicCookieStore();
		client = HttpClientBuilder.create()
				.setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(cookieStore)
				.build();
	}

	public void loadManagers() {
		profileManager.load();
		lessonsManager.load();
	}

	/**
	 * Выполняет указанный запрос и возвращает страницу в виде {@link Document},
	 * готовую для парсинга.
	 *
	 * @param request экземпляр настроенного запроса
	 * @return страницу в виде {@link Document}, готовую для парсинга.
	 * @throws IOException при возникновении ошибки во время получения/разбора ответа.
	 */
	@NotNull
	public Document executeRequestAndParseResponse(@NotNull HttpUriRequest request) throws IOException {
		check();

		try (CloseableHttpResponse response = client.execute(request)) {
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
				throw new IOException("Invalid status: " + statusLine.getStatusCode() + " "
						+ statusLine.getReasonPhrase());
			}
			Header contentTypeHeader = response.getEntity().getContentType();
			if (contentTypeHeader == null) {
				throw new IOException("'Content-Type' in response header is not specified");
			}

			String contentType = contentTypeHeader.getValue();
			if (!contentType.toLowerCase().contains("text/html")) {
				throw new IOException("Invalid 'Content-Type': " + contentType);
			}

			return Jsoup.parse(response.getEntity().getContent(), "UTF-8", request.getURI().toString());
		}
	}

	/**
	 * Очищает все куки из данного контекста
	 */
	public void clearCookies() {
		check();
		cookieStore.clear();
	}

	/**
	 * Получает куки по имени из тех, что хранит данный контекст
	 * @param name имя куки
	 * @return найденный куки или <code>null</code>
	 */
	@Nullable
	public Cookie getCookie(@NotNull String name) {
		check();

		for (Cookie cookie: cookieStore.getCookies()) {
			if (cookie.getName().equals(name)) {
				return cookie;
			}
		}
		return null;
	}

	/**
	 * Получает згачегие куки по имени из тех, что хранит данный контекст
	 * @param name имя куки
	 * @return значение куки или <code>null</code>
	 */
	@Nullable
	public String getCookieValue(@NotNull String name) {
		check();
		Cookie cookie = getCookie(name);
		return cookie != null ? cookie.getValue() : null;
	}

	/**
	 * Добавляет куки в хранение у контекста. Куки заменяется новым, если
	 * уже присутсвует в списке куки.
	 * @param name имя куки
	 * @param value значение куки или <code>null</code>
	 * @param domain домен куки
	 * @param path путь до куки или <code>null</code>
	 */
	public void setCookie(@NotNull String name, @Nullable Object value, @NotNull String domain, @NotNull String path) {
		check();

		BasicClientCookie cookie = new BasicClientCookie(name, value == null ? "" : value.toString());
		cookie.setDomain(domain);
		cookie.setPath(path);
		cookie.setSecure(false);
		cookie.setAttribute("SameSite", "None");
		cookie.setAttribute("HttpOnly", "false");
		cookieStore.addCookie(cookie);
	}

	/**
	 * Создает новый GET-запрос, готовый для выполнения.
	 * @param uri юрл запроса
	 * @param values параметры запроса
	 * @return новый GET-запрос, готовый для выполнения.
	 */
	@NotNull
	public HttpGet buildGetRequest(@NotNull String uri, @Nullable Map<String, String> values) {
		check();

		RequestConfig config = RequestConfig.copy(RequestConfig.DEFAULT)
				.setRedirectsEnabled(true)
				.setRelativeRedirectsAllowed(true)
				.setCircularRedirectsAllowed(true)
				.setMaxRedirects(MAX_REDIRECTS)
				.build();

		List<NameValuePair> params = Lists.newArrayList();
		if (values != null) {
			values.forEach((k, v) -> params.add(new BasicNameValuePair(k, v)));
		}

		try {
			HttpGet request;
			if (params.isEmpty()) {
				request = new HttpGet(uri);
			} else {
				URIBuilder uriBuilder = new URIBuilder(uri);
				uriBuilder.setParameters(params);
				request = new HttpGet(uriBuilder.build());
			}
			request.setConfig(config);
			return request;
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Создает новый POST-запрос, готовый для выполнения.
	 * @param uri юрл запроса
	 * @param values параметры запроса
	 * @return новый POST-запрос, готовый для выполнения.
	 */
	@NotNull
	public HttpPost buildPostRequest(@NotNull String uri, @Nullable Map<String, String> values) {
		check();

		RequestConfig config = RequestConfig.copy(RequestConfig.DEFAULT)
				.setRedirectsEnabled(true)
				.setRelativeRedirectsAllowed(true)
				.setCircularRedirectsAllowed(true)
				.setMaxRedirects(MAX_REDIRECTS)
				.build();

		List<NameValuePair> params = Lists.newArrayList();
		if (values != null) {
			values.forEach((k, v) -> params.add(new BasicNameValuePair(k, v)));
		}

		HttpPost request = new HttpPost(uri);
		request.setConfig(config);
		try {
			request.setEntity(new UrlEncodedFormEntity(params));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return request;
	}

	/**
	 * Уничтожает контекст, закрывая {@link #client}.
	 * Далее, контекстные действия не могут выполнятся.
	 */
	public void destroy() {
		if (client != null) {
			try {
				client.close();
			} catch (IOException ignored) {  }
			client = null;
		}
	}
}
