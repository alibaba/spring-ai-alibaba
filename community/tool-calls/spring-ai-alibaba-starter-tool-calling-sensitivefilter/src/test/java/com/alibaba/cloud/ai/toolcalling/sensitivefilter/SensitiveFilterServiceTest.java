package com.alibaba.cloud.ai.toolcalling.sensitivefilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SensitiveFilterService 单元测试
 */
@DisplayName("敏感信息过滤服务测试")
class SensitiveFilterServiceTest {

	private SensitiveFilterProperties properties;

	private SensitiveFilterService service;

	@BeforeEach
	void setUp() {
		properties = new SensitiveFilterProperties();
		service = new SensitiveFilterService(properties);
	}

	@Test
	@DisplayName("测试手机号脱敏")
	void testPhoneNumberFiltering() {
		String text = "我的手机号是13912345678，请联系我";
		String result = service.apply(text);
		assertThat(result).isEqualTo("我的手机号是***，请联系我");
	}

	@Test
	@DisplayName("测试身份证号脱敏")
	void testIdCardFiltering() {
		String text = "我的身份证号是110101199001011234";
		String result = service.apply(text);
		assertThat(result).isEqualTo("我的身份证号是***");
	}

	@Test
	@DisplayName("测试银行卡号脱敏")
	void testBankCardFiltering() {
		String text = "我的银行卡号是4123456789012345";
		String result = service.apply(text);
		assertThat(result).isEqualTo("我的银行卡号是***");
	}

	@Test
	@DisplayName("测试邮箱脱敏")
	void testEmailFiltering() {
		String text = "我的邮箱是user@example.com";
		String result = service.apply(text);
		assertThat(result).isEqualTo("我的邮箱是***");
	}

	@Test
	@DisplayName("测试多种敏感信息混合脱敏")
	void testMixedSensitiveInfoFiltering() {
		String text = "联系方式：手机13912345678，邮箱user@example.com，身份证110101199001011234";
		String result = service.apply(text);
		assertThat(result).isEqualTo("联系方式：手机***，邮箱***，身份证***");
	}

	@Test
	@DisplayName("测试自定义脱敏规则")
	void testCustomPatternFiltering() {
		// 配置自定义QQ号脱敏规则
		SensitiveFilterProperties.CustomPattern qqPattern = new SensitiveFilterProperties.CustomPattern();
		qqPattern.setName("qq");
		qqPattern.setPattern("QQ[：:]?\\d{5,11}");
		qqPattern.setReplacement("[QQ号]");
		qqPattern.setEnabled(true);

		List<SensitiveFilterProperties.CustomPattern> customPatterns = new ArrayList<>();
		customPatterns.add(qqPattern);
		properties.setCustomPatterns(customPatterns);

		service = new SensitiveFilterService(properties);

		String text = "我的QQ：123456789";
		String result = service.apply(text);
		assertThat(result).isEqualTo("我的[QQ号]");
	}

	@Test
	@DisplayName("测试自定义替换文本")
	void testCustomReplacement() {
		properties.setReplacement("[已脱敏]");
		service = new SensitiveFilterService(properties);

		String text = "手机号：13912345678";
		String result = service.apply(text);
		assertThat(result).isEqualTo("手机号：[已脱敏]");
	}

	@Test
	@DisplayName("测试禁用手机号过滤")
	void testDisablePhoneNumberFiltering() {
		properties.setFilterPhoneNumber(false);
		service = new SensitiveFilterService(properties);

		String text = "手机号：13912345678";
		String result = service.apply(text);
		assertThat(result).isEqualTo("手机号：13912345678");
	}

	@Test
	@DisplayName("测试禁用身份证号过滤")
	void testDisableIdCardFiltering() {
		properties.setFilterIdCard(false);
		service = new SensitiveFilterService(properties);

		String text = "身份证：110101199001011234";
		String result = service.apply(text);
		assertThat(result).isEqualTo("身份证：110101199001011234");
	}

	@Test
	@DisplayName("测试禁用银行卡号过滤")
	void testDisableBankCardFiltering() {
		properties.setFilterBankCard(false);
		service = new SensitiveFilterService(properties);

		String text = "银行卡：4123456789012345";
		String result = service.apply(text);
		assertThat(result).isEqualTo("银行卡：4123456789012345");
	}

	@Test
	@DisplayName("测试禁用邮箱过滤")
	void testDisableEmailFiltering() {
		properties.setFilterEmail(false);
		service = new SensitiveFilterService(properties);

		String text = "邮箱：user@example.com";
		String result = service.apply(text);
		assertThat(result).isEqualTo("邮箱：user@example.com");
	}

	@Test
	@DisplayName("测试空输入")
	void testNullAndEmptyInput() {
		assertThat(service.apply(null)).isNull();
		assertThat(service.apply("")).isEmpty();
		assertThat(service.apply("   ")).isEqualTo("   ");
	}

	@Test
	@DisplayName("测试不包含敏感信息的文本")
	void testTextWithoutSensitiveInfo() {
		String text = "这是一段普通的文本，没有任何敏感信息";
		String result = service.apply(text);
		assertThat(result).isEqualTo(text);
	}

	@Test
	@DisplayName("测试边界情况 - 不完整的手机号")
	void testIncompletePhoneNumber() {
		String text = "不完整手机号：139123456";
		String result = service.apply(text);
		assertThat(result).isEqualTo("不完整手机号：139123456");
	}

	@Test
	@DisplayName("测试边界情况 - 不完整的身份证号")
	void testIncompleteIdCard() {
		String text = "不完整身份证：11010119900101";
		String result = service.apply(text);
		assertThat(result).isEqualTo("不完整身份证：11010119900101");
	}

	@Test
	@DisplayName("测试Function接口实现")
	void testFunctionInterface() {
		java.util.function.Function<String, String> function = service;
		String result = function.apply("手机：13912345678");
		assertThat(result).isEqualTo("手机：***");
	}

	@Test
	@DisplayName("测试无效正则表达式的自定义规则")
	void testInvalidCustomPattern() {
		SensitiveFilterProperties.CustomPattern invalidPattern = new SensitiveFilterProperties.CustomPattern();
		invalidPattern.setName("invalid");
		invalidPattern.setPattern("[invalid"); // 无效的正则表达式
		invalidPattern.setEnabled(true);

		List<SensitiveFilterProperties.CustomPattern> customPatterns = new ArrayList<>();
		customPatterns.add(invalidPattern);
		properties.setCustomPatterns(customPatterns);

		// 应该不会抛出异常，而是忽略无效的规则
		service = new SensitiveFilterService(properties);
		String text = "测试文本";
		String result = service.apply(text);
		assertThat(result).isEqualTo("测试文本");
	}

	@Test
	@DisplayName("测试禁用的自定义规则")
	void testDisabledCustomPattern() {
		SensitiveFilterProperties.CustomPattern disabledPattern = new SensitiveFilterProperties.CustomPattern();
		disabledPattern.setName("disabled");
		disabledPattern.setPattern("test");
		disabledPattern.setEnabled(false);

		List<SensitiveFilterProperties.CustomPattern> customPatterns = new ArrayList<>();
		customPatterns.add(disabledPattern);
		properties.setCustomPatterns(customPatterns);

		service = new SensitiveFilterService(properties);

		String text = "test content";
		String result = service.apply(text);
		assertThat(result).isEqualTo("test content");
	}

	@Test
	@DisplayName("测试复杂场景 - 避免误匹配")
	void testAvoidFalseMatches() {
		// 测试不应该被误匹配的情况
		String text = "商品编号：123456789012345，这不是银行卡号";
		String result = service.apply(text);
		// 应该保持原样，因为不是以4-6开头的银行卡号
		assertThat(result).isEqualTo(text);
	}

}