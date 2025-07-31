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
   * 获取所有prompt
   */
  static async getAll(): Promise<Prompt[]> {
    const response = await fetch(this.BASE_URL)
    if (!response.ok) {
      throw new Error(`Failed to fetch prompts: ${response.statusText}`)
    }
    return response.json()
  }

  /**
   * 根据命名空间获取prompt
   */
  static async getAllByNamespace(namespace: string): Promise<Prompt[]> {
    const response = await fetch(`${this.BASE_URL}/namespace/${namespace}`)
    if (!response.ok) {
      throw new Error(`Failed to fetch prompts for namespace ${namespace}: ${response.statusText}`)
    }
    return response.json()
  }



  /**
   * 根据ID获取prompt
   */
  static async getById(id: string): Promise<Prompt> {
    const response = await fetch(`${this.BASE_URL}/${id}`)
    if (!response.ok) {
      throw new Error(`Failed to fetch prompt ${id}: ${response.statusText}`)
    }
    return response.json()
  }

  /**
   * 创建prompt
   */
  static async create(prompt: Omit<Prompt, 'id'>): Promise<Prompt> {
    const response = await fetch(this.BASE_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(prompt),
    })
    if (!response.ok) {
      throw new Error(`Failed to create prompt: ${response.statusText}`)
    }
    return response.json()
  }

  /**
   * 更新prompt
   */
  static async update(id: string, prompt: Prompt): Promise<Prompt> {
    const response = await fetch(`${this.BASE_URL}/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(prompt),
    })
    if (!response.ok) {
      throw new Error(`Failed to update prompt ${id}: ${response.statusText}`)
    }
    return response.json()
  }

  /**
   * 删除prompt
   */
  static async delete(id: string): Promise<void> {
    const response = await fetch(`${this.BASE_URL}/${id}`, {
      method: 'DELETE',
    })
    if (!response.ok) {
      throw new Error(`Failed to delete prompt ${id}: ${response.statusText}`)
    }
  }

  /**
   * 获取支持的语言列表
   */
  static async getSupportedLanguages(): Promise<string[]> {
    const response = await fetch(`${this.BASE_URL}/languages`)
    if (!response.ok) {
      throw new Error(`Failed to fetch supported languages: ${response.statusText}`)
    }
    return response.json()
  }

  /**
   * 针对特定prompt从resource覆盖指定语言的内容到数据库
   */
  static async importSpecificPromptFromLanguage(promptName: string, language: string): Promise<void> {
    const response = await fetch(`${this.BASE_URL}/import/${promptName}/language/${language}`, {
      method: 'POST',
    })
    if (!response.ok) {
      throw new Error(`Failed to import specific prompt ${promptName} for language ${language}: ${response.statusText}`)
    }
  }

  /**
   * 批量重置所有prompt为指定语言的默认值
   */
  static async importAllPromptsFromLanguage(language: string): Promise<void> {
    const response = await fetch(`/admin/prompts/switch-language?language=${language}`, {
      method: 'POST',
    })
    if (!response.ok) {
      throw new Error(`Failed to switch all prompts to language ${language}: ${response.statusText}`)
    }
  }

}
