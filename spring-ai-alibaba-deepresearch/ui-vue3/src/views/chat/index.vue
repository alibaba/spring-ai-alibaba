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
          <Bubble.List style="min-height: 100%" :roles="roles" :items="bubbleList"> </Bubble.List>
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
      
    </Flex>
  </div>
</template>

<script setup lang="tsx">
import { Button, Card, Flex, Spin, theme, Typography } from 'ant-design-vue'
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

const router = useRouter()
const route = useRoute()
// 会话ID
const convId = route.params.convId as string

const uploadFileList = ref([])
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
  },
}

const conversationStore = useConversationStore()
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
      case 'normal': {
        const xStreamBody = new XStreamBody('/deep-research/chat/stream', {
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

        content = xStreamBody.content()
        break
      }

      case 'startDS': {
        current.deepResearchDetail = true

        if (current.autoAccepted) {
          content = 'startDS阶段，已自动接受，跳过调用'
          break
        }

        const xStreamBody = new XStreamBody('/deep-research/chat/resume', {
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

        content = xStreamBody.content()
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

    if (current.deepResearch) {
      messageStore.nextAIType()
    }

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
  if (his_messages) {
    messages.value = [...his_messages]
  }
}

const content = ref('')
const senderLoading = ref(false)

const submitHandle = (nextContent: any) => {
  current.aiType = 'normal'
  onRequest(nextContent)
  content.value = ''
  if (!convId) {
    const { key } = conversationStore.newOne()
    router.push(`/chat/${key}`)
  }
}

function startDeepResearch() {
  // messageStore.startDeepResearch()
  onRequest('开始研究')
}

function deepResearch() {
  current.deepResearchDetail = !current.deepResearchDetail
}

function parseMessage(status: MessageStatus, msg: any, isCurrent: boolean): any {
  switch (status) {
    case 'loading': 
      const items: ThoughtChainProps['items'] = [
        {
          title: '正在思考中',
          description: 'AI 正在思考中',
          icon: h(LoadingOutlined),
          status: 'pending'
        }
      ]
      return (
            <>
              请稍后，AI 正在思考中...
              <Card style={{ width: '500px', backgroundColor: '#EEF2F8' }}>
                <h2>{{ msg }}</h2>
                <ThoughtChain items={items} />
              </Card>
            </>
          )
    case 'success':
      if (!isCurrent) {
        // todo 历史数据渲染
        return <MD content={msg} />
      }
      if (current.deepResearch) {
        if (current.aiType === 'startDS') {
          const { Paragraph, Text } = Typography

          const jsonArray = parseJsonTextStrict(msg)
          if(jsonArray.filter((item) => item.node === 'information').length === 0) {
            return <MD content={msg} />
          }
          const informationNode= jsonArray.filter((item) => item.node === 'information')[0]
          const items: ThoughtChainProps['items'] = [
            {
              status: 'error',
              title: '查询重写',
              icon: <IeOutlined />,
              extra: '',
              content: (
                <Typography>
                  <Paragraph>
                    <MD content={informationNode.data?.optimize_queries} />
                  </Paragraph>
                </Typography>
              ),
            },
            {
              status: 'success',
              title: '研究计划',
              icon: <DotChartOutlined />,
              extra: '',
              content: (
                <Typography>
                  <Paragraph>
                    <MD content={informationNode.data?.current_plan.thought} />
                  </Paragraph>
                </Typography>
              )
            },

            {
              status: 'success',
              title: '研究报告',
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

          return (
            <>
              这是该主题的研究方案。如果你需要进行更新，请告诉我。
              <Card style={{ width: '500px', backgroundColor: '#EEF2F8' }}>
                <h2>{{ msg }}</h2>
                <ThoughtChain items={items} />
              </Card>
            </>
          )
        }
        if (current.aiType === 'onDS') {
          return (
            <div>
              <Button type="primary" onClick={deepResearch}>
                正在研究
              </Button>
            </div>
          )
        }
      }
    default:
      return ''
  }
}

function parseFooter(status: MessageStatus, isCurrent: boolean): any {
  switch (status) {
    case 'success':
      return (
        <div class="bubble-footer">
          <Flex gap="middle" class={isCurrent ? '' : 'toggle-bubble-footer'}>
            <CopyOutlined />
            <ShareAltOutlined />
            <MoreOutlined />
          </Flex>
        </div>
      )
    default:
      return ''
  }
}

const bubbleList = computed(() => {
  const len = messages.value.length
  messageStore.history[convId] = messages.value
  // TODO 当状态是loading的时候，是每个chunk，然后succes，把之前所有的chunk 全部返回
  return messages.value.map(({ id, message, status }, idx) => ({
    key: id,
    role: status === 'local' ? 'local' : 'ai',
    content: parseMessage(status, message, idx === len - 1),
    footer: parseFooter(status, idx === len - 1),
  }))
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
