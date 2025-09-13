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

import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DateUtils}.
 */
class DateUtilsTests {

	@Test
	void parseDateStringReturnsDateWhenFormatMatches() throws Exception {
		String time = "Wed Sep 11 12:00:00 GMT 2024";
		Date expected = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH).parse(time);
		assertThat(DateUtils.parseDateString(time)).isEqualTo(expected);
	}

	@Test
	void parseDateStringReturnsNullOnParseFailure() {
		assertThat(DateUtils.parseDateString("invalid-date")).isNull();
	}

}
