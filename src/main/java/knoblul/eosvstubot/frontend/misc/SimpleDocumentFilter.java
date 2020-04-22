/*
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
package knoblul.eosvstubot.frontend.misc;

import com.google.common.collect.Sets;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.util.Set;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 22.04.2020 10:40
 * @author Knoblul
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
