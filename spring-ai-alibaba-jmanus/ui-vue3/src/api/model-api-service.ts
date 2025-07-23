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

// Define interface types
export interface Model {
    id: string
    headers: Headers | null
    baseUrl: string
    apiKey: string
    modelName: string
    modelDescription: string
    type: string
}

export interface Headers {
    key: string
    value: string
}

export interface ApiResponse<T> {
    success: boolean
    data?: T
    message?: string
}

/**
 * Model API service class
 * Responsible for interacting with backend ModelController
 */
export class ModelApiService {
    private static readonly BASE_URL = '/api/models'

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
     * Get all Model list
     */
    static async getAllModels(): Promise<Model[]> {
        try {
            const response = await fetch(this.BASE_URL)
            const result = await this.handleResponse(response)
            return await result.json()
        } catch (error) {
            console.error('Failed to get Model list:', error)
            throw error
        }
    }

    /**
     * Get all Types list
     */
    static async getAllTypes(): Promise<string[]> {
        try {
            const response = await fetch(`${this.BASE_URL}/types`)
            const result = await this.handleResponse(response)
            return await result.json()
        } catch (error) {
            console.error('Failed to get Model list:', error)
            throw error
        }
    }

    /**
     * Get Model details by ID
     */
    static async getModelById(id: string): Promise<Model> {
        try {
            const response = await fetch(`${this.BASE_URL}/${id}`)
            const result = await this.handleResponse(response)
            return await result.json()
        } catch (error) {
            console.error(`Failed to get Model[${id}] details:`, error)
            throw error
        }
    }

    /**
     * Create new Model
     */
    static async createModel(modelConfig: Omit<Model, 'id'>): Promise<Model> {
        try {
            const response = await fetch(this.BASE_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(modelConfig)
            })
            const result = await this.handleResponse(response)
            return await result.json()
        } catch (error) {
            console.error('Failed to create Model:', error)
            throw error
        }
    }

    /**
     * Update Model configuration
     */
    static async updateModel(id: string, modelConfig: Model): Promise<Model> {
        try {
            const response = await fetch(`${this.BASE_URL}/${id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(modelConfig)
            })
            if (response.status === 499) {
                throw new Error('Request rejected, please modify the model configuration in the configuration file')
            }
            const result = await this.handleResponse(response)
            return await result.json()
        } catch (error) {
            console.error(`Failed to update Model[${id}]:`, error)
            throw error
        }
    }

    /**
     * Delete Model
     */
    static async deleteModel(id: string): Promise<void> {
        try {
            const response = await fetch(`${this.BASE_URL}/${id}`, {
                method: 'DELETE'
            })
            if (response.status === 400) {
                throw new Error('Cannot delete default Model')
            }
            if (response.status === 499) {
                throw new Error('Request rejected, please modify the model configuration in the configuration file')
            }
            await this.handleResponse(response)
        } catch (error) {
            console.error(`Failed to delete Model[${id}]:`, error)
            throw error
        }
    }

}

/**
 * Model configuration management model class
 * Provides CRUD operations and local state management for Model configuration
 */
export class ModelConfigModel {
    public models: Model[] = []
    public currentModel: Model | null = null

    /**
     * Load all Model list
     */
    async loadModels(): Promise<Model[]> {
        try {
            this.models = await ModelApiService.getAllModels()
            return this.models
        } catch (error) {
            console.error('Failed to load Model list:', error)
            throw error
        }
    }

    /**
     * Load Model details
     */
    async loadModelDetails(id: string): Promise<Model> {
        try {
            this.currentModel = await ModelApiService.getModelById(id)
            return this.currentModel
        } catch (error) {
            console.error('Failed to load Model details:', error)
            throw error
        }
    }

    /**
     * Save Model configuration
     */
    async saveModel(modelData: Model, isImport = false): Promise<Model> {
        try {
            let result: Model

            if (isImport) {
                // Import new Model, remove ID
                const importData = { ...modelData }
                delete (importData as any).id
                result = await ModelApiService.createModel(importData)
            } else if (modelData.id) {
                // Update existing Model
                result = await ModelApiService.updateModel(modelData.id, modelData)
            } else {
                // Create new Model
                result = await ModelApiService.createModel(modelData)
            }

            // Update local data
            const index = this.models.findIndex(model => model.id === result.id)
            if (index !== -1) {
                this.models[index] = result
            } else {
                this.models.push(result)
            }

            return result
        } catch (error) {
            console.error('Failed to save Model:', error)
            throw error
        }
    }

    /**
     * Delete Model
     */
    async deleteModel(id: string): Promise<void> {
        try {
            await ModelApiService.deleteModel(id)
            this.models = this.models.filter(model => model.id !== id)

            // If deleted is the current selected Model, clear selection
            if (this.currentModel?.id === id) {
                this.currentModel = null
            }
        } catch (error) {
            console.error('Failed to delete Model:', error)
            throw error
        }
    }

    /**
     * Find Model by ID
     */
    findModelById(id: string): Model | undefined {
        return this.models.find(model => model.id === id)
    }

    /**
     * Export Model configuration as JSON
     */
    exportModel(model: Model): string {
        return JSON.stringify(model, null, 2)
    }

    /**
     * Import Model configuration from JSON string
     */
    parseModelFromJson(jsonString: string): Model {
        try {
            const model = JSON.parse(jsonString)
            // Basic validation
            if (!model.name || !model.description) {
                throw new Error('Model configuration format is incorrect: missing required fields')
            }
            return model
        } catch (error) {
            console.error('Failed to parse Model JSON:', error)
            throw new Error('Model configuration format is incorrect')
        }
    }
}

// Default export an instance for use
export default new ModelConfigModel()
