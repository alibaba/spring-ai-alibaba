package com.alibaba.cloud.ai.toolcalling.sensitivefilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SensitiveFilterProperties 单元测试
 */
@DisplayName("敏感信息过滤配置属性测试")
class SensitiveFilterPropertiesTest {

	private SensitiveFilterProperties properties;

	@BeforeEach
	void setUp() {
		properties = new SensitiveFilterProperties();
	}

	@Test
	@DisplayName("测试默认配置值")
	void testDefaultValues() {
		assertThat(properties.getReplacement()).isEqualTo("***");
		assertThat(properties.isFilterPhoneNumber()).isTrue();
		assertThat(properties.isFilterIdCard()).isTrue();
		assertThat(properties.isFilterBankCard()).isTrue();
		assertThat(properties.isFilterEmail()).isTrue();
		assertThat(properties.getCustomPatterns()).isNotNull().isEmpty();
	}

	@Test
	@DisplayName("测试替换文本设置")
	void testReplacementSetting() {
		properties.setReplacement("[HIDDEN]");
		assertThat(properties.getReplacement()).isEqualTo("[HIDDEN]");
	}

	@Test
	@DisplayName("测试手机号过滤开关")
	void testPhoneNumberFilterToggle() {
		properties.setFilterPhoneNumber(false);
		assertThat(properties.isFilterPhoneNumber()).isFalse();

		properties.setFilterPhoneNumber(true);
		assertThat(properties.isFilterPhoneNumber()).isTrue();
	}

	@Test
	@DisplayName("测试身份证号过滤开关")
	void testIdCardFilterToggle() {
		properties.setFilterIdCard(false);
		assertThat(properties.isFilterIdCard()).isFalse();

		properties.setFilterIdCard(true);
		assertThat(properties.isFilterIdCard()).isTrue();
	}

	@Test
	@DisplayName("测试银行卡号过滤开关")
	void testBankCardFilterToggle() {
		properties.setFilterBankCard(false);
		assertThat(properties.isFilterBankCard()).isFalse();

		properties.setFilterBankCard(true);
		assertThat(properties.isFilterBankCard()).isTrue();
	}

	@Test
	@DisplayName("测试邮箱过滤开关")
	void testEmailFilterToggle() {
		properties.setFilterEmail(false);
		assertThat(properties.isFilterEmail()).isFalse();

		properties.setFilterEmail(true);
		assertThat(properties.isFilterEmail()).isTrue();
	}

	@Test
	@DisplayName("测试自定义模式列表设置")
	void testCustomPatternsList() {
		List<SensitiveFilterProperties.CustomPattern> customPatterns = new ArrayList<>();

		SensitiveFilterProperties.CustomPattern pattern1 = new SensitiveFilterProperties.CustomPattern();
		pattern1.setName("test1");
		pattern1.setPattern("\\d{4}");
		customPatterns.add(pattern1);

		properties.setCustomPatterns(customPatterns);
		assertThat(properties.getCustomPatterns()).hasSize(1);
		assertThat(properties.getCustomPatterns().get(0).getName()).isEqualTo("test1");
	}

	@Test
	@DisplayName("测试自定义模式 - 基本属性")
	void testCustomPatternBasicProperties() {
		SensitiveFilterProperties.CustomPattern pattern = new SensitiveFilterProperties.CustomPattern();

		pattern.setName("testPattern");
		assertThat(pattern.getName()).isEqualTo("testPattern");

		pattern.setPattern("\\d+");
		assertThat(pattern.getPattern()).isEqualTo("\\d+");

		pattern.setReplacement("[NUM]");
		assertThat(pattern.getReplacement()).isEqualTo("[NUM]");
	}

	@Test
	@DisplayName("测试自定义模式 - 启用状态")
	void testCustomPatternEnabledState() {
		SensitiveFilterProperties.CustomPattern pattern = new SensitiveFilterProperties.CustomPattern();

		// 默认启用
		assertThat(pattern.isEnabled()).isTrue();

		pattern.setEnabled(false);
		assertThat(pattern.isEnabled()).isFalse();

		pattern.setEnabled(true);
		assertThat(pattern.isEnabled()).isTrue();
	}

	@Test
	@DisplayName("测试配置前缀常量")
	void testConfigurationPrefix() {
		// 通过反射或者直接访问静态常量来验证前缀
		assertThat(SensitiveFilterProperties.SENSITIVE_FILTER_PREFIX).contains("toolcalling")
			.contains("sensitivefilter");
	}

	@Test
	@DisplayName("测试复杂自定义模式配置")
	void testComplexCustomPatternConfiguration() {
		List<SensitiveFilterProperties.CustomPattern> customPatterns = new ArrayList<>();

		// QQ号模式
		SensitiveFilterProperties.CustomPattern qqPattern = new SensitiveFilterProperties.CustomPattern();
		qqPattern.setName("qq");
		qqPattern.setPattern("QQ[：:]?\\d{5,11}");
		qqPattern.setReplacement("[QQ]");
		qqPattern.setEnabled(true);

		// 微信号模式
		SensitiveFilterProperties.CustomPattern wechatPattern = new SensitiveFilterProperties.CustomPattern();
		wechatPattern.setName("wechat");
		wechatPattern.setPattern("微信[：:]?[a-zA-Z][a-zA-Z0-9_-]{5,19}");
		wechatPattern.setReplacement("[微信]");
		wechatPattern.setEnabled(false);

		customPatterns.add(qqPattern);
		customPatterns.add(wechatPattern);

		properties.setCustomPatterns(customPatterns);

		assertThat(properties.getCustomPatterns()).hasSize(2);

		SensitiveFilterProperties.CustomPattern retrievedQq = properties.getCustomPatterns().get(0);
		assertThat(retrievedQq.getName()).isEqualTo("qq");
		assertThat(retrievedQq.getPattern()).isEqualTo("QQ[：:]?\\d{5,11}");
		assertThat(retrievedQq.getReplacement()).isEqualTo("[QQ]");
		assertThat(retrievedQq.isEnabled()).isTrue();

		SensitiveFilterProperties.CustomPattern retrievedWechat = properties.getCustomPatterns().get(1);
		assertThat(retrievedWechat.getName()).isEqualTo("wechat");
		assertThat(retrievedWechat.isEnabled()).isFalse();
	}

	@Test
	@DisplayName("测试null值处理")
	void testNullValueHandling() {
		properties.setReplacement(null);
		assertThat(properties.getReplacement()).isNull();

		properties.setCustomPatterns(null);
		assertThat(properties.getCustomPatterns()).isNull();

		SensitiveFilterProperties.CustomPattern pattern = new SensitiveFilterProperties.CustomPattern();
		pattern.setName(null);
		pattern.setPattern(null);
		pattern.setReplacement(null);

		assertThat(pattern.getName()).isNull();
		assertThat(pattern.getPattern()).isNull();
		assertThat(pattern.getReplacement()).isNull();
	}

}