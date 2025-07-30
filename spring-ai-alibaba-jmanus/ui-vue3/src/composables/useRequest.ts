import { ref } from 'vue'
import type { ApiResponse } from '@/types/mcp'

export function useRequest() {
  const loading = ref(false)

  const executeRequest = async <T>(
    requestFn: () => Promise<ApiResponse<T>>,
    successMessage?: string,
    errorMessage?: string
  ): Promise<ApiResponse<T> | null> => {
    try {
      loading.value = true
      const result = await requestFn()
      
      if (result.success && successMessage) {
        // 这里需要外部传入showMessage函数，避免循环依赖
        console.log(successMessage)
      } else if (!result.success && errorMessage) {
        console.error(errorMessage)
      }
      
      return result
    } catch (error) {
      console.error('请求执行失败:', error)
      if (errorMessage) {
        console.error(errorMessage)
      }
      return null
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    executeRequest
  }
} 
