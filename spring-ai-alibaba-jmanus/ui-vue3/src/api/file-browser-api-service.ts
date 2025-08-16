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
export interface FileNode {
    name: string
    path: string
    type: 'file' | 'directory'
    size: number
    lastModified: string
    children?: FileNode[]
}

export interface FileContent {
    content: string
    mimeType: string
    size: number
}

export interface ApiResponse<T> {
    success: boolean
    data?: T
    message?: string
}

/**
 * File Browser API service class
 * Responsible for interacting with backend FileBrowserController
 */
export class FileBrowserApiService {
    private static readonly BASE_URL = '/api/file-browser'

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
     * Get file tree for a specific plan ID
     */
    static async getFileTree(planId: string): Promise<FileNode> {
        try {
            const response = await fetch(`${this.BASE_URL}/tree/${planId}`)
            const result = await this.handleResponse(response)
            const apiResponse: ApiResponse<FileNode> = await result.json()

            if (!apiResponse.success) {
                throw new Error(apiResponse.message || 'Failed to get file tree')
            }

            return apiResponse.data!
        } catch (error) {
            console.error('Failed to get file tree:', error)
            throw error
        }
    }

    /**
     * Get file content
     */
    static async getFileContent(planId: string, filePath: string): Promise<FileContent> {
        try {
            const response = await fetch(`${this.BASE_URL}/content/${planId}?path=${encodeURIComponent(filePath)}`)
            const result = await this.handleResponse(response)
            const apiResponse: ApiResponse<FileContent> = await result.json()

            if (!apiResponse.success) {
                throw new Error(apiResponse.message || 'Failed to get file content')
            }

            return apiResponse.data!
        } catch (error) {
            console.error('Failed to get file content:', error)
            throw error
        }
    }

    /**
     * Download file
     */
    static async downloadFile(planId: string, filePath: string, fileName?: string): Promise<void> {
        try {
            const response = await fetch(`${this.BASE_URL}/download/${planId}?path=${encodeURIComponent(filePath)}`)
            await this.handleResponse(response)

            const blob = await response.blob()
            const url = window.URL.createObjectURL(blob)
            const a = document.createElement('a')
            a.href = url
            a.download = fileName || filePath.split('/').pop() || 'download'
            document.body.appendChild(a)
            a.click()
            window.URL.revokeObjectURL(url)
            document.body.removeChild(a)
        } catch (error) {
            console.error('Failed to download file:', error)
            throw error
        }
    }

    /**
     * Check if file is text-based (can be displayed in editor)
     */
    static isTextFile(mimeType: string, fileName: string): boolean {
        const textMimeTypes = [
            'text/',
            'application/json',
            'application/xml',
            'application/javascript',
            'application/typescript'
        ]

        const textExtensions = [
            '.txt', '.md', '.json', '.xml', '.html', '.css', '.js', '.ts',
            '.vue', '.jsx', '.tsx', '.py', '.java', '.cpp', '.c', '.h',
            '.sh', '.bat', '.yml', '.yaml', '.properties', '.conf', '.cfg'
        ]

        // Check mime type
        if (textMimeTypes.some(type => mimeType.startsWith(type))) {
            return true
        }

        // Check file extension
        const lowerFileName = fileName.toLowerCase()
        return textExtensions.some(ext => lowerFileName.endsWith(ext))
    }

    /**
     * Get file icon based on file type
     */
    static getFileIcon(node: FileNode): string {
        if (node.type === 'directory') {
            return 'carbon:folder'
        }

        const fileName = node.name.toLowerCase()

        // Programming languages
        if (fileName.endsWith('.js')) return 'vscode-icons:file-type-js'
        if (fileName.endsWith('.ts')) return 'vscode-icons:file-type-typescript'
        if (fileName.endsWith('.vue')) return 'vscode-icons:file-type-vue'
        if (fileName.endsWith('.java')) return 'vscode-icons:file-type-java'
        if (fileName.endsWith('.py')) return 'vscode-icons:file-type-python'
        if (fileName.endsWith('.json')) return 'vscode-icons:file-type-json'
        if (fileName.endsWith('.xml')) return 'vscode-icons:file-type-xml'
        if (fileName.endsWith('.html')) return 'vscode-icons:file-type-html'
        if (fileName.endsWith('.css')) return 'vscode-icons:file-type-css'
        if (fileName.endsWith('.md')) return 'vscode-icons:file-type-markdown'
        if (fileName.endsWith('.yml') || fileName.endsWith('.yaml')) return 'vscode-icons:file-type-yaml'

        // Documents
        if (fileName.endsWith('.pdf')) return 'vscode-icons:file-type-pdf2'
        if (fileName.endsWith('.doc') || fileName.endsWith('.docx')) return 'vscode-icons:file-type-word'
        if (fileName.endsWith('.xls') || fileName.endsWith('.xlsx')) return 'vscode-icons:file-type-excel'
        if (fileName.endsWith('.ppt') || fileName.endsWith('.pptx')) return 'vscode-icons:file-type-powerpoint'

        // Images
        if (fileName.match(/\.(jpg|jpeg|png|gif|bmp|svg)$/)) return 'carbon:image'

        // Archives
        if (fileName.match(/\.(zip|rar|7z|tar|gz)$/)) return 'carbon:archive'

        // Default file icon
        return 'carbon:document'
    }
}
