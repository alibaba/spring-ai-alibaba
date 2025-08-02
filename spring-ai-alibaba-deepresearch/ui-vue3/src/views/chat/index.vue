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
      <Flex class="chat" vertical gap="middle" flex="1" align="center">
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
            :autoSize="{ minRows: 2, maxRows: 3 }"
            :loading="senderLoading"
            v-model:value="content"
            @submit="submitHandle"
            :actions="false"
            placeholder="type an issue"
          >
            <template #header>
              <a-carousel :slidesToShow="2" arrows style="width: 100%; padding: 12px">
                <a-tag style="left: 5px" :id="f.uid" :closable="true" v-for="f in uploadFileList">
                  <LinkOutlined />
                  {{ f.name }}
                </a-tag>
              </a-carousel>
            </template>
            <template
              #footer="{
                info: {
                  components: { SendButton, LoadingButton, ClearButton, SpeechButton },
                },
              }"
            >
              <Flex justify="space-between" align="center">
                <Flex align="center">
                  <a-upload
                    :multiple="true"
                    name="uploadFileList"
                    v-model:file-list="uploadFileList"
                    :showUploadList="false"
                  >
                    <a-button size="small" style="border-radius: 15px" type="text">
                      <LinkOutlined />
                    </a-button>
                  </a-upload>

                  <a-button
                    size="small"
                    style="border-radius: 15px; margin-right: 8px"
                    type="text"
                    @click="deepResearch"
                    :style="{ color: current.deepResearchDetail ? token.colorPrimary : '' }"
                  >
                    <BgColorsOutlined />
                    Report
                  </a-button>

                  <a-switch
                    un-checked-children="Deep Research"
                    checked-children="Deep Research"
                    v-model:checked="current.deepResearch"
                  ></a-switch>
                </Flex>
                <Flex>
                  <component :is="ClearButton" />
                  <component :is="SpeechButton" />
                  <component
                    :is="LoadingButton"
                    v-if="senderLoading"
                    type="default"
                    style="display: block"
                    :disabled="true"
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
      <Report :visible="current.deepResearchDetail" :convId="convId" @close="current.deepResearchDetail = false" />
      <!-- HTML 渲染组件弹窗 -->
      <a-modal
        v-model:open="htmlModalVisible"
        title="HTML 报告"
        :width="1200"
        :footer="null"
        :destroyOnClose="true"
        @cancel="closeHtmlModal"
      >
        <HtmlRenderer
          ref="htmlRendererRef"
          :htmlChunks="htmlChunks"
          :loading="htmlLoading"
          style="height: 600px;"
        />
      </a-modal>
    </Flex>
  </div>
</template>

<script setup lang="tsx">
import { Button, Card, Flex, Modal, Spin, theme, Typography } from 'ant-design-vue'
import {
  CheckCircleOutlined,
  CloseOutlined,
  CopyOutlined,
  GlobalOutlined,
  LinkOutlined,
  MoreOutlined,
  SendOutlined,
  IeOutlined,
  BgColorsOutlined,
  DotChartOutlined,
  ShareAltOutlined,
  LoadingOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import {
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
import HtmlRenderer from '@/components/html/index.vue'
import { XStreamBody } from '@/utils/stream'
import request from '@/utils/request'
import { ScrollController } from '@/utils/scroll'
import { useAuthStore } from '@/store/AuthStore'
import { useMessageStore } from '@/store/MessageStore'
import { useConversationStore } from '@/store/ConversationStore'
import { useRoute, useRouter } from 'vue-router'
import { useConfigStore } from '@/store/ConfigStore'
import { parseJsonTextStrict } from '@/utils/jsonParser';
import type { NormalNode } from '@/types/node';
import type { UploadFile } from '@/types/upload';

const router = useRouter()
const route = useRoute()
const conversationStore = useConversationStore()
// 会话ID
let convId = route.params.convId as string
if (!convId) {
  const { key } = conversationStore.newOne()
  router.push(`/chat/${key}`) 
}
const uploadFileList = ref<UploadFile[]>([])
const { useToken } = theme
const { token } = useToken()
const username = useAuthStore().token
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
messageStore.convId = convId
let current = messageStore.current
if (!current) {
  current = reactive({})
  if (convId) {
    messageStore.currentState[convId] = current
  }
}

const sendResumeStream  =  async(message: string | undefined, onUpdate: (content: any) => void, onError: (error: any) => void): Promise<string> => {
    const xStreamBody = new XStreamBody('/chat/resume', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'text/event-stream',
          },
          body: {
            feed_back_content: message,
            feed_back: true,
            thread_id: convId
          },
        })

        try {
          await xStreamBody.readStream((chunk: any) => {
            onUpdate(chunk)
          })
        } catch (e: any) {
          console.error(e.statusText)
          onError(e.statusText)
        }

     return xStreamBody.content()
}

const sendChatStream = async (message: string | undefined, onUpdate: (content: any) => void, onError: (error: any) => void): Promise<string> => {
  const xStreamBody = new XStreamBody('/chat/stream', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'text/event-stream',
          },
          body: {
            ...configStore.chatConfig,
            query: message,
            thread_id: convId
          },
        })

        try {
          await xStreamBody.readStream((chunk: any) => {
            onUpdate(chunk)
          })
        } catch (e: any) {
          console.error(e.statusText)
          onError(e.statusText)
        }
  return xStreamBody.content()
}

const [agent] = useXAgent({
  request: async ({ message }, { onSuccess, onUpdate, onError }) => {
    senderLoading.value = true

    if (!current.deepResearch) {
      senderLoading.value = false
      onSuccess(`暂未实现: ${message}`)
      return
    }

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

      case 'onDS': {
        const xStreamBody = new XStreamBody('/chat/resume', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'text/event-stream',
          },
          body: {
            ...configStore.chatConfig,
            query: message,
            feed_back: false,
          },
        })

        try {
          await xStreamBody.readStream((chunk: any) => {
            onUpdate(chunk)
          })
        } catch (e: any) {
          console.error(e.statusText)
          onError(e.statusText)
        }

        content = xStreamBody.content()
        break
      }

      case 'endDS': {
        const xStreamBody = new XStreamBody('/chat/resume', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Accept: 'text/event-stream',
          },
          body: {
            ...configStore.chatConfig,
            query: message,
            feed_back: true,
          },
        })

        try {
          await xStreamBody.readStream((chunk: any) => {
            onUpdate(chunk)
          })
        } catch (e: any) {
          console.error(e.statusText)
          onError(e.statusText)
        }

        content = xStreamBody.content()
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
if (convId) {
  const his_messages = messageStore.history[convId]
  console.log('his_messages', his_messages)
  if (his_messages) {
    messages.value = [...his_messages]
  }
}

const content = ref('')
const senderLoading = ref(false)

// HTML 渲染组件相关状态
const htmlModalVisible = ref(false)
const htmlChunks = ref<string[]>([])
const htmlLoading = ref(false)
const htmlRendererRef = ref(null)


const submitHandle = (nextContent: any) => {  
  current.aiType = 'normal'
  // 如果是深度研究，需要切换到下一个aiType
  if (current.deepResearch) {
      messageStore.nextAIType()
      current.deepResearchDetail = true
  }
  // 自动接受，需要再转为下一个状态
  if(configStore.chatConfig.auto_accepted_plan){
    messageStore.nextAIType()
  }
  onRequest(nextContent)
  content.value = ''
  conversationStore.updateTitle(convId, nextContent)
}

// 开始研究
function startDeepResearch() {
  messageStore.nextAIType()
  onRequest('开始研究')
}

// 展示HTML报告
async function htmlDeepResearch(){
    // 先打开弹窗
    htmlModalVisible.value = true
    htmlLoading.value = true
    htmlChunks.value = []
    
    
    if(messageStore.htmlReport[convId]){
      htmlChunks.value = messageStore.htmlReport[convId]
      htmlLoading.value = false
      return
    }

    const xStreamBody = new XStreamBody('/api/reports/build-html?threadId=' + convId, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'text/event-stream',
        }})
    let success = true
    try {
        await xStreamBody.readStream((chunk: any) => {
            // 将接收到的HTML片段添加到数组中
            const chunkNode = JSON.parse(chunk)
            htmlChunks.value.push(chunkNode.result.output.text)
        })
        htmlLoading.value = false
    } catch (e: any) {
        console.error(e.statusText)
        htmlLoading.value = false
        // 如果出错，可以显示错误信息
        htmlChunks.value = [`<div style="color: red; padding: 20px;">加载HTML报告时出错: ${e.statusText}</div>`]
        success = false
    }
    // 缓存html报告
    if(success) {
        messageStore.htmlReport[convId] = htmlChunks.value
    }
    
}

// 关闭HTML模态框
function closeHtmlModal() {
    htmlModalVisible.value = false
    htmlChunks.value = []
    htmlLoading.value = false
}

// 下载报告
function downDeepResearch(){
  request({
    url: '/api/reports/export',
    method: 'POST',
    data: {
      thread_id: convId,
      format: 'pdf'
    }
  }).then((response: any) => {
    if(response.status === 'success') {
      window.open(import.meta.env.VITE_BASE_URL + response.report_information.download_url, '_blank')
    }
  })
}

function deepResearch() {
  current.deepResearchDetail = !current.deepResearchDetail
}

function buildPendingNodeThoughtChain() : any {
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

let tempJsonArray: any[] = []
function buildStartDSThoughtChain(jsonArray: any[]) : any {
    // 重置数组
    if(tempJsonArray.length > 0) {
      tempJsonArray = []
    }
    const { Paragraph, Text } = Typography
    // 获取背景调查节点
    const backgroundInvestigatorNode = jsonArray.filter((item) => item.nodeName === 'background_investigator')[0]
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
            <Button type="primary">修改方案</Button>
            <Button type="primary" onClick={startDeepResearch}>开始研究</Button>
          </Flex>
        ),
        extra: '',
      },
    ]
    tempJsonArray = jsonArray
    return (
      <>
        这是该主题的研究方案。如果你需要进行更新，请告诉我。
        <Card style={{ width: '500px', backgroundColor: '#EEF2F8' }}>
          {/* <h2>{{ msg }}</h2> */}
          <ThoughtChain items={items} collapsible={{ expandedKeys: ['backgroundInvestigator'] }} />
        </Card>
      </>
    )
}

function buildOnDSThoughtChain() : any {
    if(tempJsonArray.length === 0){
      return
    }
    const { Paragraph, Text } = Typography
    // 获取背景调查节点
    const backgroundInvestigatorNode = tempJsonArray.filter((item) => item.nodeName === 'background_investigator')[0]
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
        status: 'pending',
        title: '正在分析结果',
        icon: <LoadingOutlined />,
        extra: '',
      }
    ]

    return (
      <>
        这是该主题的研究方案。正在分析结果中...
        <Card style={{ width: '500px', backgroundColor: '#EEF2F8' }}>
          {/* <h2>{{ msg }}</h2> */}
          <ThoughtChain items={items} collapsible={{ expandedKeys: ['backgroundInvestigator'] }} />
        </Card>
      </>
    )
}

function buildEndDSThoughtChain(jsonArray: any[]): JSX.Element | undefined {
  if(tempJsonArray.length === 0 && jsonArray.length === 0){
    return undefined
  }
  const curJsonArray = tempJsonArray.length > 0 ? tempJsonArray : jsonArray
  const { Paragraph, Text } = Typography
  const items: ThoughtChainProps['items'] = []
  let collapsible = { }
  // 获取背景调查节点
  const backgroundInvestigatorNode = curJsonArray.filter((item) => item.nodeName === 'background_investigator')[0]
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
      collapsible = { expandedKeys: ['backgroundInvestigator'] }
  }
  const completeItem: ThoughtChainItem = {
      status: 'success',
      title: '分析结果',
      icon: <CheckCircleOutlined />,
      footer: (
          <Flex style="margin-left: auto" gap="middle">
            <Button type="primary" onClick={downDeepResearch}>下载报告</Button>
            <Button type="primary" onClick={htmlDeepResearch}>在线报告</Button>
          </Flex>
        ),
    }
  items.push(completeItem)
  const endItem: ThoughtChainItem = {
          title: '完成',
          icon: h(CheckCircleOutlined),
          status: 'success'
        }
  
  items.push(endItem)
  return (
    <>
      这是该主题的研究方案已完成，可以点击下载报告
      <Card style={{ width: '500px', backgroundColor: '#EEF2F8' }}>
        {/* <h2>{{ msg }}</h2> */}
        <ThoughtChain items={items} collapsible={collapsible} />
      </Card>
    </>
  )
}

function parseLoadingMessage(): any{
    if(current.deepResearch){
    // 准备开始研究
    if(current.aiType === 'startDS') {
      return buildPendingNodeThoughtChain()
    }
    if(current.aiType === 'onDS' && configStore.chatConfig.auto_accepted_plan) {
      return buildPendingNodeThoughtChain()
    }
    // 正在研究中
    if(current.aiType === 'onDS') {
      return  buildOnDSThoughtChain()
    }
  }
  return buildPendingNodeThoughtChain()
}

function parseSuccessMessage(msg: any, isCurrent: boolean) {
    // 解析完整数据
    const jsonArray: NormalNode[] = parseJsonTextStrict(msg)
    // 历史数据渲染
    if (current.deepResearch && !isCurrent) {
      // 研究网站、分析结果、生成报告
      return <MD content={ '进行下一步处理' } />
    }
    
    if (current.deepResearch && isCurrent) {
      // 不启用研究模式，闲聊模式
      if(jsonArray.filter((item) => item.nodeName === 'coordinator').length > 0) {
        const coordinatorNode = jsonArray.filter((item) => item.nodeName === 'coordinator')[0];
        if(!coordinatorNode.content) {
          return (jsonArray.filter((item) => item.nodeName === '__END__')[0].content as any).output
        }
      }
      if (current.aiType === 'startDS') {
        // 如果不包含背景调查，则提示用户重新输入
        if(jsonArray.filter((item) => item.nodeName === 'background_investigator').length === 0) {
          return <MD content={'未进行背景调查，请重新输入话题进行研究'} />
        }
        return buildStartDSThoughtChain(jsonArray)
      }
      // 研究完成，TODO 这里应该流为endDS状态
      if (current.aiType === 'onDS') {
        return buildEndDSThoughtChain(jsonArray)
      }
    }
}

function getTargetNode(jsonArray: NormalNode[]): NormalNode | undefined {
  // TODO: 实现获取目标节点的逻辑
  return undefined
}

// 解析消息记录 
// status === local 表示人类  loading表示stream流正在返回  success表示steram完成返回
// msg  当status === loading的时候，返回stream流的chunk  当status === success的时候，返回所有chunk的拼接字符串
// isCurrent  true表示当前消息是最新的，false表示历史消息
function parseMessage(status: MessageStatus, msg: any, isCurrent: boolean): any {
  switch (status) {
    // 人类信息
    case 'local':
      return msg
    case 'loading':
      return parseLoadingMessage()
    case 'success':
      return parseSuccessMessage(msg, isCurrent)
      case 'error':
        return msg
    default:
      return ''
  }
}

function parseFooter(status: MessageStatus, isCurrent: boolean): any {
  switch (status) {
    case 'success':
      // return (
      //   <div class="bubble-footer">
      //     <Flex gap="middle" class={isCurrent ? '' : 'toggle-bubble-footer'}>
      //       <CopyOutlined />
      //       <ShareAltOutlined />
      //       <MoreOutlined />
      //     </Flex>
      //   </div>
      // )
      return ''
    default:
      return ''
  }
}

const bubbleList = computed(() => {
  const len = messages.value.length
  messageStore.history[convId] = messages.value
  //  当状态是loading的时候，是每个chunk，然后succes，把之前所有的chunk 全部返回
  const list =  messages.value.map(({ id, message, status }, idx) => ({
    key: id,
    role: status === 'local' ? 'local' : 'ai',
    content: parseMessage(status, message, idx === len - 1),
    footer: parseFooter(status, idx === len - 1),
  }))
  return list;
})

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
    padding: 20px;
    height: 100%;
    box-sizing: border-box;
  }



  .chat {
    padding-top: 20px;
    height: 100%;
    box-sizing: border-box;

    .bubble-list {
      width: 100%;
      overflow-y: auto;
      min-height: calc(100vh - 280px);
      max-height: calc(100vh - 280px);
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
      box-sizing: border-box;
      max-width: 750px;
      width: 100%;

      .tag-deep-research {
        cursor: pointer;

        &checked {
        }

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
