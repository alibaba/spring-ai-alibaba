/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.studio.core.utils.common;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for date and time operations.
 *
 * @since 1.0.0.3
 */
public class DateUtils extends org.apache.commons.lang3.time.DateUtils {

	private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

	// Date format patterns
	public static final String DATE_JFP_STR = "yyyyMM";

	public static final String DATE_YMD_STR = "yyyyMMdd";

	public static final String DATE_SMALL_STR = "yyyy-MM-dd";

	public static final String DATE_FULL_STR = "yyyy-MM-dd HH:mm:ss";

	public static final String DATE_KEY_STR = "yyyyMMddHHmmss";

	public static final String END_TIME_STR = "9999-12-31 23:59:59";

	public static final String DATE_SMALL_STR_SHOW = "yyyy年MM月dd号HH点mm分";

	public static final String DATE_MONTH_DAY_STR_SHOW = "MM月dd号";

	// Supported date parse patterns
	private static final String[] parsePatterns = { "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM",
			"yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM", "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss",
			"yyyy.MM.dd HH:mm", "yyyy.MM", "yyyyMMddHHmmss", "yyyyMMdd" };

	/**
	 * Format date to yyyyMMdd string
	 */
	public static String formatSimple(Date date) {
		if (date == null) {
			return null;
		}
		return DateFormatUtils.format(date, DATE_YMD_STR);
	}

	/**
	 * Get current date and time in full format (yyyy-MM-dd HH:mm:ss)
	 */
	public static String getFullDate() {
		return getDate(DATE_FULL_STR);
	}

	/**
	 * Get current date in yyyy-MM-dd format
	 */
	public static String getDate() {
		return getDate(DATE_SMALL_STR);
	}

	/**
	 * Get current date string in specified pattern
	 * @param pattern Date format pattern
	 */
	public static String getDate(String pattern) {
		return DateFormatUtils.format(new Date(), pattern);
	}

	/**
	 * Format date to string with specified pattern
	 * @param date Date to format
	 * @param pattern Format pattern
	 */
	public static String formatDate(Date date, Object... pattern) {
		String formatDate = null;
		if (pattern != null && pattern.length > 0) {
			formatDate = DateFormatUtils.format(date, pattern[0].toString());
		}
		else {
			formatDate = DateFormatUtils.format(date, DATE_SMALL_STR);
		}
		return formatDate;
	}

	/**
	 * Format date to full datetime string (yyyy-MM-dd HH:mm:ss)
	 */
	public static String formatDateTimeFull(Date date) {
		if (date == null) {
			return null;
		}
		return formatDate(date, DATE_FULL_STR);
	}

	/**
	 * Format date to specified format
	 */
	public static String formatDateTimeSmall(Date date, String format) {
		return formatDate(date, format);
	}

	/**
	 * Get current datetime in yyyyMMddHHmmss format
	 */
	public static String getDateTime14() {
		long currTime = System.currentTimeMillis();
		Date date = new Date(currTime);
		SimpleDateFormat format = new SimpleDateFormat(DATE_KEY_STR);
		return format.format(date);
	}

	/**
	 * Format timestamp to yyyyMMddHHmmss string
	 */
	public static String getDateTime14(long time) {
		Date date = new Date(time);
		SimpleDateFormat format = new SimpleDateFormat(DATE_FULL_STR);
		return format.format(date);
	}

	/**
	 * Get current time in HH:mm:ss format
	 */
	public static String getTime() {
		return formatDate(new Date(), "HH:mm:ss");
	}

	/**
	 * Get current datetime in yyyy-MM-dd HH:mm:ss format
	 */
	public static String getDateTime() {
		return formatDate(new Date(), DATE_FULL_STR);
	}

	/**
	 * Get current year
	 */
	public static String getYear() {
		return formatDate(new Date(), "yyyy");
	}

	/**
	 * Get current month
	 */
	public static String getMonth() {
		return formatDate(new Date(), "MM");
	}

	/**
	 * Get current day
	 */
	public static String getDay() {
		return formatDate(new Date(), "dd");
	}

	/**
	 * Get current weekday
	 */
	public static String getWeek() {
		return formatDate(new Date(), "E");
	}

	/**
	 * Get current hour
	 */
	public static Integer getHour() {
		Calendar calendar = Calendar.getInstance();
		return calendar.get(Calendar.HOUR_OF_DAY);
	}

	/**
	 * Parse date string to Date object Supports multiple date formats
	 */
	public static Date parseDate(Object str) {
		if (str == null) {
			return null;
		}
		try {
			return org.apache.commons.lang3.time.DateUtils.parseDate(str.toString(), parsePatterns);
		}
		catch (ParseException e) {
			return null;
		}
	}

	/**
	 * Parse date string in EEE MMM dd HH:mm:ss zzz yyyy format
	 */
	public static Date parseDateString(String time) {
		SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
		try {
			return sdf.parse(time);
		}
		catch (ParseException e) {
			logger.warn("Failed to parse date string: {}", time, e);
			return null;
		}
	}

	/**
	 * Calculate days between now and given date
	 */
	public static long pastDays(Date date) {
		long t = System.currentTimeMillis() - date.getTime();
		return t / (24 * 60 * 60 * 1000);
	}

	/**
	 * Calculate hours between now and given date
	 */
	public static long pastHour(Date date) {
		long t = System.currentTimeMillis() - date.getTime();
		return t / (60 * 60 * 1000);
	}

	/**
	 * Calculate minutes between now and given date
	 */
	public static long pastMinutes(Date date) {
		long t = System.currentTimeMillis() - date.getTime();
		return t / (60 * 1000);
	}

	/**
	 * Format milliseconds to time string (days,hours:minutes:seconds.milliseconds)
	 */
	public static String formatDateTime(long timeMillis) {
		long day = timeMillis / (24 * 60 * 60 * 1000);
		long hour = (timeMillis / (60 * 60 * 1000) - day * 24);
		long min = ((timeMillis / (60 * 1000)) - day * 24 * 60 - hour * 60);
		long s = (timeMillis / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);
		long sss = (timeMillis - day * 24 * 60 * 60 * 1000 - hour * 60 * 60 * 1000 - min * 60 * 1000 - s * 1000);
		return (day > 0 ? day + "," : "") + hour + ":" + min + ":" + s + "." + sss;
	}

	/**
	 * Calculate days between two dates
	 */
	public static double getDistanceOfTwoDate(Date before, Date after) {
		long beforeTime = before.getTime();
		long afterTime = after.getTime();
		return (afterTime - beforeTime) / (1000 * 60 * 60 * 24);
	}

	/**
	 * Add days to given date
	 */
	public static Date calculateDate(Date date, int num) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DATE, num);
		return calendar.getTime();
	}

	/**
	 * Calculate absolute days difference between two dates
	 */
	public static Integer differDate(Date newDate, Date oldDate) {
		int days = (int) ((newDate.getTime() - oldDate.getTime()) / (1000 * 3600 * 24));
		return Math.abs(days);
	}

	/**
	 * Calculate absolute hours difference between two dates
	 */
	public static Integer differDateHours(Date newDate, Date oldDate) {
		int hours = (int) ((newDate.getTime() - oldDate.getTime()) / (1000 * 60 * 60));
		return Math.abs(hours);
	}

	/**
	 * Calculate absolute minutes difference between two dates
	 */
	public static Integer differDateMinutes(Date newDate, Date oldDate) {
		int minutes = (int) ((newDate.getTime() - oldDate.getTime()) / (1000 * 60));
		return Math.abs(minutes);
	}

	/**
	 * Calculate minutes difference between two dates
	 */
	public static Integer differMinutes(Date newDate, Date oldDate) {
		return (int) ((newDate.getTime() - oldDate.getTime()) / (1000 * 60));
	}

	/**
	 * Calculate absolute seconds difference between two dates
	 */
	public static Integer differDateSeconds(Date newDate, Date oldDate) {
		int minutes = (int) ((newDate.getTime() - oldDate.getTime()) / (1000));
		return Math.abs(minutes);
	}

	/**
	 * Calculate seconds difference between two dates
	 */
	public static Integer differSeconds(Date newDate, Date oldDate) {
		int seconds = (int) ((newDate.getTime() - oldDate.getTime()) / (1000));
		return Math.max(seconds, 0);
	}

	/**
	 * Get first day of current month in yyyy-MM-dd format
	 */
	public static String getMonthFristDay() {
		SimpleDateFormat format = new SimpleDateFormat(DATE_SMALL_STR);
		// 获取当前月第一天：
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, 0);
		c.set(Calendar.DAY_OF_MONTH, 1);// 设置为1号,当前日期既为本月第一天
		String first = format.format(c.getTime());
		return first;
	}

	/**
	 * Get last day of current month in yyyy-MM-dd format
	 */
	public static String getMonthLastDay() {
		SimpleDateFormat format = new SimpleDateFormat(DATE_SMALL_STR);
		Calendar ca = Calendar.getInstance();
		ca.set(Calendar.DAY_OF_MONTH, ca.getActualMaximum(Calendar.DAY_OF_MONTH));
		String last = format.format(ca.getTime());
		System.out.println("===============last:" + last);
		return last;
	}

	/**
	 * Get yesterday's date in yyyyMMdd format
	 */
	public static String getYesterday() {
		SimpleDateFormat format = new SimpleDateFormat(DATE_YMD_STR);
		Calendar ca = Calendar.getInstance();
		ca.add(Calendar.DAY_OF_MONTH, -1);
		return format.format(ca.getTime());
	}

	/**
	 * Convert Unix timestamp to Date
	 */
	public static Date unixTimeToDate(Long unixTime) {
		SimpleDateFormat format = new SimpleDateFormat(DATE_FULL_STR);
		Long time = unixTime * 1000;
		String d = format.format(time);
		Date date = null;
		try {
			return format.parse(d);
		}
		catch (ParseException e) {
			e.getMessage();
			e.printStackTrace();
		}
		return date;
	}

	/**
	 * Convert Date to Unix timestamp
	 */
	public static Long toUnixTimestamp(Date date) {
		if (date == null) {
			return null;
		}

		return date.getTime() / 1000;
	}

	/**
	 * Get current hour's start time
	 */
	public static Date getCurrHourTime() {
		Calendar ca = Calendar.getInstance();
		ca.set(Calendar.MINUTE, 0);
		ca.set(Calendar.SECOND, 0);
		return ca.getTime();
	}

	/**
	 * Get N hours before current hour's start time
	 */
	public static Date getNextHourTime(int n) {
		Calendar ca = Calendar.getInstance();
		ca.set(Calendar.MINUTE, 0);
		ca.set(Calendar.SECOND, 0);
		ca.set(Calendar.HOUR_OF_DAY, ca.get(Calendar.HOUR_OF_DAY) - n);
		return ca.getTime();
	}

	/**
	 * Get date N days after current date
	 */
	public static Date getAfterDayTime(int n) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.DATE, n);
		return cal.getTime();
	}

	/**
	 * Get first second of N days after current date
	 */
	public static Date getFirstSecondOfAfterDayTime(int n) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, n);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		return cal.getTime();
	}

	/**
	 * Get date N hours after current date
	 */
	public static Date getAfterHourTime(int n) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.HOUR, n);
		return cal.getTime();
	}

	/**
	 * Get date N minutes after current date
	 */
	public static Date getAfterMinTime(int n) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.MINUTE, n);
		return cal.getTime();
	}

	/**
	 * Get date N minutes after given date
	 */
	public static Date getAfterMinTime(Date date, int n) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.MINUTE, n);
		return cal.getTime();
	}

	/**
	 * Get date N hours after given date
	 */
	public static Date getAfterHourTimeForDate(Date date, int n) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.HOUR, n);
		return cal.getTime();
	}

	/**
	 * Get date N hours before given date
	 */
	public static Date getBeforeHourTimeForDate(Date date, int n) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.HOUR, -n);
		return cal.getTime();
	}

	/**
	 * Get end time (9999-12-31 23:59:59)
	 */
	public static Date endTime() {
		return parseDate(END_TIME_STR);
	}

	/**
	 * Get last second of given date
	 */
	public static Date getLastSecondOfDay(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	/**
	 * Get last second of given hour
	 */
	public static Date getLastSecondOfHour(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	/**
	 * Get datetime for given hour of current day
	 */
	public static Date getGivenHourDateTime(String hourTime) {
		// 得到当天yyyy-mm-dd格式
		String dayTime = getDate();
		String fullTimeStr = dayTime + " " + hourTime;
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FULL_STR);
		try {
			Date date = sdf.parse(fullTimeStr);
			return date;
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * Calculate MetaQ delay level based on date difference
	 */
	public static int getMetaqDelayLevel(Date date) {
		int diffMinutes = differDateMinutes(new Date(), date);
		if (diffMinutes >= 0 && diffMinutes <= 10) {
			return diffMinutes + 4;
		}
		else if (diffMinutes > 10 && diffMinutes < 60) {
			return 14;
		}
		else if (diffMinutes >= 60 && diffMinutes < 120) {
			return 17;
		}
		return 18;

	}

	/**
	 * Validate if date string matches given format
	 */
	public static boolean isLegalDate(String sDate, String format) {
		if ((sDate == null) || (sDate.length() != format.length())) {
			return false;
		}
		DateFormat formatter = new SimpleDateFormat(format);
		try {
			Date date = formatter.parse(sDate);
			return sDate.equals(formatter.format(date));
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * Convert date string from one format to another
	 */
	public static String covertDateFormat(String dateStr, String format) {

		try {
			Date date = DateUtils.parseDate(dateStr, DateUtils.DATE_YMD_STR);
			return DateUtils.formatDateTimeSmall(date, format);
		}
		catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get start of current day
	 */
	public static Date getToday() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		return calendar.getTime();
	}

}
