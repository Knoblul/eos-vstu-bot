package knoblul.eosvstubot.frontend.misc;

import com.google.common.base.Throwables;
import knoblul.eosvstubot.utils.Log;

import javax.swing.*;
import java.awt.*;

/**
 * REVOM ENGINE / ProjectCataclysm
 *
 * @author Knoblul
 * Created: 22.04.2020 0:51
 */
public class DialogUtils {
	public static void showWarning(String message) {
		JOptionPane.showMessageDialog(null, message, "Внимание", JOptionPane.WARNING_MESSAGE);
	}

	public static void showError(String message, Throwable t) {
		Log.error(t, message);

		JDialog dialog = new JDialog((Frame) null, "Ошибка", true);
		dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		dialog.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weightx = 1;
		gbc.insets.set(10, 10, 10, 10);
		dialog.add(new JLabel(message), gbc);
		dialog.setSize(500, 400);
		dialog.setLocationRelativeTo(null);

		gbc.weightx = 0;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.insets.set(0, 10, 0, 10);

		JTextArea ta = new JTextArea();
		ta.setFont(new Font("Consolas", Font.PLAIN, 12));
		ta.setText(Throwables.getStackTraceAsString(t));
		ta.setCaretPosition(0);

		JScrollPane jscp = new JScrollPane(ta, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jscp.setMaximumSize(new Dimension(400, 500));
		dialog.add(jscp, gbc);

		JButton ok = new JButton("Ок");
		ok.addActionListener(e -> dialog.setVisible(false));

		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.insets.set(10, 10, 10, 10);
		gbc.gridy = 2;
		gbc.weightx = 0;
		gbc.weighty = 0;
		dialog.add(ok, gbc);

		dialog.setVisible(true);
	}
}
