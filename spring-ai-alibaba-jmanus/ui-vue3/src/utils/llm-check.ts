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
 * LLM configuration check utility class
 * Check if model is configured before performing LLM-related operations
 */
export class LlmCheckService {
  private static cachedStatus: { initialized: boolean; lastCheck: number } | null = null
  private static readonly CACHE_DURATION = 30000 // 30 seconds cache

  /**
   * Check if LLM is configured
   */
  public static async checkLlmConfiguration(): Promise<{ initialized: boolean; message?: string }> {
    // Check cache
    const now = Date.now()
    if (this.cachedStatus && (now - this.cachedStatus.lastCheck) < this.CACHE_DURATION) {
      return { initialized: this.cachedStatus.initialized }
    }

    try {
      const response = await fetch('/api/init/status')
      if (!response.ok) {
        throw new Error(`Check failed: ${response.status}`)
      }

      const result = await response.json()
      const initialized = result.success && result.initialized

      // Update cache
      this.cachedStatus = {
        initialized,
        lastCheck: now
      }

      if (!initialized) {
        return {
          initialized: false,
          message: 'System has not configured LLM model yet, please configure API key through initialization page first.'
        }
      }

      return { initialized: true }
    } catch (error) {
      console.error('[LlmCheckService] Failed to check LLM configuration:', error)
      return {
        initialized: false,
        message: 'Unable to check LLM configuration status, please ensure system is running normally.'
      }
    }
  }

  /**
   * Ensure LLM is configured, throw error or redirect to initialization page if not configured
   */
  public static async ensureLlmConfigured(options?: {
    showAlert?: boolean
    redirectToInit?: boolean
  }): Promise<void> {
    const { showAlert = true, redirectToInit = true } = options || {}

    const checkResult = await this.checkLlmConfiguration()

    if (!checkResult.initialized) {
      const message = checkResult.message || 'Please configure LLM model first'

      if (showAlert) {
        alert(message)
      }

      if (redirectToInit) {
        // Clear initialization status, force redirect to initialization page
        localStorage.removeItem('hasInitialized')
        window.location.href = '/ui/#/init'
        throw new Error('Redirecting to initialization page')
      }

      throw new Error(message)
    }
  }

  /**
   * Clear cache status
   */
  public static clearCache(): void {
    this.cachedStatus = null
  }

  /**
   * Wrap API calls, automatically check LLM configuration before calling
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
