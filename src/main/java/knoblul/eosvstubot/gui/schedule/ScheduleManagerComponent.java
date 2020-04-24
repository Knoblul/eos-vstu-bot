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
package knoblul.eosvstubot.gui.schedule;

import knoblul.eosvstubot.api.schedule.LessonsManager;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormatSymbols;

/**
 * Компонент менеджера расписания.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 23.04.2020 13:14
 * @author Knoblul
 */
public class ScheduleManagerComponent extends JComponent {
	public static final String[] WEEKDAY_NAMES = new DateFormatSymbols().getWeekdays();

	private final LessonsManager lessonsManager;

	private JLabel currentWeekLabel;
	private long lastWeekUpdateLabelTime;

	public ScheduleManagerComponent(LessonsManager lessonsManager) {
		this.lessonsManager = lessonsManager;
		fill();
	}

	private void updateWeekLabel() {
		currentWeekLabel.setText("Автоматич. - текущая неделя: " + (lessonsManager.getCurrentWeekIndex()+1) + ",");
	}

	public void update() {
		long time = System.currentTimeMillis();
		if (time > lastWeekUpdateLabelTime + 10000) {
			updateWeekLabel();
			lastWeekUpdateLabelTime = time;
		}
	}

	private void fill() {
		setLayout(new BorderLayout());

		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));

		p.add(new ScheduleTable(lessonsManager, 0));
		p.add(new ScheduleTable(lessonsManager, 1));

		add(p, BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		bottomPanel.add(currentWeekLabel = new JLabel());
		bottomPanel.add(new JLabel("смещение рассчета:"));
		JSpinner spinner = new JSpinner(new SpinnerNumberModel(lessonsManager.getFirstWeekOfYearIndex()+1,
				1, 2, 1));
		spinner.addChangeListener((e) -> {
			lessonsManager.setFirstWeekOfYearIndex((int) spinner.getModel().getValue() - 1);
			updateWeekLabel();
			lessonsManager.save();
		});
		bottomPanel.add(spinner);
		add(bottomPanel, BorderLayout.SOUTH);

		updateWeekLabel();
	}
}
