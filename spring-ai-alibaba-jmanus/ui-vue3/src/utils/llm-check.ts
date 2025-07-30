/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * LLM配置检查工具类
 * 在进行LLM相关操作前检查是否已配置模型
 */
export class LlmCheckService {
  private static cachedStatus: { initialized: boolean; lastCheck: number } | null = null
  private static readonly CACHE_DURATION = 30000 // 30秒缓存

  /**
   * 检查LLM是否已配置
   */
  public static async checkLlmConfiguration(): Promise<{ initialized: boolean; message?: string }> {
    // 检查缓存
    const now = Date.now()
    if (this.cachedStatus && (now - this.cachedStatus.lastCheck) < this.CACHE_DURATION) {
      return { initialized: this.cachedStatus.initialized }
    }

    try {
      const response = await fetch('/api/init/status')
      if (!response.ok) {
        throw new Error(`检查失败: ${response.status}`)
      }
      
      const result = await response.json()
      const initialized = result.success && result.initialized
      
      // 更新缓存
      this.cachedStatus = {
        initialized,
        lastCheck: now
      }
      
      if (!initialized) {
        return {
          initialized: false,
          message: '系统尚未配置LLM模型，请先通过初始化页面配置API密钥。'
        }
      }
      
      return { initialized: true }
    } catch (error) {
      console.error('[LlmCheckService] 检查LLM配置失败:', error)
      return {
        initialized: false,
        message: '无法检查LLM配置状态，请确保系统正常运行。'
      }
    }
  }

  /**
   * 确保LLM已配置，如果未配置则抛出错误或跳转到初始化页面
   */
  public static async ensureLlmConfigured(options?: {
    showAlert?: boolean
    redirectToInit?: boolean
  }): Promise<void> {
    const { showAlert = true, redirectToInit = true } = options || {}
    
    const checkResult = await this.checkLlmConfiguration()
    
    if (!checkResult.initialized) {
      const message = checkResult.message || '请先配置LLM模型'
      
      if (showAlert) {
        alert(message)
      }
      
      if (redirectToInit) {
        // 清除初始化状态，强制跳转到初始化页面
        localStorage.removeItem('hasInitialized')
        window.location.href = '/ui/#/init'
        throw new Error('Redirecting to initialization page')
      }
      
      throw new Error(message)
    }
  }

  /**
   * 清除缓存状态
   */
  public static clearCache(): void {
    this.cachedStatus = null
  }

  /**
   * 包装API调用，在调用前自动检查LLM配置
   */
  public static async withLlmCheck<T>(
    apiCall: () => Promise<T>,
    options?: {
      showAlert?: boolean
      redirectToInit?: boolean
    }
  ): Promise<T> {
    await this.ensureLlmConfigured(options)
    return apiCall()
  }
} 
