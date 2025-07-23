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

export interface Prompt {
  id: string
  builtIn: boolean
  type: string
  promptName: string
  messageType: string
  promptDescription: string
  promptContent: string
  namespace?: string
}

export class PromptApiService {
  private static readonly BASE_URL = '/api/prompt'

  /**
   * Handle HTTP response
   */
  private static async handleResponse(response: Response) {
    if (!response.ok) {
      try {
        const errorData = await response.json()
        throw new Error(errorData.message || `API request failed: ${response.status}`)
      } catch {
        throw new Error(`API request failed: ${response.status} ${response.statusText}`)
      }
    }
    return response
  }

  /**
   * Get all Prompt list
   */
  static async getAllPrompts(namespace: string): Promise<Prompt[]> {
    try {
      const response = await fetch(`${this.BASE_URL}/namespace/${namespace}`)
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error('Failed to get Prompt list:', error)
      throw error
    }
  }

  /**
   * Get Prompt details by ID
   */
  static async getPromptById(id: string): Promise<Prompt> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`)
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error(`Failed to get Pr'o'm'p't[${id}] details:`, error)
      throw error
    }
  }

  /**
   * Create new Prompt
   */
  static async createPrompt(promptConfig: Omit<Prompt, 'id'>): Promise<Prompt> {
    try {
      const response = await fetch(this.BASE_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(promptConfig),
      })
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error('Failed to create Prompt:', error)
      throw error
    }
  }
  /**
   * Update Prompt configuration
   */
  static async updatePrompt(id: string, PromptConfig: Prompt): Promise<Prompt> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(PromptConfig),
      })
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error(`Failed to update Prompt[${id}]:`, error)
      throw error
    }
  }

  /**
   * Delete Prompt
   */
  static async deletePrompt(id: string): Promise<void> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`, {
        method: 'DELETE',
      })
      if (response.status === 400) {
        throw new Error('Cannot delete default Prompt')
      }
      await this.handleResponse(response)
    } catch (error) {
      console.error(`Failed to delete Prompt[${id}]:`, error)
      throw error
    }
  }
}
