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
import { computed, h } from 'vue'
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
let isLoading = false
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
           if(!node.node){
              // TODO llm_stream 节点应该渲染loading 节点
              return
           }
           const item = processNode(node)
           if(item) {
              arrayTemp.push(item)
           }
           
           
      }
      //  完整的text， 历史记录的渲染
      //  当stream完成，xchat还会返回一次success，为避免思维链重复渲染，如果是loading状态，则不在重复增加节点
      if(msg.status === 'success' && !isLoading) {
          isLoading = false
          const jsonArray = parseJsonTextStrict(msg.message)
          jsonArray.forEach(node => {
            if(!node.node) {
              // TODO llm_stream 节点应该渲染loading 节点
              return
            }
            // 处理节点渲染
            const item = processNode(node)
            if(item) {
              arrayTemp.push(item)
            }
          })
      }
    })
    array.push(...arrayTemp)
    return array
})

const processNode = (node: any): ThoughtChainItem => {
    let title = ''
    let description = ''
    let content = null
    
    // 根据不同节点类型处理
    switch(node.node) {
      case '__START__':
        title = '【开始】任务启动'
        description = '开始处理用户请求'
        break
        
      case 'coordinator':
        title = '【意图识别】分析用户需求'
        description = '正在识别和理解用户的意图'
        break
        
      case 'rewrite_multi_query':
        title = '【查询优化】重写查询语句'
        description = '优化查询以获得更好的搜索结果'
        if(node.data?.optimize_queries && Array.isArray(node.data.optimize_queries)) {
          const queries = node.data.optimize_queries
          const markdownContent = queries.map((query, index) => `${index + 1}. ${query}`).join('\n')
          content = h(MD, { content: markdownContent })
        }
        break
        
      case 'background_investigator':
        title = '【背景调研】收集相关信息'
        description = '正在收集和分析背景信息'
        if(node.data?.background_investigation_results && Array.isArray(node.data.background_investigation_results)) {
          const results = node.data.background_investigation_results
          const markdownContent = results.map((result, index) => `${index + 1}. ${result}`).join('\n')
          content = h(MD, { content: markdownContent })
        }
        break
        
      case 'information':
        title = node.data?.current_plan?.title || '【信息收集】'
        description = node.data?.current_plan?.thought || '正在收集相关信息'
        break
        
      case 'human_feedback':
        title = node.data?.current_plan?.title || '【人工反馈】'
        description = node.data?.current_plan?.thought || '等待人工反馈'
        break
        
      case '__PARALLEL__(parallel_executor)':
        title = node.data?.current_plan?.title || '【并行执行】'
        description = node.data?.current_plan?.thought || '正在并行执行任务'
        if(node.data?.current_plan?.steps?.[0]?.executionRes) {
          content = h(MD, { content: node.data.current_plan.steps[0].executionRes })
        }
        break
        
      case 'reporter':
        title = '【报告生成】生成最终报告'
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