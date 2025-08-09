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

export interface Message {
    id: string;
    memoryId: string;
    memoryName: string;
    createTime: string;
    preview: string[];
    messages: MessageBubble[];
    expanded: boolean;
}

export interface MessageBubble {
    messageType: string;
    text: string;
}

export class MemoryApiService {
    private static readonly BASE_URL = '/api/memories'

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

    static async getMemories(): Promise<Message[]> {
        try {
            const response = await fetch(`${this.BASE_URL}`)
            const result = await this.handleResponse(response)
            return await result.json()
        } catch (error) {
            console.error('Failed to get memory list:', error)
            throw error
        }
    }

    static async getMemory(memoryId: string): Promise<Message> {
        try {
            const response = await fetch(`${this.BASE_URL}/single?memoryId=${memoryId}`)
            const result = await this.handleResponse(response)
            return await result.json()
        } catch (error) {
            console.error('Failed to get memory :', error)
            throw error
        }
    }

    static async update(memoryId: string, memoryName: string): Promise<Message> {
        try {
            const response = await fetch(`${this.BASE_URL}/update`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    memoryId: memoryId,
                    memoryName: memoryName
                })
            })
            const result = await this.handleResponse(response)
            return await result.json()
        } catch (error) {
            console.error('Failed to update memory:', error)
            throw error
        }
    }

    static async delete(memoryId: string): Promise<void> {
        try {
            const response = await fetch(`${this.BASE_URL}/delete?memoryId=${memoryId}`)
            await this.handleResponse(response)
        } catch (error) {
            console.error('Failed to update memory:', error)
            throw error
        }
    }
}
