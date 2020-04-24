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

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;

/**
 * Простой swing-компонент для выбора времени.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 23.04.2020 15:26
 * @author Knoblul
 */
public class TimeChooser extends JComponent {
	private SpinnerNumberModel hourSpinnerModel;
	private SpinnerNumberModel minuteSpinnerModel;
	private SpinnerNumberModel secondSpinnerModel;

	public TimeChooser() {
		fill();
	}

	private void fill() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		hourSpinnerModel = new SpinnerNumberModel(0, 0, 24, 1);
		JSpinner hourSpinner = new JSpinner(hourSpinnerModel);
		hourSpinner.setEditor(new JSpinner.NumberEditor(hourSpinner, "##"));
		add(hourSpinner);

		add(Box.createHorizontalStrut(2));
		add(new JLabel(":"));
		add(Box.createHorizontalStrut(2));

		minuteSpinnerModel = new SpinnerNumberModel(0, 0, 60, 5);
		JSpinner minuteSpinner = new JSpinner(minuteSpinnerModel);
		minuteSpinner.setEditor(new JSpinner.NumberEditor(minuteSpinner, "00"));
		add(minuteSpinner);

		add(Box.createHorizontalStrut(2));
		add(new JLabel(":"));
		add(Box.createHorizontalStrut(2));

		secondSpinnerModel = new SpinnerNumberModel(0, 0, 60, 5);
		JSpinner secondSpinner = new JSpinner(secondSpinnerModel);
		secondSpinner.setEditor(new JSpinner.NumberEditor(secondSpinner, "00"));
		add(secondSpinner);
	}

	/**
	 * Устанавливает спиннеры на указанные значения
	 * @param hour час, который нужно установить спиннеру часов
	 * @param minute минута, которую нужно установить спиннеру минут
	 * @param second секунда, которую нужно установить спиннеру секунд
	 */
	public void set(int hour, int minute, int second) {
		hourSpinnerModel.setValue(hour);
		minuteSpinnerModel.setValue(minute);
		secondSpinnerModel.setValue(second);
	}

	/**
	 * @return выбранный час
	 */
	public int getHour() {
		return hourSpinnerModel.getNumber().intValue();
	}

	/**
	 * @return выбранная минута
	 */
	public int getMinute() {
		return minuteSpinnerModel.getNumber().intValue();
	}

	/**
	 * @return выбранная секунда
	 */
	public int getSecond() { return secondSpinnerModel.getNumber().intValue(); }

	/**
	 * Устанавливает спиннеры на нужное время с помощью указанного времени в миллисекундах.
	 * @param time время, в миллисекундах, которое нужно применить к спиннерам
	 */
	public void setTimeMillis(long time) {
		TimeUnit unit = TimeUnit.MILLISECONDS;
		hourSpinnerModel.setValue(unit.toHours(time)%24);
		minuteSpinnerModel.setValue(unit.toMinutes(time)%60);
		secondSpinnerModel.setValue(unit.toSeconds(time)%60);
	}

	/**
	 * @return выбранное время в миллисекундах
	 */
	public long getTimeMillis() {
		return TimeUnit.SECONDS.toMillis((getHour() * 60 + getMinute())
				* 60 + getSecond());
	}

	@Override
	public void setToolTipText(String text) {
		// костыль, чтобы установить тултип всем спиннерам
		super.setToolTipText(text);
		for (Component component: getComponents()) {
			if (component instanceof JComponent) {
				((JComponent) component).setToolTipText(text);
			}
		}
	}
}
