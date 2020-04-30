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

package knoblul.eosvstubot.gui.scripting;

import knoblul.eosvstubot.api.scripting.Script;
import knoblul.eosvstubot.gui.BotMainWindow;
import knoblul.eosvstubot.utils.swing.DialogUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.script.ScriptException;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 29.04.2020 19:46
 *
 * @author Knoblul
 */
public class ScriptEditorDialog extends JDialog {
	private RSyntaxTextArea textArea;
	private Popup popup;
	private ScriptEditorDialogPopupComponent popupComponent;

	public ScriptEditorDialog() {
		super(BotMainWindow.instance, "Редактор скриптов", true);
		setResizable(true);
		fill();
	}

	void hidePopup() {
		if (popup != null) {
			popup.hide();
			popup = null;
		}
	}

	public void setEditingScript(String content) {
		hidePopup();
		textArea.setText(content);
	}

	public String getEditingScript() {
		return textArea.getText();
	}

	/**
	 * Находит смещение в тексте до начала указанной линии (строки)
	 * @param row строка для которой нужно рассчитать смещение
	 * @return смещение в тексте до начала указанной линии (строки)
	 * @throws BadLocationException выкидывается из Utilities.getRowStart
	 */
	private int calculateRowOffset(int row) throws BadLocationException {
		int prevRowOffset = -1;
		int rowIndex = 0;
		for (int i = 0; i < textArea.getDocument().getLength(); i++) {
			int rowOffset = Utilities.getRowStart(textArea, i);
			if (rowOffset != prevRowOffset) {
				if (rowIndex == row) {
					return rowOffset;
				}

				prevRowOffset = rowOffset;
				rowIndex++;
			}
		}
		return 0;
	}

	private void showPopup(int row, int column, String message) {
		Point location = textArea.getLocationOnScreen();
		Rectangle errorLocation = new Rectangle(0, 0, 0, 0);
		try {
			int errorPosition = calculateRowOffset(row - 1) + 1 + column;
			errorLocation = textArea.modelToView(errorPosition);
			textArea.setCaretPosition(errorPosition);
		} catch (BadLocationException | IllegalArgumentException ignored) {
		}

		int popupX = location.x + errorLocation.x;
		int popupY = location.y + errorLocation.y + errorLocation.height;

		popupComponent.prepare(message);

		// подгоняем попап под ширину редактора скрипта
		if (popupX + popupComponent.getPreferredSize().width >= location.x + textArea.getWidth() - 20) {
			popupX = location.x + textArea.getWidth() - popupComponent.getPreferredSize().width - 20;
		}

		if (popupY + popupComponent.getPreferredSize().height >= location.y + textArea.getHeight() - 20) {
			popupY = location.y + textArea.getHeight() - popupComponent.getPreferredSize().height - 20;
		}

		popup = PopupFactory.getSharedInstance().getPopup(textArea, popupComponent, popupX, popupY);
		popup.show();
		popup.hide();
		// странный баг - попап не ресайзится в зависимости
		// от размеров компонента. Делает он это только на
		// следующий вызов попапа WeirdChamp

		popup = PopupFactory.getSharedInstance().getPopup(textArea, popupComponent, popupX, popupY);
		popup.show();
	}

	private boolean compile(ActionEvent event) {
		hidePopup();
		try {
			Script script = new Script();
			script.setContent(textArea.getText());
			script.recompile();
			return true;
		} catch (ScriptException e) {
			showPopup(e.getLineNumber(), e.getColumnNumber(), e.getMessage());
			return false;
		}
	}

	private void fill() {
		textArea = new RSyntaxTextArea();
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
		textArea.setCodeFoldingEnabled(true);
		textArea.setLineWrap(false);
		textArea.setWrapStyleWord(false);
		RTextScrollPane sp = new RTextScrollPane(textArea);
		sp.registerKeyboardAction(this::compile, "compile",
				KeyStroke.getKeyStroke('S', InputEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

		setLayout(new BorderLayout());
		add(sp, BorderLayout.CENTER);

		popupComponent = new ScriptEditorDialogPopupComponent(this);

		setSize(900, 650);
		setMinimumSize(getSize());
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (!compile(null) && !DialogUtils
						.showConfirmation("Возникли ошибки при компиляции скрипта." +
								" Вы уверены, что хотите закрыть редактор?")) {
					return;
				}

				setVisible(false);
			}
		});
	}
}