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
package knoblul.eosvstubot.utils;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Calendar;

/**
 * Простой класс-утилита, который помогает в синхронизации
 * с настоящим временем и временем на компьютере.
 * В ленивой инициализации поулчает смещение, которое
 * нужно отнять от локального времени чтобы получить
 * настоящее UTC время.
 *
 * <br><br>Module: eos-vstu-bot
 * <br>Created: 28.04.2020 16:48
 * @author Knoblul
 */
public class TimeUtils {
	private static final String NTP_SERVER_HOST = "pool.ntp.org";
	private static long ntpOffset;

	static {
		requestNtpCorrection();
	}

	private static void requestNtpCorrection() {
		Log.trace("Requesting NTP time correction...");
		NTPUDPClient ntpClient = new NTPUDPClient();
		try {
			ntpClient.open();
			TimeInfo info = ntpClient.getTime(InetAddress.getByName(NTP_SERVER_HOST));
			NtpV3Packet message = info.getMessage();
			ntpOffset = message.getReceiveTimeStamp().getTime() - message.getOriginateTimeStamp().getTime()
					+ message.getTransmitTimeStamp().getTime() - info.getReturnTime();
			Log.trace("NTP time correction request successfull. UTC offset generated.");
		} catch (IOException e) {
			Log.warn(e, "Failed to get NTP time correction");
		} finally {
			ntpClient.close();
		}
	}

	/**
	 * @return смещение времени, которое нужно вычесть из локального, чтобы
	 * получить UTC-локальное время.
	 */
	public static long getUtcOffset() {
		Calendar calendar = Calendar.getInstance();
		return calendar.getTimeZone().getOffset(calendar.getTimeInMillis());
	}

	/**
	 * Конвертирует UTC-время в локальное.
	 * @param utcTime UTC-время, в миллисекундах
	 * @return локальное время, в миллисекундах
	 */
	public static long convertUTCtoLocal(long utcTime) {
		return utcTime - ntpOffset + getUtcOffset();
	}

	/**
	 * Конвертирует локальное время в "настоящее" (из интернета) UTC.
	 * @param localTime локальное время, в миллисекундах
	 * @return "Настоящее" (интернет) UTC-время, в миллисекундах
	 */
	public static long convertLocalToUTC(long localTime) {
		return localTime + ntpOffset - getUtcOffset();
	}
}
