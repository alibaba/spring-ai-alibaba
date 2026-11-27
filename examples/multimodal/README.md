# Multimodal Example - Spring AI Alibaba

Upload a PNG or JPEG with a question, run the same workflow through Agent Framework or Graph Core, and download the generated illustration from the application.

## Overview

| Feature | Description |
|---------|-------------|
| **Input** | Images (URL or local file) via `UserMessage.media` |
| **Output** | Downloadable image artifacts via Agent tools or Graph nodes, plus direct audio synthesis |
| **Model** | DashScope vision and tools (`qwen4-max`), image generation (`qwen-image-3.0-pro`), speech synthesis (`qwen-audio-3.1-tts-plus`) |
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
./mvnw -f examples/multimodal/pom.xml spring-boot:run
```

Or with explicit API key:

```bash
AI_DASHSCOPE_API_KEY=your_key ./mvnw -f examples/multimodal/pom.xml spring-boot:run
```

## Image workflows

Open `http://localhost:8080/`, choose **Agent Framework** or **Graph Core** in the image workflow card, and upload a PNG or JPEG with a question. Enable illustration generation to see and download the resulting file.

Both routes accept the same multipart form. Use `agent` or `graph` for `mode`.

```bash
mode=agent
curl --fail-with-body --silent --show-error \
  -F 'image=@examples/multimodal/src/main/resources/images/sample.png' \
  -F 'question=Describe the main objects and colors in the picture.' \
  -F 'createIllustration=true' \
  "http://localhost:8080/api/workflows/$mode" > workflow.json

curl --fail-with-body --silent --show-error \
  "http://localhost:8080$(jq -r '.artifacts[0].url' workflow.json)" -o illustration
```

The response contains `observation` and an `artifacts` array. Each completed artifact has an `id`, `mimeType`, `sizeBytes`, and application-relative `url`. Sending `createIllustration=false` runs only visual inspection and returns an empty array.

- **Agent Framework** sends the user's text and image to `ReactAgent`. With generation enabled, the model calls `generate_illustration` with the visual observation and a prompt. The service reads the artifact ID from the tool response.
- **Graph Core** stages the upload, puts its ID in `StateGraph`, resolves it inside `inspect`, and follows the conditional edge to `illustrate` only when generation was requested. The staged upload is removed when the request ends.
- Both routes use `ImageModel` to request a single Base64 image from Qwen Image 3.0 Pro. The application saves it before returning a download URL. Set `MULTIMODAL_ARTIFACT_DIRECTORY` to keep generated files in a directory you control. The example serves artifacts by ID and is intended for local use.

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

`AudioService` calls `DashScopeAudioSpeechModel` and returns MP3. Omit `voice` to use the model's default or supply a [voice supported by the selected Qwen Audio model](https://www.alibabacloud.com/help/en/model-studio/qwen-audio-tts-voice-list).

```bash
# POST /api/audio/tts
curl -X POST http://localhost:8080/api/audio/tts \
  -H "Content-Type: application/json" \
  -d '{"text":"你好，欢迎使用语音合成示例。","outputFormat":"base64"}'
```

| outputFormat | Result |
|--------------|--------|
| `url` (default) | Save to file, return `{"url":"file://...","mimeType":"audio/mpeg"}` |
| `base64` | Return `{"data":"data:audio/mpeg;base64,...","mimeType":"audio/mpeg"}` |

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `spring.ai.dashscope.api-key` | `${AI_DASHSCOPE_API_KEY}` | DashScope API key |
| `DASHSCOPE_VISION_MODEL` | `qwen4-max` | DashScope chat model. The Agent workflow requires image input and tool calling |
| `spring.ai.dashscope.chat.multi-model` | `true` | Use the DashScope multimodal generation endpoint |
| `DASHSCOPE_ILLUSTRATION_MODEL` | `qwen-image-3.0-pro` | Image model. The two workflows require synchronous `b64_json` output |
| `DASHSCOPE_TTS_MODEL` | `qwen-audio-3.1-tts-plus` | Speech synthesis model. Custom voices must match the selected model |
| `MULTIMODAL_ARTIFACT_DIRECTORY` | `${java.io.tmpdir}/saa-multimodal` | Local directory for generated images and temporary uploads |

The Graph workflow runs within one HTTP request. The staged input is available to the graph until the request finishes. Generated artifacts remain in the configured directory.

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

The TTS endpoint accepts `outputFormat`. The standalone `generate_image` tool uses the URL converter by default.

| Value | Default | Use case |
|-------|---------|----------|
| **url** | ✓ | Small text result containing a reference the client can display or download. |
| **base64** | | Inline data the client can display without another request. Keep large payloads out of model context. |

DashScope tool responses carry text. To inspect an image produced by a tool, make a separate multimodal request that attaches the actual image as `Media`.
