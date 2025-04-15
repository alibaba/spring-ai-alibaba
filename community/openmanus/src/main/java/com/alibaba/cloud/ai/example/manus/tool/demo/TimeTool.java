package com.alibaba.cloud.ai.example.manus.tool.demo;

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * 时间工具 用于处理时间格式转换，获取当前时间/时间戳等功能
 *
 * @time: 2025/4/14
 */
public class TimeTool implements ToolCallBiFunctionDef {

	private static final Logger log = LoggerFactory.getLogger(TimeTool.class);

	private BaseAgent agent;

	private static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";

	private static final String name = "time_tool";

	private static final String description = "日期时间处理工具，支持获取当前时间、时间戳转换、日期格式转换等功能。支持getCurrentTime(获取当前时间), getCurrentTimestamp(获取当前时间戳), formatTime(时间戳转格式化时间), parseTime(格式化时间转时间戳), convertFormat(时间格式转换)";

	private static final String PARAMETERS = """
			{
			  "type": "object",
			  "properties": {
			    "action": {
			      "type": "string",
			      "enum": ["getCurrentTime", "getCurrentTimestamp", "formatTime", "parseTime", "convertFormat"],
			      "description": "要执行的操作: getCurrentTime(获取当前时间), getCurrentTimestamp(获取当前时间戳), formatTime(时间戳转格式化时间), parseTime(格式化时间转时间戳), convertFormat(时间格式转换)"
			    },
			    "timestamp": {
			      "type": "number",
			      "description": "Unix时间戳(毫秒)。在formatTime操作中使用。"
			    },
			    "timeString": {
			      "type": "string",
			      "description": "格式化的时间字符串。在parseTime或convertFormat操作中使用。"
			    },
			    "sourceFormat": {
			      "type": "string",
			      "description": "源时间格式。在parseTime或convertFormat操作中使用。例如'yyyy-MM-dd HH:mm:ss'"
			    },
			    "targetFormat": {
			      "type": "string",
			      "description": "目标时间格式。在formatTime或convertFormat操作中使用。例如'yyyy/MM/dd HH:mm:ss'"
			    },
			    "timeZone": {
			      "type": "string",
			      "description": "时区，例如'Asia/Shanghai'，默认使用系统时区"
			    }
			  },
			  "required": ["action"]
			}
			""";

	/**
	 * 获取函数工具回调
	 * @return FunctionToolCallback实例
	 */
	public static FunctionToolCallback getFunctionToolCallback() {
		return FunctionToolCallback.builder(name, new TimeTool())
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.toolMetadata(ToolMetadata.builder().returnDirect(true).build())
			.build();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getParameters() {
		return PARAMETERS;
	}

	@Override
	public Class<?> getInputType() {
		return String.class;
	}

	@Override
	public boolean isReturnDirect() {
		return true;
	}

	@Override
	public void setAgent(BaseAgent agent) {
		this.agent = agent;
	}

	@Override
	public String getCurrentToolStateString() {
		return "TimeTool状态正常";
	}

	@Override
	public void cleanup(String planId) {
		// 无需清理资源
	}

	@Override
	public ToolExecuteResult apply(String input, ToolContext toolContext) {
		try {
			JSONObject params = JSON.parseObject(input);
			String action = params.getString("action");

			if (action == null || action.isEmpty()) {
				return new ToolExecuteResult("必须提供action参数");
			}

			String result;
			switch (action) {
				case "getCurrentTime":
					result = getCurrentTime(params);
					break;
				case "getCurrentTimestamp":
					result = getCurrentTimestamp();
					break;
				case "formatTime":
					result = formatTime(params);
					break;
				case "parseTime":
					result = parseTime(params);
					break;
				case "convertFormat":
					result = convertFormat(params);
					break;
				default:
					return new ToolExecuteResult("不支持的action: " + action);
			}

			return new ToolExecuteResult(result);
		}
		catch (Exception e) {
			log.error("TimeTool执行异常", e);
			return new ToolExecuteResult("执行异常: " + e.getMessage());
		}
	}

	/**
	 * 获取当前时间
	 * @param params 参数，可包含targetFormat和timeZone
	 * @return 格式化的当前时间
	 */
	private String getCurrentTime(JSONObject params) {
		String format = params.getString("targetFormat");
		if (format == null || format.isEmpty()) {
			format = DEFAULT_FORMAT;
		}

		String timeZoneId = params.getString("timeZone");
		ZoneId zoneId = timeZoneId != null && !timeZoneId.isEmpty() ? ZoneId.of(timeZoneId) : ZoneId.systemDefault();

		ZonedDateTime now = ZonedDateTime.now(zoneId);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

		return now.format(formatter);
	}

	/**
	 * 获取当前时间戳（毫秒）
	 * @return 当前时间戳字符串
	 */
	private String getCurrentTimestamp() {
		return String.valueOf(System.currentTimeMillis());
	}

	/**
	 * 时间戳转格式化时间
	 * @param params 参数，包含timestamp、targetFormat和可选的timeZone
	 * @return 格式化的时间字符串
	 */
	private String formatTime(JSONObject params) {
		Long timestamp = params.getLong("timestamp");
		if (timestamp == null) {
			return "错误：必须提供timestamp参数";
		}

		String format = params.getString("targetFormat");
		if (format == null || format.isEmpty()) {
			format = DEFAULT_FORMAT;
		}

		String timeZoneId = params.getString("timeZone");
		ZoneId zoneId = timeZoneId != null && !timeZoneId.isEmpty() ? ZoneId.of(timeZoneId) : ZoneId.systemDefault();

		LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

		return dateTime.format(formatter);
	}

	/**
	 * 格式化时间转时间戳
	 * @param params 参数，包含timeString、sourceFormat和可选的timeZone
	 * @return 时间戳字符串
	 */
	private String parseTime(JSONObject params) {
		String timeString = params.getString("timeString");
		if (timeString == null || timeString.isEmpty()) {
			return "错误：必须提供timeString参数";
		}

		String format = params.getString("sourceFormat");
		if (format == null || format.isEmpty()) {
			format = DEFAULT_FORMAT;
		}

		String timeZoneId = params.getString("timeZone");

		try {
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			if (timeZoneId != null && !timeZoneId.isEmpty()) {
				sdf.setTimeZone(TimeZone.getTimeZone(timeZoneId));
			}

			Date date = sdf.parse(timeString);
			return String.valueOf(date.getTime());
		}
		catch (ParseException e) {
			log.error("解析时间字符串失败", e);
			return "错误：解析时间失败：" + e.getMessage();
		}
	}

	/**
	 * 时间格式转换
	 * @param params 参数，包含timeString、sourceFormat、targetFormat和可选的timeZone
	 * @return 转换后的时间字符串
	 */
	private String convertFormat(JSONObject params) {
		String timeString = params.getString("timeString");
		if (timeString == null || timeString.isEmpty()) {
			return "错误：必须提供timeString参数";
		}

		String sourceFormat = params.getString("sourceFormat");
		if (sourceFormat == null || sourceFormat.isEmpty()) {
			sourceFormat = DEFAULT_FORMAT;
		}

		String targetFormat = params.getString("targetFormat");
		if (targetFormat == null || targetFormat.isEmpty()) {
			return "错误：必须提供targetFormat参数";
		}

		String timeZoneId = params.getString("timeZone");
		TimeZone timeZone = timeZoneId != null && !timeZoneId.isEmpty() ? TimeZone.getTimeZone(timeZoneId)
				: TimeZone.getDefault();

		try {
			// 先解析为Date对象
			SimpleDateFormat sourceSdf = new SimpleDateFormat(sourceFormat);
			sourceSdf.setTimeZone(timeZone);
			Date date = sourceSdf.parse(timeString);

			// 然后格式化为新格式
			SimpleDateFormat targetSdf = new SimpleDateFormat(targetFormat);
			targetSdf.setTimeZone(timeZone);
			return targetSdf.format(date);
		}
		catch (ParseException e) {
			log.error("时间格式转换失败", e);
			return "错误：时间格式转换失败：" + e.getMessage();
		}
	}

	/**
	 * 获取工具使用示例
	 * @return 示例JSON
	 */
	public static String getExamples() {
		Map<String, Object> examples = new HashMap<>();

		// 获取当前时间示例
		Map<String, Object> example1 = new HashMap<>();
		example1.put("action", "getCurrentTime");
		example1.put("targetFormat", "yyyy-MM-dd HH:mm:ss");
		examples.put("获取当前时间", example1);

		// 获取当前时间戳示例
		Map<String, Object> example2 = new HashMap<>();
		example2.put("action", "getCurrentTimestamp");
		examples.put("获取当前时间戳", example2);

		// 时间戳转格式化时间示例
		Map<String, Object> example3 = new HashMap<>();
		example3.put("action", "formatTime");
		example3.put("timestamp", 1618456789000L);
		example3.put("targetFormat", "yyyy年MM月dd日 HH时mm分ss秒");
		examples.put("时间戳转格式化时间", example3);

		// 格式化时间转时间戳示例
		Map<String, Object> example4 = new HashMap<>();
		example4.put("action", "parseTime");
		example4.put("timeString", "2025-04-14 12:34:56");
		example4.put("sourceFormat", "yyyy-MM-dd HH:mm:ss");
		examples.put("格式化时间转时间戳", example4);

		// 时间格式转换示例
		Map<String, Object> example5 = new HashMap<>();
		example5.put("action", "convertFormat");
		example5.put("timeString", "2025-04-14 12:34:56");
		example5.put("sourceFormat", "yyyy-MM-dd HH:mm:ss");
		example5.put("targetFormat", "MM/dd/yyyy hh:mm:ss a");
		examples.put("时间格式转换", example5);

		return JSON.toJSONString(examples, true);
	}

	public static void main(String[] args) {
		System.out.println(getExamples());
	}

}
