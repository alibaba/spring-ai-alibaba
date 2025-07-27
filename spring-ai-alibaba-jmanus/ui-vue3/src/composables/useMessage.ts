import { reactive } from 'vue'
import type { Message, MessageType } from '@/types/mcp'

export function useMessage() {
  const message = reactive<Message>({
    show: false,
    text: '',
    type: 'success'
  })

  const showMessage = (text: string, type: MessageType = 'success') => {
    console.log(`显示消息: ${text}, 类型: ${type}`)
    
    message.text = text
    message.type = type
    message.show = true

    // 根据消息类型设置不同的显示时间
    const displayTime = type === 'error' ? 5000 : 3000 // 错误消息显示5秒，其他3秒

    console.log(`消息将在 ${displayTime}ms 后自动隐藏`)

    setTimeout(() => {
      message.show = false
      console.log('消息已隐藏')
    }, displayTime)
  }

  return {
    message,
    showMessage
  }
} 
