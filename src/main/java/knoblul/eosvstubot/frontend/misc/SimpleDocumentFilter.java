package knoblul.eosvstubot.frontend.misc;

import com.google.common.collect.Sets;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.util.Set;

/**
 * @author Knoblul
 * Created: 22.04.2020 10:40
 */
public class SimpleDocumentFilter extends DocumentFilter {
	private Set<Character> filteredChars = Sets.newHashSet();

	private boolean checkString(String string) {
		char[] chars = string.toCharArray();
		for (char c: chars) {
			if (filteredChars.contains(c)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
			throws BadLocationException {
		if (checkString(string)) {
			super.insertString(fb, offset, string, attr);
		} else {
			Toolkit.getDefaultToolkit().beep();
		}
	}

	@Override
	public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
			throws BadLocationException {
		if (checkString(text)) {
			super.replace(fb, offset, length, text, attrs);
		} else {
			Toolkit.getDefaultToolkit().beep();
		}
	}

	public DocumentFilter filterChar(char c) {
		filteredChars.add(c);
		return this;
	}
}
