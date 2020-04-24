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


import org.apache.http.concurrent.FutureCallback;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

/**
 * Класс содержит методы-утилиты для создания декораторов
 * к коллбекам.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 24.04.2020 20:01
 * @author Knoblul
 */
public class HttpCallbacks {
	@NotNull
	@Contract(value = "_ -> new", pure = true)
	public static <T> FutureCallback<T> onCompletion(Consumer<T> function) {
		return new FutureCallback<T>() {
			@Override
			public void completed(T result) {
				function.accept(result);
			}

			@Override
			public void failed(Exception ex) {

			}

			@Override
			public void cancelled() {

			}
		};
	}

	@NotNull
	@Contract(value = "_, _ -> new", pure = true)
	public static <T> FutureCallback<T> onFailure(FutureCallback<T> source, Consumer<Exception> function) {
		return new FutureCallback<T>() {
			@Override
			public void completed(T result) {
				source.completed(result);
			}

			@Override
			public void failed(Exception ex) {
				function.accept(ex);
			}

			@Override
			public void cancelled() {
				function.accept(new CancellationException());
			}
		};
	}

	@NotNull
	public static <T> FutureCallback<T> onEither(Consumer<T> onCompletion, Consumer<Exception> onFailure) {
		return onFailure(onCompletion(onCompletion), onFailure);
	}
}
