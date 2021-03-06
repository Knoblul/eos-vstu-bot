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
package knoblul.eosvstubot.utils.swing;

import knoblul.eosvstubot.gui.BotMainWindow;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;

/**
 * log4j2 Appender, который пишет логи в {@link #consoleComponent}
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 21:50
 * @author Knoblul
 */
@Plugin(name = "TextPaneAppender", category = "Core", elementType = "appender", printObject = true)
public class TextPaneAppender extends AbstractAppender {
	public static JTextPane consoleComponent = new JTextPane() {
		{
			setFont(new Font("Consolas", Font.PLAIN, 12));
			setEditable(false);
		}

		// костыль, чтобы избавится от вордврапа, когда родитель JScrollPane.
		@Override
		public boolean getScrollableTracksViewportWidth() {
			return getUI().getPreferredSize(this).width <= getParent().getSize().width;
		}
	};

	public static boolean wasConsoleErrors;

	private TextPaneAppender(String name, Layout<?> layout, Filter filter, boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
	}

	@Nullable
	@Contract("!null, _, !null, _ -> new")
	@SuppressWarnings("unused")
	@PluginFactory
	public static TextPaneAppender createAppender(@PluginAttribute("name") String name,
												  @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
												  @PluginElement("Layout") Layout<?> layout,
												  @PluginElement("Filters") Filter filter) {
		if (name == null) {
			LOGGER.error("No name provided for JTextAreaAppender");
			return null;
		}

		if (layout == null) {
			layout = PatternLayout.createDefaultLayout();
		}

		return new TextPaneAppender(name, layout, filter, ignoreExceptions);
	}

	@Override
	public void append(@NotNull LogEvent event) {
		// если ошибка, цвет текста = красный, иначе черный
		boolean err = event.getLevel().isMoreSpecificThan(Level.WARN);
		if (err) {
			wasConsoleErrors = true;
			if (BotMainWindow.instance != null) {
				BotMainWindow.instance.markConsoleErrors();
			}
		}

		Color color = err ? Color.RED.brighter().brighter() : Color.BLACK;
		StyleContext context = StyleContext.getDefaultStyleContext();
		AttributeSet attributes = context.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);

		String message = new String(getLayout().toByteArray(event));
		int len = consoleComponent.getDocument().getLength();
		consoleComponent.setEditable(true);
		consoleComponent.setCaretPosition(len);
		consoleComponent.setCharacterAttributes(attributes, false);
		consoleComponent.replaceSelection(message);
		consoleComponent.setCaretPosition(consoleComponent.getDocument().getLength());
		consoleComponent.setEditable(false);
	}
}
