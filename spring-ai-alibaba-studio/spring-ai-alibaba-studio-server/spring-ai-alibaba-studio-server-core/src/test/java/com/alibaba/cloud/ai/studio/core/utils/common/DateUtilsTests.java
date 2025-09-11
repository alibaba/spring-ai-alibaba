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
