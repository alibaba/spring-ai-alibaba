# Multimodal Example - Spring AI Alibaba

This example demonstrates **multimodal (vision) capabilities** with Spring AI Alibaba, using DashScope's vision models (e.g., `qwen-vl-plus`) to understand and analyze images.

## Overview

| Feature | Description |
|---------|-------------|
| **Input** | Images (URL or local file) via `UserMessage.media` |
| **Output** | Image via tools (GenerateImageTool); Audio via `DashScopeAudioSpeechModel` directly |
| **Model** | DashScope vision (`qwen-vl-plus`), image (Wanx), TTS (CosyVoice) |
| **Scenarios** | Image understanding, ReactAgent with media input, **Image generation via tools**, **TTS via /api/audio/tts** |
| **Reference** | [Spring AI Multimodality](https://docs.spring.io/spring-ai/reference/api/multimodality.html)|

## Prerequisites

- **JDK 17+**
- **DashScope API Key** - Get from [Alibaba Cloud Bailian Console](https://bailian.console.aliyun.com/)

```bash
export AI_DASHSCOPE_API_KEY=your_api_key
```

## Quick Start

```bash
# From project root
./mvnw -pl examples/multimodal spring-boot:run
```

Or with explicit API key:

```bash
AI_DASHSCOPE_API_KEY=your_key ./mvnw -pl examples/multimodal spring-boot:run
```

## Scenarios Demonstrated

### 1. Image from URL (ChatModel)

Pass an image via public URL using `UserMessage.media`:

```java
UserMessage userMessage = UserMessage.builder()
    .text("Explain what do you see in this picture?")
    .media(List.of(new Media(MimeTypeUtils.IMAGE_PNG, URI.create("https://example.com/image.png"))))
    .build();

ChatResponse response = chatModel.call(new Prompt(userMessage));
```

### 2. Image from Local Resource

Use `ClassPathResource` or `FileSystemResource` for local images:

```java
var resource = new ClassPathResource("images/sample.png");
UserMessage userMessage = UserMessage.builder()
    .text("Please describe this image.")
    .media(new Media(MimeTypeUtils.IMAGE_PNG, resource))
    .build();
```

Add your own image to `src/main/resources/images/sample.png` to try Scene 2.

### 3. ReactAgent with Multimodal Input

Pass `UserMessage` with media directly to `ReactAgent`:

```java
UserMessage userMessage = UserMessage.builder()
    .text("What objects are in this image?")
    .media(new Media(MimeTypeUtils.IMAGE_PNG, URI.create(imageUrl)))
    .build();

AssistantMessage response = visionAgent.call(userMessage);
```

### 4. Image Generation via Tools

Use **GenerateImageTool** to wrap `ImageModel`. The creative agent uses it for image generation:

```java
ReactAgent creativeAgent = ReactAgent.builder()
    .model(chatModel)
    .methodTools(generateImageTool)
    .build();
creativeAgent.call("Generate an image of a sunset over mountains");
```

### 5. TTS via DashScopeAudioSpeechModel (Direct API)

TTS uses `DashScopeAudioSpeechModel` directly (not as a tool). `AudioService` uses `call()` or `stream()` based on model support; `outputFormat` controls url vs base64:

```bash
# POST /api/audio/tts
curl -X POST http://localhost:8080/api/audio/tts \
  -H "Content-Type: application/json" \
  -d '{"text":"Hello world","voice":"Cherry","outputFormat":"url"}'
```

| outputFormat | Result |
|--------------|--------|
| `url` (default) | Save to file, return `{"url":"file://...","mimeType":"audio/mpeg"}` |
| `base64` | Return `{"data":"data:audio/mpeg;base64,...","mimeType":"audio/mpeg"}` |

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `spring.ai.dashscope.api-key` | `${AI_DASHSCOPE_API_KEY}` | DashScope API key |
| `spring.ai.dashscope.chat.options.model` | `qwen-vl-plus` | Vision model (use `-vl` models for multimodal) |

Supported vision models: `qwen-vl-plus`, `qwen-vl-max`, `qwen2-vl-plus`, `qwen3-vl-plus`.

## Limitations (Current)
- **Checkpoint/Resume**: Media may not persist in Graph state serialization
- **Studio**: Media field may be stripped in Studio's API pipeline

## Tool Multimodal Result

Tool return values are converted to text via `ToolCallResultConverter`. This example uses
`ToolMultimodalResult` + `MultimodalToolCallResultConverter` to return structured JSON.
`ToolMultimodalResult` reuses `org.springframework.ai.content.Media`, supporting both:

- **URL/URI**: `mediaFromUrl(url, mimeType)` or `mediaFromUri(uri, mimeType)` → JSON `{"url":"...","mimeType":"..."}`
- **Raw bytes**: `mediaFromBytes(data, mimeType)` or `mediaFromResource(resource, mimeType)` → JSON `{"data":"data:audio/mpeg;base64,...","mimeType":"..."}`
- **URL + base64**: `mediaFromUrlAndBase64(url, base64, mimeType)` when the model returns both (e.g. `ImageResponse` with url and b64Json). The converter picks the format matching OutputFormat without unnecessary fetch.

Any tool returning `ToolMultimodalResult` with `resultConverter = MultimodalToolCallResultConverter.class`
produces JSON like: `{"text":"...","media":[{"type":"image","url":"..."}]}` or `{"type":"audio","data":"data:..."}`.

### Output Format Parameter (url vs base64)

`generate_image` and `AudioService` (TTS) support an `outputFormat` parameter:

| Value | Default | Use case |
|-------|---------|----------|
| **url** | ✓ | Model-friendly; tool result sent to model as context. Minimal token usage. |
| **base64** | | Inline data for client (e.g., `returnDirect=true`). **Note:** Base64 significantly increases token usage when sent to the model; prefer `url` for agent reasoning loops. |

Industry practice: Use URLs for tool results that feed back into the model (OpenAI, LangChain, etc.). Use base64 when the consumer needs inline display without a separate fetch.
