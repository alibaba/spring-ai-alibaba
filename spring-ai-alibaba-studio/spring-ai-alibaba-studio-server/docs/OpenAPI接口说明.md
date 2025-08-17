# OpenAPI接口说明

### 一、应用调用

#### 1.1 智能体调用

```plain
https://localhost:8080/api/v1/apps/chat/completions
```

##### 功能描述

智能体调用api说明。

##### 入参描述

|  一级参数  |  二级参数  |  类型  |  必选  |  描述  |
| --- | --- | --- | --- | --- |
|  Headers (HTTP 请求头)  |  Content-Type  |  string  |  是  |  请求类型：application/json  |
|  |  Authorization  |  string  |  是  |  API-Key，例如：Bearer sk-d1\*\*2a  |
|  |  X-Aagentscope-WorkSpace  |  string  |  是  |  业务空间id。  |
|  app\_id  |   |  string  |  是  |  智能体应用唯一标识。  |
|  conversation\_id  |   |  string  |  否  |  对话历史会话唯一标识, 传入conversation\_id后，将在云端存储对话历史进行记录。  |
|  stream  |   |  bool  |  是  |  是否流式输出  |
|  messages  |   |  list  |  是  |  由历史对话组成的消息列表。  |
|  |  role  |  string  |  是  |  用户消息的角色，固定为user。  |
|  |  content  |  string\|map  |  是  |  消息内容， 包括文本，图像，音频等类型。  |
|  |  content\_type  |  string  |  是  |  内容类型，默认为text text: content为string类型的文本 multimodal: 多模态类型数组  |
|  prompt\_variables  |   |  map  |  否  |  prompt自定义变量  |
|  extra\_params  |   |  map  |  否  |  插件和流程画布的补充参数信息。传入此参数后，调用插件api时会传入此参数值。 { "tool\_xxxxxx": { "user\_id": 10001, "location": "北京" } }  |
|  is\_draft  |   |  bool  |  否  |  是否调用草稿版本，默认为false  |

**MultimodelContent定义**:

|  字段名  |  字段类型  |  是否必填  |  说明  |
| --- | --- | --- | --- |
|  type  |  string  |  是  |  多模态类型, text: 文本 image: 图像  |
|  text  |  string  |  否  |  文本内容  |
|  path  |  string  |  否  |  本地路径  |
|  url  |  string  |  否  |  音频/图片/视频地址  |
|  data  |  string  |  否  |  音频/图片/视频的base64格式数据  |

##### 出参描述

|  一级参数  |  二级参数  |  三级参数  |  四级参数  |  类型  |  描述  |
| --- | --- | --- | --- | --- | --- |
|  调用是否成功（http status code）  |   |   |   |  int  |  调用是否成功通过 http 的返回 code 来判断，返回 200 则表示此次调用成功，返回其他值，则通过 code 和 message 来设置对应的失败信息。  |
|  conversation\_id  |   |   |   |  string  |  多轮会话id  |
|  status  |   |   |   |  string  |  有三种情况： completed failed in\_progress  |
|  message  |   |   |   |  object  |  模型生成回复为message。  |
|   |  role  |   |   |  string  |  包括assisistant、plugin或者function。  |
|   |  name  |   |   |  string  |  role为plugin或者function时对应插件名或者函数名  |
|   |  content  |   |   |  string  |  模型生成的内容。  |
|   |  tool\_calls  |   |   |  list  |  插件返回的状态  |
|   |   |  id  |   |  string  |  插件唯一标识。  |
|   |   |  type  |   |  string  |  函数调用类型, 默认为function。 function: 本地函数 tool\_call: 远程函数调用 tool\_result: 远程函数调用结果。 file\_search\_call: 文档检索调用 file\_search\_result: 文档检索结果  |
|   |   |  function  |   |  map  |  执行的工具信息。  |
|   |   |   |  name  |  string  |  工具名。  |
|   |   |   |  arguments  |  string  |  json字符串格式的工具调用参数。  |
|   |   |   |  output  |  string  |  json字符串格式的工具调用结果。  |
|  usage  |  prompt\_tokens  |   |   |  int  |  输入的token数量。  |
|  |  completion\_tokens  |   |   |  int  |  输出的token数量。  |
|  |  total\_tokens  |   |   |  int  |  输入为文本时候返回，为input\_tokens和output\_tokens的和  |
|  error  |  code  |   |   |  string  |  出错的时候的错误代码，参考标准错误码列表。  |
|   |  message  |   |   |  string  |  出错的时候的详细错误信息。  |
|  request\_id  |   |   |   |  string  |  本次请求的系统唯一码  |

##### 请求示例（SSE 打开）

```shell
curl -X 'POST' \
  'http://127.0.0.1:8080/api/v1/apps/chat/completions' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer sk-xxxxxxxxxxxxxxxxxx' \
  -H 'Content-Type: application/json' \
  -d '{
  "app_id": "1918564389287088129",
  "messages": [
    {
      "role": "user",
      "content": "Spring AI Alibaba 是什么",
      "content_type": "text"
    }
  ],
  "stream": true
}'
```

##### 响应示例（SSE 打开）

```plain
data:{"status":"IN_PROGRESS","message":{"role":"assistant","content":"","content_type":"text","tool_calls":[{"id":"06853c54-4dba-423b-9836-f092ad4d04ed","type":"file_search_call","function":{"arguments":"{\"search_options\":{\"retrieve_max_length\":2000,\"similarity_threshold\":0.2,\"hybrid_weight\":0.7,\"search_type\":\"hybrid\",\"enable_rerank\":false,\"kb_ids\":[\"1917515442268696577\"],\"enable_search\":true,\"enable_citation\":false,\"top_k\":5},\"query\":\"srping ai alibaba是什么\"}"}}]},"model":"","usage":{"input_tokens":0,"output_tokens":0,"total_tokens":0},"request_id":"cd9cf2a3-50f3-4bc8-8418-fe3a4a04e32f","conversation_id":"1918572071586775041"}

data:{"status":"IN_PROGRESS","message":{"role":"assistant","content":"","content_type":"text","tool_calls":[{"id":"5a365014-2fe8-4b07-ab5f-a70828eeb197","type":"file_search_result","function":{"output":"[{\"title\":\"null\",\"text\":\"spring ai alibaba....",\"score\":0.19030983642877547,\"enabled\":true,\"doc_id\":\"1917917458546270209\",\"doc_name\":\"efa4ea5dad6c4107a16ba5983c27747c_1746099370182.pdf\",\"page_number\":3,\"chunk_id\":\"bf99eeec-ffa0-450a-9392-20b676ace11c\"}]"}}]},"model":"","usage":{"input_tokens":0,"output_tokens":0,"total_tokens":0},"request_id":"cd9cf2a3-50f3-4bc8-8418-fe3a4a04e32f","conversation_id":"1918572071586775041"}

data:{"status":"IN_PROGRESS","message":{"role":"assistant","content":"参考官方文档或","content_type":"text"},"model":"qwen-plus","usage":{"input_tokens":0,"output_tokens":0,"total_tokens":0},"request_id":"cd9cf2a3-50f3-4bc8-8418-fe3a4a04e32f","conversation_id":"1918572071586775041"}

data:{"status":"IN_PROGRESS","message":{"role":"assistant","content":"示例代码。","content_type":"text"},"model":"qwen-plus","usage":{"input_tokens":0,"output_tokens":0,"total_tokens":0},"request_id":"cd9cf2a3-50f3-4bc8-8418-fe3a4a04e32f","conversation_id":"1918572071586775041"}

data:{"status":"COMPLETED","message":{"role":"assistant","content":"","content_type":"text"},"model":"qwen-plus","usage":{"input_tokens":1791,"output_tokens":263,"total_tokens":2054},"request_id":"cd9cf2a3-50f3-4bc8-8418-fe3a4a04e32f","conversation_id":"1918572071586775041"}
```

在产生首个响应包之前出错的情况下，输出的结果中会通过 code 和 message 指明出错原因。

##### 异常响应示例

```plain
{
  "success": false,
  "request_id": "",
  "error": {
    "type": "invalid_request_error",
    "code":"InvalidApiKey",
    "message":"Invalid API-key provided.",
    "status_code": 401
  }
}
```

#### 1.2 图片识别-URL

```json
{
  "app_id": "1919725185502654466",
  "messages": [
    {
      "role": "user",
      "content": [
        {
          "type": "text",
          "text": "这是什么"
        },
        {
          "type": "image",
          "url": "https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg"
        }
      ],
      "content_type": "multimodal",
      "name": "string"
    }
  ],
  "stream": true
}
```

**图片识别-path**

**注：path是通过上传接口上传后返回的地址**

```json
{
  "app_id": "1919725185502654466",
  "messages": [
    {
      "role": "user",
      "content": [
        {
          "type": "text",
          "text": "这是什么"
        },
        {
          "type": "image",
          "url": "image/10000/1/20250507/543dfc03bd28431ea047deec6c19a80b_1746583938176.jpeg"
        }
      ],
      "content_type": "multimodal",
      "name": "string"
    }
  ],
  "stream": true
}
```

#### 2.1 工作流调用

```plain
https://localhost:8080/api/v1/apps/workflow/completions
```

##### 功能描述

工作流调用api说明。

##### 入参描述

|  一级参数  |  二级参数  |  类型  |  必选  |  描述  |
| --- | --- | --- | --- | --- |
|  Headers (HTTP 请求头)  |  Content-Type  |  string  |  是  |  请求类型：application/json  |
|  |  Authorization  |  string  |  是  |  API-Key，例如：Bearer sk-d1\*\*2a  |
|  |  X-Aagentscope-WorkSpace  |  string  |  是  |  业务空间id。  |
|  app\_id  |   |  string  |  是  |  工作流应用唯一标识。  |
|  conversation\_id  |   |  string  |  否  |  对话历史会话唯一标识, 传入conversation\_id后，将在云端存储对话历史进行记录。  |
|  stream  |   |  bool  |  否  |  是否流式输出，默认为false  |
|  messages  |   |  list  |  是  |  由历史对话组成的消息列表。  |
|  |  role  |  string  |  是  |  用户消息的角色，固定为user。  |
|  |  content  |  string\|map  |  是  |  消息内容， 包括文本，图像，音频等类型。  |
|  |  content\_type  |  string  |  是  |  内容类型，默认为text text: content为string类型的文本 multimodal: 多模态类型数组  |
|  input\_params  |   |  list  |  否  |  工作流执行入参，包含系统参数query和开始节点入参 \[ { "key": "query", "type": "String", "desc": "用户问句", "required": false, "source": "sys", "value": "杭州" } \]  |
|  draft  |   |  bool  |  否  |  是否调用草稿版本，默认为false  |

**MultimodelContent定义**:

|  字段名  |  字段类型  |  是否必填  |  说明  |
| --- | --- | --- | --- |
|  type  |  string  |  是  |  多模态类型, text: 文本 image: 图像  |
|  text  |  string  |  否  |  文本内容  |
|  path  |  string  |  否  |  本地路径  |
|  url  |  string  |  否  |  音频/图片/视频地址  |
|  data  |  string  |  否  |  音频/图片/视频的base64格式数据  |

##### 出参描述

|  一级参数  |  二级参数  |  类型  |  描述  |
| --- | --- | --- | --- |
|  调用是否成功（http status code）  |   |  int  |  调用是否成功通过 http 的返回 code 来判断，返回 200 则表示此次调用成功，返回其他值，则通过 code 和 message 来设置对应的失败信息。  |
|  conversation\_id  |   |  string  |  多轮会话id  |
|  status  |   |  string  |  有三种情况： completed failed in\_progress  |
|  message  |   |  object  |  模型生成回复为message。  |
|   |  role  |  string  |  包括assisistant  |
|   |  content  |  string  |  模型生成的内容。  |
|  error  |  code  |  string  |  出错的时候的错误代码，参考标准错误码列表。  |
|   |  message  |  string  |  出错的时候的详细错误信息。  |
|  task\_id  |   |  string  |  工作流执行任务id  |
|  node\_id  |   |  string  |  输出当前内容的节点的id  |
|  node\_name  |   |  string  |  输出当前内容的节点的name  |
|  node\_type  |   |  string  |  输出当前内容的节点的类型  |
|  node\_status  |   |  string  |  输出当前内容的节点的状态  |
|  node\_msg\_seq\_id  |   |  int  |  当前应用推送出的第x包数据  |
|  node\_is\_completed  |   |  bool  |  输出当前内容的节点是否输出完成  |
|  request\_id  |   |  string  |  本次请求的系统唯一码  |

##### 请求示例（SSE 打开）

```shell
curl -X 'POST' \
'http://120.26.121.86/api/v1/apps/workflow/completions' \
-H 'accept: */*' \
-H 'Authorization: Bearer sk-xxx' \
-H 'Content-Type: application/json' \
-d '{
  "app_id": "1922840526808092673",
  "inputParams": [
    {
      "key": "query",
      "type": "String",
      "desc": "用户问句",
      "required": false,
      "source": "sys",
      "value": "介绍一下阿里云百炼"
    }
  ],
  "stream": true
}'
```

##### 响应示例（SSE 打开）

```plain
data:{"status":"in_progress","message":{"role":"assistant","content":"阿里"},"request_id":"e545aae7-b56f-42df-a440-c2737b1cedf5","conversation_id":"2bd96fd6-09fc-48d9-ac83-dbac189a5262","task_id":"6bcbe130-dd94-4ab5-9da3-3d0e398a5505","node_id":"End_QCEE","node_name":"结束","node_type":"End","node_status":"executing","node_msg_seq_id":1,"node_is_completed":false}

data:{"status":"in_progress","message":{"role":"assistant","content":"云"},"request_id":"e545aae7-b56f-42df-a440-c2737b1cedf5","conversation_id":"2bd96fd6-09fc-48d9-ac83-dbac189a5262","task_id":"6bcbe130-dd94-4ab5-9da3-3d0e398a5505","node_id":"End_QCEE","node_name":"结束","node_type":"End","node_status":"executing","node_msg_seq_id":2,"node_is_completed":false}

data:{"status":"in_progress","message":{"role":"assistant","content":"百"},"request_id":"e545aae7-b56f-42df-a440-c2737b1cedf5","conversation_id":"2bd96fd6-09fc-48d9-ac83-dbac189a5262","task_id":"6bcbe130-dd94-4ab5-9da3-3d0e398a5505","node_id":"End_QCEE","node_name":"结束","node_type":"End","node_status":"executing","node_msg_seq_id":3,"node_is_completed":false}

data:{"status":"in_progress","message":{"role":"assistant","content":"炼是通义"},"request_id":"e545aae7-b56f-42df-a440-c2737b1cedf5","conversation_id":"2bd96fd6-09fc-48d9-ac83-dbac189a5262","task_id":"6bcbe130-dd94-4ab5-9da3-3d0e398a5505","node_id":"End_QCEE","node_name":"结束","node_type":"End","node_status":"executing","node_msg_seq_id":4,"node_is_completed":false}

data:{"status":"in_progress","message":{"role":"assistant","content":"千问的训练"},"request_id":"e545aae7-b56f-42df-a440-c2737b1cedf5","conversation_id":"2bd96fd6-09fc-48d9-ac83-dbac189a5262","task_id":"6bcbe130-dd94-4ab5-9da3-3d0e398a5505","node_id":"End_QCEE","node_name":"结束","node_type":"End","node_status":"executing","node_msg_seq_id":5,"node_is_completed":false}

data:{"status":"in_progress","message":{"role":"assistant","content":"框架，支持大规模"},"request_id":"e545aae7-b56f-42df-a440-c2737b1cedf5","conversation_id":"2bd96fd6-09fc-48d9-ac83-dbac189a5262","task_id":"6bcbe130-dd94-4ab5-9da3-3d0e398a5505","node_id":"End_QCEE","node_name":"结束","node_type":"End","node_status":"executing","node_msg_seq_id":6,"node_is_completed":false}

data:{"status":"in_progress","message":{"role":"assistant","content":"分布式训练、高效"},"request_id":"e545aae7-b56f-42df-a440-c2737b1cedf5","conversation_id":"2bd96fd6-09fc-48d9-ac83-dbac189a5262","task_id":"6bcbe130-dd94-4ab5-9da3-3d0e398a5505","node_id":"End_QCEE","node_name":"结束","node_type":"End","node_status":"executing","node_msg_seq_id":7,"node_is_completed":false}

data:{"status":"in_progress","message":{"role":"assistant","content":"数据处理和模型"},"request_id":"e545aae7-b56f-42df-a440-c2737b1cedf5","conversation_id":"2bd96fd6-09fc-48d9-ac83-dbac189a5262","task_id":"6bcbe130-dd94-4ab5-9da3-3d0e398a5505","node_id":"End_QCEE","node_name":"结束","node_type":"End","node_status":"executing","node_msg_seq_id":8,"node_is_completed":false}

data:{"status":"in_progress","message":{"role":"assistant","content":"调优，助力"},"request_id":"e545aae7-b56f-42df-a440-c2737b1cedf5","conversation_id":"2bd96fd6-09fc-48d9-ac83-dbac189a5262","task_id":"6bcbe130-dd94-4ab5-9da3-3d0e398a5505","node_id":"End_QCEE","node_name":"结束","node_type":"End","node_status":"executing","node_msg_seq_id":9,"node_is_completed":false}

data:{"status":"in_progress","message":{"role":"assistant","content":"打造高性能语言模型"},"request_id":"e545aae7-b56f-42df-a440-c2737b1cedf5","conversation_id":"2bd96fd6-09fc-48d9-ac83-dbac189a5262","task_id":"6bcbe130-dd94-4ab5-9da3-3d0e398a5505","node_id":"End_QCEE","node_name":"结束","node_type":"End","node_status":"executing","node_msg_seq_id":10,"node_is_completed":false}

data:{"status":"in_progress","message":{"role":"assistant","content":"。"},"request_id":"e545aae7-b56f-42df-a440-c2737b1cedf5","conversation_id":"2bd96fd6-09fc-48d9-ac83-dbac189a5262","task_id":"6bcbe130-dd94-4ab5-9da3-3d0e398a5505","node_id":"End_QCEE","node_name":"结束","node_type":"End","node_status":"executing","node_msg_seq_id":11,"node_is_completed":false}

data:{"status":"in_progress","message":{"role":"assistant","content":""},"request_id":"e545aae7-b56f-42df-a440-c2737b1cedf5","conversation_id":"2bd96fd6-09fc-48d9-ac83-dbac189a5262","task_id":"6bcbe130-dd94-4ab5-9da3-3d0e398a5505","node_id":"End_QCEE","node_name":"结束","node_type":"End","node_status":"success","node_msg_seq_id":12,"node_is_completed":true}

data:{"status":"completed","request_id":"e545aae7-b56f-42df-a440-c2737b1cedf5","conversation_id":"2bd96fd6-09fc-48d9-ac83-dbac189a5262","task_id":"6bcbe130-dd94-4ab5-9da3-3d0e398a5505"}
```

在产生首个响应包之前出错的情况下，输出的结果中会通过 code 和 message 指明出错原因。

##### 异常响应示例

```json
{
    "code": "ApiKeyNotFound",
    "message": "Api key can not be found.",
    "request_id": "44ad228e-7272-4d73-8be1-e8b45e35f336"
}
```