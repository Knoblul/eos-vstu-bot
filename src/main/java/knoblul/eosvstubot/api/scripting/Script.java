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
package knoblul.eosvstubot.api.scripting;

import javax.script.*;

/**
 * Потокобезопасное представления скрипта.
 * Содержит в себе контент скрипта, который готов
 * на передачу движку. Происходит это через вызов
 * метода {@link #recompile()}.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 30.04.2020 15:04
 *
 * @author Knoblul
 */
public class Script {
	private static final ScriptEngineManager factory = new ScriptEngineManager();
	private transient Invocable invocableEngine;
	private transient ScriptEngine engine;
	private transient ScriptContext context;
	private transient Bindings bindings = new SimpleBindings();

	/**
	 * Контент скрипта
	 */
	private String content;

	public Script() {
		setContent("");
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * Устанавливает биндинг для выполнения скрипта. По сути, устанавливает
	 * глобальную переменную (дефайн), которую движок передает скрипту.
	 *
	 * @param name  название биндинга
	 * @param value значение биндинга
	 */
	public void putBinding(String name, Object value) {
		bindings.put(name, value);
	}

	/**
	 * Очищает биндинги. Нужно перед рекомпиляцией.
	 */
	public void clearBindings() {
		bindings.clear();
	}

	/**
	 * Вызывает функцию внутри скрипта.
	 * <p>До вызова этого метода необходимо чтобы скрипт был
	 * передан движку через вызов {@link #recompile()}</p>
	 *
	 * @param functionName      название скрипт-функции
	 * @param functionArguments аргументы, которые передавать в скрипт-функцию
	 * @return то, что вернет скрипт-функция
	 * @throws ScriptException       если произошла ошибка во время выполнения скрипт-функции.
	 * @throws NoSuchMethodException если скрипт-функция не найдена внутри скрипта,
	 *                               либо скрипт еще не был скомпилирован.
	 */
	public Object invokeFunction(String functionName, Object... functionArguments)
			throws ScriptException, NoSuchMethodException {
		if (invocableEngine == null) {
			throw new NoSuchMethodException("Script recompilation required");
		}

		engine.setContext(context);
		return invocableEngine.invokeFunction(functionName, functionArguments);
	}

	/**
	 * Рекомпилирует скрипт - передает его на обработку к движку (выполняет).
	 * Далее можно пользоватся методом {@link #invokeFunction(String, Object...)}
	 *
	 * @throws ScriptException если движку не удалось выполнить скрипт
	 */
	public void recompile() throws ScriptException {
		engine = factory.getEngineByName("Nashorn");
		if (!(engine instanceof Invocable)) {
			throw new RuntimeException("Valid scripting engine not found");
		}
		invocableEngine = (Invocable) engine;
		context = new SimpleScriptContext();
		context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
		Bindings scope = context.getBindings(ScriptContext.ENGINE_SCOPE);
		scope.putAll(bindings);
		engine.eval(content, context);
	}
}
