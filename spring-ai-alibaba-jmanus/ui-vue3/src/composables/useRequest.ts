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
        // Need to pass showMessage function from outside to avoid circular dependencies
        console.log(successMessage)
      } else if (!result.success && errorMessage) {
        console.error(errorMessage)
      }

      return result
    } catch (error) {
      console.error('Request execution failed:', error)
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
