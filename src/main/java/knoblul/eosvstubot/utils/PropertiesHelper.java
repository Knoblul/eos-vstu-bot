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

import com.google.common.base.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 22.04.2020 15:33
 * @author Knoblul
 */
public class PropertiesHelper {
	/**
	 * Сохраняет значения всех переменных, помеченных {@link PropertyField}
	 * @param clazz класс, для которого вызывается метод
	 * @param instance экземпляр класса, для которого вызывается метод
	 * @param file файл, в который нужно сохранить значения полей
	 */
	public static void save(@NotNull Class<?> clazz, @Nullable Object instance, @NotNull Path file) {
		try (BufferedWriter writer = Files.newBufferedWriter(file)) {
			Properties properties = new Properties();
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				if (field.isAnnotationPresent(PropertyField.class)) {
					PropertyField annotation = field.getAnnotation(PropertyField.class);
					String propertyName = Strings.isNullOrEmpty(annotation.name()) ? field.getName() : annotation.name();
					boolean wasAccessible = field.isAccessible();
					field.setAccessible(true);
					String propertyValue = Objects.toString(field.get(instance));
					field.setAccessible(wasAccessible);

					if (!Strings.isNullOrEmpty(annotation.writeMethodName())) {
						Method method = clazz.getDeclaredMethod(annotation.writeMethodName());
						wasAccessible = method.isAccessible();
						method.setAccessible(true);
						propertyValue = (String) method.invoke(instance);
						method.setAccessible(wasAccessible);
					}

					properties.setProperty(propertyName, propertyValue);
				}
			}
			properties.store(writer, "");
		} catch (Throwable e) {
			Log.warn(e, "Failed to save properties file %s", file);
		}
	}

	/**
	 * Загружает значения всех переменных, помеченных {@link PropertyField}.
	 * Если файл отсутсвует на диске, то он будет автоматически создан. Нужно
	 * это для того, чтобы переменным присвоились их дефолтовые значения.
	 * @param clazz класс, для которого вызывается метод
	 * @param instance экземпляр класса, для которого вызывается метод
	 * @param file файл, из которого нужно загрузить значения полей
	 */
	public static void load(@NotNull Class<?> clazz, @Nullable Object instance, @NotNull Path file) {
		// если файла нету на диске, создаем его
		if (!Files.exists(file)) {
			try {
				Files.createFile(file);
			} catch (IOException e) {
				Log.warn(e, "Failed to create properties file %s", file);
				return;
			}
		}

		try (BufferedReader reader = Files.newBufferedReader(file)) {
			Properties properties = new Properties();
			properties.load(reader);

			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {
				if (field.isAnnotationPresent(PropertyField.class)) {
					PropertyField annotation = field.getAnnotation(PropertyField.class);
					String propertyName = Strings.isNullOrEmpty(annotation.name()) ? field.getName() : annotation.name();
					String defaultValue = annotation.defaultValue();
					String propertyStringValue = properties.getProperty(propertyName, defaultValue);
					if (!Strings.isNullOrEmpty(annotation.readMethodName())) {
						Method method = clazz.getDeclaredMethod(annotation.readMethodName(), String.class);
						boolean wasAccessible = method.isAccessible();
						method.setAccessible(true);
						method.invoke(instance, propertyStringValue);
						method.setAccessible(wasAccessible);
					} else {
						Object propertyValue;
						Class<?> type = field.getType();
						if (type == String.class) {
							propertyValue = propertyStringValue;
						} else if (type == boolean.class) {
							propertyValue = Boolean.parseBoolean(propertyStringValue);
						} else if (type == byte.class) {
							propertyValue = Byte.parseByte(propertyStringValue);
						} else if (type == char.class) {
							propertyValue = propertyStringValue.isEmpty() ? (char) 0 : propertyStringValue.charAt(0);
						} else if (type == short.class) {
							propertyValue = Short.parseShort(propertyStringValue);
						} else if (type == int.class) {
							propertyValue = Integer.parseInt(propertyStringValue);
						} else if (type == long.class) {
							propertyValue = Long.parseLong(propertyStringValue);
						} else if (type == float.class) {
							propertyValue = Float.parseFloat(propertyStringValue);
						} else if (type == double.class) {
							propertyValue = Double.parseDouble(propertyStringValue);
						} else {
							throw new IllegalArgumentException("Unknown configuration value type: " + type.getName());
						}
						boolean wasAccessible = field.isAccessible();
						field.setAccessible(true);
						field.set(instance, propertyValue);
						field.setAccessible(wasAccessible);
					}
				}
			}
		} catch (Throwable e) {
			Log.warn(e, "Failed to load properties file %s", file);
		}
	}
}
