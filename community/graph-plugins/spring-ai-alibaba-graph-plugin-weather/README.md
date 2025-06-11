# WeatherPlugin 使用说明

## 概述

WeatherPlugin 现在使用真实的 WeatherAPI.com 免费 API 来获取天气信息，而不是返回模拟数据。

## 获取免费 API 密钥

1. 访问 [WeatherAPI.com](https://www.weatherapi.com/)
2. 注册一个免费账户
3. 获取你的 API 密钥
4. 免费账户限制：每月最多 1,000,000 次调用

## 配置

### 方法 1：环境变量（推荐）

设置环境变量：

```bash
export WEATHER_API_KEY=your_actual_api_key_here
```

### 方法 2：直接修改代码

在 `WeatherPlugin.java` 中修改 `DEFAULT_API_KEY` 常量：

```java
private static final String DEFAULT_API_KEY = "your_actual_api_key_here";
```

## 使用示例

```java
WeatherPlugin weatherPlugin = new WeatherPlugin();

// 构建请求参数
Map<String, Object> params = new HashMap<>();
params.put("location", "Beijing");

try {
    // 调用天气API
    Map<String, Object> result = weatherPlugin.execute(params);

    System.out.println("位置: " + result.get("location"));
    System.out.println("温度: " + result.get("temperature") + "°C");
    System.out.println("天气状况: " + result.get("condition"));
    System.out.println("湿度: " + result.get("humidity") + "%");
    System.out.println("风速: " + result.get("wind_speed") + " km/h");

} catch (Exception e) {
    System.err.println("获取天气信息失败: " + e.getMessage());
}
```

## 返回数据格式

成功调用后，插件会返回包含以下字段的 Map：

```json
{
  "location": "Beijing, China",
  "region": "Beijing",
  "country": "China",
  "temperature": 15.0,
  "temperature_f": 59.0,
  "condition": "Partly cloudy",
  "humidity": 45,
  "wind_speed": 10.0,
  "wind_direction": "NW",
  "pressure": 1013.0,
  "visibility": 10.0,
  "uv_index": 3.0,
  "last_updated": "2024-01-15 14:30"
}
```

## 支持的位置格式

- 城市名称：`"Beijing"`, `"New York"`, `"London"`
- 城市+国家：`"Beijing, China"`, `"New York, US"`
- 中文城市名：`"北京"`, `"上海"`, `"广州"`
- 经纬度：`"48.8567,2.3508"` (Paris)
- 机场代码：`"LAX"`, `"JFK"`
- IP 地址：`"auto:ip"` (基于当前 IP)

## 错误处理

插件包含完善的错误处理机制：

- 位置未找到：返回友好的错误消息
- API 密钥无效：提示设置正确的环境变量
- 网络错误：返回具体的错误信息

## 依赖项

确保在 `pom.xml` 中包含以下依赖：

```xml
<!-- WebFlux for WebClient support -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Jackson for JSON processing -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

## 注意事项

1. 免费 API 密钥有请求次数限制
2. 建议在生产环境中使用付费版本以获得更高的稳定性
3. API 响应时间通常在几百毫秒内
4. 插件支持中文城市名称自动转换为拼音查询
