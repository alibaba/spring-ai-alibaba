package com.alibaba.cloud.ai.functioncalling.jsonprocessor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

/**
 * @author 北极星
 */
@ConditionalOnClass({ JsonInsertService.class, JsonRemoveService.class, JsonReplaceService.class,
		JsonParseService.class })
@ConditionalOnProperty(value = "spring.ai.alibaba.functioncalling.jsonprocessor", name = "enabled",
		havingValue = "true")
public class JsonAutoConfiguration {

	@Bean
	@Description("use Gson to insert a jsonObject property field .")
	@ConditionalOnMissingBean
	public JsonInsertService jsonInsertPropertyFieldFunction() {
		return new JsonInsertService();
	}

	@Bean
	@Description("use Gson to parse String JsonObject .")
	@ConditionalOnMissingBean
	public JsonParseService jsonParsePropertyFunction() {
		return new JsonParseService();
	}

	@Bean
	@Description("use Gson to remove JsonObject property field .")
	@ConditionalOnMissingBean
	public JsonRemoveService jsonRemovePropertyFieldFunction() {
		return new JsonRemoveService();
	}

	@Bean
	@Description("use Gson to replace JsonObject Field Value .")
	@ConditionalOnMissingBean
	public JsonReplaceService jsonReplacePropertyFiledValueFunction() {
		return new JsonReplaceService();
	}

}
