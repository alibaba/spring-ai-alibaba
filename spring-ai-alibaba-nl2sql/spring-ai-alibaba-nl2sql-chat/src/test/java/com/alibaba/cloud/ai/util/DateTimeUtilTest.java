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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

class DateTimeUtilTest {

	private LocalDate testDate;

	@BeforeEach
	void setUp() {
		// 设置测试基准日期为 2024年5月15日 (周三)
		testDate = LocalDate.of(2024, 5, 15);
	}

	@Test
	void testBuildDateTimeComment() {
		List<String> expressions = Arrays.asList("今天", "昨天", "明天", "本月", "上月");
		String result = DateTimeUtil.buildDateTimeComment(expressions);

		assertNotNull(result);
		assertTrue(result.contains("今天是"));
		assertTrue(result.contains("需要计算的时间是："));
	}

	@Test
	void testBuildDateExpressions() {
		List<String> expressions = Arrays.asList("今天", "昨天", "明天");
		List<String> result = DateTimeUtil.buildDateExpressions(expressions, testDate);

		assertNotNull(result);
		assertEquals(3, result.size());
		assertTrue(result.get(0).contains("今天="));
		assertTrue(result.get(1).contains("昨天="));
		assertTrue(result.get(2).contains("明天="));
	}

	@Test
	void testSpecificYearMonthDayPattern() {
		List<String> expressions = Arrays.asList("2024年05月15日");
		List<String> result = DateTimeUtil.buildDateExpressions(expressions, testDate);

		assertEquals(1, result.size());
		assertEquals("2024年05月15日=2024年05月15日", result.get(0));
	}

	@Test
	void testGeneralYearMonthDayPattern() {
		List<String> expressions = Arrays.asList("今年05月01日", "去年05月01日");
		List<String> result = DateTimeUtil.buildDateExpressions(expressions, testDate);

		assertEquals(2, result.size());
		assertTrue(result.get(0).contains("今年05月01日=2024年05月01日"));
		assertTrue(result.get(1).contains("去年05月01日=2023年05月01日"));
	}

	@Test
	void testGetYearEx() {
		String todayYear = DateTimeUtil.getYearEx(testDate, "今年", true);
		assertEquals("2024年", todayYear);

		String lastYear = DateTimeUtil.getYearEx(testDate, "去年", true);
		assertEquals("2023年", lastYear);

		String nextYear = DateTimeUtil.getYearEx(testDate, "明年", true);
		assertEquals("2025年", nextYear);
	}

	@Test
	void testGetMonthEx() {
		String thisMonth = DateTimeUtil.getMonthEx(testDate, "本月");
		assertEquals("2024年05月", thisMonth);

		String lastMonth = DateTimeUtil.getMonthEx(testDate, "上月");
		assertEquals("2024年04月", lastMonth);

		String nextMonth = DateTimeUtil.getMonthEx(testDate, "下月");
		assertEquals("2024年06月", nextMonth);
	}

	@Test
	void testGetDayEx() {
		String today = DateTimeUtil.getDayEx(testDate, "今天");
		assertEquals("2024年05月15日", today);

		String yesterday = DateTimeUtil.getDayEx(testDate, "昨天");
		assertEquals("2024年05月14日", yesterday);

		String tomorrow = DateTimeUtil.getDayEx(testDate, "明天");
		assertEquals("2024年05月16日", tomorrow);
	}

	@Test
	void testGetWeekDayEx() {
		// 2024年5月15日是周三，所以本周第一天应该是5月13日（周一）
		String monday = DateTimeUtil.getWeekDayEx(testDate, 1);
		assertEquals("2024年05月13日", monday);

		String wednesday = DateTimeUtil.getWeekDayEx(testDate, 3);
		assertEquals("2024年05月15日", wednesday);

		String sunday = DateTimeUtil.getWeekDayEx(testDate, 7);
		assertEquals("2024年05月19日", sunday);
	}

	@Test
	void testGetGeneralWeekDayEx() {
		String thisWeekMonday = DateTimeUtil.getGeneralWeekDayEx(testDate, "本周", 1);
		assertEquals("2024年05月13日", thisWeekMonday);

		String lastWeekMonday = DateTimeUtil.getGeneralWeekDayEx(testDate, "上周", 1);
		assertEquals("2024年05月06日", lastWeekMonday);

		String nextWeekMonday = DateTimeUtil.getGeneralWeekDayEx(testDate, "下周", 1);
		assertEquals("2024年05月20日", nextWeekMonday);
	}

	@Test
	void testGetWeekEx() {
		String thisWeek = DateTimeUtil.getWeekEx(testDate, "本周");
		assertEquals("2024年05月13日至2024年05月19日", thisWeek);

		String lastWeek = DateTimeUtil.getWeekEx(testDate, "上周");
		assertEquals("2024年05月06日至2024年05月12日", lastWeek);
	}

	@Test
	void testGetQuarterEx() {
		String thisQuarter = DateTimeUtil.getQuarterEx(testDate, "本季度");
		assertEquals("2024年第2季度", thisQuarter);

		String lastQuarter = DateTimeUtil.getQuarterEx(testDate, "上季度");
		assertEquals("2024年第1季度", lastQuarter);
	}

	@Test
	void testGetRecentNDay() {
		String recent7Days = DateTimeUtil.getRecentNDay(testDate, 7);
		assertEquals("2024年05月08日至2024年05月15日", recent7Days);

		String recent30Days = DateTimeUtil.getRecentNDay(testDate, 30);
		assertEquals("2024年04月15日至2024年05月15日", recent30Days);
	}

	@Test
	void testGetRecentNDayWithoutToday() {
		String recent7DaysWithoutToday = DateTimeUtil.getRecentNDayWithoutToday(testDate, 7);
		assertEquals("2024年05月08日至2024年05月14日", recent7DaysWithoutToday);
	}

	@Test
	void testGetRecentNWeek() {
		String recent2Weeks = DateTimeUtil.getRecentNWeek(testDate, 2);
		assertEquals("2024年05月01日至2024年05月15日", recent2Weeks);
	}

	@Test
	void testGetRecentNMonth() {
		String recent3Months = DateTimeUtil.getRecentNMonth(testDate, 3);
		assertEquals("2024年02月15日至2024年05月15日", recent3Months);
	}

	@Test
	void testGetRecentNYear() {
		String recent2Years = DateTimeUtil.getRecentNYear(testDate, 2);
		assertEquals("2022年05月15日至2024年05月15日", recent2Years);
	}

	@Test
	void testGetMonthLastDayEx() {
		String thisMonthLastDay = DateTimeUtil.getMonthLastDayEx(testDate, "本月");
		assertEquals("2024年05月31日", thisMonthLastDay);

		String lastMonthLastDay = DateTimeUtil.getMonthLastDayEx(testDate, "上月");
		assertEquals("2024年04月30日", lastMonthLastDay);
	}

	@Test
	void testGetSpecificYearHalfYearEx() {
		String firstHalf = DateTimeUtil.getSpecificYearHalfYearEx(testDate, 2024, "上");
		assertEquals("2024年01月01日至2024年06月30日", firstHalf);

		String secondHalf = DateTimeUtil.getSpecificYearHalfYearEx(testDate, 2024, "下");
		assertEquals("2024年07月01日至2024年12月31日", secondHalf);
	}

	@Test
	void testGetGeneralYearHalfYearEx() {
		String thisYearFirstHalf = DateTimeUtil.getGeneralYearHalfYearEx(testDate, "今年", "上");
		assertEquals("2024年01月01日至2024年06月30日", thisYearFirstHalf);

		String lastYearSecondHalf = DateTimeUtil.getGeneralYearHalfYearEx(testDate, "去年", "下");
		assertEquals("2023年07月01日至2023年12月31日", lastYearSecondHalf);
	}

	@Test
	void testComplexExpressions() {
		List<String> expressions = Arrays.asList("2023年02月最后一周", "2024年05月01日", "今年04月22日", "本月22日", "上月今天", "今天",
				"本周第5天", "本季度", "近1周", "近2个完整月", "不包含今天的近10天", "今年上半年");

		List<String> result = DateTimeUtil.buildDateExpressions(expressions, testDate);

		assertNotNull(result);
		assertEquals(expressions.size(), result.size());

		// 验证每个表达式都有对应的转换结果
		for (int i = 0; i < expressions.size(); i++) {
			String expression = expressions.get(i);
			String resultExpression = result.get(i);
			assertTrue(resultExpression.startsWith(expression + "="));
		}
	}

	@Test
	void testEdgeCases() {
		// 测试年底边界情况
		LocalDate yearEnd = LocalDate.of(2023, 12, 31);
		String nextYear = DateTimeUtil.getYearEx(yearEnd, "明年", true);
		assertEquals("2024年", nextYear);

		// 测试月初边界情况
		LocalDate monthStart = LocalDate.of(2024, 1, 1);
		String lastMonth = DateTimeUtil.getMonthEx(monthStart, "上月");
		assertEquals("2023年12月", lastMonth);

		// 测试闰年情况
		LocalDate intercalarYear = LocalDate.of(2024, 2, 29);
		String today = DateTimeUtil.getDayEx(intercalarYear, "今天");
		assertEquals("2024年02月29日", today);
	}

}
