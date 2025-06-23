/*
 * Copyright 2025-2026 the original author or authors.
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
 *
 * @author brianxiadong
 */

package com.alibaba.cloud.ai.example.graph.mcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 利用OpenMeteo的免费天气API提供天气服务 该API无需API密钥，可以直接使用
 */
@Service
public class OpenMeteoService {

	// OpenMeteo免费天气API基础URL
	private static final String BASE_URL = "https://api.open-meteo.com/v1";

	private final RestClient restClient;

	public OpenMeteoService() {
		this.restClient = RestClient.builder()
			.baseUrl(BASE_URL)
			.defaultHeader("Accept", "application/json")
			.defaultHeader("User-Agent", "OpenMeteoClient/1.0")
			.build();
	}

	// OpenMeteo天气数据模型
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record WeatherData(@JsonProperty("latitude") Double latitude, @JsonProperty("longitude") Double longitude,
			@JsonProperty("timezone") String timezone, @JsonProperty("current") CurrentWeather current,
			@JsonProperty("daily") DailyForecast daily, @JsonProperty("current_units") CurrentUnits currentUnits) {

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record CurrentWeather(@JsonProperty("time") String time,
				@JsonProperty("temperature_2m") Double temperature,
				@JsonProperty("apparent_temperature") Double feelsLike,
				@JsonProperty("relative_humidity_2m") Integer humidity,
				@JsonProperty("precipitation") Double precipitation, @JsonProperty("weather_code") Integer weatherCode,
				@JsonProperty("wind_speed_10m") Double windSpeed,
				@JsonProperty("wind_direction_10m") Integer windDirection) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record CurrentUnits(@JsonProperty("time") String timeUnit,
				@JsonProperty("temperature_2m") String temperatureUnit,
				@JsonProperty("relative_humidity_2m") String humidityUnit,
				@JsonProperty("wind_speed_10m") String windSpeedUnit) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record DailyForecast(@JsonProperty("time") List<String> time,
				@JsonProperty("temperature_2m_max") List<Double> tempMax,
				@JsonProperty("temperature_2m_min") List<Double> tempMin,
				@JsonProperty("precipitation_sum") List<Double> precipitationSum,
				@JsonProperty("weather_code") List<Integer> weatherCode,
				@JsonProperty("wind_speed_10m_max") List<Double> windSpeedMax,
				@JsonProperty("wind_direction_10m_dominant") List<Integer> windDirection) {
		}
	}

	/**
	 * 获取天气代码对应的描述
	 */
	private String getWeatherDescription(int code) {
		return switch (code) {
			case 0 -> "晴朗";
			case 1, 2, 3 -> "多云";
			case 45, 48 -> "雾";
			case 51, 53, 55 -> "毛毛雨";
			case 56, 57 -> "冻雨";
			case 61, 63, 65 -> "雨";
			case 66, 67 -> "冻雨";
			case 71, 73, 75 -> "雪";
			case 77 -> "雪粒";
			case 80, 81, 82 -> "阵雨";
			case 85, 86 -> "阵雪";
			case 95 -> "雷暴";
			case 96, 99 -> "雷暴伴有冰雹";
			default -> "未知天气";
		};
	}

	/**
	 * 获取风向描述
	 */
	private String getWindDirection(int degrees) {
		if (degrees >= 337.5 || degrees < 22.5)
			return "北风";
		if (degrees >= 22.5 && degrees < 67.5)
			return "东北风";
		if (degrees >= 67.5 && degrees < 112.5)
			return "东风";
		if (degrees >= 112.5 && degrees < 157.5)
			return "东南风";
		if (degrees >= 157.5 && degrees < 202.5)
			return "南风";
		if (degrees >= 202.5 && degrees < 247.5)
			return "西南风";
		if (degrees >= 247.5 && degrees < 292.5)
			return "西风";
		return "西北风";
	}

	/**
	 * 获取指定经纬度的天气预报
	 * @param latitude 纬度
	 * @param longitude 经度
	 * @return 指定位置的天气预报
	 * @throws RestClientException 如果请求失败
	 */
	@Tool(description = "获取指定经纬度的天气预报")
	public String getWeatherForecastByLocation(double latitude, double longitude) {
		// 获取天气数据（当前和未来7天）
		var weatherData = restClient.get()
			.uri("/forecast?latitude={latitude}&longitude={longitude}&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,weather_code,wind_speed_10m_max,wind_direction_10m_dominant&timezone=auto&forecast_days=7",
					latitude, longitude)
			.retrieve()
			.body(WeatherData.class);

		// 拼接天气信息
		StringBuilder weatherInfo = new StringBuilder();

		// 添加当前天气信息
		WeatherData.CurrentWeather current = weatherData.current();
		String temperatureUnit = weatherData.currentUnits() != null ? weatherData.currentUnits().temperatureUnit()
				: "°C";
		String windSpeedUnit = weatherData.currentUnits() != null ? weatherData.currentUnits().windSpeedUnit() : "km/h";
		String humidityUnit = weatherData.currentUnits() != null ? weatherData.currentUnits().humidityUnit() : "%";

		weatherInfo.append(String.format("""
				当前天气:
				温度: %.1f%s (体感温度: %.1f%s)
				天气: %s
				风向: %s (%.1f %s)
				湿度: %d%s
				降水量: %.1f 毫米

				""", current.temperature(), temperatureUnit, current.feelsLike(), temperatureUnit,
				getWeatherDescription(current.weatherCode()), getWindDirection(current.windDirection()),
				current.windSpeed(), windSpeedUnit, current.humidity(), humidityUnit, current.precipitation()));

		// 添加未来天气预报
		weatherInfo.append("未来天气预报:\n");
		WeatherData.DailyForecast daily = weatherData.daily();

		for (int i = 0; i < daily.time().size(); i++) {
			String date = daily.time().get(i);
			double tempMin = daily.tempMin().get(i);
			double tempMax = daily.tempMax().get(i);
			int weatherCode = daily.weatherCode().get(i);
			double windSpeed = daily.windSpeedMax().get(i);
			int windDir = daily.windDirection().get(i);
			double precip = daily.precipitationSum().get(i);

			// 格式化日期
			LocalDate localDate = LocalDate.parse(date);
			String formattedDate = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd (EEE)"));

			weatherInfo.append(String.format("""
					%s:
					温度: %.1f%s ~ %.1f%s
					天气: %s
					风向: %s (%.1f %s)
					降水量: %.1f 毫米

					""", formattedDate, tempMin, temperatureUnit, tempMax, temperatureUnit,
					getWeatherDescription(weatherCode), getWindDirection(windDir), windSpeed, windSpeedUnit, precip));
		}

		return weatherInfo.toString();
	}

	/**
	 * 获取指定位置的空气质量信息 (使用备用模拟数据) 注意：由于OpenMeteo的空气质量API可能需要额外配置或不可用，这里提供备用数据
	 * @param latitude 纬度
	 * @param longitude 经度
	 * @return 空气质量信息
	 */
	@Tool(description = "获取指定位置的空气质量信息（模拟数据）")
	public String getAirQuality(@ToolParam(description = "纬度") double latitude,
			@ToolParam(description = "经度") double longitude) {

		try {
			// 从天气数据中获取基本信息
			var weatherData = restClient.get()
				.uri("/forecast?latitude={latitude}&longitude={longitude}&current=temperature_2m&timezone=auto",
						latitude, longitude)
				.retrieve()
				.body(WeatherData.class);

			// 模拟空气质量数据 - 实际情况下应该从真实API获取
			// 根据经纬度生成一些随机但相对合理的数据
			int europeanAqi = (int) (Math.random() * 100) + 1;
			int usAqi = (int) (europeanAqi * 1.5);
			double pm10 = Math.random() * 50 + 5;
			double pm25 = Math.random() * 25 + 2;
			double co = Math.random() * 500 + 100;
			double no2 = Math.random() * 40 + 5;
			double so2 = Math.random() * 20 + 1;
			double o3 = Math.random() * 80 + 20;

			String aqiLevel = getAqiLevel(europeanAqi);
			String usAqiLevel = getUsAqiLevel(usAqi);

			// 构建空气质量信息字符串
			String aqiInfo = String.format("""
					空气质量信息 (纬度: %.4f, 经度: %.4f, 时区: %s):

					欧洲空气质量指数 (EAQI): %d (%s)
					美国空气质量指数 (US AQI): %d (%s)

					详细污染物信息:
					PM10: %.1f μg/m³
					PM2.5: %.1f μg/m³
					一氧化碳 (CO): %.1f μg/m³
					二氧化氮 (NO2): %.1f μg/m³
					二氧化硫 (SO2): %.1f μg/m³
					臭氧 (O3): %.1f μg/m³

					注意：以上是模拟数据，仅供示例。
					""", latitude, longitude, weatherData.timezone(), europeanAqi, aqiLevel, usAqi, usAqiLevel, pm10,
					pm25, co, no2, so2, o3);

			return aqiInfo;
		}
		catch (Exception e) {
			return "无法获取空气质量信息: " + e.getMessage();
		}
	}

	/**
	 * 获取欧洲AQI等级描述
	 */
	private String getAqiLevel(Integer aqi) {
		if (aqi <= 20) {
			return "优 (0-20): 空气质量非常好";
		}
		else if (aqi <= 40) {
			return "良 (20-40): 空气质量良好";
		}
		else if (aqi <= 60) {
			return "中等 (40-60): 对敏感人群可能有影响";
		}
		else if (aqi <= 80) {
			return "较差 (60-80): 对所有人群健康有影响";
		}
		else if (aqi <= 100) {
			return "差 (80-100): 可能对所有人群健康造成损害";
		}
		else {
			return "非常差 (>100): 对所有人群健康有严重影响";
		}
	}

	/**
	 * 获取美国AQI等级描述
	 */
	private String getUsAqiLevel(Integer aqi) {
		if (aqi <= 50) {
			return "优 (0-50): 空气质量令人满意，污染风险很低";
		}
		else if (aqi <= 100) {
			return "良 (51-100): 空气质量尚可，对极少数敏感人群可能有影响";
		}
		else if (aqi <= 150) {
			return "对敏感人群不健康 (101-150): 敏感人群可能会经历健康影响";
		}
		else if (aqi <= 200) {
			return "不健康 (151-200): 所有人可能开始经历健康影响";
		}
		else if (aqi <= 300) {
			return "非常不健康 (201-300): 健康警告，所有人可能经历更严重的健康影响";
		}
		else {
			return "危险 (>300): 健康警报，所有人更可能受到影响";
		}
	}

	public static void main(String[] args) {
		OpenMeteoService service = new OpenMeteoService();
		// 测试北京的天气预报
		System.out.println("北京天气预报:");
		System.out.println(service.getWeatherForecastByLocation(39.9042, 116.4074));

		// 测试北京的空气质量
		System.out.println("北京空气质量:");
		System.out.println(service.getAirQuality(39.9042, 116.4074));
	}

}
