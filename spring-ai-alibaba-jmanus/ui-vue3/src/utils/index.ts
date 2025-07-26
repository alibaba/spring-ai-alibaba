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

export function notEmpty(value: unknown): boolean {
  if (value == null) return false

  // Empty string
  if (typeof value === 'string' && value.trim() === '') return false

  // Empty array
  if (Array.isArray(value) && value.length === 0) return false

  // Empty object (no enumerable properties)
  if (typeof value === 'object' && Object.keys(value).length === 0) return false

  return true
}

/**
 * Export object as file
 * @param data The object data to export
 * @param filename The filename for the exported file
 * @param fileType The file type, default is 'application/json'
 */
export function exportObjectAsFile(
  data: object,
  filename: string,
  fileType: string = 'application/json'
): void {
  // Convert object to string
  const dataStr = JSON.stringify(data, null, 2)

  // Create Blob object
  const blob = new Blob([dataStr], { type: fileType })

  // Create download URL
  const url = URL.createObjectURL(blob)

  // Create hidden download link element
  const link = document.createElement('a')
  link.href = url
  link.download = filename

  // Trigger download
  document.body.appendChild(link)
  link.click()

  // Cleanup
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
/**
 * Read and parse uploaded file
 * @param fileType File type ('json' | 'text')
 * @returns Promise that resolves with parsed data
 */
export function importFile(fileType: 'json' | 'text' = 'json'): Promise<any> {
  return new Promise((resolve, reject) => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = `.${fileType}`
    input.onchange = event => {
      const file = (event.target as HTMLInputElement).files?.[0]
      if (file) {
        const reader = new FileReader()
        reader.onload = event => {
          try {
            const content = event.target?.result as string

            if (fileType === 'json') {
              const parsedData = JSON.parse(content)
              resolve(parsedData)
            } else {
              resolve(content)
            }
          } catch (error) {
           reject(new Error(`Failed to parse file: ${error instanceof Error ? error.message : 'Unknown error'}`));
          }
        }

        reader.onerror = () => {
          reject(new Error('Failed to read file'))
        }

        if (fileType === 'json') {
          reader.readAsText(file)
        } else {
          reader.readAsText(file)
        }
      }
    }
    input.click()
  })
}
