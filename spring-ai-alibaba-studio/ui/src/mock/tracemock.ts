/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

///
/// Copyright 2024-2025 the original author or authors.
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///      https://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


export const traceDetailList = [
    {
        resource: {
            attributes: [
                {
                    key: 'service.name',
                    value: {
                        stringValue: 'unknown_service',
                    },
                },
                {
                    key: 'telemetry.sdk.language',
                    value: {
                        stringValue: 'java',
                    },
                },
                {
                    key: 'telemetry.sdk.name',
                    value: {
                        stringValue: 'opentelemetry',
                    },
                },
                {
                    key: 'telemetry.sdk.version',
                    value: {
                        stringValue: '1.37.0',
                    },
                },
            ],
        },
        scopeSpans: [
            {
                scope: {
                    name: 'org.springframework.boot',
                    version: '3.3.0',
                    attributes: [

                    ],
                },
                spans: [
                    {
                        traceId: '5b3a9bf1970c1cd3932a73e3462a738b',
                        spanId: 'f4f70aa65efd3c60',
                        name: 'http get /studio/api/observation/clearAll',
                        kind: 2,
                        startTimeUnixNano: '1733482361572937300',
                        endTimeUnixNano: '1733482361574912200',
                        attributes: [
                            {
                                key: 'exception',
                                value: {
                                    stringValue: 'none',
                                },
                            },
                            {
                                key: 'http.url',
                                value: {
                                    stringValue: '/studio/api/observation/clearAll',
                                },
                            },
                            {
                                key: 'method',
                                value: {
                                    stringValue: 'GET',
                                },
                            },
                            {
                                key: 'outcome',
                                value: {
                                    stringValue: 'SUCCESS',
                                },
                            },
                            {
                                key: 'status',
                                value: {
                                    stringValue: '200',
                                },
                            },
                            {
                                key: 'uri',
                                value: {
                                    stringValue: '/studio/api/observation/clearAll',
                                },
                            },
                        ],
                        events: [

                        ],
                        links: [

                        ],
                        status: {

                        },
                        flags: 1,
                    },
                ],
            },
        ],
    },
    {
        resource: {
            attributes: [
                {
                    key: 'service.name',
                    value: {
                        stringValue: 'unknown_service',
                    },
                },
                {
                    key: 'telemetry.sdk.language',
                    value: {
                        stringValue: 'java',
                    },
                },
                {
                    key: 'telemetry.sdk.name',
                    value: {
                        stringValue: 'opentelemetry',
                    },
                },
                {
                    key: 'telemetry.sdk.version',
                    value: {
                        stringValue: '1.37.0',
                    },
                },
            ],
        },
        scopeSpans: [
            {
                scope: {
                    name: 'org.springframework.boot',
                    version: '3.3.0',
                    attributes: [

                    ],
                },
                spans: [
                    {
                        traceId: 'c227f936a3eb510e7da12fda10a8ffab',
                        spanId: '23ccddcad598c3b2',
                        parentSpanId: '884c2f1abf7f956b',
                        name: 'http post',
                        kind: 3,
                        startTimeUnixNano: '1733482368339640500',
                        endTimeUnixNano: '1733482369314750800',
                        attributes: [
                            {
                                key: 'client.name',
                                value: {
                                    stringValue: 'dashscope.aliyuncs.com',
                                },
                            },
                            {
                                key: 'exception',
                                value: {
                                    stringValue: 'none',
                                },
                            },
                            {
                                key: 'http.url',
                                value: {
                                    stringValue: 'https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation',
                                },
                            },
                            {
                                key: 'method',
                                value: {
                                    stringValue: 'POST',
                                },
                            },
                            {
                                key: 'outcome',
                                value: {
                                    stringValue: 'SUCCESS',
                                },
                            },
                            {
                                key: 'status',
                                value: {
                                    stringValue: '200',
                                },
                            },
                            {
                                key: 'uri',
                                value: {
                                    stringValue: '/api/v1/services/aigc/text-generation/generation',
                                },
                            },
                        ],
                        events: [

                        ],
                        links: [

                        ],
                        status: {

                        },
                        flags: 1,
                    },
                ],
            },
        ],
    },
    {
        resource: {
            attributes: [
                {
                    key: 'service.name',
                    value: {
                        stringValue: 'unknown_service',
                    },
                },
                {
                    key: 'telemetry.sdk.language',
                    value: {
                        stringValue: 'java',
                    },
                },
                {
                    key: 'telemetry.sdk.name',
                    value: {
                        stringValue: 'opentelemetry',
                    },
                },
                {
                    key: 'telemetry.sdk.version',
                    value: {
                        stringValue: '1.37.0',
                    },
                },
            ],
        },
        scopeSpans: [
            {
                scope: {
                    name: 'org.springframework.boot',
                    version: '3.3.0',
                    attributes: [

                    ],
                },
                spans: [
                    {
                        traceId: 'c227f936a3eb510e7da12fda10a8ffab',
                        spanId: '884c2f1abf7f956b',
                        parentSpanId: '293e5b7de78ff24c',
                        name: 'chat qwen-max',
                        kind: 1,
                        startTimeUnixNano: '1733482368328941400',
                        endTimeUnixNano: '1733482372190192300',
                        attributes: [
                            {
                                key: 'gen_ai.operation.name',
                                value: {
                                    stringValue: 'chat',
                                },
                            },
                            {
                                key: 'gen_ai.request.model',
                                value: {
                                    stringValue: 'qwen-max',
                                },
                            },
                            {
                                key: 'gen_ai.request.temperature',
                                value: {
                                    stringValue: '0.8',
                                },
                            },
                            {
                                key: 'gen_ai.response.finish_reasons',
                                value: {
                                    stringValue: '["STOP"]',
                                },
                            },
                            {
                                key: 'gen_ai.response.id',
                                value: {
                                    stringValue: '1218e052-8dbf-9436-b97b-9b7f26160a6f',
                                },
                            },
                            {
                                key: 'gen_ai.response.model',
                                value: {
                                    stringValue: 'none',
                                },
                            },
                            {
                                key: 'gen_ai.system',
                                value: {
                                    stringValue: 'dashscope',
                                },
                            },
                            {
                                key: 'gen_ai.usage.input_tokens',
                                value: {
                                    stringValue: '836',
                                },
                            },
                            {
                                key: 'gen_ai.usage.output_tokens',
                                value: {
                                    stringValue: '79',
                                },
                            },
                            {
                                key: 'gen_ai.usage.total_tokens',
                                value: {
                                    stringValue: '915',
                                },
                            },
                        ],
                        events: [
                            {
                                timeUnixNano: '1733482372189939700',
                                name: 'gen_ai.content.completion',
                                attributes: [
                                    {
                                        key: 'gen_ai.completion',
                                        value: {
                                            arrayValue: {
                                                values: [
                                                    {
                                                        stringValue: '您好！很高兴能帮助您预订机票。不过，目前我可以通过这个聊天系统帮您查询已有预订的详情、更改预订日期或取消预订。新的预订需要通过我们的官方网站或者移动应用来完成。在那里您可以方便地选择航班、填写个人信息并完成支付。如果有其他关于现有预订的问题，欢迎随时告诉我您的预订号和姓名，我会很乐意提供帮助！',
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                            {
                                timeUnixNano: '1733482372189959700',
                                name: 'gen_ai.content.prompt',
                                attributes: [
                                    {
                                        key: 'gen_ai.prompt',
                                        value: {
                                            arrayValue: {
                                                values: [
                                                    {
                                                        stringValue: '\t您是“Funnair”航空公司的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。\r\n\t您正在通过在线聊天系统与客户互动。\r\n\t您能够支持已有机票的预订详情查询、机票日期改签、机票预订取消等操作，其余功能将在后续版本中添加，如果用户问的问题不支持请告知详情。\r\n   在提供有关机票预订详情查询、机票日期改签、机票预订取消等操作之前，您必须始终从用户处获取以下信息：预订号、客户姓名。\r\n   在询问用户之前，请检查消息历史记录以获取预订号、客户姓名等信息，尽量避免重复询问给用户造成困扰。\r\n   在更改预订之前，您必须确保条款允许这样做。\r\n   如果更改需要收费，您必须在继续之前征得用户同意。\r\n   使用提供的功能获取预订详细信息、更改预订和取消预订。\r\n   如果需要，您可以调用相应函数辅助完成。\r\n   请讲中文。\r\n   今天的日期是 2024-12-06.\r\n\r\n\r\nUse the conversation memory from the MEMORY section to provide accurate answers.\r\n\r\n---------------------\r\nMEMORY:\r\n---------------------\r\n\r\n',
                                                    },
                                                    {
                                                        stringValue: "帮我订一张机票\r\nContext information is below.\r\n---------------------\r\nThese Terms of Service govern your experience with Funnair. By booking a flight, you agree to these terms.\r\n\r\n1. Booking Flights\r\n- Book via our website or mobile app.\r\n- Full payment required at booking.\r\n- Ensure accuracy of personal information (Name, ID, etc.) as corrections may incur a $25 fee.\r\n\r\n2. Changing Bookings\r\n- Changes allowed up to 24 hours before flight.\r\n- Change via online or contact our support.\r\n- Change fee: $50 for Economy, $30 for Premium Economy, Free for Business Class.\r\n\r\n3. Cancelling Bookings\r\n- Cancel up to 48 hours before flight.\r\n- Cancellation fees: $75 for Economy, $50 for Premium Economy, $25 for Business Class.\r\n- Refunds processed within 7 business days.\r\n---------------------\r\nGiven the context and provided history information and not prior knowledge,\r\nreply to the user comment. If the answer is not in the context, inform\r\nthe user that you can't answer the question.\r\n",
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                        ],
                        links: [

                        ],
                        status: {

                        },
                        flags: 1,
                    },
                    {
                        traceId: 'c227f936a3eb510e7da12fda10a8ffab',
                        spanId: '34d47bbe2a8ac705',
                        parentSpanId: '237afa79e0543bfa',
                        name: 'spring_ai chat_client',
                        kind: 1,
                        startTimeUnixNano: '1733482368055418500',
                        endTimeUnixNano: '1733482372193597100',
                        attributes: [
                            {
                                key: 'gen_ai.operation.name',
                                value: {
                                    stringValue: 'framework',
                                },
                            },
                            {
                                key: 'gen_ai.system',
                                value: {
                                    stringValue: 'spring_ai',
                                },
                            },
                            {
                                key: 'spring.ai.chat.client.advisor.params',
                                value: {
                                    stringValue: '["chat_memory_response_size":"100", "chat_memory_conversation_id":"123"]',
                                },
                            },
                            {
                                key: 'spring.ai.chat.client.advisors',
                                value: {
                                    stringValue: '["CallAroundAdvisor", "StreamAroundAdvisor", "PromptChatMemoryAdvisor", "QuestionAnswerAdvisor", "LoggingAdvisor", "CallAroundAdvisor", "StreamAroundAdvisor"]',
                                },
                            },
                            {
                                key: 'spring.ai.chat.client.stream',
                                value: {
                                    stringValue: 'true',
                                },
                            },
                            {
                                key: 'spring.ai.chat.client.system.params',
                                value: {
                                    stringValue: '["current_date":"2024-12-06"]',
                                },
                            },
                            {
                                key: 'spring.ai.chat.client.system.text',
                                value: {
                                    stringValue: '\t您是“Funnair”航空公司的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。\n\t您正在通过在线聊天系统与客户互动。\n\t您能够支持已有机票的预订详情查询、机票日期改签、机票预订取消等操作，其余功能将在后续版本中添加，如果用户问的问题不支持请告知详情。\n   在提供有关机票预订详情查询、机票日期改签、机票预订取消等操作之前，您必须始终从用户处获取以下信息：预订号、客户姓名。\n   在询问用户之前，请检查消息历史记录以获取预订号、客户姓名等信息，尽量避免重复询问给用户造成困扰。\n   在更改预订之前，您必须确保条款允许这样做。\n   如果更改需要收费，您必须在继续之前征得用户同意。\n   使用提供的功能获取预订详细信息、更改预订和取消预订。\n   如果需要，您可以调用相应函数辅助完成。\n   请讲中文。\n   今天的日期是 {current_date}.\n',
                                },
                            },
                            {
                                key: 'spring.ai.chat.client.tool.function.names',
                                value: {
                                    stringValue: '["getBookingDetails", "changeBooking", "cancelBooking"]',
                                },
                            },
                            {
                                key: 'spring.ai.chat.client.user.text',
                                value: {
                                    stringValue: '帮我订一张机票',
                                },
                            },
                            {
                                key: 'spring.ai.kind',
                                value: {
                                    stringValue: 'chat_client',
                                },
                            },
                        ],
                        events: [

                        ],
                        links: [

                        ],
                        status: {

                        },
                        flags: 1,
                    },
                    {
                        traceId: 'c227f936a3eb510e7da12fda10a8ffab',
                        spanId: '2cc4c387d633177d',
                        parentSpanId: '237afa79e0543bfa',
                        name: 'prompt_chat_memory',
                        kind: 1,
                        startTimeUnixNano: '1733482368058143500',
                        endTimeUnixNano: '1733482372193802000',
                        attributes: [
                            {
                                key: 'spring.ai.advisor.name',
                                value: {
                                    stringValue: 'PromptChatMemoryAdvisor',
                                },
                            },
                            {
                                key: 'spring.ai.advisor.order',
                                value: {
                                    stringValue: '-2147482648',
                                },
                            },
                            {
                                key: 'spring.ai.advisor.type',
                                value: {
                                    stringValue: 'AROUND',
                                },
                            },
                            {
                                key: 'spring.ai.kind',
                                value: {
                                    stringValue: 'advisor',
                                },
                            },
                        ],
                        events: [

                        ],
                        links: [

                        ],
                        status: {

                        },
                        flags: 1,
                    },
                    {
                        traceId: 'c227f936a3eb510e7da12fda10a8ffab',
                        spanId: '17429d2ff596381e',
                        parentSpanId: '2cc4c387d633177d',
                        name: 'logging',
                        kind: 1,
                        startTimeUnixNano: '1733482368071441700',
                        endTimeUnixNano: '1733482372193927800',
                        attributes: [
                            {
                                key: 'spring.ai.advisor.name',
                                value: {
                                    stringValue: 'LoggingAdvisor',
                                },
                            },
                            {
                                key: 'spring.ai.advisor.order',
                                value: {
                                    stringValue: '0',
                                },
                            },
                            {
                                key: 'spring.ai.advisor.type',
                                value: {
                                    stringValue: 'AROUND',
                                },
                            },
                            {
                                key: 'spring.ai.kind',
                                value: {
                                    stringValue: 'advisor',
                                },
                            },
                        ],
                        events: [

                        ],
                        links: [

                        ],
                        status: {

                        },
                        flags: 1,
                    },
                    {
                        traceId: 'c227f936a3eb510e7da12fda10a8ffab',
                        spanId: '0cd6146350ae492d',
                        parentSpanId: '17429d2ff596381e',
                        name: 'question_answer',
                        kind: 1,
                        startTimeUnixNano: '1733482368080853700',
                        endTimeUnixNano: '1733482372194066500',
                        attributes: [
                            {
                                key: 'spring.ai.advisor.name',
                                value: {
                                    stringValue: 'QuestionAnswerAdvisor',
                                },
                            },
                            {
                                key: 'spring.ai.advisor.order',
                                value: {
                                    stringValue: '0',
                                },
                            },
                            {
                                key: 'spring.ai.advisor.type',
                                value: {
                                    stringValue: 'AROUND',
                                },
                            },
                            {
                                key: 'spring.ai.kind',
                                value: {
                                    stringValue: 'advisor',
                                },
                            },
                        ],
                        events: [

                        ],
                        links: [

                        ],
                        status: {

                        },
                        flags: 1,
                    },
                    {
                        traceId: 'c227f936a3eb510e7da12fda10a8ffab',
                        spanId: '293e5b7de78ff24c',
                        parentSpanId: '0cd6146350ae492d',
                        name: 'stream',
                        kind: 1,
                        startTimeUnixNano: '1733482368209384600',
                        endTimeUnixNano: '1733482372194171500',
                        attributes: [
                            {
                                key: 'spring.ai.advisor.name',
                                value: {
                                    stringValue: 'StreamAroundAdvisor',
                                },
                            },
                            {
                                key: 'spring.ai.advisor.order',
                                value: {
                                    stringValue: '2147483647',
                                },
                            },
                            {
                                key: 'spring.ai.advisor.type',
                                value: {
                                    stringValue: 'AROUND',
                                },
                            },
                            {
                                key: 'spring.ai.kind',
                                value: {
                                    stringValue: 'advisor',
                                },
                            },
                        ],
                        events: [

                        ],
                        links: [

                        ],
                        status: {

                        },
                        flags: 1,
                    },
                    {
                        traceId: 'c227f936a3eb510e7da12fda10a8ffab',
                        spanId: '237afa79e0543bfa',
                        name: 'http get /api/assistant/chat',
                        kind: 2,
                        startTimeUnixNano: '1733482368033994400',
                        endTimeUnixNano: '1733482372201313900',
                        attributes: [
                            {
                                key: 'exception',
                                value: {
                                    stringValue: 'none',
                                },
                            },
                            {
                                key: 'http.url',
                                value: {
                                    stringValue: '/api/assistant/chat',
                                },
                            },
                            {
                                key: 'method',
                                value: {
                                    stringValue: 'GET',
                                },
                            },
                            {
                                key: 'outcome',
                                value: {
                                    stringValue: 'SUCCESS',
                                },
                            },
                            {
                                key: 'status',
                                value: {
                                    stringValue: '200',
                                },
                            },
                            {
                                key: 'uri',
                                value: {
                                    stringValue: '/api/assistant/chat',
                                },
                            },
                        ],
                        events: [

                        ],
                        links: [

                        ],
                        status: {

                        },
                        flags: 1,
                    },
                ],
            },
        ],
    },
    {
        resource: {
            attributes: [
                {
                    key: 'service.name',
                    value: {
                        stringValue: 'cli-debug-example',
                    },
                },
                {
                    key: 'telemetry.sdk.language',
                    value: {
                        stringValue: 'java',
                    },
                },
                {
                    key: 'telemetry.sdk.name',
                    value: {
                        stringValue: 'opentelemetry',
                    },
                },
                {
                    key: 'telemetry.sdk.version',
                    value: {
                        stringValue: '1.37.0',
                    },
                },
            ],
        },
        scopeSpans: [
            {
                scope: {
                    name: 'org.springframework.boot',
                    version: '3.3.3',
                    attributes: [

                    ],
                },
                spans: [
                    {
                        traceId: 'fa48084bdfd709ecafe3c8f8dad6ad9b',
                        spanId: '1c6f7c3ebaa14bd7',
                        parentSpanId: '8a9944329ee78a50',
                        name: 'http post',
                        kind: 3,
                        startTimeUnixNano: '1733552341723258200',
                        endTimeUnixNano: '1733552343197490300',
                        attributes: [
                            {
                                key: 'client.name',
                                value: {
                                    stringValue: 'dashscope.aliyuncs.com',
                                },
                            },
                            {
                                key: 'exception',
                                value: {
                                    stringValue: 'none',
                                },
                            },
                            {
                                key: 'http.url',
                                value: {
                                    stringValue: 'https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation',
                                },
                            },
                            {
                                key: 'method',
                                value: {
                                    stringValue: 'POST',
                                },
                            },
                            {
                                key: 'outcome',
                                value: {
                                    stringValue: 'SUCCESS',
                                },
                            },
                            {
                                key: 'status',
                                value: {
                                    stringValue: '200',
                                },
                            },
                            {
                                key: 'uri',
                                value: {
                                    stringValue: '/api/v1/services/aigc/text-generation/generation',
                                },
                            },
                        ],
                        events: [

                        ],
                        links: [

                        ],
                        status: {

                        },
                        flags: 1,
                    },
                    {
                        traceId: 'fa48084bdfd709ecafe3c8f8dad6ad9b',
                        spanId: '8a9944329ee78a50',
                        parentSpanId: 'a046db2e79e24a63',
                        name: 'chat qwen-plus',
                        kind: 1,
                        startTimeUnixNano: '1733552341703735000',
                        endTimeUnixNano: '1733552343202522500',
                        attributes: [
                            {
                                key: 'gen_ai.operation.name',
                                value: {
                                    stringValue: 'chat',
                                },
                            },
                            {
                                key: 'gen_ai.request.model',
                                value: {
                                    stringValue: 'qwen-plus',
                                },
                            },
                            {
                                key: 'gen_ai.request.temperature',
                                value: {
                                    stringValue: '0.8',
                                },
                            },
                            {
                                key: 'gen_ai.response.finish_reasons',
                                value: {
                                    stringValue: '["TOOL_CALLS"]',
                                },
                            },
                            {
                                key: 'gen_ai.response.id',
                                value: {
                                    stringValue: '9a4fa657-b2a2-9d7a-8927-9a8a5f740149',
                                },
                            },
                            {
                                key: 'gen_ai.response.model',
                                value: {
                                    stringValue: 'none',
                                },
                            },
                            {
                                key: 'gen_ai.system',
                                value: {
                                    stringValue: 'dashscope',
                                },
                            },
                            {
                                key: 'gen_ai.usage.input_tokens',
                                value: {
                                    stringValue: '183',
                                },
                            },
                            {
                                key: 'gen_ai.usage.output_tokens',
                                value: {
                                    stringValue: '15',
                                },
                            },
                            {
                                key: 'gen_ai.usage.total_tokens',
                                value: {
                                    stringValue: '198',
                                },
                            },
                        ],
                        events: [
                            {
                                timeUnixNano: '1733552343202314300',
                                name: 'gen_ai.content.completion',
                                attributes: [
                                    {
                                        key: 'gen_ai.completion',
                                        value: {
                                            arrayValue: {
                                                values: [

                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                            {
                                timeUnixNano: '1733552343202328000',
                                name: 'gen_ai.content.prompt',
                                attributes: [
                                    {
                                        key: 'gen_ai.prompt',
                                        value: {
                                            arrayValue: {
                                                values: [
                                                    {
                                                        stringValue: '现在几点？',
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                        ],
                        links: [

                        ],
                        status: {

                        },
                        flags: 1,
                    },
                    {
                        traceId: 'fa48084bdfd709ecafe3c8f8dad6ad9b',
                        spanId: 'ce42d820e35ba313',
                        parentSpanId: 'a33f0af700231f12',
                        name: 'http post',
                        kind: 3,
                        startTimeUnixNano: '1733552343212865800',
                        endTimeUnixNano: '1733552345462583300',
                        attributes: [
                            {
                                key: 'client.name',
                                value: {
                                    stringValue: 'dashscope.aliyuncs.com',
                                },
                            },
                            {
                                key: 'exception',
                                value: {
                                    stringValue: 'none',
                                },
                            },
                            {
                                key: 'http.url',
                                value: {
                                    stringValue: 'https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation',
                                },
                            },
                            {
                                key: 'method',
                                value: {
                                    stringValue: 'POST',
                                },
                            },
                            {
                                key: 'outcome',
                                value: {
                                    stringValue: 'SUCCESS',
                                },
                            },
                            {
                                key: 'status',
                                value: {
                                    stringValue: '200',
                                },
                            },
                            {
                                key: 'uri',
                                value: {
                                    stringValue: '/api/v1/services/aigc/text-generation/generation',
                                },
                            },
                        ],
                        events: [

                        ],
                        links: [

                        ],
                        status: {

                        },
                        flags: 1,
                    },
                    {
                        traceId: 'fa48084bdfd709ecafe3c8f8dad6ad9b',
                        spanId: 'a33f0af700231f12',
                        parentSpanId: 'a046db2e79e24a63',
                        name: 'chat qwen-plus',
                        kind: 1,
                        startTimeUnixNano: '1733552343211499200',
                        endTimeUnixNano: '1733552345463755900',
                        attributes: [
                            {
                                key: 'gen_ai.operation.name',
                                value: {
                                    stringValue: 'chat',
                                },
                            },
                            {
                                key: 'gen_ai.request.model',
                                value: {
                                    stringValue: 'qwen-plus',
                                },
                            },
                            {
                                key: 'gen_ai.request.temperature',
                                value: {
                                    stringValue: '0.8',
                                },
                            },
                            {
                                key: 'gen_ai.response.finish_reasons',
                                value: {
                                    stringValue: '["STOP"]',
                                },
                            },
                            {
                                key: 'gen_ai.response.id',
                                value: {
                                    stringValue: 'ab55143c-2c5c-9d7d-87a9-1dadee80e69b',
                                },
                            },
                            {
                                key: 'gen_ai.response.model',
                                value: {
                                    stringValue: 'none',
                                },
                            },
                            {
                                key: 'gen_ai.system',
                                value: {
                                    stringValue: 'dashscope',
                                },
                            },
                            {
                                key: 'gen_ai.usage.input_tokens',
                                value: {
                                    stringValue: '236',
                                },
                            },
                            {
                                key: 'gen_ai.usage.output_tokens',
                                value: {
                                    stringValue: '30',
                                },
                            },
                            {
                                key: 'gen_ai.usage.total_tokens',
                                value: {
                                    stringValue: '266',
                                },
                            },
                        ],
                        events: [
                            {
                                timeUnixNano: '1733552345463618100',
                                name: 'gen_ai.content.completion',
                                attributes: [
                                    {
                                        key: 'gen_ai.completion',
                                        value: {
                                            arrayValue: {
                                                values: [
                                                    {
                                                        stringValue: '当前时间是 2024年12月7日 14:19:03（中国标准时间）。',
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                            {
                                timeUnixNano: '1733552345463637400',
                                name: 'gen_ai.content.prompt',
                                attributes: [
                                    {
                                        key: 'gen_ai.prompt',
                                        value: {
                                            arrayValue: {
                                                values: [
                                                    {
                                                        stringValue: '现在几点？',
                                                    },
                                                    {
                                                        stringValue: '',
                                                    },
                                                    {
                                                        stringValue: '',
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                        ],
                        links: [

                        ],
                        status: {

                        },
                        flags: 1,
                    },
                    {
                        traceId: 'fa48084bdfd709ecafe3c8f8dad6ad9b',
                        spanId: 'a046db2e79e24a63',
                        parentSpanId: '5c78be717236cd2c',
                        name: 'call',
                        kind: 1,
                        startTimeUnixNano: '1733552341702750200',
                        endTimeUnixNano: '1733552345464021400',
                        attributes: [
                            {
                                key: 'spring.ai.advisor.name',
                                value: {
                                    stringValue: 'CallAroundAdvisor',
                                },
                            },
                            {
                                key: 'spring.ai.advisor.order',
                                value: {
                                    stringValue: '2147483647',
                                },
                            },
                            {
                                key: 'spring.ai.advisor.type',
                                value: {
                                    stringValue: 'AROUND',
                                },
                            },
                            {
                                key: 'spring.ai.kind',
                                value: {
                                    stringValue: 'advisor',
                                },
                            },
                        ],
                        events: [

                        ],
                        links: [

                        ],
                        status: {

                        },
                        flags: 1,
                    },
                    {
                        traceId: 'fa48084bdfd709ecafe3c8f8dad6ad9b',
                        spanId: '5c78be717236cd2c',
                        parentSpanId: '83164dcb8f2ced8e',
                        name: 'spring_ai chat_client',
                        kind: 1,
                        startTimeUnixNano: '1733552341702543200',
                        endTimeUnixNano: '1733552345464145400',
                        attributes: [
                            {
                                key: 'gen_ai.operation.name',
                                value: {
                                    stringValue: 'framework',
                                },
                            },
                            {
                                key: 'gen_ai.system',
                                value: {
                                    stringValue: 'spring_ai',
                                },
                            },
                            {
                                key: 'spring.ai.chat.client.advisors',
                                value: {
                                    stringValue: '["CallAroundAdvisor", "StreamAroundAdvisor", "CallAroundAdvisor", "StreamAroundAdvisor"]',
                                },
                            },
                            {
                                key: 'spring.ai.chat.client.stream',
                                value: {
                                    stringValue: 'false',
                                },
                            },
                            {
                                key: 'spring.ai.chat.client.tool.function.callbacks',
                                value: {
                                    stringValue: '["Get the current local time"]',
                                },
                            },
                            {
                                key: 'spring.ai.chat.client.user.text',
                                value: {
                                    stringValue: '现在几点？',
                                },
                            },
                            {
                                key: 'spring.ai.kind',
                                value: {
                                    stringValue: 'chat_client',
                                },
                            },
                        ],
                        events: [

                        ],
                        links: [

                        ],
                        status: {

                        },
                        flags: 1,
                    },
                    {
                        traceId: 'fa48084bdfd709ecafe3c8f8dad6ad9b',
                        spanId: '83164dcb8f2ced8e',
                        name: 'http get /studio/api/observation/chatClient',
                        kind: 2,
                        startTimeUnixNano: '1733552341644610000',
                        endTimeUnixNano: '1733552345467847800',
                        attributes: [
                            {
                                key: 'exception',
                                value: {
                                    stringValue: 'none',
                                },
                            },
                            {
                                key: 'http.url',
                                value: {
                                    stringValue: '/studio/api/observation/chatClient',
                                },
                            },
                            {
                                key: 'method',
                                value: {
                                    stringValue: 'GET',
                                },
                            },
                            {
                                key: 'outcome',
                                value: {
                                    stringValue: 'SUCCESS',
                                },
                            },
                            {
                                key: 'status',
                                value: {
                                    stringValue: '200',
                                },
                            },
                            {
                                key: 'uri',
                                value: {
                                    stringValue: '/studio/api/observation/chatClient',
                                },
                            },
                        ],
                        events: [

                        ],
                        links: [

                        ],
                        status: {

                        },
                        flags: 1,
                    },
                ],
            },
        ],
    },
];