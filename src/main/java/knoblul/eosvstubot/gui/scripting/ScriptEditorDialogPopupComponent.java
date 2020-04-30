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

import javax.swing.*;
import java.awt.*;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 29.04.2020 19:00
 *
 * @author Knoblul
 */
class ScriptEditorDialogPopupComponent extends JComponent {
	private final ScriptEditorDialog parent;
	private JTextArea description;
	private JScrollPane descriptionScrollPane;

	ScriptEditorDialogPopupComponent(ScriptEditorDialog parent) {
		this.parent = parent;
		fill();
	}

	private void fill() {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		JLabel title = new JLabel("Ошибка компиляции");
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets.set(4, 4, 4, 4);
        gbc.gridx = 0;
        add(title, gbc);

        JButton closeButton = new JButton("x");
        closeButton.getInsets().set(0, 0, 0, 0);
        closeButton.setBorder(null);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setPreferredSize(new Dimension(20, 20));
        closeButton.setMaximumSize(closeButton.getPreferredSize());
        closeButton.setMinimumSize(closeButton.getPreferredSize());
        closeButton.addActionListener(e -> parent.hidePopup());
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.insets.set(0, 0, 0, 0);
        gbc.gridx = 1;
        add(closeButton, gbc);

		description = new JTextArea();
		description.setFont(new Font("Consolas", Font.PLAIN, 12));
		description.setLineWrap(false);
		description.setWrapStyleWord(false);
		description.setForeground(Color.RED);
		description.setOpaque(false);
		description.setBackground(new Color(255, 255, 255, 100));
		description.setEditable(false);
		description.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		descriptionScrollPane = new JScrollPane(description, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(450, super.getPreferredSize().height+16);
			}
		};
		descriptionScrollPane.setBorder(null);
		descriptionScrollPane.setOpaque(false);
		descriptionScrollPane.setBackground(new Color(255, 255, 255, 100));
        gbc.insets.set(4, 4, 4, 4);
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridwidth = 2;
        gbc.gridx = 0;
		add(descriptionScrollPane, gbc);

		setBorder(BorderFactory.createLineBorder(new Color(255, 0, 0, 100)));
		setOpaque(false);
		setBackground(new Color(255, 255, 255, 100));
	}

	void prepare(String message) {
		description.setText(message);
        description.setCaretPosition(0);
	}
}
