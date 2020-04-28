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
package knoblul.eosvstubot.tests.passing;

import knoblul.eosvstubot.utils.Log;
import knoblul.eosvstubot.utils.TimeUtils;
import org.junit.Assert;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 28.04.2020 11:40
 * @author Knoblul
 */
public class TimeZoneTest extends Assert {
	@Test
	public void testTimeUtils() {
		long systemTime = System.currentTimeMillis();
		Log.info(new SimpleDateFormat("dd.MM.YYYY HH:mm:ss").format(new Date(systemTime)));

		long utcTime = TimeUtils.convertLocalToUTC(systemTime);
		Log.info(new SimpleDateFormat("dd.MM.YYYY HH:mm:ss").format(new Date(utcTime)));

		long localTime = TimeUtils.convertUTCtoLocal(utcTime);
		Log.info(new SimpleDateFormat("dd.MM.YYYY HH:mm:ss").format(new Date(localTime)));

		TimeUnit unit = TimeUnit.MILLISECONDS;
		long lessonTime = TimeUnit.HOURS.toMillis(10) + TimeUnit.MINUTES.toMillis(10);
		Log.info("%02d:%02d:%02d", unit.toHours(lessonTime)%24, unit.toMinutes(lessonTime)%60, unit.toSeconds(lessonTime)%60);

		long lessonUtcTime = lessonTime - TimeUtils.getUtcOffset();
		Log.info("%02d:%02d:%02d", unit.toHours(lessonUtcTime)%24, unit.toMinutes(lessonUtcTime)%60, unit.toSeconds(lessonUtcTime)%60);

		long lessonLocalTime = TimeUtils.convertUTCtoLocal(lessonUtcTime);
		Log.info("%02d:%02d:%02d", unit.toHours(lessonLocalTime)%24, unit.toMinutes(lessonLocalTime)%60, unit.toSeconds(lessonLocalTime)%60);

		long lessonNTPUTCTime = TimeUtils.convertLocalToUTC(lessonLocalTime);
		Log.info("%02d:%02d:%02d", unit.toHours(lessonNTPUTCTime)%24, unit.toMinutes(lessonNTPUTCTime)%60, unit.toSeconds(lessonNTPUTCTime)%60);

		Assert.assertEquals(systemTime, localTime);
	}
}
