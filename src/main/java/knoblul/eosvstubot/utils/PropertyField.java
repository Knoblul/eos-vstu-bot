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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Properties;

/**
 * Аннотация для управления переменными пропертиес.
 * Если поле класса помечено этой аннотацией,
 * то загрузка и сохранение будет выполнятся.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 22.04.2020 15:45
 * @author Knoblul
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyField {
	/**
	 * Стандартное значение, котрое будет присвоено переменной
	 * при остуствии в properties при вызывое
	 * {@link PropertiesHelper#load(Class, Object, Properties)}.
	 */
	String defaultValue() default "";

	/**
	 * Если у поля собственный метод парсинга значения из пропертиесов,
	 * то указать имя этого метода в этом параметре. Вместо стандартной конвертации примитивов,
	 * будет вызыван указанный метод.
	 * <b>Метод обязательно должен быть void и принимать аргумент типа String.</b>
	 */
	String readMethodName() default "";

	/**
	 * Если у поля собственный метод записи значения в пропертиес,
	 * то указать имя этого метода в этом параметре. Вместо стандартной конвертации
	 * значения переменной в строку, будет вызван этот метод.
	 * <b>Метод обязательно должен возвращать String и не принимать аргументов.</b>
	 */
	String writeMethodName() default "";

	/**
	 * Кастомное имя переменной в пропертиес. Вместо реального имени переменной
	 * при записи/чтении будет использоваться значение этого параметра.
	 */
	String name() default "";
}
