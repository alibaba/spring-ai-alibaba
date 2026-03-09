# Voice Agent Example

A Spring AI Alibaba voice agent implementing the **Sandwich architecture** (STT → Agent → TTS).

## Architecture

```
User Input (text or audio) → STT (optional) → Sandwich Agent → TTS → Audio Output
```

- **STT**: DashScope realtime ASR (streaming audio in → `stt_chunk` / `stt_output`), e.g. paraformer-realtime-v2
- **Agent**: ReactAgent with sandwich order tools (`add_to_order`, `confirm_order`)
- **TTS**: DashScope CosyVoice (streaming text in → `tts_chunk`), e.g. cosyvoice-v3-flash

## Voice Agent Architecture Patterns

### 1. Native S2S (Speech-to-Speech)

单模型直接处理：音频输入 → 音频输出，无中间文本。

```
User Audio → [Single Model] → Audio Output
```

| 厂商 | 模型/产品 | 说明 |
|------|-----------|------|
| **OpenAI** | `gpt-realtime` (Realtime API) | 单模型处理音频输入输出，低延迟，支持工具调用 |
| **Google** | Gemini 2.5 Flash + Gemini Live API | 原生音频输入输出，30+ HD 音色、24 种语言 |
| **ElevenLabs** | Conversational AI 2.0 | 端到端语音对话，<100ms 延迟，32+ 语言 |

**特点**：延迟更低、音色/情感保留更好；厂商选择较少。

### 2. Combined S2S（组合式：STT + LLM + TTS）

本示例采用的 **Sandwich 架构**：三个组件串联。

```
User Audio → STT → LLM Agent → TTS → Audio Output
```

| 厂商 | 能力 | 说明 |
|------|------|------|
| **阿里云** | 智能语音交互 (ISI) | 实时 ASR + 通义 LLM + 语音合成，需自行串联 |
| **科大讯飞** | 星火语音大模型 | ASR + 语音合成，语音交互平台 |
| **百度** | SpeechT5 等 | 统一语音模型，支持 ASR、TTS、翻译 |
| **微软** | Azure Speech + Copilot | Speech Services + 对话模型，需组合 |

**特点**：组件可替换、灵活性高；延迟略高，依赖 TTS 质量。

### 3. 对比

| 类型 | 延迟 | 音色/情感 | 厂商数量 |
|------|------|-----------|----------|
| **原生 S2S** | 更低（单模型） | 更好 | 较少 |
| **组合式 S2S** | 略高（多步串联） | 依赖 TTS | 较多 |

**DashScope**：提供实时 ASR（如 paraformer-realtime-v2，流式音频输入）和 TTS（如 cosyvoice-v3-flash，流式文本输入），本示例采用组合式串联，支持端到端流式。

## Prerequisites

- JDK 17+
- DashScope API key (`AI_DASHSCOPE_API_KEY`)

## Run

```bash
export AI_DASHSCOPE_API_KEY=your-api-key
./mvnw -pl examples/voice-agent spring-boot:run
```

## API (WebSocket only)


## Web UI

Start the app and open http://localhost:8080 in a browser for a real-time voice interaction UI:

- **Voice mode**: Click "Start Session", speak, then "End Session" to process
- **Text mode**: Type a message and press Enter or click "Send"

## Sandwich Shop Tools

- `add_to_order(item, quantity)` – Add item to the order
- `confirm_order(order_summary)` – Confirm the final order

Available: lettuce, tomato, onion, pickles, mayo, mustard; turkey, ham, roast beef; swiss, cheddar, provolone.

### WebSocket

#### Audio input (realtime: STT → Agent → TTS stream)

Connect to `ws://localhost:8080/voice/ws/audio`:

1. Send **binary** messages (PCM 16kHz, 16-bit, mono)
2. Send **text** `{"type":"end"}` when done
3. Server streams: `stt_chunk`, `stt_output`, then agent + TTS events

Uses DashScope realtime ASR (e.g. paraformer-realtime-v2) and streaming TTS (e.g. cosyvoice-v3-flash).

#### Text input (full pipeline or TTS only)

Connect to `ws://localhost:8080/voice/ws` and send JSON:

```json
{"text": "I would like a turkey sandwich with lettuce and mayo"}
```

For TTS-only (no agent), send:

```json
{"text": "One turkey sandwich", "ttsOnly": true}
```

---

Server streams JSON events:

| Event        | Fields                          | Description                    |
|-------------|----------------------------------|--------------------------------|
| `stt_chunk`   | `transcript`, `ts`              | Partial STT (realtime audio)   |
| `stt_output`  | `transcript`, `ts`              | Final STT result               |
| `agent_chunk` | `text`, `ts`                    | Agent response text chunk     |
| `tool_call`   | `id`, `name`, `args`, `ts`      | Tool invocation                |
| `tool_result` | `toolCallId`, `name`, `result`, `ts` | Tool execution result   |
| `agent_end`   | `ts`                            | Agent finished                 |
| `tts_chunk`   | `audio` (base64), `ts`          | Synthesized audio chunk        |
| `error`       | `message`                       | Error message                  |

Example (Node.js):

```javascript
const ws = new WebSocket('ws://localhost:8080/voice/ws');
ws.onopen = () => ws.send(JSON.stringify({text: "One turkey sandwich"}));
ws.onmessage = (e) => {
  const ev = JSON.parse(e.data);
  if (ev.type === 'agent_chunk') process.stdout.write(ev.text);
  if (ev.type === 'tts_chunk') { /* decode base64, play audio */ }
};
```
