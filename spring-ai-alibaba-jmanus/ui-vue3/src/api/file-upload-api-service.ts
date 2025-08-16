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

// Remove unused import

export class FileUploadApiService {
  
  /**
   * Delete an uploaded file
   * @param planId Plan ID
   * @param fileName File name to delete
   */
  public static async deleteFile(planId: string, fileName: string): Promise<any> {
    try {
      console.log('[FileUploadApiService] Deleting file:', fileName, 'from plan:', planId)
      
      const response = await fetch(`/api/file-upload/files/${planId}/${encodeURIComponent(fileName)}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
        },
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const result = await response.json()
      console.log('[FileUploadApiService] File deleted successfully:', result)
      return result
      
    } catch (error) {
      console.error('[FileUploadApiService] Error deleting file:', error)
      throw error
    }
  }

  /**
   * Get uploaded files for a plan
   * @param planId Plan ID
   */
  public static async getUploadedFiles(planId: string): Promise<any> {
    try {
      console.log('[FileUploadApiService] Getting uploaded files for plan:', planId)
      
      const response = await fetch(`/api/file-upload/files/${planId}`)
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const result = await response.json()
      console.log('[FileUploadApiService] Got uploaded files:', result.totalCount)
      return result
      
    } catch (error) {
      console.error('[FileUploadApiService] Error getting uploaded files:', error)
      throw error
    }
  }
}
