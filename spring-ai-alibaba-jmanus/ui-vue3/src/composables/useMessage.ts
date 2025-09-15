import { reactive } from 'vue'
import type { Message, MessageType } from '@/types/mcp'

export function useMessage() {
  const message = reactive<Message>({
    show: false,
    text: '',
    type: 'success'
  })

  const showMessage = (text: string, type: MessageType = 'success') => {
    console.log(`Showing message: ${text}, Type: ${type}`)

    message.text = text
    message.type = type
    message.show = true

    // Set different display times based on message type
    const displayTime = type === 'error' ? 5000 : 3000 // Error messages display for 5 seconds, others for 3 seconds

    console.log(`Message will be automatically hidden after ${displayTime}ms`)

    setTimeout(() => {
      message.show = false
      console.log('Message hidden')
    }, displayTime)
  }

  return {
    message,
    showMessage
  }
}
