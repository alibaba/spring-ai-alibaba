/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.util;

import org.apache.commons.lang3.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTimeUtil {

	public static final Pattern SPECIFIC_YEAR_MONTH_DAY_PATTERN = Pattern.compile("\\d{4}年\\d{2}月\\d{2}日");

	public static final Pattern GENERAL_YEAR_MONTH_DAY_PATTERN = Pattern.compile("(今年|去年|前年|明年|后年)(\\d{2}月\\d{2}日)");

	public static final Pattern GENERAL_MONTH_DAY_PATTERN = Pattern.compile("(本月|上月|上上月|下月)(\\d{2}日)");

	public static final Pattern GENERAL_DAY_PATTERN = Pattern.compile("(今天|昨天|前天|明天|后天|上月今天|上上月今天)");

	public static final Pattern WEEK_DAY_PATTERN = Pattern.compile("本周第(\\d)天");

	public static final Pattern GENERAL_MONTH_LAST_DAY_PATTERN = Pattern.compile("(本月|上月)最后一天");

	public static final Pattern GENERAL_YEAR_MONTH_LAST_DAY_PATTERN = Pattern.compile("(今年)(\\d{2})月最后一天");

	public static final Pattern GENERAL_WEEK_SPECIFIC_DAY_PATTERN = Pattern.compile("(本周|上周|上上周|下周|下下周)星期(\\d)");

	public static final Pattern SPECIFIC_YEAR_MONTH_PATTERN = Pattern.compile("\\d{4}年\\d{2}月");

	public static final Pattern GENERAL_YEAR_MONTH_PATTERN = Pattern.compile("(今年|去年|前年|明年|后年)(\\d{2}月)");

	public static final Pattern GENERAL_MONTH_PATTERN = Pattern.compile("(本月|上月|上上月|下月|去年本月)");

	public static final Pattern SPECIFIC_YEAR_PATTERN = Pattern.compile("(\\d{4})年");

	public static final Pattern GENERAL_YEAR_PATTERN = Pattern.compile("(今年|去年|前年|明年|后年)");

	public static final Pattern SPECIFIC_YEAR_QUARTER_PATTERN = Pattern.compile("\\d{4}年第\\d季度");

	public static final Pattern GENERAL_YEAR_QUARTER_PATTERN = Pattern.compile("(今年|去年|前年|明年|后年)(第\\d季度)");

	public static final Pattern GENERAL_QUARTER_PATTERN = Pattern.compile("(本季度|上季度|下季度|去年本季度)");

	public static final Pattern GENERAL_WEEK_PATTERN = Pattern.compile("(本周|上周|上上周|下周|下下周)");

	public static final Pattern SPECIFIC_YEAR_WEEK_PATTERN = Pattern.compile("(\\d{4})年第(\\d{2})周");

	public static final Pattern SPECIFIC_YEAR_MONTH_WEEK_PATTERN = Pattern.compile("(\\d{4})年(\\d{2})月第(\\d)周");

	public static final Pattern GENERAL_YEAR_WEEK_PATTERN = Pattern.compile("(今年|去年|前年|明年|后年)第(\\d{2})周");

	public static final Pattern GENERAL_MONTH_WEEK_PATTERN = Pattern.compile("(本月|上月)第(\\d)周");

	public static final Pattern GENERAL_YEAR_MONTH_WEEK_PATTERN = Pattern.compile("(今年|去年|前年|明年|后年)(\\d{2})月第(\\d)周");

	public static final Pattern SPECIFIC_YEAR_MONTH_LAST_WEEK_PATTERN = Pattern.compile("(\\d{4})年(\\d{2})月最后一周");

	public static final Pattern GENERAL_MONTH_LAST_WEEK_PATTERN = Pattern.compile("(本月|上月|上上月)最后一周");

	public static final Pattern SPECIFIC_YEAR_MONTH_COMPLETE_WEEK_PATTERN = Pattern
		.compile("(\\d{4})年(\\d{2})月第(\\d)个完整周");

	public static final Pattern GENERAL_YEAR_COMPLETE_WEEK_PATTERN = Pattern.compile("(今年|去年|前年|明年|后年)第(\\d{2})个完整周");

	public static final Pattern SPECIFIC_YEAR_COMPLETE_WEEK_PATTERN = Pattern.compile("(\\d{4})年第(\\d{2})个完整周");

	public static final Pattern GENERAL_YEAR_MONTH_COMPLETE_WEEK_PATTERN = Pattern
		.compile("(今年|去年|前年|明年|后年)(\\d{2})月第(\\d)个完整周");

	public static final Pattern GENERAL_MONTH_COMPLETE_WEEK_PATTERN = Pattern.compile("(本月|上月)第(\\d)个完整周");

	public static final Pattern GENERAL_MONTH_LAST_COMPLETE_WEEK_PATTERN = Pattern.compile("(本月|上月|上上月)最后一个完整周");

	public static final Pattern RECENT_N_YEAR_PATTERN = Pattern.compile("近(\\d+)年");

	public static final Pattern RECENT_N_MONTH_PATTERN = Pattern.compile("近(\\d+)个月");

	public static final Pattern RECENT_N_WEEK_PATTERN = Pattern.compile("近(\\d+)周");

	public static final Pattern RECENT_N_DAY_PATTERN = Pattern.compile("近(\\d+)天");

	public static final Pattern RECENT_N_COMPLETE_YEAR_PATTERN = Pattern.compile("近(\\d+)个完整年");

	public static final Pattern RECENT_N_COMPLETE_QUARTER_PATTERN = Pattern.compile("近(\\d+)个完整季度");

	public static final Pattern RECENT_N_COMPLETE_MONTH_PATTERN = Pattern.compile("近(\\d+)个完整月");

	public static final Pattern RECENT_N_COMPLETE_WEEK_PATTERN = Pattern.compile("近(\\d+)个完整周");

	public static final Pattern RECENT_N_DAY_WITHOUT_TODAY_PATTERN = Pattern.compile("不包含今天的近(\\d+)天");

	public static final Pattern RECENT_N_QUARTER_WITH_CURRENT_PATTERN = Pattern.compile("包含当前季度的近(\\d+)个季度");

	public static final Pattern SPECIFIC_YEAR_HALF_YEAR_PATTERN = Pattern.compile("(\\d{4})年(上|下)半年");

	public static final Pattern GENERAL_YEAR_HALF_YEAR_PATTERN = Pattern.compile("(今年|去年|前年|明年|后年)(上|下)半年");

	public static final Pattern HALF_YEAR_PATTERN = Pattern.compile("(上|下)半年");

	public static String buildDateTimeComment(List<String> expressions) {
		LocalDate now = LocalDate.now();
		// 获取年，月，日
		int year = now.getYear();
		int month = now.getMonthValue();
		int day = now.getDayOfMonth();

		// 获取当年的季度
		int quarter = now.get(IsoFields.QUARTER_OF_YEAR);

		String todayComment = String.format("今天是%d年%02d月%02d日，是%d年的第%d季度", year, month, day, year, quarter);

		List<String> dateTimeCommentList = buildDateExpressions(expressions, now);

		StringBuilder finalExpression = new StringBuilder();
		finalExpression.append(todayComment).append("\n");
		finalExpression.append("需要计算的时间是：\n");
		dateTimeCommentList.forEach(comment -> finalExpression.append(comment).append("\n"));
		return finalExpression.toString();
	}

	public static List<String> buildDateExpressions(List<String> expressions, LocalDate now) {
		List<String> dateTimeCommentList = new ArrayList<>();
		for (String expression : expressions) {
			Matcher specificYearMonthDayMatcher = SPECIFIC_YEAR_MONTH_DAY_PATTERN.matcher(expression);
			if (specificYearMonthDayMatcher.matches()) {
				dateTimeCommentList.add(expression + "=" + expression);
				continue;
			}

			Matcher generalYearMonthDayMatcher = GENERAL_YEAR_MONTH_DAY_PATTERN.matcher(expression);
			if (generalYearMonthDayMatcher.matches()) {
				String yearEx = generalYearMonthDayMatcher.group(1);
				String comment = getYearEx(now, yearEx, false) + generalYearMonthDayMatcher.group(2);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher generalMonthDayMatcher = GENERAL_MONTH_DAY_PATTERN.matcher(expression);
			if (generalMonthDayMatcher.matches()) {
				String monthEx = generalMonthDayMatcher.group(1);
				String comment = getMonthEx(now, monthEx) + generalMonthDayMatcher.group(2);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher yearMonthLastDayMatcher = GENERAL_YEAR_MONTH_LAST_DAY_PATTERN.matcher(expression);
			if (yearMonthLastDayMatcher.matches()) {
				String yearEx = yearMonthLastDayMatcher.group(1);
				String monthEx = yearMonthLastDayMatcher.group(2);
				String comment = getGeneralYearMonthLastDayEx(now, yearEx, Integer.valueOf(monthEx));
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher monthLastDayMatcher = GENERAL_MONTH_LAST_DAY_PATTERN.matcher(expression);
			if (monthLastDayMatcher.matches()) {
				String monthEx = monthLastDayMatcher.group(1);
				String comment = getMonthLastDayEx(now, monthEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher weekDayMatcher = WEEK_DAY_PATTERN.matcher(expression);
			if (weekDayMatcher.matches()) {
				int weekDay = Integer.parseInt(weekDayMatcher.group(1));
				String comment = getWeekDayEx(now, weekDay);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher generalWeekDayMatcher = GENERAL_WEEK_SPECIFIC_DAY_PATTERN.matcher(expression);
			if (generalWeekDayMatcher.matches()) {
				String weekEx = generalWeekDayMatcher.group(1);
				int day = Integer.parseInt(generalWeekDayMatcher.group(2));
				String comment = getGeneralWeekDayEx(now, weekEx, day);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher specificYearQuarterMatcher = SPECIFIC_YEAR_QUARTER_PATTERN.matcher(expression);
			if (specificYearQuarterMatcher.matches()) {
				dateTimeCommentList.add(expression + "=" + expression);
				continue;
			}

			Matcher generalYearQuarterMatcher = GENERAL_YEAR_QUARTER_PATTERN.matcher(expression);
			if (generalYearQuarterMatcher.matches()) {
				String yearEx = generalYearQuarterMatcher.group(1);
				String quarterEx = generalYearQuarterMatcher.group(2);
				String comment = getYearEx(now, yearEx, false) + quarterEx;
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher generalQuarterMatcher = GENERAL_QUARTER_PATTERN.matcher(expression);
			if (generalQuarterMatcher.matches()) {
				String quarterEx = generalQuarterMatcher.group(1);
				String comment = getQuarterEx(now, quarterEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher generalWeekMatcher = GENERAL_WEEK_PATTERN.matcher(expression);
			if (generalWeekMatcher.matches()) {
				String weekEx = generalWeekMatcher.group(1);
				String comment = getWeekEx(now, weekEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher specificYearWeekMatcher = SPECIFIC_YEAR_WEEK_PATTERN.matcher(expression);
			if (specificYearWeekMatcher.matches()) {
				int yearEx = Integer.parseInt(specificYearWeekMatcher.group(1));
				int weekEx = Integer.parseInt(specificYearWeekMatcher.group(2));
				String comment = getSpecificYearWeekEx(now, yearEx, weekEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher generalYearWeekMatcher = GENERAL_YEAR_WEEK_PATTERN.matcher(expression);
			if (generalYearWeekMatcher.matches()) {
				String yearEx = generalYearWeekMatcher.group(1);
				int weekEx = Integer.parseInt(generalYearWeekMatcher.group(2));
				String comment = getGeneralYearWeekEx(now, yearEx, weekEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher generalMonthWeekMatcher = GENERAL_MONTH_WEEK_PATTERN.matcher(expression);
			if (generalMonthWeekMatcher.matches()) {
				String monthEx = generalMonthWeekMatcher.group(1);
				int weekEx = Integer.parseInt(generalMonthWeekMatcher.group(2));
				String comment = getGeneralMonthWeekEx(now, monthEx, weekEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher specificYearMonthLastWeekMatcher = SPECIFIC_YEAR_MONTH_LAST_WEEK_PATTERN.matcher(expression);
			if (specificYearMonthLastWeekMatcher.matches()) {
				int yearEx = Integer.parseInt(specificYearMonthLastWeekMatcher.group(1));
				int monthEx = Integer.parseInt(specificYearMonthLastWeekMatcher.group(2));
				String comment = getSpecificYearMonthLastWeek(now, yearEx, monthEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher generalMonthLastWeekMatcher = GENERAL_MONTH_LAST_WEEK_PATTERN.matcher(expression);
			if (generalMonthLastWeekMatcher.matches()) {
				String monthEx = generalMonthLastWeekMatcher.group(1);
				String comment = getGeneralMonthLastWeek(now, monthEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher generalMonthLastCompleteWeekMatcher = GENERAL_MONTH_LAST_COMPLETE_WEEK_PATTERN.matcher(expression);
			if (generalMonthLastCompleteWeekMatcher.matches()) {
				String monthEx = generalMonthLastCompleteWeekMatcher.group(1);
				String comment = getGeneralMonthLastCompleteWeekEx(now, monthEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher recentNYearMatcher = RECENT_N_YEAR_PATTERN.matcher(expression);
			if (recentNYearMatcher.matches()) {
				int n = Integer.parseInt(recentNYearMatcher.group(1));
				String comment = getRecentNYear(now, n);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher recentNMonthMatcher = RECENT_N_MONTH_PATTERN.matcher(expression);
			if (recentNMonthMatcher.matches()) {
				int n = Integer.parseInt(recentNMonthMatcher.group(1));
				String comment = getRecentNMonth(now, n);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher recentNWeekMatcher = RECENT_N_WEEK_PATTERN.matcher(expression);
			if (recentNWeekMatcher.matches()) {
				int n = Integer.parseInt(recentNWeekMatcher.group(1));
				String comment = getRecentNWeek(now, n);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher recentNDayWithoutTodayMatcher = RECENT_N_DAY_WITHOUT_TODAY_PATTERN.matcher(expression);
			if (recentNDayWithoutTodayMatcher.matches()) {
				int n = Integer.parseInt(recentNDayWithoutTodayMatcher.group(1));
				String comment = getRecentNDayWithoutToday(now, n);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher recentNDayMatcher = RECENT_N_DAY_PATTERN.matcher(expression);
			if (recentNDayMatcher.matches()) {
				int n = Integer.parseInt(recentNDayMatcher.group(1));
				String comment = getRecentNDay(now, n);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher recentNCompleteYearMatcher = RECENT_N_COMPLETE_YEAR_PATTERN.matcher(expression);
			if (recentNCompleteYearMatcher.matches()) {
				int n = Integer.parseInt(recentNCompleteYearMatcher.group(1));
				String comment = getRecentNCompleteYear(now, n);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher recentNCompleteQuarterMatcher = RECENT_N_COMPLETE_QUARTER_PATTERN.matcher(expression);
			if (recentNCompleteQuarterMatcher.matches()) {
				int n = Integer.parseInt(recentNCompleteQuarterMatcher.group(1));
				String comment = getRecentNCompleteQuarter(now, n);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher recentNCompleteMonthMatcher = RECENT_N_COMPLETE_MONTH_PATTERN.matcher(expression);
			if (recentNCompleteMonthMatcher.matches()) {
				int n = Integer.parseInt(recentNCompleteMonthMatcher.group(1));
				String comment = getRecentNCompleteMonth(now, n);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher recentNCompleteWeekMatcher = RECENT_N_COMPLETE_WEEK_PATTERN.matcher(expression);
			if (recentNCompleteWeekMatcher.matches()) {
				int n = Integer.parseInt(recentNCompleteWeekMatcher.group(1));
				String comment = getRecentNCompleteWeek(now, n);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher recentNQuarterWithCurrentMatcher = RECENT_N_QUARTER_WITH_CURRENT_PATTERN.matcher(expression);
			if (recentNQuarterWithCurrentMatcher.matches()) {
				int n = Integer.parseInt(recentNQuarterWithCurrentMatcher.group(1));
				String comment = getRecentNQuarterWithCurrent(now, n);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher specificYearMonthMatcher = SPECIFIC_YEAR_MONTH_PATTERN.matcher(expression);
			if (specificYearMonthMatcher.matches()) {
				dateTimeCommentList.add(expression + "=" + expression);
				continue;
			}

			Matcher generalYearMonthMatcher = GENERAL_YEAR_MONTH_PATTERN.matcher(expression);
			if (generalYearMonthMatcher.matches()) {
				String yearEx = generalYearMonthMatcher.group(1);
				String comment = getYearEx(now, yearEx, false) + generalYearMonthMatcher.group(2);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher generalDayMatcher = GENERAL_DAY_PATTERN.matcher(expression);
			if (generalDayMatcher.matches()) {
				String dayEx = generalDayMatcher.group(1);
				String comment = getDayEx(now, dayEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher generalMonthMatcher = GENERAL_MONTH_PATTERN.matcher(expression);
			if (generalMonthMatcher.matches()) {
				String monthEx = generalMonthMatcher.group(1);
				String comment = getMonthEx(now, monthEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher specificYearMatcher = SPECIFIC_YEAR_PATTERN.matcher(expression);
			if (specificYearMatcher.matches()) {
				int yearEx = Integer.parseInt(specificYearMatcher.group(1));
				String comment = String.valueOf(yearEx) + "年";
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher generalYearMatcher = GENERAL_YEAR_PATTERN.matcher(expression);
			if (generalYearMatcher.matches()) {
				String yearEx = generalYearMatcher.group(1);
				String comment = getYearEx(now, yearEx, true);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher specificYearMonthWeekMatcher = SPECIFIC_YEAR_MONTH_WEEK_PATTERN.matcher(expression);
			if (specificYearMonthWeekMatcher.matches()) {
				int yearEx = Integer.parseInt(specificYearMonthWeekMatcher.group(1));
				int monthEx = Integer.parseInt(specificYearMonthWeekMatcher.group(2));
				int weekEx = Integer.parseInt(specificYearMonthWeekMatcher.group(3));
				String comment = getSpecificYearMonthWeekEx(now, yearEx, monthEx, weekEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher generalYearMonthWeekMatcher = GENERAL_YEAR_MONTH_WEEK_PATTERN.matcher(expression);
			if (generalYearMonthWeekMatcher.matches()) {
				String yearEx = generalYearMonthWeekMatcher.group(1);
				int monthEx = Integer.parseInt(generalYearMonthWeekMatcher.group(2));
				int weekEx = Integer.parseInt(generalYearMonthWeekMatcher.group(3));
				String comment = getGeneralYearMonthWeekEx(now, yearEx, monthEx, weekEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher specificYearMonthCompleteWeekMatcher = SPECIFIC_YEAR_MONTH_COMPLETE_WEEK_PATTERN
				.matcher(expression);
			if (specificYearMonthCompleteWeekMatcher.matches()) {
				int yearEx = Integer.parseInt(specificYearMonthCompleteWeekMatcher.group(1));
				int monthEx = Integer.parseInt(specificYearMonthCompleteWeekMatcher.group(2));
				int weekEx = Integer.parseInt(specificYearMonthCompleteWeekMatcher.group(3));
				String comment = getSpecificYearMonthCompleteWeekEx(now, yearEx, monthEx, weekEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher generalYearMonthCompleteWeekMatcher = GENERAL_YEAR_MONTH_COMPLETE_WEEK_PATTERN.matcher(expression);
			if (generalYearMonthCompleteWeekMatcher.matches()) {
				String yearEx = generalYearMonthCompleteWeekMatcher.group(1);
				int monthEx = Integer.parseInt(generalYearMonthCompleteWeekMatcher.group(2));
				int weekEx = Integer.parseInt(generalYearMonthCompleteWeekMatcher.group(3));
				String comment = getGeneralYearMonthCompleteWeekEx(now, yearEx, monthEx, weekEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher generalMonthCompleteWeekMatcher = GENERAL_MONTH_COMPLETE_WEEK_PATTERN.matcher(expression);
			if (generalMonthCompleteWeekMatcher.matches()) {
				String monthEx = generalMonthCompleteWeekMatcher.group(1);
				int weekEx = Integer.parseInt(generalMonthCompleteWeekMatcher.group(2));
				String comment = getGeneralMonthCompleteWeekEx(now, monthEx, weekEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher specificYearCompleteWeekMatcher = SPECIFIC_YEAR_COMPLETE_WEEK_PATTERN.matcher(expression);
			if (specificYearCompleteWeekMatcher.matches()) {
				int yearEx = Integer.parseInt(specificYearCompleteWeekMatcher.group(1));
				int weekEx = Integer.parseInt(specificYearCompleteWeekMatcher.group(2));
				String comment = getSpecificYearCompleteWeekEx(now, yearEx, weekEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher generalYearCompleteWeekMatcher = GENERAL_YEAR_COMPLETE_WEEK_PATTERN.matcher(expression);
			if (generalYearCompleteWeekMatcher.matches()) {
				String yearEx = generalYearCompleteWeekMatcher.group(1);
				int weekEx = Integer.parseInt(generalYearCompleteWeekMatcher.group(2));
				String comment = getGeneralYearCompleteWeekEx(now, yearEx, weekEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher specificYearHalfYearMatcher = SPECIFIC_YEAR_HALF_YEAR_PATTERN.matcher(expression);
			if (specificYearHalfYearMatcher.matches()) {
				int yearEx = Integer.parseInt(specificYearHalfYearMatcher.group(1));
				String halfYearEx = specificYearHalfYearMatcher.group(2);
				String comment = getSpecificYearHalfYearEx(now, yearEx, halfYearEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher generalYearHalfYearMatcher = GENERAL_YEAR_HALF_YEAR_PATTERN.matcher(expression);
			if (generalYearHalfYearMatcher.matches()) {
				String yearEx = generalYearHalfYearMatcher.group(1);
				String halfYearEx = generalYearHalfYearMatcher.group(2);
				String comment = getGeneralYearHalfYearEx(now, yearEx, halfYearEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

			Matcher halfYearMatcher = HALF_YEAR_PATTERN.matcher(expression);
			if (halfYearMatcher.matches()) {
				String halfYearEx = halfYearMatcher.group(1);
				String comment = getSpecificYearHalfYearEx(now, now.getYear(), halfYearEx);
				dateTimeCommentList.add(expression + "=" + comment);
				continue;
			}

		}

		return dateTimeCommentList;
	}

	public static String getYearEx(LocalDate now, String yearEx, boolean applyDomainLogic) {
		String comment = "";
		int year = 0, month = 0, quarter = 0;
		if (yearEx.equals("今年")) {
			year = now.getYear();
		}
		else if (yearEx.equals("去年")) {
			year = now.getYear() - 1;
		}
		else if (yearEx.equals("前年")) {
			year = now.getYear() - 2;
		}
		else if (yearEx.equals("明年")) {
			year = now.getYear() + 1;
		}
		else if (yearEx.equals("后年")) {
			year = now.getYear() + 2;
		}

		comment = String.valueOf(year) + "年";

		return comment;
	}

	public static String getMonthEx(LocalDate now, String monthEx) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月");
		String comment = "";
		if (monthEx.equals("本月")) {
			comment = formatter.format(YearMonth.from(now));
		}
		else if (monthEx.equals("上月")) {
			comment = formatter.format(YearMonth.from(now).minusMonths(1));
		}
		else if (monthEx.equals("上上月")) {
			comment = formatter.format(YearMonth.from(now).minusMonths(2));
		}
		else if (monthEx.equals("下月")) {
			comment = formatter.format(YearMonth.from(now).plusMonths(1));
		}
		else if (monthEx.equals("去年本月")) {
			comment = formatter.format(YearMonth.from(now).minusYears(1));
		}
		return comment;
	}

	public static String getDayEx(LocalDate now, String dayEx) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		String comment = "";
		try {
			if (dayEx.equals("今天")) {
				comment = formatter.format(now);
			}
			else if (dayEx.equals("昨天")) {
				comment = formatter.format(now.minusDays(1));
			}
			else if (dayEx.equals("前天")) {
				comment = formatter.format(now.minusDays(2));
			}
			else if (dayEx.equals("明天")) {
				comment = formatter.format(now.plusDays(1));
			}
			else if (dayEx.equals("后天")) {
				comment = formatter.format(now.plusDays(2));
			}
			else if (dayEx.equals("上月今天")) {
				comment = formatter.format(YearMonth.from(now).minusMonths(1).atDay(now.getDayOfMonth()));
			}
			else if (dayEx.equals("上上月今天")) {
				comment = formatter.format(YearMonth.from(now).minusMonths(2).atDay(now.getDayOfMonth()));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return comment;
	}

	public static final String getWeekDayEx(LocalDate now, int x) {

		// 计算本周第一天（周一）的日期
		LocalDate monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		// 通过加上(x - 1)天来得到本周第x天的日期
		LocalDate desiredDay = monday.plusDays(x - 1);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(desiredDay);
	}

	public static final String getGeneralWeekDayEx(LocalDate now, String weekEx, int day) {
		LocalDate thisMonday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate desiredDay = thisMonday.plusDays(day - 1);
		if (weekEx.equals("本周")) {

		}
		else if (weekEx.equals("上周")) {
			desiredDay = desiredDay.minusWeeks(1);
		}
		else if (weekEx.equals("上上周")) {
			desiredDay = desiredDay.minusWeeks(2);
		}
		else if (weekEx.equals("下周")) {
			desiredDay = desiredDay.plusWeeks(1);
		}
		else if (weekEx.equals("下下周")) {
			desiredDay = desiredDay.plusWeeks(2);
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(desiredDay);
	}

	public static final String getWeekEx(LocalDate now, String weekEx) {
		LocalDate desireMonday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate desireSunday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
		if (weekEx.equals("本周")) {

		}
		else if (weekEx.equals("上周")) {
			desireMonday = desireMonday.minusWeeks(1);
			desireSunday = desireSunday.minusWeeks(1);
		}
		else if (weekEx.equals("上上周")) {
			desireMonday = desireMonday.minusWeeks(2);
			desireSunday = desireSunday.minusWeeks(2);
		}
		else if (weekEx.equals("下周")) {
			desireMonday = desireMonday.plusWeeks(1);
			desireSunday = desireSunday.plusWeeks(1);
		}
		else if (weekEx.equals("下下周")) {
			desireMonday = desireMonday.plusWeeks(2);
			desireSunday = desireSunday.plusWeeks(2);
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(desireMonday) + "至" + formatter.format(desireSunday);
	}

	public static String getSpecificYearWeekEx(LocalDate now, int year, int week) {
		LocalDate firstDayOfYear = LocalDate.of(year, 1, 1);
		LocalDate targetWeekFirstDay = firstDayOfYear.plusWeeks(week - 1);
		LocalDate targetWeekLastDay = targetWeekFirstDay.plusDays(6);
		LocalDate lastDayOfYear = firstDayOfYear.with(TemporalAdjusters.lastDayOfYear());
		if (lastDayOfYear.isBefore(targetWeekLastDay)) {
			targetWeekLastDay = lastDayOfYear;
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(targetWeekFirstDay) + "至" + formatter.format(targetWeekLastDay);
	}

	public static String getGeneralYearWeekEx(LocalDate now, String yearEx, int week) {
		int year = now.getYear();
		if (yearEx.equals("今年")) {

		}
		else if (yearEx.equals("去年")) {
			year = now.getYear() - 1;
		}
		else if (yearEx.equals("前年")) {
			year = now.getYear() - 2;
		}
		else if (yearEx.equals("明年")) {
			year = now.getYear() + 1;
		}
		else if (yearEx.equals("后年")) {
			year = now.getYear() + 2;
		}
		return getSpecificYearWeekEx(now, year, week);
	}

	public static String getSpecificYearMonthWeekEx(LocalDate now, int year, int month, int week) {
		LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
		LocalDate targetWeekFirstDay = firstDayOfMonth.plusWeeks(week - 1);
		LocalDate targetWeekLastDay = targetWeekFirstDay.plusDays(6);
		LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());
		if (lastDayOfMonth.isBefore(targetWeekLastDay)) {
			targetWeekLastDay = lastDayOfMonth;
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(targetWeekFirstDay) + "至" + formatter.format(targetWeekLastDay);
	}

	public static String getGeneralYearMonthWeekEx(LocalDate now, String yearEx, int month, int week) {
		int year = now.getYear();
		if (yearEx.equals("今年")) {

		}
		else if (yearEx.equals("去年")) {
			year = now.getYear() - 1;
		}
		else if (yearEx.equals("前年")) {
			year = now.getYear() - 2;
		}
		else if (yearEx.equals("明年")) {
			year = now.getYear() + 1;
		}
		else if (yearEx.equals("后年")) {
			year = now.getYear() + 2;
		}
		return getSpecificYearMonthWeekEx(now, year, month, week);
	}

	public static String getGeneralMonthWeekEx(LocalDate now, String monthEx, int week) {
		int year = now.getYear();
		int month = now.getMonthValue();
		if (monthEx.equals("本月")) {

		}
		else if (monthEx.equals("上月")) {
			month = now.getMonthValue() - 1;
			if (month <= 0) {
				year--;
				month = 12 + month;
			}
		}
		LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
		LocalDate targetWeekFirstDay = firstDayOfMonth.plusWeeks(week - 1);
		LocalDate targetWeekLastDay = targetWeekFirstDay.plusDays(6);
		LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());
		if (lastDayOfMonth.isBefore(targetWeekLastDay)) {
			targetWeekLastDay = lastDayOfMonth;
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(targetWeekFirstDay) + "至" + formatter.format(targetWeekLastDay);
	}

	public static String getSpecificYearMonthCompleteWeekEx(LocalDate now, int year, int month, int week) {
		LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
		LocalDate firstMonday = firstDayOfMonth.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
		LocalDate targetStartDate = firstMonday.plusWeeks(week - 1);
		LocalDate targetEndDate = targetStartDate.plusDays(6);
		LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());
		if (lastDayOfMonth.isBefore(targetEndDate)) {
			return StringUtils.EMPTY;
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(targetStartDate) + "至" + formatter.format(targetEndDate);
	}

	public static String getGeneralYearMonthCompleteWeekEx(LocalDate now, String yearEx, int month, int week) {
		int year = now.getYear();
		if (yearEx.equals("今年")) {

		}
		else if (yearEx.equals("去年")) {
			year = now.getYear() - 1;
		}
		else if (yearEx.equals("前年")) {
			year = now.getYear() - 2;
		}
		else if (yearEx.equals("明年")) {
			year = now.getYear() + 1;
		}
		else if (yearEx.equals("后年")) {
			year = now.getYear() + 2;
		}
		return getSpecificYearMonthCompleteWeekEx(now, year, month, week);
	}

	public static String getGeneralMonthCompleteWeekEx(LocalDate now, String monthEx, int week) {
		int year = now.getYear();
		int month = now.getMonthValue();
		if (monthEx.equals("本月")) {

		}
		else if (monthEx.equals("上月")) {
			month = now.getMonthValue() - 1;
			if (month <= 0) {
				year--;
				month = 12 + month;
			}
		}
		else if (monthEx.equals("上上月")) {
			month = now.getMonthValue() - 2;
			if (month <= 0) {
				year--;
				month = 12 + month;
			}
		}
		else if (monthEx.equals("下月")) {
			month = now.getMonthValue() + 1;
			if (month > 12) {
				year++;
				month = month - 12;
			}
		}
		return getSpecificYearMonthCompleteWeekEx(now, year, month, week);
	}

	public static String getSpecificYearCompleteWeekEx(LocalDate now, int year, int week) {
		LocalDate firstDayOfYear = LocalDate.of(year, 1, 1);
		LocalDate firstMonday = firstDayOfYear.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
		LocalDate targetStartDate = firstMonday.plusWeeks(week - 1);
		LocalDate targetEndDate = targetStartDate.plusDays(6);
		LocalDate lastDayOfYear = firstDayOfYear.with(TemporalAdjusters.lastDayOfYear());
		if (lastDayOfYear.isBefore(targetEndDate)) {
			return StringUtils.EMPTY;
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(targetStartDate) + "至" + formatter.format(targetEndDate);
	}

	public static String getGeneralYearCompleteWeekEx(LocalDate now, String yearEx, int week) {
		int year = now.getYear();
		if (yearEx.equals("今年")) {

		}
		else if (yearEx.equals("去年")) {
			year = now.getYear() - 1;
		}
		else if (yearEx.equals("前年")) {
			year = now.getYear() - 2;
		}
		else if (yearEx.equals("明年")) {
			year = now.getYear() + 1;
		}
		else if (yearEx.equals("后年")) {
			year = now.getYear() + 2;
		}
		return getSpecificYearCompleteWeekEx(now, year, week);
	}

	public static String getSpecificYearMonthLastWeek(LocalDate now, int year, int month) {
		LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
		LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());
		LocalDate previousMonday = lastDayOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(previousMonday) + "至" + formatter.format(lastDayOfMonth);
	}

	public static String getGeneralMonthLastWeek(LocalDate now, String monthEx) {
		int year = now.getYear();
		int month = now.getMonthValue();
		if (monthEx.equals("本月")) {

		}
		else if (monthEx.equals("上月")) {
			month = now.getMonthValue() - 1;
			if (month <= 0) {
				year--;
				month = 12 + month;
			}
		}
		else if (monthEx.equals("上上月")) {
			month = now.getMonthValue() - 2;
			if (month <= 0) {
				year--;
				month = 12 + month;
			}
		}
		else if (monthEx.equals("下月")) {
			month = now.getMonthValue() + 1;
			if (month > 12) {
				year++;
				month = month - 12;
			}
		}
		return getSpecificYearMonthLastWeek(now, year, month);
	}

	public static String getSpecificYearMonthLastCompleteWeekEx(LocalDate now, int year, int month) {
		LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
		LocalDate lastSunday = firstDayOfMonth.with(TemporalAdjusters.lastInMonth(DayOfWeek.SUNDAY));
		LocalDate lastMonday = lastSunday.minusDays(6);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(lastMonday) + "至" + formatter.format(lastSunday);
	}

	public static String getGeneralMonthLastCompleteWeekEx(LocalDate now, String monthEx) {
		int year = now.getYear();
		int month = now.getMonthValue();
		if (monthEx.equals("本月")) {

		}
		else if (monthEx.equals("上月")) {
			month = now.getMonthValue() - 1;
			if (month <= 0) {
				year--;
				month = 12 + month;
			}
		}
		else if (monthEx.equals("上上月")) {
			month = now.getMonthValue() - 2;
			if (month <= 0) {
				year--;
				month = 12 + month;
			}
		}
		else if (monthEx.equals("下月")) {
			month = now.getMonthValue() + 1;
			if (month > 12) {
				year++;
				month = month - 12;
			}
		}
		return getSpecificYearMonthLastCompleteWeekEx(now, year, month);
	}

	public static String getQuarterEx(LocalDate now, String quarterEx) {
		int currentQuarter = now.get(IsoFields.QUARTER_OF_YEAR);
		// 计算上一个季度和下一个季度
		int lastQuarter = currentQuarter == 1 ? 4 : currentQuarter - 1;
		int nextQuarter = currentQuarter == 4 ? 1 : currentQuarter + 1;
		int currentYear = now.getYear();
		int yearOfLastQuarter = (currentQuarter == 1) ? currentYear - 1 : currentYear;
		int yearOfNextQuarter = (currentQuarter == 4) ? currentYear + 1 : currentYear;
		int yearOfSameQuarterLastYear = currentYear - 1;

		String comment = "";
		if (quarterEx.equals("本季度")) {
			comment = currentYear + "年第" + currentQuarter + "季度";
		}
		else if (quarterEx.equals("上季度")) {
			comment = yearOfLastQuarter + "年第" + lastQuarter + "季度";
		}
		else if (quarterEx.equals("下季度")) {
			comment = yearOfNextQuarter + "年第" + nextQuarter + "季度";
		}
		else if (quarterEx.equals("去年本季度")) {
			comment = yearOfSameQuarterLastYear + "年第" + currentQuarter + "季度";
		}
		return comment;
	}

	public static String getRecentNYear(LocalDate now, int n) {
		LocalDate startDate = now.minusYears(n);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(startDate) + "至" + formatter.format(now);
	}

	public static String getRecentNMonth(LocalDate now, int n) {
		LocalDate startDate = now.minusMonths(n);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(startDate) + "至" + formatter.format(now);
	}

	public static String getRecentNWeek(LocalDate now, int n) {
		LocalDate startDate = now.minusWeeks(n);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(startDate) + "至" + formatter.format(now);
	}

	public static String getRecentNDay(LocalDate now, int n) {
		LocalDate startDate = now.minusDays(n);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(startDate) + "至" + formatter.format(now);
	}

	public static String getRecentNCompleteYear(LocalDate now, int n) {
		LocalDate endDate;
		if (now.getMonthValue() == 12 && now.getDayOfMonth() == 31) {
			endDate = now;
		}
		else {
			endDate = now.with(TemporalAdjusters.lastDayOfYear()).minusYears(1);
		}
		LocalDate startDate = endDate.minusYears(n).plusDays(1);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(startDate) + "至" + formatter.format(endDate);
	}

	public static String getRecentNCompleteMonth(LocalDate now, int n) {
		LocalDate endDate;
		if (now.equals(now.with(TemporalAdjusters.lastDayOfMonth()))) {
			endDate = now;
		}
		else {
			endDate = now.with(TemporalAdjusters.firstDayOfMonth())
				.minusMonths(1)
				.with(TemporalAdjusters.lastDayOfMonth());
		}
		LocalDate startDate = endDate.minusMonths(n).plusDays(1);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(startDate) + "至" + formatter.format(endDate);
	}

	public static String getRecentNCompleteQuarter(LocalDate now, int n) {
		LocalDate endDate;
		int currentMonth = now.getMonthValue();
		if (currentMonth % 4 == 0 && now.getDayOfMonth() == 31) {
			endDate = now;
		}
		else {
			if (currentMonth < 4) {
				endDate = LocalDate.of(now.getYear() - 1, 12, 31);
			}
			else if (currentMonth < 7) {
				endDate = LocalDate.of(now.getYear(), 3, 31);
			}
			else if (currentMonth < 10) {
				endDate = LocalDate.of(now.getYear(), 6, 30);
			}
			else {
				endDate = LocalDate.of(now.getYear(), 9, 30);
			}
		}
		LocalDate startDate = endDate.minusMonths(n * 3).plusDays(1);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(startDate) + "至" + formatter.format(endDate);
	}

	public static String getRecentNCompleteWeek(LocalDate now, int n) {
		LocalDate endDate;
		if (now.getDayOfWeek().getValue() == 7) {
			endDate = now;
		}
		else {
			endDate = now.minusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
		}
		LocalDate startDate = endDate.minusWeeks(n).plusDays(1);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(startDate) + "至" + formatter.format(endDate);
	}

	public static String getRecentNDayWithoutToday(LocalDate now, int n) {
		LocalDate startDate = now.minusDays(n);
		LocalDate endDate = now.minusDays(1);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(startDate) + "至" + formatter.format(endDate);
	}

	public static String getRecentNQuarterWithCurrent(LocalDate now, int n) {
		LocalDate endDate;
		int currentMonth = now.getMonthValue();
		if (currentMonth < 4) {
			endDate = LocalDate.of(now.getYear(), 3, 31);
		}
		else if (currentMonth < 7) {
			endDate = LocalDate.of(now.getYear(), 6, 30);
		}
		else if (currentMonth < 10) {
			endDate = LocalDate.of(now.getYear(), 9, 30);
		}
		else {
			endDate = LocalDate.of(now.getYear(), 12, 31);
		}
		LocalDate startDate = endDate.minusMonths(n * 3).plusDays(1);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(startDate) + "至" + formatter.format(endDate);
	}

	public static String getMonthLastDayEx(LocalDate now, String monthEx) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		String comment = "";
		if (monthEx.equals("本月")) {
			comment = formatter.format(YearMonth.from(now).atEndOfMonth());
		}
		else if (monthEx.equals("上月")) {
			comment = formatter.format(YearMonth.from(now).minusMonths(1).atEndOfMonth());
		}
		return comment;
	}

	public static String getGeneralYearMonthLastDayEx(LocalDate now, String yearEx, int month) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		String comment = "";
		int year = 0;
		if (yearEx.equals("今年")) {
			year = now.getYear();
		}
		else if (yearEx.equals("去年")) {
			year = now.getYear() - 1;
		}
		else if (yearEx.equals("前年")) {
			year = now.getYear() - 2;
		}
		else if (yearEx.equals("明年")) {
			year = now.getYear() + 1;
		}
		else if (yearEx.equals("后年")) {
			year = now.getYear() + 2;
		}
		comment = formatter.format(YearMonth.of(year, month).atEndOfMonth());
		return comment;
	}

	public static String getSpecificYearHalfYearEx(LocalDate now, int year, String halfYearEx) {
		LocalDate startDate;
		LocalDate endDate;
		if (halfYearEx.equals("上")) {
			startDate = LocalDate.of(year, 1, 1);
			endDate = LocalDate.of(year, 6, 30);
		}
		else {
			startDate = LocalDate.of(year, 7, 1);
			endDate = LocalDate.of(year, 12, 31);
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
		return formatter.format(startDate) + "至" + formatter.format(endDate);
	}

	public static String getGeneralYearHalfYearEx(LocalDate now, String yearEx, String halfYearEx) {
		int year = 0;
		if (yearEx.equals("今年")) {
			year = now.getYear();
		}
		else if (yearEx.equals("去年")) {
			year = now.getYear() - 1;
		}
		else if (yearEx.equals("前年")) {
			year = now.getYear() - 2;
		}
		else if (yearEx.equals("明年")) {
			year = now.getYear() + 1;
		}
		else if (yearEx.equals("后年")) {
			year = now.getYear() + 2;
		}
		return getSpecificYearHalfYearEx(now, year, halfYearEx);
	}

	public static void main(String[] args) {
		List<String> expressions = Arrays.asList("2023年02月最后一周", "2024年05月01日", "今年04月22日", "去年04月22日", "前年04月22日",
				"明年04月22日", "后年04月22日", "本月22日", "上月22日", "上上月22日", "下月22日", "上月今天", "上上月今天", "今天", "明天", "后天", "昨天",
				"前天", "本周第5天", "2023年06月", "2024年05月", "今年03月", "去年03月", "前年03月", "明年03月", "后年03月", "本月", "上月", "上上月",
				"下月", "去年本月", "2024年", "2023年", "今年", "去年", "前年", "明年", "后年", "2023年第3季度", "今年第1季度", "去年第3季度", "前年第3季度",
				"明年第3季度", "后年第3季度", "本季度", "上季度", "下季度", "去年本季度", "本周星期3", "本周星期1", "上周星期1", "下周星期7", "近1周", "近1个完整周",
				"本月最后一周", "本月最后一天", "近2个完整月", "不包含今天的近10天", "今年02月最后一天", "2024年02月最后一周", "今年上半年");

		// JSONObject domainObject = new JSONObject();
		// domainObject.put("currentYearTemplate",
		// "${year}年，${year}年是当前年，查询年度数据时，如果使用月表，取${year}年${month}月的数据，如果使用季度表，取${year}年第${quarter}季度的数据");
		// domainObject.put("pastYearTemplate",
		// "${year}年，${year}是历史年，查询年度数据时，如果使用月表，取${year}年12月的数据，如果使用季度表，取${year}年第4季度的数据");

		System.out.println(DateTimeUtil.buildDateTimeComment(expressions));
	}

}
