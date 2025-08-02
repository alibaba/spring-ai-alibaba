<template>
  <Flex class="aux" v-if="visible" style="width: 60%" vertical>
    <a-card style="height: 100%">
      <template #title>
        <Flex justify="space-between">
          研究细节
          <Button type="text" @click="handleClose">
            <CloseOutlined />
          </Button>
        </Flex>
      </template>
      <div class="message-list" v-if="items && items.length > 0">
        <ThoughtChain
          :items="items"
          collapsible
        />
      </div>
      <div v-else class="no-messages">
        暂无消息记录
      </div>
    </a-card>
  </Flex>
</template>

<script setup lang="ts">
import { Flex, Button } from 'ant-design-vue'
import { CloseOutlined, LoadingOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import { parseJsonTextStrict } from '@/utils/jsonParser';
import { useMessageStore } from '@/store/MessageStore'
import { computed, h, watch, onUnmounted, ref } from 'vue'
import { ThoughtChain, type ThoughtChainProps, type ThoughtChainItem } from 'ant-design-x-vue';
import MD from '@/components/md/index.vue'

const messageStore = useMessageStore()

interface Props {
  visible?: boolean
  convId: string
}

interface Emits {
  (e: 'close'): void
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  convId: ''
})

const arrayTemp: ThoughtChainProps['items'] = []
// 用于控制历史记录的显示
let isLoading = false
// 用于缓存llm_stream节点的内容
const llmStreamCache = new Map<string, { item: ThoughtChainItem, content: string }>()
// 从messageStore 拿出消息，然后进行解析并且渲染
const items = computed(() => {
    // 思维链显示的列表
    const array: ThoughtChainProps['items'] = []
    if(!props.convId || !messageStore.history[props.convId]){
      return array
    }
    // 过滤出非人类的消息
    const messages = messageStore.history[props.convId].filter(item => item.status != 'local')
    // 遍历messages 用于渲染思维链
    messages.forEach(msg => {
      // 单个chunk
      // xchat组件的第一个chunk是 Waiting... 所以需要跳过
      if(msg.status === 'loading' && msg.message != 'Waiting...') {
           isLoading = true
           const node = JSON.parse(msg.message)
           if(node.nodeName) {
              processJsonNodeLogic(node)
           }else{
             processLlmStreamNodeLogic(node)
           }
      }
      //  完整的text， 历史记录的渲染
      //  当stream完成，xchat还会返回一次success，为避免思维链重复渲染，如果是loading状态，则不在重复增加节点
      if(msg.status === 'success' && !isLoading) {
          isLoading = false
          const jsonArray = parseJsonTextStrict(msg.message)
          jsonArray.forEach(node => {
            if(node.nodeName) {
              processJsonNodeLogic(node)
            }else{
              processLlmStreamNodeLogic(node)
            }
          })

      }
    })

    array.push(...arrayTemp)
    return array
})

// 处理json节点
const processJsonNodeLogic = (node: any) => {
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
    item = processLlmStreamNode(node, key)
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
    if (arrayTemp[i].status === 'pending' && arrayTemp[i].title === '【处理中】正在请求后端内容') {
      arrayTemp.splice(i, 1)
    }
  }
}
const appendPendingNode = () => {
  // 检查是否已存在pending节点
  removeLastPendingNode()

  // 如果不存在pending节点，创建一个新的
  const pendingItem: ThoughtChainItem = {
      title: '【处理中】正在请求后端内容',
      description: '正在向后端发送请求并等待响应',
      icon: h(LoadingOutlined),
      status: 'pending'
    }
    arrayTemp.push(pendingItem)
}

const processLlmStreamNode = (node: any, key: string): ThoughtChainItem => {
  // 检查缓存中是否已存在该节点
  if (llmStreamCache.has(key)) {
    const cached = llmStreamCache.get(key)!
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
    llmStreamCache.set(key, {
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
        description = node.content
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
          const results = node.siteInformation
          const markdownContent = results.map((result: any, index: number) => {
            const { title, url, content, icon, weight } = result
            return `### ${index + 1}. [${title}](${url})\n\n**权重:** ${weight}\n\n**内容摘要:** ${content}\n\n**来源:** ![favicon](${icon}) [${url}](${url})\n\n---\n`
          }).join('\n')
          content = h(MD, { content: markdownContent })
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
        }
        break

      case '__END__':
        title = node.displayTitle
        description = '研究完成'
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

// 监听convId变化，清理缓存
watch(() => props.convId, () => {
  llmStreamCache.clear()
})

// 组件卸载时清理缓存
onUnmounted(() => {
  llmStreamCache.clear()
})

const emit = defineEmits<Emits>()

const handleClose = () => {
  emit('close')
}

const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleString()
}
</script>

<style lang="less" scoped>
.aux {
  padding-top: 20px;
  height: 100%;
  padding-bottom: 38px;
}

.message-list {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  padding: 16px 0;
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
</style>
