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
           let item: ThoughtChainItem | undefined
           // 检查是否为llm_stream类型的节点
           if(node.node) {
              // 当开始普通节点时，也要完成之前的流式节点
              finalizePreviousStreamNodes('')
              item = processJsonNode(node)
           }
           // llm_stream 形式的节点，需要流式渲染
           if(node['researcher_llm_stream_0']){
              // 当开始新的流式节点时，将之前的流式节点状态更新为完成
              finalizePreviousStreamNodes('researcher_llm_stream_0')
              item = processLlmStreamNode(node, 'researcher_llm_stream_0')
           }
           if(node['reporter_llm_stream']){
              // 当开始新的流式节点时，将之前的流式节点状态更新为完成
              finalizePreviousStreamNodes('reporter_llm_stream')
              item = processLlmStreamNode(node, 'reporter_llm_stream')
           }
           // 这个是非标准节点，加入缓存，但是不进行渲染
           if(node['planner_llm_stream']){
              processLlmStreamNode(node, 'planner_llm_stream')
           }
           // information 节点标识等待人类反馈，需要移除最后一个pending节点
           if(node.node === 'information'){
              finalizePreviousStreamNodes('')
              return
           }
           if(item) {
              // 检查是否已经存在相同的item（针对llm_stream节点）
              const existingIndex = arrayTemp.findIndex(existingItem => existingItem === item)
              if(existingIndex === -1) {
                arrayTemp.push(item)
              }
           }
      }
      //  完整的text， 历史记录的渲染
      //  当stream完成，xchat还会返回一次success，为避免思维链重复渲染，如果是loading状态，则不在重复增加节点
      if(msg.status === 'success' && !isLoading) {
          isLoading = false
          const jsonArray = parseJsonTextStrict(msg.message)
          jsonArray.forEach(node => {
            if(!node.node) {
              return
            }
            let item = processJsonNode(node)
            if(item) {
              arrayTemp.push(item)
            }
          })
      }
    })
    // 检查是否需要添加pending节点（当没有流式节点正在进行时）
    const hasActiveStreamNode = Array.from(llmStreamCache.values()).some(cached => cached.item.status === 'pending')
    if (!hasActiveStreamNode && arrayTemp.length > 0) {
      // 创建一个pending状态的节点表示正在请求后端内容
      const pendingItem: ThoughtChainItem = {
        title: '【处理中】正在请求后端内容',
        description: '正在向后端发送请求并等待响应',
        icon: h(LoadingOutlined),
        status: 'pending',
        type: 'pending' // 扩展字段
      }
      arrayTemp.push(pendingItem)
    }
    array.push(...arrayTemp)
    return array
})

// 完成之前的流式节点和pending节点，避免多个节点同时处于pending状态
const finalizePreviousStreamNodes = (currentKey: string) => {
  // 完成缓存中的流式节点
  llmStreamCache.forEach((cached, key) => {
    if (key !== currentKey && cached.item.status === 'pending') {
      cached.item.status = 'success'
      cached.item.icon = h(CheckCircleOutlined)
      cached.item.description = 'AI分析内容生成完成'
    }
  })

  // 移除所有添加的pending节点
  for (let i = arrayTemp.length - 1; i >= 0; i--) {
    if (arrayTemp[i].status === 'pending' && arrayTemp[i].type === 'pending') {
      arrayTemp.splice(i, 1)
    }
  }
}

const processLlmStreamNode = (node: any, key: string): ThoughtChainItem => {
  if(!node[key]) {
    return
  }
  // 检查缓存中是否已存在该节点
  if (llmStreamCache.has(key)) {
    const cached = llmStreamCache.get(key)!
    // 累积新的内容
    cached.content += node[key]
    // 更新MD组件的内容
    cached.item.content = h(MD, { content: cached.content })
    return cached.item
  } else {
    // 创建新的ThoughtChainItem
    const initialContent =  node[key]
    const item: ThoughtChainItem = {
      key: key,
      title: key + '-' + '【AI分析】正在生成分析内容',
      description: '正在使用AI模型分析和生成内容',
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

const processJsonNode = (node: any): ThoughtChainItem => {
    let title = ''
    let description = ''
    let content = null

    // 根据不同节点类型处理
    switch(node.node) {
      case '__START__':
        title = node.node + '-' + '【开始】任务启动'
        description = '开始处理用户请求'
        break

      case 'coordinator':
        title = node.node + '-' +'【意图识别】分析用户需求'
        description = '正在识别和理解用户的意图'
        break

      case 'rewrite_multi_query':
        title = node.node + '-' +'【查询优化】重写查询语句'
        description = '优化查询以获得更好的搜索结果'
        if(node.data?.optimize_queries && Array.isArray(node.data.optimize_queries)) {
          const queries = node.data.optimize_queries
          const markdownContent = queries.map((query, index) => `${index + 1}. ${query}`).join('\n')
          content = h(MD, { content: markdownContent })
        }
        break

      case node.node + '-' +'background_investigator':
        title = '【背景调研】收集相关信息'
        description = '正在收集和分析背景信息'
        if(node.data?.background_investigation_results && Array.isArray(node.data.background_investigation_results)) {
          const results = node.data.background_investigation_results
          const markdownContent = results.map((result, index) => `${index + 1}. ${result}`).join('\n')
          content = h(MD, { content: markdownContent })
        }
        break

      case 'human_feedback':
        title = node.node + '-' + (node.data?.current_plan?.title || '【人工反馈】')
        description = node.data?.current_plan?.thought || '等待人工反馈'
        break

      case '__PARALLEL__(parallel_executor)':
        title = node.node + '-' + (node.data?.current_plan?.title || '【并行执行】')
        description = node.data?.current_plan?.thought || '正在并行执行任务'
        if(node.data?.current_plan?.steps?.[0]?.executionRes) {
          content = h(MD, { content: node.data.current_plan.steps[0].executionRes })
        }
        break

      case 'reporter':
        title = node.node + '-' + '【报告生成】生成最终报告'
        description = '正在整理和生成最终研究报告'
        if(node.data?.final_report) {
          content = h(MD, { content: node.data.final_report })
        }
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
    return item
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
