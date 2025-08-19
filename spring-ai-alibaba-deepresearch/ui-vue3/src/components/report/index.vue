<template>
  <transition name="fade" mode="out-in">
    <Flex class="aux" v-if="visible" style="width: 60%" vertical>
      <a-card style="height: 100%">
          <template #title>
            <Flex justify="space-between" align="center">
              <span style="font-weight: 500;">
                {{ endFlag ? '报告' : '思考过程' }}
              </span>
              <Flex gap="small" align="center" v-if="endFlag">
                <Button type="text" size="small" @click="handleOnlineReport">
                  <GlobalOutlined />
                  在线报告
                </Button>
                <Button type="text" size="small" @click="handleDownloadReport">
                  <DownloadOutlined />
                  下载报告
                </Button>
                <Button type="text" size="small" @click="handleClose">
                  <CloseOutlined />
                </Button>
              </Flex>
            </Flex>
          </template>
        <!-- 思考过程 -->
        <div class="message-list" v-if="items && items.length > 0 && !endFlag">
          <ThoughtChain
            :items="items"
            collapsible
          />
        </div>
        <!-- END 节点返回之后显示的内容 -->
        <div v-else-if="endFlag" class="end-content">
            <MD :content="endContent" />
            <!-- 思考过程 -->
            <a-collapse :bordered="false" class="thought-collapse">
              <a-collapse-panel header="参考来源" key="1" class="thought-panel">
                <ReferenceSources :sources="sources" />
              </a-collapse-panel>
              <a-collapse-panel header="思路" key="2" class="thought-panel">
                <ThoughtChain
                  :items="items"
                  collapsible
                />
              </a-collapse-panel>
            </a-collapse>
        </div>
        
        <!-- 无消息记录时的提示 -->
        <div v-else class="no-messages">
          暂无消息记录
        </div>
      </a-card>
      
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
  </transition>
</template>

<script setup lang="ts">
import { Flex, Button, Modal } from 'ant-design-vue'
import { CloseOutlined, LoadingOutlined, CheckCircleOutlined, GlobalOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import { parseJsonTextStrict } from '@/utils/jsonParser';
import { useMessageStore } from '@/store/MessageStore'
import { computed, h, watch, onUnmounted, onMounted, ref } from 'vue'
import { ThoughtChain, type ThoughtChainProps, type ThoughtChainItem } from 'ant-design-x-vue';
import MD from '@/components/md/index.vue'
import HtmlRenderer from '@/components/html/index.vue'
import ReferenceSources from '@/components/reference-sources/index.vue'
import { XStreamBody } from '@/utils/stream'
import request from '@/utils/request'
import type { NormalNode } from '@/types/node';

const messageStore = useMessageStore()

interface Props {
  visible?: boolean
  convId: string
  threadId: string
}

interface Emits {
  (e: 'close'): void
  (e: 'onlineReport'): void
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  convId: '',
  threadId: ''
})

// HTML 渲染组件相关状态
const htmlModalVisible = ref(false)
const htmlChunks = ref<string[]>([])
const htmlLoading = ref(false)
const htmlRendererRef = ref(null)

const endFlag = ref(false)
const endContent = ref('')
const sources = ref([])

const arrayTemp: ThoughtChainProps['items'] = []
// 用于缓存llm_stream节点的内容
const llmStreamCache = new Map<string, { item: ThoughtChainItem, content: string }>()
// 从messageStore 拿出消息，然后进行解析并且渲染
const items = computed(() => {
    // 思维链显示的列表
    const array: ThoughtChainProps['items'] = []
    if(!props.threadId || !messageStore.report[props.threadId]){
      return array
    }
    // TODO 性能问题？
    const messages = messageStore.report[props.threadId]
    arrayTemp.length = 0
    llmStreamCache.clear()
    endFlag.value = false
    endContent.value = ''
    sources.value = []
    // 遍历messages 用于渲染思维链
    messages.forEach(node => {
      if(node.nodeName) {
        processJsonNodeLogic(node)
      }else{
        processLlmStreamNodeLogic(node)
      }
    })

    array.push(...arrayTemp)
    return array
})

// 处理json节点
const processJsonNodeLogic = (node: NormalNode) => {
    // 渲染普通节点
    processJsonNode(node)
    // 普通节点处理完后，添加pending节点
    appendPendingNode()
    // information 或者 end 节点 说明等待用户反馈 或者 结束
    if(node.nodeName === 'planner' || node.nodeName === '__END__'){
      removeLastPendingNode()
    }
}

// 处理llm_stream节点
const processLlmStreamNodeLogic = (node: any) => {
  if(!node.visible) {
    return
  }
  let item: ThoughtChainItem | undefined
  // llm_stream 形式的节点，需要流式渲染
  // 动态遍历node对象的key，只要包含'llm_stream'就执行相应处理
  const llmStreamKeys = Object.keys(node).filter(key => key.includes('llm_stream'))

  for (const key of llmStreamKeys) {
    // 流式节点：移除pending节点，完成之前的流式节点
    removeLastPendingNode()
    const k = node.graphId.thread_id + '-' + key
    item = processLlmStreamNode(node, key, k)
  }
  if(item) {
    // 检查是否已经存在相同的item（针对llm_stream节点）
    const existingIndex = arrayTemp.findIndex(existingItem => existingItem === item)
    if(existingIndex === -1) {
      arrayTemp.push(item)
    }
  }
}

// 移除所有pending状态的节点
const removeLastPendingNode = () => {
  for (let i = arrayTemp.length - 1; i >= 0; i--) {
    if (arrayTemp[i].status === 'pending' && arrayTemp[i].title === '思考中') {
      arrayTemp.splice(i, 1)
    }
  }
}
const appendPendingNode = () => {
  // 检查是否已存在pending节点
  removeLastPendingNode()

  // 如果不存在pending节点，创建一个新的
  const pendingItem: ThoughtChainItem = {
      title: '思考中',
      description: '正在等待思考结果',
      icon: h(LoadingOutlined),
      status: 'pending'
    }
    arrayTemp.push(pendingItem)
}

const processLlmStreamNode = (node: any, key: string, cacheKey: string): ThoughtChainItem => {
  // 检查缓存中是否已存在该节点
  if (llmStreamCache.has(cacheKey)) {
    const cached = llmStreamCache.get(cacheKey)!
    // 累积新的内容
    cached.content += node[key]
    // 更新MD组件的内容
    cached.item.content = h(MD, { content: cached.content })
    // 结束组件
    if(node.finishReason === 'STOP') {
      cached.item.status = 'success'
      cached.item.icon = h(CheckCircleOutlined)
      cached.item.description = '内容生成完成'
    }

    return cached.item
  } else {
    // 创建新的ThoughtChainItem
    const initialContent =  node[key]
    const item: ThoughtChainItem = {
      key: key,
      title: node.step_title ? node.step_title : key,
      description: '正在生成内容',
      icon: h(LoadingOutlined),
      status: 'pending',
      content: h(MD, { content: initialContent })
    }

    // 缓存该节点
    llmStreamCache.set(cacheKey, {
      item,
      content: initialContent
    })

    return item
  }
}
//  渲染普通节点
const processJsonNode = (node: any) => {
    let title = ''
    let description = ''
    let content = null

    // 根据不同节点类型处理
    switch(node.nodeName) {
      case '__START__':
        title = node.displayTitle
        description = node.content.query
        break
      case 'coordinator':
        title = node.displayTitle
        description = '当前是否启动研究模式:' + node.content
        break

      case 'rewrite_multi_query':
        title = node.displayTitle
        description = '优化查询以获得更好的搜索结果'
        if(node.content?.optimize_queries && Array.isArray(node.content.optimize_queries)) {
          const queries = node.content.optimize_queries
          const markdownContent = queries.map((query: any, index: number) => `${index + 1}. ${query}`).join('\n')
          content = h(MD, { content: markdownContent })
        }
        break

      case 'background_investigator':
        title = node.displayTitle
        description = '正在收集和分析背景信息'
        if(node.siteInformation && Array.isArray(node.siteInformation)) {
          content = h(ReferenceSources, { sources: node.siteInformation })
          sources.value = node.siteInformation
        }
        break

      case 'planner':
        title = node.displayTitle
        const json = JSON.parse(node.content)
        description = json.title
        const stepsContent= json.steps.map((step: any, index: number) => {
          const { title, description} = step
          return `### ${index + 1}. ${title}\n\n${description}\n\n---\n`
        }).join('\n')
        content = h(MD, { content: stepsContent })
        break

      case 'human_feedback':
        title = node.displayTitle
        description = '开始研究'
        break


      case 'reporter':
        title = node.displayTitle
        description = '生成最终研究报告'
        if(node.content) {
          content = h(MD, { content: node.content })
          endContent.value = node.content
        }
        break

      case '__END__':
        title = node.displayTitle
        description = '研究完成'
        endFlag.value = true
        break

      default:
        console.log('default', node)
        return
    }
    const item: ThoughtChainItem = {
        title,
        description,
        icon: h(CheckCircleOutlined),
        status: 'success',
    }

    if(content) {
      item.content = content
    }
    arrayTemp.push(item)
}
onMounted(() => {
  llmStreamCache.clear()
  arrayTemp.length = 0
})
// 监听convId变化，清理缓存
watch(() => props.threadId, () => {
  llmStreamCache.clear()
  arrayTemp.length = 0
})

// 组件卸载时清理缓存
onUnmounted(() => {
  llmStreamCache.clear()
  arrayTemp.length = 0
})

const emit = defineEmits<Emits>()

const handleClose = () => {
  emit('close')
}

// 展示HTML报告
const handleOnlineReport = async () => {
  // 先打开弹窗
  htmlModalVisible.value = true
  htmlLoading.value = true
  htmlChunks.value = []
  
  if(messageStore.htmlReport[props.convId]){
    htmlChunks.value = messageStore.htmlReport[props.convId]
    htmlLoading.value = false
    return
  }

  const xStreamBody = new XStreamBody('/api/reports/build-html?threadId=' + props.threadId, {
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
      messageStore.htmlReport[props.convId] = htmlChunks.value
  }
}

// 关闭HTML模态框
const closeHtmlModal = () => {
  htmlModalVisible.value = false
  htmlChunks.value = []
  htmlLoading.value = false
}

const handleDownloadReport = () => {
  request({
    url: '/api/reports/export',
    method: 'POST',
    data: {
      thread_id: props.threadId,
      format: 'pdf'
    }
  }).then((response: any) => {
    if(response.status === 'success') {
      window.open(import.meta.env.VITE_BASE_URL + response.report_information.download_url, '_blank')
    }
  })
}

</script>

<style lang="less" scoped>
.aux {
  padding-top: 20px;
  height: 100%;
  // padding-bottom: 38px;
}

:deep(.ant-card) {
  border-radius: 20px;
}

.message-list {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  padding: 16px 0;
}

.end-content {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  padding: 16px;
  line-height: 1.6;
  
  :deep(img) {
    max-width: 100%;
    height: auto;
  }
  
  :deep(pre) {
    overflow-x: auto;
    white-space: pre-wrap;
    word-wrap: break-word;
  }
}

// 思考过程折叠面板样式
.thought-collapse {
  margin-top: 24px;
  background: transparent;
  
  :deep(.ant-collapse-item) {
    border: none;
    background: linear-gradient(135deg, #f6f8ff 0%, #f0f4ff 100%);
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.08);
    margin-bottom: 8px;
    overflow: hidden;
    transition: all 0.3s ease;
    
    &:hover {
      box-shadow: 0 4px 16px rgba(24, 144, 255, 0.12);
      transform: translateY(-1px);
    }
  }
  
  :deep(.ant-collapse-header) {
    padding: 16px 20px;
    background: transparent;
    border: none;
    font-weight: 600;
    font-size: 15px;
    color: #1890ff;
    transition: all 0.3s ease;
    
    &:hover {
      color: #40a9ff;
    }
    
    .ant-collapse-arrow {
      color: #1890ff;
      font-size: 14px;
      transition: all 0.3s ease;
    }
  }
  
  :deep(.ant-collapse-content) {
    border: none;
    background: transparent;
    
    .ant-collapse-content-box {
      padding: 0 20px 20px 20px;
    }
  }
  
  :deep(.ant-collapse-item-active) {
    .ant-collapse-header {
      border-bottom: 1px solid rgba(24, 144, 255, 0.1);
    }
  }
}

.message-item {
  margin-bottom: 16px;
  padding: 12px;
  border-radius: 8px;
  background-color: #f8f9fa;

  .message-role {
    font-weight: bold;
    color: #1890ff;
    margin-bottom: 8px;
    font-size: 14px;
  }

  .message-content {
    margin-bottom: 8px;
    line-height: 1.6;
  }

  .message-time {
    font-size: 12px;
    color: #999;
    text-align: right;
  }
}

.no-messages {
  text-align: center;
  color: #999;
  padding: 40px 0;
  font-size: 16px;
}

// 淡入淡出过渡动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
