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
package knoblul.eosvstubot;

import knoblul.eosvstubot.backend.BotContext;
import knoblul.eosvstubot.frontend.BotUI;
import knoblul.eosvstubot.utils.Log;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 21.04.2020 12:17
 * @author Knoblul
 */
public class EosVstuBot {
	public static final String NAME = "EosVstuBot";
	public static final String VERSION = "0.1.1";
	public static final String SITE = "http://eos.vstu.ru";

	public static void main(String[] args) {
		Log.info("%s v%s", NAME, VERSION);
		BotContext context = new BotContext();
		try {
			context.create();
			new BotUI(context);
			context.processMainThreadCommands();
		} finally {
			context.destroy();
		}

//		SimpleDateFormat sdf = new SimpleDateFormat("E HH:mm:ss", Locale.ENGLISH);

//		Calendar scheduleTime = Calendar.getInstance();
//		scheduleTime.setTime(new Date(0));
//		scheduleTime.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
//		scheduleTime.set(Calendar.HOUR, 16);
//		scheduleTime.set(Calendar.MINUTE, 45);
//		scheduleTime.set(Calendar.SECOND, 0);
//		int scheduleWeekIndex = 0;
//
//		while (true) {
//			try {
//				Calendar calendar = Calendar.getInstance();
//
//				Calendar scheduleCalendar = Calendar.getInstance();
//				scheduleCalendar.set(Calendar.DAY_OF_WEEK, scheduleTime.get(Calendar.DAY_OF_WEEK));
//				scheduleCalendar.set(Calendar.HOUR, scheduleTime.get(Calendar.HOUR));
//				scheduleCalendar.set(Calendar.MINUTE, scheduleTime.get(Calendar.MINUTE));
//				scheduleCalendar.set(Calendar.SECOND, scheduleTime.get(Calendar.SECOND));
//
//				int currentWeekIndex = calendar.get(Calendar.WEEK_OF_YEAR) % 2;
//				if (currentWeekIndex != scheduleWeekIndex || calendar.after(scheduleCalendar)) {
//					// переносим на некст неделю
//					scheduleCalendar.add(Calendar.WEEK_OF_MONTH, 1);
//				}
//
//				long remainingTime = scheduleCalendar.getTimeInMillis() - calendar.getTimeInMillis();
//				Log.info("Remaining: %d:%02d:%02d:%02d", TimeUnit.MILLISECONDS.toDays(remainingTime),
//						TimeUnit.MILLISECONDS.toHours(remainingTime)%24, TimeUnit.MILLISECONDS.toMinutes(remainingTime)%60,
//						TimeUnit.MILLISECONDS.toSeconds(remainingTime)%60);
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				break;
//			}
//		}
	}
}
