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
import { Flex, Button,Typography  } from 'ant-design-vue'
import { CloseOutlined, LoadingOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import { parseJsonTextStrict } from '@/utils/jsonParser';
import { useMessageStore } from '@/store/MessageStore'
import { computed,cloneVNode,h } from 'vue'
import { ThoughtChain, type ThoughtChainProps } from 'ant-design-x-vue';
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

const { Paragraph, Text } = Typography;
const arrayTemp: ThoughtChainProps['items'] = []
let isLoading = false
// 从messageStore 拿出消息，然后进行解析并且渲染
const items = computed(() => {
  if (props.convId && messageStore.history[props.convId]) {
    // 思维链显示的列表
    const array: ThoughtChainProps['items'] = []
    // 过滤出非人类的消息
    const messages = messageStore.history[props.convId].filter(item => item.status != 'local')
    messages.forEach(msg => {
      // 先加载一个Loading
      if(!isLoading) {
          isLoading = true
          arrayTemp.push({
              title: 'AI 思考中',
              description: '',
              icon: h(LoadingOutlined),
              status: 'pending',
          })
      }
      // 单个chunk
      if(msg.status === 'loading' && msg.message != 'Waiting...') { 
           const node = JSON.parse(JSON.parse(msg.message))
           console.log('chunk-node', node)
           // llm_stream 节点跳过
           if(!node.node){
              return
           }
           const item = processNode(node)
           arrayTemp.push(item)
           
      }
      // 完整的text
      if(msg.status === 'success') {
          isLoading = false
          const jsonArray = parseJsonTextStrict(msg.message)
          jsonArray.forEach(node => {
            // llm_stream 节点跳过
            if(!node.node) {
              return
            }
            // 处理节点渲染
            const item = processNode(node)
            if(item) {
              arrayTemp.push(item)
            }
          })
      }

      if(!isLoading) {
          // 如果不是loading状态，移除arrayTemp中的第一个loading节点
          if(arrayTemp.length > 0 && arrayTemp[0].status === 'pending') {
              arrayTemp.shift()
          }
      }
    })

    array.push(...arrayTemp)
    return array
    
  }
})

const processNode = (node: any) => {
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
          content = cloneVNode(h(Text, {}, () => {
            return queries.map((query, index) => `${index + 1}. ${query}`).join('\n')
          }))
        }
        break
        
      case 'background_investigator':
        title = '【背景调研】收集相关信息'
        description = '正在收集和分析背景信息'
        if(node.data?.background_investigation_results && Array.isArray(node.data.background_investigation_results)) {
          const results = node.data.background_investigation_results
          content = cloneVNode(h(Text, {}, () => {
            return results.map((result, index) => `${index + 1}. ${result}`).join('\n')
          }))
        }
        break
        
      case 'planner':
        title = '【规划制定】生成执行计划'
        description = '制定详细的执行计划'
        if(node.data?.planner_content) {
          content = cloneVNode(h(Text, {}, () => {
            return node.data.planner_content
          }))
        }
        break
        
      default:
        // 没有匹配到则返回空
        return
    }
    const item: any = {
        title,
        description,
        icon: h(CheckCircleOutlined),
        status: 'success',
    }
    
    if(content) {
      item.content = cloneVNode(content)
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