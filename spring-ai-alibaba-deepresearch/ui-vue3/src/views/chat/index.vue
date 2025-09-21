<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements.  See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache License, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License.  You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
-->
<template>
  <div class="__container_chat_index">
    <Flex class="body" gap="middle">
      <Flex class="chat" vertical gap="middle" :style="{ width: current.deepResearchDetail ? '40%' : '100%' }" align="center">
        <div
          ref="scrollContainer"
          align="center"
          class="bubble-list"
          v-show="bubbleList.length > 0"
        >
          <Bubble.List style="min-height: 85%" :roles="roles" :items="bubbleList"> </Bubble.List>
          <Gap height="100px" />
          <!--          <div style="height: 100px; width: 0px" class="bottom-spacer"></div>-->
        </div>
        <Flex v-show="bubbleList.length === 0" class="bubble-list" justify="center" align="center">
          <div class="welcome">
            <span class="gradient-text">{{ $t('welcome') }}, {{ username }}</span>
          </div>
        </Flex>
        <div class="sender-wrapper">
          <sender
            class-name="sender"
            :header="headerNode"
            :autoSize="{ minRows: 2, maxRows: 3 }"
            :loading="senderLoading"
            v-model:value="content"
            @submit="submitHandle"
            :actions="false"
            placeholder="type an issue"
          >
            <template
              #footer="{
                info: {
                  components: { SendButton, LoadingButton, ClearButton },
                },
              }"
            >
              <Flex justify="space-between" align="center">
                <Flex align="center">
                  <a-button size="small" style="border-radius: 15px" type="text" @click="headerOpen = !headerOpen">
                      <LinkOutlined />
                  </a-button>

                  <a-switch
                    un-checked-children="极速模式"
                    checked-children="深度模式"
                    v-model:checked="current.deepResearch"
                  ></a-switch>
                </Flex>
                <Flex>
                  <component :is="ClearButton" />
                  <!-- <component :is="SpeechButton" /> -->
                  <component
                    :is="LoadingButton"
                    v-if="senderLoading"
                    type="default"
                    style="display: block"
                    @click="stopHandle"
                  >
                    <template #icon>
                      <Spin size="small" />
                    </template>
                  </component>
                  <component
                    :is="SendButton"
                    v-else
                    :icon="h(SendOutlined)"
                    shape="default"
                    type="text"
                    :style="{ color: token.colorPrimary }"
                    :disabled="false"
                  />
                </Flex>
              </Flex>
            </template>
          </sender>
        </div>
      </Flex>
      <Report :visible="current.deepResearchDetail" :threadId="current.threadId" :convId="convId" @close="current.deepResearchDetail = false" />
    </Flex>
  </div>
</template>

<script setup lang="tsx">
import { Button, Card, Flex, Spin, theme, Typography, message } from 'ant-design-vue'
import {
  CheckCircleOutlined,
  GlobalOutlined,
  LinkOutlined,
  SendOutlined,
  IeOutlined,
  BgColorsOutlined,
  DotChartOutlined,
  LoadingOutlined,
  UserOutlined,
  CloudUploadOutlined,
} from '@ant-design/icons-vue'
import {
  Attachments,
  Bubble,
  type BubbleListProps,
  type MessageStatus,
  Sender,
  ThoughtChain,
  type ThoughtChainItem,
  type ThoughtChainProps,
  useXAgent,
  useXChat,
} from 'ant-design-x-vue'
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import type { JSX } from 'vue/jsx-runtime'
import MD from '@/components/md/index.vue'
import Gap from '@/components/toolkit/Gap.vue'
import Report from '@/components/report/index.vue'
import { XStreamBody } from '@/utils/stream'
import { ScrollController } from '@/utils/scroll'
import { useAuthStore } from '@/store/AuthStore'
import { useMessageStore } from '@/store/MessageStore'
import { useConversationStore } from '@/store/ConversationStore'
import { useRoute, useRouter } from 'vue-router'
import { useConfigStore } from '@/store/ConfigStore'
import { parseJsonTextStrict } from '@/utils/jsonParser';
import type { NormalNode, SiteInformation } from '@/types/node';
import type { MessageState } from '@/types/message';
import service, { post } from '@/utils/request'

const router = useRouter()
const route = useRoute()
const conversationStore = useConversationStore()
// TODO 是否有更好的方式，发送消息之后才启动一个新的会话 
let convId = route.params.convId as string
if (!convId) {
  const { key } = conversationStore.newOne()
  router.push(`/chat/${key}`) 
}
const { useToken } = theme
const { token } = useToken()
const username = useAuthStore().token

// 定义消息列表角色配置
const roles: BubbleListProps['roles'] = {
  ai: {
    placement: 'start',
    avatar: {
        icon: <GlobalOutlined />,
        shape: 'square',
        style: { background: 'linear-gradient(to right, #f67ac4, #6b4dee)' },
      },
    style: {
      maxWidth: '100%',
    },
    rootClassName: 'ai',
  },
  local: {
    placement: 'end',
    shape: 'corner',
    avatar: {
        icon: <UserOutlined />,
        style: {},
      },
    rootClassName: 'local',
  }
}


const messageStore = useMessageStore()
const configStore = useConfigStore()

// 设置当前会话信息
messageStore.convId = convId
let current = messageStore.current
if (!current) {
  current = reactive({} as MessageState)
  if (convId) {
    messageStore.currentState[convId] = current
  }
}

// 处理人类反馈的请求
const sendResumeStream  =  async(message: string | undefined, onUpdate: (content: any) => void, onError: (error: any) => void): Promise<string> => {
    const xStreamBody = new XStreamBody('/chat/resume', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'text/event-stream',
          },
          body: {
            feedback_content: message,
            feedback: true,
            session_id: convId,
            thread_id: current.threadId,
          },
        })

        try {
          await xStreamBody.readStream((chunk: any) => {
            if (chunk) {
              messageStore.addReport(chunk)
            }
            onUpdate(chunk)            
          })
        } catch (e: any) {
          console.error('sendResumeStreamError', e)
          onError(e.statusText)
        }

     return xStreamBody.content()
}

// 处理发送消息的请求
const sendChatStream = async (message: string | undefined, onUpdate: (content: any) => void, onError: (error: any) => void): Promise<string> => {
  const xStreamBody = new XStreamBody('/chat/stream', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'text/event-stream',
          },
          body: {
            ...configStore.chatConfig,
            enable_deepresearch: current.deepResearch,
            query: message,
            session_id: convId
          },
        })

        try {
          await xStreamBody.readStream((chunk: any) => {
            if (chunk) {
              messageStore.addReport(chunk)
            }
            onUpdate(chunk)
            
          })
        } catch (e: any) {
          console.error('sendChatStream', e)
          onError(e.statusText)
        }
  return xStreamBody.content()
}

// 定义 agent
const senderLoading = ref(false)
const [agent] = useXAgent({
  request: async ({ message }, { onSuccess, onUpdate, onError }) => {
    senderLoading.value = true
    let content = ''
    switch (current.aiType) {
      case 'normal':
      case 'startDS': {
        content = await sendChatStream(message, onUpdate, onError)
        break
      }

      case 'onDS': {
        content = await (configStore.chatConfig.auto_accepted_plan ? sendChatStream(message, onUpdate, onError) : sendResumeStream(message, onUpdate, onError))
        break
      }

      default: {
        onError(new Error(`未知的 aiType: ${current.aiType}`))
        senderLoading.value = false
        return
      }
    }

    // 最后会返回本次stream的所有内容
    onSuccess(content)
    senderLoading.value = false
  },
})

const { onRequest, messages } = useXChat({
  agent: agent.value,
  requestPlaceholder: 'Waiting...',
  requestFallback: 'Failed return. Please try again later.',
})
// 定义发送消息的内容
const content = ref('')

const submitHandle = (nextContent: any) => {  
  current.aiType = 'normal'
  messageStore.nextAIType()
  
  // 自动接受，需要再转为下一个状态
  if(configStore.chatConfig.auto_accepted_plan){
    messageStore.nextAIType()
  }
  onRequest(nextContent)
  content.value = ''
  conversationStore.updateTitle(convId, nextContent)
}

const stopHandle = async () => {
  // 发送上传请求
    await post<string>('/chat/stop', {
      session_id: convId,
      thread_id: current.threadId,
    })

    message.success('停止成功')

}

// 开始研究
function startDeepResearch() {
  messageStore.nextAIType()
  onRequest('开始研究')
}

function openDeepResearch(threadId: string) {
  current.threadId = threadId
  current.deepResearchDetail = !current.deepResearchDetail
}

// 构建待处理节点的思考链
function buildPendingNodeThoughtChain(jsonArray: any[]) : any {
    const items: ThoughtChainProps['items'] = [
        {
          title: '请稍后...',
          icon: h(LoadingOutlined),
          status: 'pending'
        }
      ]
    return (
          <>
            <Card style={{ width: '500px', backgroundColor: '#EEF2F8' }}>
              {/* <h2>{{ msg }}</h2> */}
              <ThoughtChain items={items} />
            </Card>
          </>
        )
}
const collapsible = ref(['backgroundInvestigator'])
const onExpand = (keys: string[]) => {
  collapsible.value = keys;
};

// 构建开始研究的思考链
function buildStartDSThoughtChain(jsonArray: any[]) : any {
    const { Paragraph } = Typography
    // 获取背景调查节点
    const backgroundInvestigatorNodeArray = jsonArray.filter((item) => item.nodeName === 'background_investigator')
    if(backgroundInvestigatorNodeArray.length === 0){
      return buildPendingNodeThoughtChain(jsonArray)
    }
    const backgroundInvestigatorNode = backgroundInvestigatorNodeArray[0]
    const results = backgroundInvestigatorNode.siteInformation
    const markdownContent = results.map((result: any, index: number) => {
        return `${index + 1}. [${result.title}](${result.url})\n\n`
    }).join('\n')
    const items: ThoughtChainProps['items'] = [
      {
        status: 'error',
        title: '研究网站',
        icon: <IeOutlined />,
        key: 'backgroundInvestigator',
        extra: '',
        content: (
          <Typography>
            <Paragraph>
              <MD content={markdownContent} />
            </Paragraph>
          </Typography>
        ),
      },
      {
        status: 'success',
        title: '分析结果',
        icon: <DotChartOutlined />,
        extra: '',
      },

      {
        status: 'success',
        title: '生成报告',
        icon: <BgColorsOutlined />,
        description: <i>只需要几分钟就可以准备好</i>,
        footer: (
          <Flex style="margin-left: auto" gap="middle">
            { messageStore.isEnd(backgroundInvestigatorNode.graphId.thread_id) ? 
              <Button type="link" >已完成</Button> : <Button type="primary" onClick={startDeepResearch}>开始研究</Button>}
          </Flex>
        ),
        extra: '',
      },
    ]
    return (
      <>
        这是该主题的研究方案。如果你需要进行更新，请告诉我。
        <p/>
        <Card style={{ width: '500px', backgroundColor: '#EEF2F8' }}>
          <ThoughtChain items={items} collapsible={{ expandedKeys: collapsible.value, onExpand }} />
        </Card>
      </>
    )
}
// 构建正在分析结果的思考链
function buildOnDSThoughtChain(jsonArray: any[]) : any {
    const { Paragraph } = Typography
    // 获取背景调查节点
    const backgroundInvestigatorNodeArray = jsonArray.filter((item) => item.nodeName === 'background_investigator')
    if(backgroundInvestigatorNodeArray.length === 0){
      return buildPendingNodeThoughtChain(jsonArray)
    }
    const backgroundInvestigatorNode = backgroundInvestigatorNodeArray[0]
    const results: SiteInformation[] = backgroundInvestigatorNode.siteInformation
    const markdownContent = results.map((result: SiteInformation, index: number) => {
        return `${index + 1}. [${result.title}](${result.url})\n\n`
    }).join('\n')
    const items: ThoughtChainProps['items'] = [
      {
        status: 'error',
        title: '研究网站',
        icon: <IeOutlined />,
        key: 'backgroundInvestigator',
        extra: '',
        content: (
          <Typography>
            <Paragraph>
              <MD content={markdownContent} />
            </Paragraph>
          </Typography>
        ),
      },
      {
        status: 'pending',
        title: '正在分析结果',
        icon: <LoadingOutlined />,
        extra: '',
      }
    ]

    return (
      <>
        这是该主题的研究方案。正在分析结果中...
        <p/>
        <Card style={{ width: '500px', backgroundColor: '#EEF2F8' }}>
          {/* <h2>{{ msg }}</h2> */}
          <ThoughtChain items={items} collapsible={{ expandedKeys: collapsible.value, onExpand }} />
        </Card>
      </>
    )
}
// 构建分析完成的思考链
function buildEndDSThoughtChain(jsonArray: NormalNode[]): JSX.Element | undefined {
  const { Paragraph } = Typography
  const items: ThoughtChainProps['items'] = []
  // 获取背景调查节点
  const backgroundInvestigatorNode = jsonArray.filter((item) => item.nodeName === 'background_investigator')[0]
  if(backgroundInvestigatorNode && backgroundInvestigatorNode.siteInformation){
      const results = backgroundInvestigatorNode.siteInformation
      const markdownContent = results.map((result: any, index: number) => {
          return `${index + 1}. [${result.title}](${result.url})\n\n`
      }).join('\n')
      const item: ThoughtChainItem = {
          status: 'error',
          title: '研究网站',
          icon: <IeOutlined />,
          key: 'backgroundInvestigator',
          extra: '',
          content: (
            <Typography>
              <Paragraph>
                <MD content={markdownContent} />
              </Paragraph>
            </Typography>
          ),
      }
      items.push(item)
  }
  // 分析结果节点
  const startNode = jsonArray.filter((item) => item.nodeName === '__START__')[0]
  const humanFeedbackNode = jsonArray.filter((item) => item.nodeName === 'human_feedback')[0]
  if(startNode || humanFeedbackNode) {
    const threadId = startNode ? startNode.graphId.thread_id : humanFeedbackNode.graphId.thread_id
    const completeItem: ThoughtChainItem = {
        status: 'success',
        title: '分析结果',
        icon: <CheckCircleOutlined />,
        footer: (
            <Flex style="margin-left: auto" gap="middle">
            <Button type="primary" onClick={() => openDeepResearch(threadId)}>
              { current.deepResearchDetail && current.threadId === threadId ? '关闭' : '打开' }
            </Button>
            </Flex>
          ),
      }
      items.push(completeItem)
    }
  // 完成节点
  const endItem: ThoughtChainItem = {
          title: '完成',
          icon: h(CheckCircleOutlined),
          status: 'success'
        }
  
  items.push(endItem)
  return (
    <>
      这是该主题的研究方案已完成，可以点击下载报告
      <p/>
      <Card style={{ width: '500px', backgroundColor: '#EEF2F8' }}>
        <ThoughtChain items={items} collapsible={{expandedKeys: collapsible.value, onExpand}} />
      </Card>
    </>
  )
}

// 解析 status = loading 的消息
function parseLoadingMessage(msg: string): any{
  // 准备开始研究
  const jsonArray = parseJsonTextStrict(msg)
  if(!jsonArray || jsonArray.length === 0){
    return buildPendingNodeThoughtChain(jsonArray)
  }
  // 深度研究模式， 需要设置 threadId 并且 打开 report 面板
  const coordinatorNode = jsonArray.filter((item) => item.nodeName === 'coordinator')[0];
  if(coordinatorNode && coordinatorNode.content) {
    current.threadId = coordinatorNode.graphId.thread_id
    current.deepResearchDetail = true
  }
  const report = messageStore.report[jsonArray[0].graphId.thread_id]
  if(!report) {
    return buildPendingNodeThoughtChain(jsonArray)
  }
  // 如果已经有数，则渲染思维链
  const backgroundInvestigatorNode = report.filter((item) => item.nodeName === 'background_investigator')[0]
  if(backgroundInvestigatorNode) {
    return buildOnDSThoughtChain(report)
  }

  return buildPendingNodeThoughtChain(jsonArray)
}
function findNode(jsonArray: NormalNode[], nodeName: string): NormalNode | undefined {
  return jsonArray.filter((item) => item.nodeName === nodeName)[0]
}
// 解析 status = success 的消息
function parseSuccessMessage(msg: string) {
    // 解析完整数据
    const jsonArray: NormalNode[] = parseJsonTextStrict(msg)
    // 闲聊模式
    const coordinatorNode = findNode(jsonArray, 'coordinator')
    if(coordinatorNode && !coordinatorNode.content) {
      const endNode = findNode(jsonArray, '__END__')
      return endNode?.content.output
      }
    // 用户终止
    const endNode = findNode(jsonArray, '__END__')
    if(endNode && endNode.content.reason === '用户终止') {
      current.deepResearchDetail = false
      return endNode.content.reason
    }
    // 需要用用户反馈
    if(!endNode) {
      return buildStartDSThoughtChain(jsonArray)
    }
    // 人类恢复模式 或者 直接 end 模式
    const humanFeedbackNode = findNode(jsonArray, 'human_feedback')
    if(humanFeedbackNode || endNode) {
      return buildEndDSThoughtChain(jsonArray)
    }
}


// 解析消息记录 
// status === local 表示人类  loading表示stream流正在返回  success表示steram完成返回
// msg  当status === loading的时候，返回stream流的chunk  当status === success的时候，返回所有chunk的拼接字符串
function parseMessage(status: MessageStatus, msg: string): any {
  switch (status) {
    // 人类信息
    case 'local':
      return msg
    case 'loading':
      return parseLoadingMessage(msg)
    case 'success':
      return parseSuccessMessage(msg)
      case 'error':
        return msg
    default:
      return ''
  }
}
// TODO 分享、拷贝、更多操作
function parseFooter(status: MessageStatus): any {
  switch (status) {
    case 'success':
      return ''
    default:
      return ''
  }
}
// 初始化消息
if (convId) {
  const his_messages = messageStore.history[convId]
  if (his_messages) {
    messages.value = his_messages as any
  }
}
// 消息列表
const bubbleList = computed(() => {
  let isError = false
  for(const item of messages.value) {
    if(item.status === 'error') {
      isError = true
    }
  }
  // 避免异常，导致整个消息列表被覆盖
  if(isError) {
    return []
  }

  messageStore.history[convId] = messages.value
  return messages.value.map(({id, status, message}, idx) => {
      return {
        key: idx,
        role: status === 'local' ? 'local' : 'ai',
        content: parseMessage(status, message),
        footer: parseFooter(status),
      }
  })
})


const headerOpen = ref(false)
function handleFileChange({file, fileList}){
  console.log('handleFileChange', file)
  if(file.status === 'removed'){
    messageStore.removeUploadedFile(convId, file.uid)
    message.success('文件已删除')
    return
  }
  if(file.status === 'done'){
    const uploadedFile = {
      uid: file.uid,
      name: file.name,
      size: file.size,
      type: file.type,
      uploadTime: new Date().toISOString(),
      status: 'success' as const
    }
    messageStore.addUploadedFile(convId, uploadedFile)
    message.success(`${file.name} 上传成功`)
  }
}
// 文件上传前的验证
function beforeUpload(file: File) {
  // 检查文件类型
  const allowedTypes = ['application/pdf', 'text/plain', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/markdown']
  if (!allowedTypes.includes(file.type)) {
    message.error('只支持 PDF、TXT、DOC、DOCX、MD 格式的文件')
    return false
  }

  // 检查文件大小（限制为 10MB）
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    message.error('文件大小不能超过 10MB')
    return false
  }

  return true
}

// 处理文件上传
async function handleFileUpload(options: any) {
  const file = options.file as File

  try {
    // 构建表单数据
    const formData = new FormData()
    formData.append('files', file, file.name)
    formData.append('session_id', convId)

    // 发送上传请求
    const res = await service.post('/api/rag/user/batch-upload', formData, {
      timeout: 30000 // 30秒超时
    })
    options.onSuccess?.(res)
  } catch (error: any) {
    console.error('文件上传失败:', error)
    // 根据错误类型显示不同的错误信息
    let errorMessage = `${file.name} 上传失败`
    if (error.code === 'ECONNABORTED') {
      errorMessage += '：请求超时'
    } else if (error.response?.status === 413) {
      errorMessage += '：文件过大'
    } else if (error.response?.status === 415) {
      errorMessage += '：不支持的文件格式'
    } else if (error.response?.data?.message) {
      errorMessage += `：${error.response.data.message}`
    }
    message.error(errorMessage)
    options.onError?.(errorMessage)
  }
}

const headerNode = computed(() => {
  const filesList = messageStore.uploadedFiles?.[convId] || []
  return (

    <Sender.Header title="上传文件" open={headerOpen.value} onOpenChange={v => headerOpen.value = v}>
      <Attachments items={filesList} overflow="scrollX" onChange={handleFileChange} beforeUpload={beforeUpload} customRequest={handleFileUpload} placeholder={(type) => type === 'drop'
              ? {
                title: '点击或拖拽文件到这里上传',
              }
              : {
                icon: <CloudUploadOutlined />,
                title: '点击上传文件',
                description: '支持上传PDF、TXT、DOC、DOCX、MD等文件',
              }}/>
    </Sender.Header>

)});

const scrollContainer = ref<Element | any>(null)
const sc = new ScrollController()


onMounted(() => {
  sc.init(scrollContainer)
})


watch(
  () => messages.value,
  (o, n) => {
    sc.init(scrollContainer)
    sc.fresh()
  },
  { deep: true }
)
</script>

<style lang="less" scoped>
.__container_chat_index {
  width: 100%;
  height: 100%;
  box-sizing: border-box;


  .body {
    // padding: 20px;
    height: 100%;
    box-sizing: border-box;
  }

  .chat {
    padding-top: 20px;
    padding-bottom: 120px;
    height: 100%;
    box-sizing: border-box;
    position: relative;
    transition: width 0.3s ease, margin 0.3s ease, padding 0.3s ease;

    .bubble-list {
      width: 100%;
      overflow-y: auto;
      min-height: calc(100vh - 280px);
      max-height: calc(100vh - 280px);
    }

    :deep(.ant-card) {
      border-radius: 20px;
    }

    :deep(.ant-bubble-content-wrapper) {
      .bubble-footer {
        font-size: 18px;
        font-weight: bolder;
        padding-left: 16px;

        .toggle-bubble-footer {
          display: none !important;
        }
      }
    }

    :deep(.ant-bubble-content-wrapper:hover .bubble-footer .toggle-bubble-footer) {
      display: flex !important;
    }

    :deep(.ant-bubble) {
      &.ai .ant-bubble-content {
        padding-right: 40px;
        text-align: left;
        background: none !important;
        margin-top: -10px;
      }

      .ant-avatar {
        border-radius: 5px;
        border: none;
      }
    }

    :deep(.ant-sender-actions-btn) {
      box-shadow: none;
    }

    :deep(.ant-bubble-list) {
      max-width: 750px;
      width: 100%;
      overflow: hidden !important;
    }

    .sender-wrapper {
      position: absolute;
      bottom: 20px;
      left: 50%;
      transform: translateX(-50%);
      box-sizing: border-box;
      max-width: 750px;
      width: 100%;
      z-index: 10;

      .tag-deep-research {
        cursor: pointer;

        &unchecked {
          background: #fff;
        }
      }
    }

    .sender {
      border-radius: 18px;

      &:focus-within:after {
        border-width: 1px;
        border-color: white;
      }
    }

    .welcome {
      font-size: 32px;
      font-weight: 500;

      .gradient-text {
        background: linear-gradient(to right, #f67ac4, #6b4dee); /* 渐变背景 */
        -webkit-background-clip: text; /* 裁剪背景到文本 */
        -webkit-text-fill-color: transparent; /* 文本填充透明 */
        background-clip: text; /* 标准属性 */
      }
    }
  }


}
</style>
