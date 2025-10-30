import {
  CopilotRuntimeChatCompletionRequest,
  CopilotRuntimeChatCompletionResponse,
  CopilotServiceAdapter
} from "@copilotkit/runtime";
import {randomUUID} from "@copilotkit/shared";

/**
 * Base interface for common properties in all server-sent events.
 */
interface BaseMessage {
    type: string;
    timestamp: number;
}

/**
 * Interface for the RUN_STARTED event, indicating the start of a new run.
 */
interface RunStarted extends BaseMessage {
    type: 'RUN_STARTED';
    thread_id: string;
}

/**
 * Interface for the TEXT_MESSAGE_START event, indicating the start of a text message.
 */
interface TextMessageStart extends BaseMessage {
    type: 'TEXT_MESSAGE_START';
    message_id: string;
    role: 'assistant' | 'user';  // Add other roles if needed
}

/**
 * Interface for the TEXT_MESSAGE_CONTENT event, containing a chunk of a text message.
 */
interface TextMessageContent extends BaseMessage {
    type: 'TEXT_MESSAGE_CONTENT';
    message_id: string;
    delta: string;
}

/**
 * Interface for the TEXT_MESSAGE_END event, indicating the end of a text message.
 */
interface TextMessageEnd extends BaseMessage {
    type: 'TEXT_MESSAGE_END';
    message_id: string;
}

/**
 * Interface for the RUN_FINISHED event, indicating the end of a run.
 */
interface RunFinished extends BaseMessage {
    type: 'RUN_FINISHED';
    thread_id: string;
}

/**
 * Interface for the TOOL_CALL_START event, indicating the start of a tool call.
 */
interface ToolCallStart extends BaseMessage {
    type: 'TOOL_CALL_START';
    tool_call_id: string;
    tool_call_name: string; // Name of the tool being called
    parent_message_id?: string; // ID of the parent message that initiated the tool call
}

/**
 * Interface for the TOOL_CALL_END event, indicating the end of a tool call.
 */
interface ToolCallEnd extends BaseMessage {
    type: 'TOOL_CALL_END';
    tool_call_id: string;
}

/**
 * Interface for the TOOL_CALL_ARGS event, containing the arguments for a tool call.
 */
interface ToolCallArgs extends BaseMessage {
    type: 'TOOL_CALL_ARGS';
    tool_call_id: string;
    tool_call_args: string; // Arguments passed to the tool
}

/**
 * Union type for all possible server-sent event message types from the Spring AI Alibaba backend.
 */
type Message =
    RunStarted
    | TextMessageStart
    | TextMessageContent
    | TextMessageEnd
    | RunFinished
    | ToolCallStart
    | ToolCallEnd
    | ToolCallArgs;


async function* fetchMessages(reader: ReadableStreamDefaultReader<string>): AsyncGenerator<Message> {
    let buffer = ''

    const {done, value} = await reader.read();

    if (done) {
        return;
    }

    buffer += value;

    // Split buffer by newlines and process complete messages
    const lines = buffer.split('\n');
    const lastLine = lines.pop(); // Keep the last incomplete line in buffer

    const regex = /^data:(.+)$/m;

    for (const line of lines) {
        const match = line.match(regex);
        if (match) {
            yield JSON.parse(match[1]);
        }
    }

    if (lastLine) {
        const m = lastLine.match(regex);
        if (m) {
            try {
                yield JSON.parse(m[1]);
                buffer = ''; // Clear buffer
            } catch (error) {
                buffer = lastLine; // Keep the last line in buffer for next iteration
                console.warn("fetch is incomplete. LastLine :", lastLine);
            }
        }
    } else {
        buffer = ''; // Clear buffer if no last line
    }

}

/**
 * Implements the `CopilotServiceAdapter` to connect CopilotKit with a Spring AI Alibaba backend.
 * This adapter handles the communication by making a POST request to the Spring AI Alibaba service
 * and processing the server-sent events (SSE) stream in response.
 */
export class AgentAdapter implements CopilotServiceAdapter {
    private abortController: AbortController;

    /**
     * Initializes a new instance of the AgentAdapter.
     */
    constructor() {
        this.abortController = new AbortController();
    }


    /**
     * Processes a chat completion request by forwarding it to the Spring AI Alibaba backend
     * and streaming the response back to the CopilotKit runtime.
     *
     * @param {CopilotRuntimeChatCompletionRequest} request The chat completion request from the CopilotKit runtime.
     * @returns {Promise<CopilotRuntimeChatCompletionResponse>} A promise that resolves with the response,
     * including the thread ID.
     */
    async process(request: CopilotRuntimeChatCompletionRequest): Promise<CopilotRuntimeChatCompletionResponse> {

        console.debug("Processing request:", request);

        const {
            threadId: threadIdFromRequest,
            eventSource,
        } = request;

        const threadId = threadIdFromRequest ?? randomUUID();

        try {
            const response = await fetch('http://localhost:8080/run_sse_copilotkit', {
                signal: this.abortController.signal,
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'text/event-stream'
                },
                body: JSON.stringify(request),

            });


            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            //const reader = response.body?.pipeThrough(new TextDecoderStream()).getReader();

            const reader = response.body?.getReader();
            const decoder = new TextDecoder();

            if (!reader) {
                throw new Error('Response body is null');
            }


            eventSource.stream(async (eventStream$) => {

                try {
                    let buffer = ''

                    const fetchMessages = (value: string | undefined) => async function* (): AsyncGenerator<Message> {

                        buffer += value;

                        // Split buffer by newlines and process complete messages
                        const lines = buffer.split('\n');
                        const lastLine = lines.pop(); // Keep the last incomplete line in buffer

                        const regex = /^data:(.+)$/m;

                        for (const line of lines) {
                            const match = line.match(regex);
                            if (match) {
                                yield JSON.parse(match[1])
                            }
                        }

                        if (lastLine) {
                            const m = lastLine.match(regex);
                            if (m) {
                                try {
                                    yield JSON.parse(m[1])
                                    buffer = ''; // Clear buffer
                                } catch (error) {
                                    buffer = lastLine; // Keep the last line in buffer for next iteration
                                    console.warn("fetch is incomplete. LastLine :", lastLine);
                                }
                            }
                        } else {
                            buffer = ''; // Clear buffer if no last line
                        }
                    }

                    let fetchEvents = true

                    while (fetchEvents) {

                        const {done, value: buffer} = await reader.read();
                        const value = decoder.decode(buffer, {stream: true});


                        console.debug(`Fetch value:`, done, value);

                        if (done) {
                            fetchEvents = false;
                            break;
                        }

                        const messageGenerator = fetchMessages(value)

                        for await (const message of messageGenerator()) {

                            console.debug(`${threadId} - Fetch message:`, message.type);

                            switch (message.type) {
                                case 'RUN_STARTED':

                                    break;
                                case 'TEXT_MESSAGE_START':
                                    eventStream$.sendTextMessageStart({
                                        messageId: message.message_id
                                    });
                                    break;
                                case 'TEXT_MESSAGE_CONTENT':
                                    eventStream$.sendTextMessageContent({
                                        messageId: message.message_id,
                                        content: message.delta,
                                    });
                                    break;
                                case 'TEXT_MESSAGE_END':
                                    eventStream$.sendTextMessageEnd({
                                        messageId: message.message_id,
                                    });
                                    break;
                                case 'RUN_FINISHED':
                                    fetchEvents = false;
                                    break;
                                case 'TOOL_CALL_START':
                                    eventStream$.sendActionExecutionStart({
                                        actionExecutionId: message.tool_call_id,
                                        actionName: message.tool_call_name,
                                        parentMessageId: message.parent_message_id,
                                    });
                                    break;
                                case 'TOOL_CALL_ARGS':
                                    eventStream$.sendActionExecutionArgs({
                                        actionExecutionId: message.tool_call_id,
                                        args: message.tool_call_args,
                                    });
                                    break;
                                case 'TOOL_CALL_END':
                                    eventStream$.sendActionExecutionEnd({
                                        actionExecutionId: message.tool_call_id,
                                    });
                                    fetchEvents = false;
                                    break;
                                default:
                                    // Handle unexpected message types
                                    console.error('Unexpected message type:', message);
                                    break;
                            }
                        }

                    }
                } finally {
                    console.debug("Processing messages completed:", threadId);
                    eventStream$.complete();
                }
            });

        } catch (error: any) {
            if ("name" in error && error.name === 'AbortError') {
                console.warn('Fetch aborted');
            } else {
                throw error;
            }
        }

        return {
            threadId
        };
    }
}
