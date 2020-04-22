package knoblul.eosvstubot.frontend.misc;

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

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.*;

/**
 * @author Knoblul
 * Created: 21.04.2020 21:50
 */
@Plugin(name = "TextPaneAppender", category = "Core", elementType = "appender", printObject = true)
public class TextPaneAppender extends AbstractAppender {
	public static JTextPane consoleComponent = new JTextPane() {
		@Override
		public boolean getScrollableTracksViewportWidth() {
			return getUI().getPreferredSize(this).width <= getParent().getSize().width;
		}
	};

	private TextPaneAppender(String name, Layout<?> layout, Filter filter, boolean ignoreExceptions) {
		super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
	}

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
	public void append(LogEvent event) {
		Color color = event.getLevel().isMoreSpecificThan(Level.WARN) ? Color.RED.brighter().brighter() : Color.BLACK;
		StyleContext sc = StyleContext.getDefaultStyleContext();
		AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color);

		String message = new String(getLayout().toByteArray(event));
		int len = consoleComponent.getDocument().getLength();
		consoleComponent.setCaretPosition(len);
		consoleComponent.setCharacterAttributes(aset, false);
		consoleComponent.replaceSelection(message);
	}
}
