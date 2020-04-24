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
package knoblul.eosvstubot.api;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import knoblul.eosvstubot.api.chat.ChatSession;
import knoblul.eosvstubot.api.profile.ProfileManager;
import knoblul.eosvstubot.api.schedule.LessonsManager;
import knoblul.eosvstubot.gui.BotMainWindow;
import knoblul.eosvstubot.utils.Log;
import org.apache.http.HttpResponse;
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
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Future;

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
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLenient().create();

	private static final int MAX_HTTP_REDIRECTS = 10;

	/**
	 * Экземпляр основной потока. Нужен для проверок
	 * корректности вызова методов контекста (и не только).
	 */
	public final Thread mainThread;

	/**
	 * Экземпляр менеджера профилей, который "привязан"
	 * к данному контексту.
	 */
	private final ProfileManager profileManager;

	/**
	 * Экземпляр менеджера расписания, который "привязан"
	 * к данному контексту.
	 */
	private final LessonsManager lessonsManager;

	/**
	 * Хранилище куки для запросов.
	 */
	private CookieStore cookieStore;

	/**
	 * Основной блокирующий HTTP-клиент
	 */
	private CloseableHttpClient client;

	/**
	 * Асинхронный HTTP-клиент
	 */
	private CloseableHttpAsyncClient asyncClient;

	/**
	 * Очередь из команд, которые должны исполнится в
	 * основном потоке
	 */
	private Queue<Runnable> mainThreadCommands = Queues.newConcurrentLinkedQueue();

	private Map<URI, ChatSession> chatSessions = Maps.newHashMap();

	public BotContext() {
		mainThread = Thread.currentThread();
		if (!mainThread.getName().equals("main")) {
			throw new IllegalStateException("Context can be created only from the main thread!");
		}

		profileManager = new ProfileManager(this);
		lessonsManager = new LessonsManager(this);
	}

	/**
	 * Проверяет экземпляр на наличие контекста.
	 */
	private void check() {
		if (client == null) {
			throw new IllegalStateException("Context not created yet");
		}
	}

	/**
	 * Создает котекст. Далее все контекстные действия могут выполнятся
	 * вплоть до вызова {@link #destroy()}.
	 *
	 * <p>Эта функция должна вызываться только из основного потока.</p>
	 * <p>Эта функция не должна вызываться, если контекст уже был создан.</p>
	 */
	public void create() {
		if (client != null) {
			throw new IllegalStateException("Context already created");
		}

		if (Thread.currentThread() != mainThread) {
			throw new IllegalStateException("This function must only be called from main thread");
		}

		cookieStore = new BasicCookieStore();

		client = HttpClientBuilder.create()
				.setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(cookieStore)
				.build();

		asyncClient = HttpAsyncClientBuilder.create()
				.setRedirectStrategy(new LaxRedirectStrategy())
				.setDefaultCookieStore(cookieStore)
				.build();

		asyncClient.start();
	}

	/**
	 * Отправляет команду на исполнение в основной поток, если текущий
	 * поток не основной. Иначе исполняет команду немедленно.
	 *
	 * <p>Эта функция не должна вызываться до вызыова {@link #create()} и после вызова {@link #destroy()}.</p>
	 * @param command команда, которую нужно исполнить
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
	 * Запускает бексконечный цикл для обработки команд из очереди.
	 * Основное назначение - блокирование текущего потока и ожидание команд.
	 * <p>Эта функция должна вызываться только из основного потока.</p>
	 * <p>Эта функция не должна вызываться до вызыова {@link #create()} и после вызова {@link #destroy()}.</p>
	 */
	public void occupyMainThread() {
		check();
		if (Thread.currentThread() != mainThread) {
			throw new IllegalStateException("This function must only be called from main thread");
		}

		Log.info("Command processing started");
		while (true) {
			update();

			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				Log.info("Command processing terminated");
				break;
			}
		}
	}

	/**
	 * Высушивает очередь из команд к главному потоку,
	 * затем выполняет их. После этого обновляет чат сессии и
	 * некоторые штуки в интерфейсе.
	 * <p>Эта функция должна вызываться только из основного потока.</p>
	 * <p>Эта функция не должна вызываться до вызыова {@link #create()} и после вызова {@link #destroy()}.</p>
	 */
	public void update() {
		check();
		if (Thread.currentThread() != mainThread) {
			throw new IllegalStateException("This function must only be called from main thread");
		}

		Runnable command;
		while ((command = mainThreadCommands.poll()) != null) {
			invokeMainThreadCommand(command);
		}

		chatSessions.values().forEach(ChatSession::update);

		if (BotMainWindow.instance != null) {
			BotMainWindow.instance.update();
		}
	}

	/**
	 * Говорит основному потоку перестать обрабатывать команды.
	 * <p>Эта функция не должна вызываться до вызыова {@link #create()} и после вызова {@link #destroy()}.</p>
	 */
	public void stopMainThreadCommandsProcessing() {
		check();
		mainThread.interrupt();
	}

	/**
	 * <p>Эта функция не должна вызываться до вызыова {@link #create()} и после вызова {@link #destroy()}.</p>
	 * @return {@link #profileManager}
	 */
	public ProfileManager getProfileManager() {
		check();
		return profileManager;
	}

	/**
	 * <p>Эта функция не должна вызываться до вызыова {@link #create()} и после вызова {@link #destroy()}.</p>
	 * @return {@link #lessonsManager}
	 */
	public LessonsManager getLessonsManager() {
		check();
		return lessonsManager;
	}

	/**
	 * Загружает все менеджеры, которые управляют какими-либо ресурсами.
	 *
	 * <p>Эта функция должна вызываться только из основного потока.</p>
	 * <p>Эта функция не должна вызываться до вызыова {@link #create()} и после вызова {@link #destroy()}.</p>
	 */
	public void loadManagers() {
		check();
		if (Thread.currentThread() != mainThread) {
			throw new IllegalStateException("This function must only be called from main thread");
		}
		profileManager.load();
		lessonsManager.load();
	}

	/**
	 * Очищает все куки из данного контекста
	 *
	 * <p>Эта функция должна вызываться только из основного потока.</p>
	 * <p>Эта функция не должна вызываться до вызыова {@link #create()} и после вызова {@link #destroy()}.</p>
	 */
	public void clearCookies() {
		check();
		if (Thread.currentThread() != mainThread) {
			throw new IllegalStateException("This function must only be called from main thread");
		}
		cookieStore.clear();
	}

	/**
	 * Получает куки по имени из тех, что хранит данный контекст
	 *
	 * <p>Эта функция должна вызываться только из основного потока.</p>
	 * <p>Эта функция не должна вызываться до вызыова {@link #create()} и после вызова {@link #destroy()}.</p>
	 * @param name имя куки
	 * @return найденный куки или <code>null</code>
	 */
	@Nullable
	public Cookie getCookie(@NotNull String name) {
		check();
		if (Thread.currentThread() != mainThread) {
			throw new IllegalStateException("This function must only be called from main thread");
		}

		for (Cookie cookie: cookieStore.getCookies()) {
			if (cookie.getName().equals(name)) {
				return cookie;
			}
		}
		return null;
	}

	/**
	 * Получает згачегие куки по имени из тех, что хранит данный контекст
	 *
	 * <p>Эта функция должна вызываться только из основного потока.</p>
	 * <p>Эта функция не должна вызываться до вызыова {@link #create()} и после вызова {@link #destroy()}.</p>
	 * @param name имя куки
	 * @return значение куки или <code>null</code>
	 */
	@Nullable
	public String getCookieValue(@NotNull String name) {
		check();
		if (Thread.currentThread() != mainThread) {
			throw new IllegalStateException("This function must only be called from main thread");
		}

		Cookie cookie = getCookie(name);
		return cookie != null ? cookie.getValue() : null;
	}

	/**
	 * Добавляет куки в хранение у контекста. Куки заменяется новым, если
	 * уже присутсвует в списке куки.
	 *
	 * <p>Эта функция должна вызываться только из основного потока.</p>
	 * <p>Эта функция не должна вызываться до вызыова {@link #create()} и после вызова {@link #destroy()}.</p>
	 * @param name имя куки
	 * @param value значение куки или <code>null</code>
	 * @param domain домен куки
	 * @param path путь до куки или <code>null</code>
	 */
	public void setCookie(@NotNull String name, @Nullable Object value, @NotNull String domain, @NotNull String path) {
		check();
		if (Thread.currentThread() != mainThread) {
			throw new IllegalStateException("This function must only be called from main thread");
		}

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
	 *
	 * <p>Эта функция должна вызываться только из основного потока.</p>
	 * <p>Эта функция не должна вызываться до вызыова {@link #create()} и после вызова {@link #destroy()}.</p>
	 * @param uri юрл запроса
	 * @param params параметры запроса, которые добавляются к GET-параметрам юрл.
	 * @return новый GET-запрос, готовый для выполнения.
	 */
	@NotNull
	public HttpUriRequest buildGetRequest(@NotNull String uri, @Nullable Map<String, String> params) {
		check();
		if (Thread.currentThread() != mainThread) {
			throw new IllegalStateException("This function must only be called from main thread");
		}

		RequestConfig config = RequestConfig.copy(RequestConfig.DEFAULT)
				.setRedirectsEnabled(true)
				.setRelativeRedirectsAllowed(true)
				.setCircularRedirectsAllowed(true)
				.setMaxRedirects(MAX_HTTP_REDIRECTS)
				.build();

		List<NameValuePair> nameValuePairs = Lists.newArrayList(URLEncodedUtils.parse(URI.create(uri), Charsets.UTF_8));
		if (params != null) {
			params.forEach((k, v) -> nameValuePairs.add(new BasicNameValuePair(k, v)));
		}

		try {
			URIBuilder uriBuilder = new URIBuilder(uri);
			uriBuilder.setParameters(nameValuePairs);
			HttpGet request = new HttpGet(uriBuilder.build());
			request.setConfig(config);
			return request;
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Создает новый POST-запрос, готовый для выполнения.
	 *
	 * <p>Эта функция должна вызываться только из основного потока.</p>
	 * <p>Эта функция не должна вызываться до вызыова {@link #create()} и после вызова {@link #destroy()}.</p>
	 * @param uri юрл запроса
	 * @param postParams параметры запроса, которые передаются в теле POST-запроса
	 * @return новый POST-запрос, готовый для выполнения.
	 */
	@NotNull
	public HttpUriRequest buildPostRequest(@NotNull String uri, @Nullable Map<String, String> postParams) {
		check();
		if (Thread.currentThread() != mainThread) {
			throw new IllegalStateException("This function must only be called from main thread");
		}

		RequestConfig config = RequestConfig.copy(RequestConfig.DEFAULT)
				.setRedirectsEnabled(true)
				.setRelativeRedirectsAllowed(true)
				.setCircularRedirectsAllowed(true)
				.setMaxRedirects(MAX_HTTP_REDIRECTS)
				.build();

		List<NameValuePair> encodedParams = Lists.newArrayList();
		if (postParams != null) {
			postParams.forEach((k, v) -> encodedParams.add(new BasicNameValuePair(k, v)));
		}

		HttpPost request = new HttpPost(uri);
		request.setConfig(config);
		try {
			request.setEntity(new UrlEncodedFormEntity(encodedParams));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		return request;
	}

	/**
	 * Выполняет указанный запрос и возвращает ответ в типе, который указан с помощью
	 * класса expectedResponseClass. Виды типов {@link Document}, {@link JsonElement}, String.
	 * Если тип не относится к этим видам, метод выкинет IllegalArgumentException
	 *
	 * <p>Эта функция должна вызываться только из основного потока.</p>
	 * <p>Эта функция не должна вызываться до вызыова {@link #create()} и после вызова {@link #destroy()}.</p>
	 * @param request экземпляр настроенного запроса
	 * @param expectedResponseClass класс, указывающий на вид типа.
	 * @param <T> вид типа {@link Document} или {@link JsonElement}.
	 * @return ответ преобразованный в тип, который был указан параметром expectedResponseClass.
	 * @throws IOException при возникновении ошибки во время получения/разбора ответа.
	 * @throws IllegalArgumentException при неверном указании типа
	 */
	@NotNull
	public <T> T executeRequest(@NotNull HttpUriRequest request, Class<T> expectedResponseClass) throws IOException {
		check();
		if (Thread.currentThread() != mainThread) {
			throw new IllegalStateException("This function must only be called from main thread");
		}

		try (CloseableHttpResponse response = client.execute(request)) {
			StatusLine statusLine = response.getStatusLine();
			if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
				throw new IOException("Invalid status: " + statusLine.getStatusCode() + " "
						+ statusLine.getReasonPhrase());
			}

			/*Header contentTypeHeader = response.getEntity().getContentType();
			String contentType = contentTypeHeader != null ? contentTypeHeader.getValue() : null;
			boolean jsonContentType = StringUtils.containsIgnoreCase(contentType, "application/json");*/

			StringBuilder content = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(),
					Charsets.UTF_8))) {
				String ln;
				while ((ln = reader.readLine()) != null) {
					content.append(ln);
				}
			}

			if (Document.class.isAssignableFrom(expectedResponseClass)) {
				try {
					return expectedResponseClass.cast(Jsoup.parse(content.toString(), ""));
				} catch (Throwable t) {
					throw new IOException(content.toString(), t);
				}
			} else if (/*jsonContentType || */JsonElement.class.isAssignableFrom(expectedResponseClass)) {
				try {
					return GSON.fromJson(content.toString(), expectedResponseClass);
				} catch (JsonParseException e) {
					throw new IOException(content.toString(), e);
				}
			} else if (String.class.isAssignableFrom(expectedResponseClass)) {
				return expectedResponseClass.cast(content.toString());
			}

			throw new IllegalArgumentException("Excpected response class is invalid: "
					+ expectedResponseClass);
		}
	}

	/**
	 * Асинхронно выполняет указанный запрос и возвращает {@link Future} запроса.
	 * Тип колббека указан с помощью класса expectedResponseClass. Виды типов {@link Document}, {@link JsonElement},
	 * String. Если тип не относится к этим видам, коллбек зафейлится с IllegalArgumentException.
	 * Для коллбека написан декоратор, который позволит получать переданному
	 * коллбеку при успехе не сырой ответ от HTTP клиента а объект в указанном виде.
	 *
	 * <p>Эта функция должна вызываться только из основного потока.</p>
	 * <p>Эта функция не должна вызываться до вызыова {@link #create()} и после вызова {@link #destroy()}.</p>
	 * @param <T> вид типа {@link Document}, {@link JsonElement}, String
	 * @param request экземпляр настроенного запроса
	 * @param expectedResponseClass класс, указывающий на вид типа.
	 * @param responseCallback коллбек, который вызывается HTTP клиентом
	 *                           после получения ответа или ошибки
	 */
	public <T> void executeRequestAsync(@NotNull HttpUriRequest request, Class<T> expectedResponseClass,
										FutureCallback<T> responseCallback) {
		check();
		if (Thread.currentThread() != mainThread) {
			throw new IllegalStateException("This function must only be called from main thread");
		}

		Exception callStackTrace = new Exception("Call stack trace");
		// добавил декоратор, чтобы получать не "сырые" ответы в коллбеках
		asyncClient.execute(request, new FutureCallback<HttpResponse>() {
			@Override
			public void completed(HttpResponse result) {
				// чтобы коллбек "фейлился" при статусе, отличном от 200 OK
				StatusLine statusLine = result.getStatusLine();
				if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
					failed(new IOException("Invalid status: " + statusLine.getStatusCode() + " "
							+ statusLine.getReasonPhrase()));
					return;
				}

				/*Header contentTypeHeader = result.getEntity().getContentType();
				String contentType = contentTypeHeader != null ? contentTypeHeader.getValue() : null;
				boolean jsonContentType = StringUtils.containsIgnoreCase(contentType, "application/json");*/

				StringBuilder content = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(result.getEntity().getContent(),
						Charsets.UTF_8))) {
					String ln;
					while ((ln = reader.readLine()) != null) {
						content.append(ln);
					}
				} catch (IOException e) {
					failed(e);
					return;
				}

				T obj;
				if (Document.class.isAssignableFrom(expectedResponseClass)) {
					try {
						obj = expectedResponseClass.cast(Jsoup.parse(content.toString(), ""));
					} catch (Exception e) {
						failed(new IOException(content.toString(), e));
						return;
					}
				} else if (/*jsonContentType || */JsonElement.class.isAssignableFrom(expectedResponseClass)) {
					try {
						obj = GSON.fromJson(content.toString(), expectedResponseClass);
					} catch (JsonParseException e) {
						failed(new IOException(content.toString(), e));
						return;
					}
				} else if (String.class.isAssignableFrom(expectedResponseClass)) {
					obj = expectedResponseClass.cast(content.toString());
				} else {
					failed(new IllegalArgumentException("Expected response class is invalid: "
							+ expectedResponseClass));
					return;
				}

				responseCallback.completed(obj);
			}

			@Override
			public void failed(Exception ex) {
				callStackTrace.initCause(ex);
				responseCallback.failed(callStackTrace);
			}

			@Override
			public void cancelled() {
				responseCallback.cancelled();
			}
		});
	}

	public ChatSession createChatSession(String chatLink) {
		check();
		if (Thread.currentThread() != mainThread) {
			throw new IllegalStateException("This function must only be called from main thread");
		}
		return chatSessions.computeIfAbsent(URI.create(chatLink), (k) -> new ChatSession(this, chatLink));
	}

	public void destroyChatSession(ChatSession session) {
		check();
		if (Thread.currentThread() != mainThread) {
			throw new IllegalStateException("This function must only be called from main thread");
		}
		chatSessions.values().remove(session);
		session.close();
	}

	/**
	 * Уничтожает контекст, закрывая {@link #client}.
	 * Далее, контекстные действия не могут выполнятся.
	 *
	 * <p>Эта функция должна вызываться только из основного потока.</p>
	 * <p>Эта функция не должна вызываться до вызыова {@link #create()} и после уничтожения контекста.</p>
	 */
	public void destroy() {
		check();
		if (Thread.currentThread() != mainThread) {
			throw new IllegalStateException("This function must only be called from main thread");
		}

		if (client != null) {
			try {
				client.close();
			} catch (IOException ignored) {  }
			client = null;
		}

		if (asyncClient != null) {
			try {
				asyncClient.close();
			} catch (IOException ignored) { }
			asyncClient = null;
		}
	}
}