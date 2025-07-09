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
 * UI state data for managing input component state
 */
export interface UIStateData {
  /** Whether the input component is enabled */
  enabled: boolean
  
  /** Placeholder text for the input component */
  placeholder?: string
  
  /** Additional UI properties */
  loading?: boolean
  
  /** Any additional metadata for UI state */
  metadata?: Record<string, any>
}
