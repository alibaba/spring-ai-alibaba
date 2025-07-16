# DashScope 视频生成功能

本文档介绍如何使用 Spring AI Alibaba 项目中的 DashScope 视频生成功能。

## 功能概述

DashScope 视频生成功能基于阿里云 DashScope 的文本到视频生成 API，支持将文本描述转换为视频内容。

## 配置

### 1. 添加依赖

确保你的项目中包含了 `spring-ai-alibaba-autoconfigure` 依赖：

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-autoconfigure</artifactId>
    <version>${version}</version>
</dependency>
```

### 2. 配置文件

在 `application.yml` 或 `application.properties` 中添加配置：

```yaml
spring:
  ai:
    alibaba:
      dashscope:
        video:
          enabled: true
          model: text2video-synthesis
          width: 1920
          height: 1080
          duration: 10
          fps: 30
          seed: 12345
          num-frames: 300
```

### 3. 配置参数说明

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| enabled | boolean | true | 是否启用视频生成功能 |
| model | String | text2video-synthesis | 视频生成模型名称 |
| width | Integer | - | 视频宽度（像素） |
| height | Integer | - | 视频高度（像素） |
| duration | Integer | - | 视频时长（秒） |
| fps | Integer | - | 视频帧率 |
| seed | Long | - | 随机种子，用于控制生成结果的一致性 |
| num-frames | Integer | - | 视频帧数 |

## 使用方法

### 1. 注入 DashScopeVideoModel

```java
@Autowired
private DashScopeVideoModel videoModel;
```

### 2. 基本使用

```java
// 简单文本生成视频
VideoGenerationResponse response = videoModel.generate("一只可爱的小猫在花园里玩耍");

// 获取生成的视频URL
if (response != null && response.getOutput() != null && response.getOutput().getResults() != null) {
    String videoUrl = response.getOutput().getResults()[0].getVideoUrl();
    System.out.println("生成的视频URL: " + videoUrl);
}
```

### 3. 自定义参数

```java
// 创建自定义选项
VideoOptions customOptions = new VideoOptions() {
    @Override
    public String getModel() {
        return "text2video-synthesis";
    }
    
    @Override
    public Integer getWidth() {
        return 1280;
    }
    
    @Override
    public Integer getHeight() {
        return 720;
    }
    
    @Override
    public Integer getDuration() {
        return 5;
    }
    
    @Override
    public Integer getFps() {
        return 25;
    }
    
    @Override
    public Long getSeed() {
        return 12345L;
    }
    
    @Override
    public Integer getNumFrames() {
        return 125;
    }
};

// 使用自定义参数生成视频
VideoGenerationResponse response = videoModel.generate("美丽的日落风景", customOptions);
```

## API 使用

### 1. 直接使用 DashScopeVideoApi

```java
@Autowired
private DashScopeVideoApi videoApi;

public void generateVideo() {
    // 创建请求
    VideoGenerationRequest request = new VideoGenerationRequest(
        "text2video-synthesis",
        new VideoGenerationRequest.VideoInput("一只小狗在草地上奔跑"),
        new VideoGenerationRequest.VideoParameters(1920, 1080, 10, 30, 12345L, 300)
    );
    
    // 提交任务
    ResponseEntity<VideoGenerationResponse> response = videoApi.submitTask(request);
    
    // 查询任务状态
    String taskId = response.getBody().getOutput().getTaskId();
    ResponseEntity<VideoGenerationResponse> statusResponse = videoApi.queryTask(taskId);
}
```

### 2. 使用 DashScopeVideoOptions

```java
@Autowired
private DashScopeVideoOptions videoOptions;

public void configureVideoOptions() {
    // 设置视频参数
    videoOptions.setWidth(1920);
    videoOptions.setHeight(1080);
    videoOptions.setDuration(10);
    videoOptions.setFps(30);
    videoOptions.setSeed(12345L);
    videoOptions.setNumFrames(300);
}
```

## 错误处理

### 1. 常见错误

- **任务超时**: 视频生成任务超过默认超时时间（10分钟）
- **任务失败**: 视频生成过程中发生错误
- **参数错误**: 传入的参数不符合要求

### 2. 异常处理

```java
try {
    VideoGenerationResponse response = videoModel.generate("生成视频的文本描述");
    // 处理成功响应
} catch (RuntimeException e) {
    // 处理异常
    System.err.println("视频生成失败: " + e.getMessage());
}
```

## 注意事项

1. **API 限制**: 请确保你的 DashScope API Key 有足够的配额和权限
2. **网络要求**: 视频生成需要稳定的网络连接
3. **处理时间**: 视频生成通常需要几分钟时间，请耐心等待
4. **文件大小**: 生成的视频文件可能较大，请注意存储空间
5. **内容合规**: 请确保生成的视频内容符合相关法律法规

## 示例项目

完整的示例项目可以参考 `spring-ai-alibaba-examples` 模块中的相关示例。

## 技术支持

如果遇到问题，请参考：
- [DashScope 官方文档](https://help.aliyun.com/zh/dashscope/)
- [Spring AI 文档](https://docs.spring.io/spring-ai/reference/)
- [项目 GitHub 仓库](https://github.com/alibaba/spring-ai-alibaba) 