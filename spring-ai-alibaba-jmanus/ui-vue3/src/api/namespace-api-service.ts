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

export interface Namespace {
  id: string
  code: string
  name: string
}

export class NamespaceApiService {
  private static readonly BASE_URL = '/api/namespace'

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
   * Get all Namespace list
   */
  static async getAllNamespaces(): Promise<Namespace[]> {
    try {
      const response = await fetch(`${this.BASE_URL}`)
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error('Failed to get Namespace list:', error)
      throw error
    }
  }

  /**
   * Get Namespace details by ID
   */
  static async getNamespaceById(id: string): Promise<Namespace> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`)
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error(`Failed to get Namespace[${id}] details:`, error)
      throw error
    }
  }

  /**
   * Create new Namespace
   */
  static async createNamespace(namespaceConfig: Omit<Namespace, 'id'>): Promise<Namespace> {
    try {
      const response = await fetch(this.BASE_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(namespaceConfig),
      })
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error('Failed to create Namespace:', error)
      throw error
    }
  }

  /**
   * Update Namespace configuration
   */
  static async updateNamespace(id: string, namespaceConfig: Namespace): Promise<Namespace> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(namespaceConfig),
      })
      const result = await this.handleResponse(response)
      return await result.json()
    } catch (error) {
      console.error(`Failed to update Namespace[${id}]:`, error)
      throw error
    }
  }

  /**
   * Delete Namespace
   */
  static async deleteNamespace(id: string): Promise<void> {
    try {
      const response = await fetch(`${this.BASE_URL}/${id}`, {
        method: 'DELETE',
      })
      if (response.status === 400) {
        throw new Error('Cannot delete default Namespace')
      }
      await this.handleResponse(response)
    } catch (error) {
      console.error(`Failed to delete Namespace[${id}]:`, error)
      throw error
    }
  }
}

