<!-- 
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
-->
<template>
  <div class="config-panel">
    <div class="config-header">
      <div class="header-left">
        <h2>{{ $t('config.basicConfig.title') }}</h2>
        <div class="config-stats">
          <span class="stat-item">
            <span class="stat-label">{{ $t('config.basicConfig.totalConfigs') }}:</span>
            <span class="stat-value">{{ configStats.total }}</span>
          </span>
          <span class="stat-item" v-if="configStats.modified > 0">
            <span class="stat-label">{{ $t('config.basicConfig.modified') }}:</span>
            <span class="stat-value modified">{{ configStats.modified }}</span>
          </span>
        </div>
      </div>
      <div class="header-right">
        <div class="import-export-actions">
          <button @click="exportConfigs" class="action-btn" :title="$t('config.basicConfig.exportConfigs')">
            📤
          </button>
          <label class="action-btn" :title="$t('config.basicConfig.importConfigs')">
            📥
            <input 
              type="file" 
              accept=".json"
              @change="importConfigs"
              style="display: none;"
            />
          </label>
        </div>
        <div class="search-box">
          <input 
            v-model="searchQuery"
            type="text" 
                            :placeholder="$t('config.search')"
            class="search-input"
          />
          <span class="search-icon">🔍</span>
        </div>
      </div>
    </div>

    <!-- Loading Status -->
    <div v-if="initialLoading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>{{ $t('config.loading') }}</p>
    </div>

    <!-- Configuration Groups -->
    <div v-else-if="filteredConfigGroups.length > 0" class="config-groups">
      <div 
        v-for="group in filteredConfigGroups" 
        :key="group.name" 
        class="config-group"
      >
        <div class="group-header">
          <div class="group-info">
            <span class="group-icon">{{ GROUP_ICONS[group.name] || '⚙️' }}</span>
          </div>
          <div class="group-actions">
            <button 
              @click="resetGroupConfigs(group.name)"
              class="reset-btn"
              :disabled="loading"
              :title="$t('config.resetGroupConfirm')"
            >
              {{ $t('config.reset') }}
            </button>
          </div>
          <div class="group-divider"></div>
        </div>
        
        <!-- Sub-groups -->
        <div class="sub-groups">
          <div 
            v-for="subGroup in group.subGroups" 
            :key="subGroup.name" 
            class="sub-group"
          >
            <div 
              class="sub-group-header"
              @click="toggleSubGroup(group.name, subGroup.name)"
            >
              <div class="sub-group-info">
                <span class="sub-group-icon">📁</span>
                <h4 class="sub-group-title">{{ $t(subGroup.displayName) }}</h4>
                <span class="item-count">({{ subGroup.items.length }})</span>
              </div>
              <span 
                class="collapse-icon"
                :class="{ 'collapsed': isSubGroupCollapsed(group.name, subGroup.name) }"
              >
                ▼
              </span>
            </div>
            
            <div 
              class="config-items" 
              v-show="!isSubGroupCollapsed(group.name, subGroup.name)"
            >
              <div 
                v-for="item in subGroup.items" 
                :key="item.id" 
                class="config-item"
                :class="{ 
                  'modified': originalConfigValues.get(item.id) !== item.configValue 
                }"
              >
                <!-- Boolean Type Configuration Items (CHECKBOX/BOOLEAN) -->
                <template v-if="item.inputType === 'BOOLEAN' || item.inputType === 'CHECKBOX'">
                  <div class="config-item-content vertical-layout">
                    <div class="config-item-info">
                      <div class="config-item-header">
                        <label class="config-label">
                          {{ $t(item.displayName) || item.description }}
                          <span class="type-badge boolean">{{ item.inputType === 'CHECKBOX' ? $t('config.types.checkbox') : $t('config.types.boolean') }}</span>
                          <span v-if="originalConfigValues.get(item.id) !== item.configValue" class="modified-badge">{{ $t('config.modified') }}</span>
                        </label>
                        <span class="config-key" :title="item.configKey">{{ item.configKey }}</span>
                      </div>
                    </div>
                    <div class="config-control">
                      <!-- If options are defined, display as a select box -->
                      <template v-if="item.options && item.options.length > 0">
                        <select 
                          class="config-input select-input"
                          :value="item.configValue"
                          @change="updateConfigValue(item, ($event.target as HTMLSelectElement)?.value || '')"
                        >
                          <option 
                            v-for="option in item.options" 
                            :key="getOptionValue(option)" 
                            :value="getOptionValue(option)"
                          >
                            {{ getOptionLabel(option) }}
                          </option>
                        </select>
                      </template>
                      <!-- Otherwise, display as a switch -->
                      <template v-else>
                        <Switch 
                          :enabled="getBooleanValue(item.configValue)"
                          label=""
                          @update:switchValue="updateConfigValue(item, $event)"
                        />
                      </template>
                    </div>
                  </div>
                </template>

                <!-- Select Type Configuration Items -->
                <template v-else-if="item.inputType === 'SELECT'">
                  <div class="config-item-content vertical-layout">
                    <div class="config-item-info">
                      <div class="config-item-header">
                        <label class="config-label">
                          {{ $t(item.displayName) || item.description }}
                          <span class="type-badge select">{{ $t('config.types.select') }}</span>
                          <span v-if="originalConfigValues.get(item.id) !== item.configValue" class="modified-badge">{{ $t('config.modified') }}</span>
                        </label>
                        <span class="config-key" :title="item.configKey">{{ item.configKey }}</span>
                      </div>
                    </div>
                    <div class="config-control">
                      <select 
                        class="config-input select-input"
                        :value="item.configValue"
                        @change="updateConfigValue(item, ($event.target as HTMLSelectElement)?.value || '')"
                      >
                        <option 
                          v-for="option in item.options || []" 
                          :key="getOptionValue(option)" 
                          :value="getOptionValue(option)"
                        >
                          {{ getOptionLabel(option) }}
                        </option>
                      </select>
                    </div>
                  </div>
                </template>

                <!-- Textarea Type Configuration Items -->
                <template v-else-if="item.inputType === 'TEXTAREA'">
                  <div class="config-item-content vertical-layout">
                    <div class="config-item-info">
                      <div class="config-item-header">
                        <label class="config-label">
                          {{ $t(item.displayName) || item.description }}
                          <span class="type-badge textarea">{{ $t('config.types.textarea') }}</span>
                          <span v-if="originalConfigValues.get(item.id) !== item.configValue" class="modified-badge">{{ $t('config.modified') }}</span>
                        </label>
                        <span class="config-key" :title="item.configKey">{{ item.configKey }}</span>
                      </div>
                    </div>
                    <div class="config-control">
                      <textarea 
                        class="config-input textarea-input"
                        :value="item.configValue"
                        @input="updateConfigValue(item, ($event.target as HTMLTextAreaElement)?.value || '')"
                        @blur="debouncedSave"
                        rows="3"
                      />
                    </div>
                  </div>
                </template>

                <!-- Number Type Configuration Items -->
                <template v-else-if="item.inputType === 'NUMBER'">
                  <div class="config-item-content vertical-layout">
                    <div class="config-item-info">
                      <div class="config-item-header">
                        <label class="config-label">
                          {{ $t(item.displayName) || item.description }}
                          <span class="type-badge number">{{ $t('config.types.number') }}</span>
                          <span v-if="originalConfigValues.get(item.id) !== item.configValue" class="modified-badge">{{ $t('config.modified') }}</span>
                        </label>
                        <span class="config-key" :title="item.configKey">{{ item.configKey }}</span>
                        <div class="config-meta" v-if="item.min || item.max">
                          <span class="range-info">
                            {{ $t('config.range') }}: {{ item.min || 0 }} - {{ item.max || '∞' }}
                          </span>
                        </div>
                      </div>
                    </div>
                    <div class="config-control">
                      <input 
                        type="number" 
                        class="config-input number-input"
                        :value="getNumberValue(item.configValue)"
                        @input="updateConfigValue(item, ($event.target as HTMLInputElement)?.value || '')"
                        @blur="debouncedSave"
                        :min="item.min || 1"
                        :max="item.max || 10000"
                      />
                    </div>
                  </div>
                </template>

                <!-- String Type Configuration Items (STRING/TEXT) -->
                <template v-else>
                  <div class="config-item-content vertical-layout">
                    <div class="config-item-info">
                      <div class="config-item-header">
                        <label class="config-label">
                          {{ $t(item.displayName) || item.description }}
                          <span class="type-badge string">{{ item.inputType === 'TEXT' ? $t('config.types.text') : $t('config.types.string') }}</span>
                          <span v-if="originalConfigValues.get(item.id) !== item.configValue" class="modified-badge">{{ $t('config.modified') }}</span>
                        </label>
                        <span class="config-key" :title="item.configKey">{{ item.configKey }}</span>
                      </div>
                    </div>
                    <div class="config-control">
                      <input 
                        type="text" 
                        class="config-input text-input"
                        :value="item.configValue"
                        @input="updateConfigValue(item, ($event.target as HTMLInputElement)?.value || '')"
                        @blur="debouncedSave"
                      />
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <p>{{ $t('config.notFound') }}</p>
    </div>

    <!-- Message Prompt -->
    <transition name="message-fade">
      <div v-if="message.show" :class="['message-toast', message.type]">
        {{ message.text }}
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import Switch from '@/components/switch/index.vue'
import { AdminApiService, type ConfigItem } from '@/api/admin-api-service'

// Initialize i18n
const { t } = useI18n()

// Define extended configuration item interface
interface ExtendedConfigItem extends ConfigItem {
  displayName: string
  min?: number
  max?: number
}

// Define the configuration subgroup interface
interface ConfigSubGroup {
  name: string
  displayName: string
  items: ExtendedConfigItem[]
}

// Define the configuration group interface
interface ConfigGroup {
  name: string
  displayName: string
  subGroups: ConfigSubGroup[]
}

// Reactive data
const initialLoading = ref(true)
const loading = ref(false)
const configGroups = ref<ConfigGroup[]>([])
const originalConfigValues = ref<Map<number, string>>(new Map())

// Subgroup collapse state
const collapsedSubGroups = ref<Set<string>>(new Set())

// Message Prompt
const message = reactive({
  show: false,
  text: '',
  type: 'success' as 'success' | 'error'
})

// Search filter state
const searchQuery = ref('')



// Configuration item display name mapping
const CONFIG_DISPLAY_NAMES: Record<string, string> = {
  // Browser Settings
  'headless': ('config.basicConfig.browserSettings.headless'), 
  'requestTimeout': ('config.basicConfig.browserSettings.requestTimeout'), 

  // General Settings
  'debugDetail': ('config.basicConfig.general.debugDetail'), 
  'baseDir': ('config.basicConfig.general.baseDir'), 

  // Interaction Settings
  'openBrowser': ('config.basicConfig.interactionSettings.openBrowser'), 

  // Agent Settings
  'maxSteps': ('config.basicConfig.agentSettings.maxSteps'), 
  'userInputTimeout': ('config.basicConfig.agentSettings.userInputTimeout'), 
  'maxMemory': ('config.basicConfig.agentSettings.maxMemory'), 
  'parallelToolCalls': ('config.basicConfig.agentSettings.parallelToolCalls'), 
  
  // Agents
  'forceOverrideFromYaml': ('config.basicConfig.agents.forceOverrideFromYaml'), 

  // Infinite Context
  'enabled': ('config.basicConfig.infiniteContext.enabled'), 
  'parallelThreads': ('config.basicConfig.infiniteContext.parallelThreads'), 
  'taskContextSize': ('config.basicConfig.infiniteContext.taskContextSize'), 

  // File System
  'allowExternalAccess': ('config.basicConfig.fileSystem.allowExternalAccess'), 

  // System Settings (not used)
  // 'systemName': t('config.basicConfig.systemSettings.systemName'),
  // 'language': t('config.basicConfig.systemSettings.language'),
  // 'maxThreads': t('config.basicConfig.systemSettings.maxThreads'),
  // 'timeoutSeconds': t('config.basicConfig.systemSettings.requestTimeout')
}

// Biggest Group display name mapping, 
// The four configuration groups 'browser', 'interaction', 'system', and 'performance' have no corresponding backend responses and have been temporarily removed.
const GROUP_DISPLAY_NAMES: Record<string, string> = {
  'manus': ('config.basicConfig.groupDisplayNames.manus'), // "Manus"
  // 'browser': t('config.basicConfig.groupDisplayNames.browser'), 
  // 'interaction': t('config.basicConfig.groupDisplayNames.interaction'),
  // 'system': t('config.basicConfig.groupDisplayNames.system'),
  // 'performance': t('config.basicConfig.groupDisplayNames.performance')
}

// Group icon mapping
const GROUP_ICONS: Record<string, string> = {
  'manus': '🤖',
  'browser': '🌐',
  'interaction': '🖥️',
  'system': '⚙️',
  'performance': '⚡'
}

// Sub-group display name mapping
const SUB_GROUP_DISPLAY_NAMES: Record<string, string> = {
  'agent': ('config.subGroupDisplayNames.agent'), 
  'browser': ('config.subGroupDisplayNames.browser'), 
  'interaction': ('config.subGroupDisplayNames.interaction'), 
  'agents': ('config.subGroupDisplayNames.agents'), 
  'infiniteContext': ('config.subGroupDisplayNames.infiniteContext'), 
  'general': ('config.subGroupDisplayNames.general'), 
  'filesystem': ('config.subGroupDisplayNames.filesystem'), 
}

// Computed property: Whether there are changes
const hasChanges = computed(() => {
  return configGroups.value.some(group => 
    group.subGroups.some(subGroup =>
      subGroup.items.some(item => 
        originalConfigValues.value.get(item.id) !== item.configValue
      )
    )
  )
})

// Utility function: Get boolean value
const getBooleanValue = (value: string): boolean => {
  return value === 'true'
}

// Utility function: Get numeric value
const getNumberValue = (value: string): number => {
  return parseFloat(value) || 0
}

// Utility function: Get the minimum value of the configuration item
const getConfigMin = (configKey: string): number => {
  const minValues: Record<string, number> = {
    'maxSteps': 1,
    'browserTimeout': 1,
    'maxThreads': 1,
    'timeoutSeconds': 5,
    'maxMemory': 1
  }
  return minValues[configKey] || 1
}

// Utility function: Get the maximum value of the configuration item
const getConfigMax = (configKey: string): number => {
  const maxValues: Record<string, number> = {
    'maxSteps': 100,
    'browserTimeout': 600,
    'maxThreads': 32,
    'timeoutSeconds': 300,
    'maxMemory': 1000
  }
  return maxValues[configKey] || 10000
}

// Utility function: Get the option value
const getOptionValue = (option: string | { value: string; label: string }): string => {
  return typeof option === 'string' ? option : option.value
}

// Utility function: Get the option label
const getOptionLabel = (option: string | { value: string; label: string }): string => {
  return typeof option === 'string' ? option : option.label
}

// Utility function: Handle boolean value updates (supports option mapping)
const handleBooleanUpdate = (item: ExtendedConfigItem, newValue: string | boolean): string => {
  // If it's a direct boolean value (from a switch)
  if (typeof newValue === 'boolean') {
    return newValue.toString()
  }
  
  // If it's a string (from a select box)
  if (typeof newValue === 'string') {
    // Handle possible option mappings (e.g., "是" -> "true", "否" -> "false")
    if (item.options && item.options.length > 0) {
      // Find the matching option
      const matchedOption = item.options.find(option => 
        (typeof option === 'string' ? option : option.label) === newValue ||
        (typeof option === 'string' ? option : option.value) === newValue
      )
      if (matchedOption) {
        return typeof matchedOption === 'string' ? matchedOption : matchedOption.value
      }
    }
    return newValue
  }
  
  // Fallback handling
  return String(newValue)
}

// Update configuration value
const updateConfigValue = (item: ExtendedConfigItem, value: any, autoSave: boolean = false) => {
  let stringValue: string
  
  // Handle the value according to the input type
  if (item.inputType === 'BOOLEAN' || item.inputType === 'CHECKBOX') {
    stringValue = handleBooleanUpdate(item, value)
  } else {
    stringValue = String(value)
  }
  
  if (item.configValue !== stringValue) {
    item.configValue = stringValue
    item._modified = true
    
    // If it's a non-text input type (e.g., switch, select), save automatically
    if (autoSave || item.inputType === 'BOOLEAN' || item.inputType === 'CHECKBOX' || item.inputType === 'SELECT') {
      debouncedSave()
    }
  }
}

// Debounce save
let saveTimeout: number | null = null
const debouncedSave = () => {
  if (saveTimeout) {
    clearTimeout(saveTimeout)
  }
  saveTimeout = window.setTimeout(() => {
    saveAllConfigs()
  }, 500)
}

// Show message
const showMessage = (text: string, type: 'success' | 'error' = 'success') => {
  message.text = text
  message.type = type
  message.show = true
  
  setTimeout(() => {
    message.show = false
  }, 3000)
}

// Load all configuration groups
const loadAllConfigs = async () => {
  try {
    initialLoading.value = true
    
    // Define known configuration groups (avoid relying on the backend's getAllGroups interface)
    // The four configuration groups 'browser', 'interaction', 'system', and 'performance' have no corresponding backend responses and have been temporarily removed.
    const knownGroups = ['manus']
    
    // Load each group's configuration
    const groupPromises = knownGroups.map(async (groupName: string) => {
      try {
        const items = await AdminApiService.getConfigsByGroup(groupName)
        // If there are no configuration items in this group, skip it
        if (items.length === 0) {
          return null
        }
        
        // Set display name for each configuration item (prioritize description)
        const processedItems: ExtendedConfigItem[] = items.map(item => ({
          ...item,
          displayName: (CONFIG_DISPLAY_NAMES[item.configKey] || item.configKey),
          min: getConfigMin(item.configKey),
          max: getConfigMax(item.configKey)
        }))
        
        // Cache original values
        processedItems.forEach(item => {
          originalConfigValues.value.set(item.id, item.configValue)
        })
        
        // Group by subgroup
        const subGroupsMap = new Map<string, ExtendedConfigItem[]>()
        
        processedItems.forEach(item => {
          const subGroupName = item.configSubGroup ?? 'general'
          if (!subGroupsMap.has(subGroupName)) {
            subGroupsMap.set(subGroupName, [])
          }
          subGroupsMap.get(subGroupName)!.push(item)
        })
        
        // Convert to sub-group array
        const subGroups: ConfigSubGroup[] = Array.from(subGroupsMap.entries()).map(([name, items]) => ({
          name,
          displayName: (SUB_GROUP_DISPLAY_NAMES[name] || name),
          items
        }))
        
        return {
          name: groupName,
          displayName: (GROUP_DISPLAY_NAMES[groupName] || groupName),
          subGroups
        }
      } catch (error) {
        console.warn(`加载配置组 ${groupName} 失败，跳过:`, error)
        return null
      }
    })
    
    const results = await Promise.all(groupPromises)
    
    // Filter out empty configuration groups
    configGroups.value = results.filter(group => group !== null) as ConfigGroup[]
    
    console.log(t('config.basicConfig.loadConfigSuccess'), configGroups.value)
  } catch (error) {
    console.error(t('config.basicConfig.loadConfigFailed'), error)
    showMessage(t('config.basicConfig.loadConfigFailed'), 'error')
  } finally {
    initialLoading.value = false
  }
}

// Save all configurations
const saveAllConfigs = async () => {
  if (loading.value || !hasChanges.value) return
  
  try {
    loading.value = true
    
    // Collect all modified configuration items
    const allModifiedConfigs: ConfigItem[] = []
    
    configGroups.value.forEach(group => {
      group.subGroups.forEach(subGroup => {
        const modifiedItems = subGroup.items.filter(item => item._modified)
        allModifiedConfigs.push(...modifiedItems)
      })
    })
    
    if (allModifiedConfigs.length === 0) {
      showMessage(t('config.basicConfig.noModified'))
      return
    }
    
    // Batch save
    const result = await AdminApiService.batchUpdateConfigs(allModifiedConfigs)
    
    if (result.success) {
      // Update the cache of original values
      allModifiedConfigs.forEach(item => {
        originalConfigValues.value.set(item.id, item.configValue)
        item._modified = false
      })
      
      showMessage(t('config.basicConfig.saveSuccess'))
    } else {
      showMessage(result.message || t('config.basicConfig.saveFailed'), 'error')
    }
  } catch (error) {
    console.error(t('config.basicConfig.saveFailed'), error)
    showMessage(t('config.basicConfig.saveFailed'), 'error')
  } finally {
    loading.value = false
  }
}

// Reset group configurations
const resetGroupConfigs = async (groupName: string) => {
  const confirmed = confirm(t('config.basicConfig.resetGroupConfirm', GROUP_DISPLAY_NAMES[groupName] || groupName))
  if (!confirmed) return
  
  try {
    loading.value = true
    
    // Find the target group
    const targetGroup = configGroups.value.find(g => g.name === groupName)
    if (!targetGroup) return
    
    // Collect all configuration items in this group
    const groupConfigs: ConfigItem[] = []
    targetGroup.subGroups.forEach(subGroup => {
      subGroup.items.forEach(item => {
        // We should call the API to get the default value here. For now, let's handle it simply.
        const defaultValue = getDefaultValueForKey(item.configKey)
        if (defaultValue !== item.configValue) {
          groupConfigs.push({
            ...item,
            configValue: defaultValue
          })
        }
      })
    })
    
    if (groupConfigs.length === 0) {
      showMessage(t('config.basicConfig.isDefault'))
      return
    }
    
    // Batch update
    const result = await AdminApiService.batchUpdateConfigs(groupConfigs)
    
    if (result.success) {
      // Reload configurations
      await loadAllConfigs()
      showMessage(t('config.basicConfig.resetSuccess', groupConfigs.length))
    } else {
      showMessage(result.message || t('config.basicConfig.resetFailed'), 'error')
    }
  } catch (error) {
    console.error(t('config.basicConfig.resetFailed'), error)
    showMessage(t('config.basicConfig.resetFailed'), 'error')
  } finally {
    loading.value = false
  }
}

// Get the default value of the configuration item
const getDefaultValueForKey = (configKey: string): string => {
  // There should be a default value mapping table here. For now, return the basic default values.
  const defaults: Record<string, string> = {
    'systemName': 'JTaskPilot',
    'language': 'zh-CN',
    'maxThreads': '8',
    'timeoutSeconds': '60',
    'autoOpenBrowser': 'false',
    'headlessBrowser': 'true',
    'maxMemory': '1000'
    // More default values can be added as needed
  }
  
  return defaults[configKey] || ''
}

// Toggle subgroup collapse
const toggleSubGroup = (groupName: string, subGroupName: string) => {
  const key = `${groupName}-${subGroupName}`
  if (collapsedSubGroups.value.has(key)) {
    collapsedSubGroups.value.delete(key)
  } else {
    collapsedSubGroups.value.add(key)
  }
}

// Check if the subgroup is collapsed
const isSubGroupCollapsed = (groupName: string, subGroupName: string): boolean => {
  return collapsedSubGroups.value.has(`${groupName}-${subGroupName}`)
}

// Calculate configuration statistics
const configStats = computed(() => {
  const total = configGroups.value.reduce((sum, group) => 
    sum + group.subGroups.reduce((subSum, subGroup) => 
      subSum + subGroup.items.length, 0), 0)
  
  const modified = configGroups.value.reduce((sum, group) => 
    sum + group.subGroups.reduce((subSum, subGroup) => 
      subSum + subGroup.items.filter(item => 
        originalConfigValues.value.get(item.id) !== item.configValue).length, 0), 0)
  
  return { total, modified }
})

// Filter configuration groups
const filteredConfigGroups = computed(() => {
  if (!searchQuery.value.trim()) {
    return configGroups.value
  }
  
  const query = searchQuery.value.toLowerCase()
  
  return configGroups.value.map(group => ({
    ...group,
    subGroups: group.subGroups.map(subGroup => ({
      ...subGroup,
      items: subGroup.items.filter(item => 
        item.displayName.toLowerCase().includes(query) ||
        item.configKey.toLowerCase().includes(query) ||
        (item.description && item.description.toLowerCase().includes(query))
      )
    })).filter(subGroup => subGroup.items.length > 0)
  })).filter(group => group.subGroups.length > 0)
})



// Export configurations
const exportConfigs = () => {
  try {
    const exportData = {
      timestamp: new Date().toISOString(),
      version: '1.0',
      configs: configGroups.value.reduce((acc, group) => {
        group.subGroups.forEach(subGroup => {
          subGroup.items.forEach(item => {
            acc[item.configKey] = item.configValue
          })
        })
        return acc
      }, {} as Record<string, string>)
    }
    
    const dataStr = JSON.stringify(exportData, null, 2)
    const dataBlob = new Blob([dataStr], { type: 'application/json' })
    
    const link = document.createElement('a')
    link.href = URL.createObjectURL(dataBlob)
    link.download = `config-export-${new Date().toISOString().split('T')[0]}.json`
    link.click()
    
    showMessage(t('config.basicConfig.exportSuccess'))
  } catch (error) {
    console.error(t('config.basicConfig.exportFailed'), error)
    showMessage(t('config.basicConfig.exportFailed'), 'error')
  }
}

// Import configurations
const importConfigs = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  
  if (!file) return
  
  const reader = new FileReader()
  reader.onload = async (e) => {
    try {
      const importData = JSON.parse(e.target?.result as string)
      
      if (!importData.configs) {
        throw new Error(t('config.basicConfig.invalidFormat'))
      }
      
      const confirmed = confirm(t('config.importConfirm'))
      if (!confirmed) return
      
      loading.value = true
      
      // Prepare the configuration items to be updated
      const configsToUpdate: ConfigItem[] = []
      
      configGroups.value.forEach(group => {
        group.subGroups.forEach(subGroup => {
          subGroup.items.forEach(item => {
            if (Object.prototype.hasOwnProperty.call(importData.configs, item.configKey)) {
              configsToUpdate.push({
                ...item,
                configValue: importData.configs[item.configKey]
              })
            }
          })
        })
      })
      
      if (configsToUpdate.length === 0) {
        showMessage(t('config.basicConfig.notFound'))
        return
      }
      
      // Batch update
      const result = await AdminApiService.batchUpdateConfigs(configsToUpdate)
      
      if (result.success) {
        await loadAllConfigs()
        showMessage(t('config.basicConfig.importSuccess'))
      } else {
        showMessage(result.message || t('config.basicConfig.importFailed'), 'error')
      }
    } catch (error) {
      console.error(t('config.basicConfig.importFailed'), error)
      showMessage(t('config.basicConfig.importFailed'), 'error')
    } finally {
      loading.value = false
      // Clear the input box
      input.value = ''
    }
  }
  
  reader.readAsText(file)
}

// Load configurations when the component is mounted
onMounted(() => {
  loadAllConfigs()
})
</script>

<style scoped>
.config-panel {
  position: relative;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.config-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
}

.config-actions {
  display: flex;
  gap: 12px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: rgba(255, 255, 255, 0.7);
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.config-groups {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.config-group {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.config-group:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.15);
}

.group-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.group-icon {
  font-size: 20px;
  margin-right: 12px;
  opacity: 0.8;
}

.group-divider {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.1) 0%, transparent 100%);
  margin-left: 16px;
}

.config-items {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 16px;
}

.config-item {
  position: relative;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  margin-bottom: 16px;
  transition: all 0.3s ease;
}

.config-item:hover {
  border-color: rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.02);
}

.config-item.modified {
  border-left: 3px solid #f9a825;
}

.config-item-content {
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

/* Vertical layout styles */
.config-item-content.vertical-layout {
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
}

.config-item-content.vertical-layout .config-item-info {
  width: 100%;
}

.config-item-content.vertical-layout .config-control {
  width: 100%;
  min-width: auto;
}

.config-item-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.config-item-info {
  flex: 1;
  min-width: 0;
}

.config-label {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

/* Label style in vertical layout */
.vertical-layout .config-label {
  margin-bottom: 0;
  font-size: 14px;
  line-height: 1.4;
}

.config-key {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 6px;
  font-family: monospace;
  background: rgba(255, 255, 255, 0.05);
  padding: 2px 6px;
  border-radius: 4px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Configuration key style in vertical layout */
.vertical-layout .config-key {
  margin-bottom: 0;
  display: inline-block;
  max-width: fit-content;
}

.config-description {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  margin: 6px 0;
  line-height: 1.4;
}

.type-badge {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 3px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: normal;
}

.type-badge.boolean {
  background: rgba(33, 150, 243, 0.2);
  color: #90caf9;
}

.type-badge.number {
  background: rgba(76, 175, 80, 0.2);
  color: #a5d6a7;
}

.type-badge.string {
  background: rgba(156, 39, 176, 0.2);
  color: #ce93d8;
}

.type-badge.select {
  background: rgba(255, 152, 0, 0.2);
  color: #ffcc80;
}

.modified-badge {
  background: rgba(249, 168, 37, 0.2);
  color: #ffcc80;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 3px;
  font-weight: normal;
}

.range-info {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.05);
  padding: 3px 8px;
  border-radius: 4px;
  display: inline-block;
  margin-top: 6px;
  font-family: monospace;
}

.config-control {
  min-width: 160px;
}

/* Adjust the style of input controls in vertical layout */
.vertical-layout .config-control {
  min-width: auto;
  max-width: 400px; /* 限制最大宽度，避免输入框过宽 */
}

/* Enhance the input box style */
.config-input {
  width: 100%;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  padding: 8px 12px;
  color: rgba(255, 255, 255, 0.9);
  transition: all 0.3s;
}

.config-input:focus {
  outline: none;
  border-color: rgba(102, 126, 234, 0.5);
  background: rgba(255, 255, 255, 0.08);
}

.config-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.config-input.number-input {
  font-family: monospace;
  text-align: right;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: rgba(255, 255, 255, 0.5);
}

.message-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 12px 20px;
  border-radius: 8px;
  color: white;
  font-weight: 500;
  z-index: 1000;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  transform: translateX(100%);
  animation: slide-in 0.3s ease-out forwards;
}

.message-toast.success {
  background: #10b981;
}

.message-toast.error {
  background: #ef4444;
}

.message-fade-enter-active,
.message-fade-leave-active {
  transition: all 0.3s ease;
}

.message-fade-enter-from {
  transform: translateX(100%);
  opacity: 0;
}

.message-fade-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

@keyframes slide-in {
  from {
    transform: translateX(100%);
  }
  to {
    transform: translateX(0);
  }
}

/* Subgroup style */
.sub-group {
  margin-bottom: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.02);
}

.sub-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  cursor: pointer;
  user-select: none;
  transition: all 0.3s ease;
}

.sub-group-header:hover {
  background: rgba(255, 255, 255, 0.08);
}

.sub-group-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sub-group-icon {
  font-size: 14px;
}

.sub-group-title {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

.item-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.1);
  padding: 2px 6px;
  border-radius: 10px;
}

.collapse-icon {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  transition: transform 0.3s ease;
}

.collapse-icon.collapsed {
  transform: rotate(-90deg);
}

.config-stats {
  margin-top: 24px;
  color: rgba(255, 255, 255, 0.6);
}

/* Enhance the header style */
.header-left,
.header-right {
  display: flex;
  align-items: center;
}

.config-stats {
  display: flex;
  margin-left: 16px;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.05);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.stat-label {
  color: rgba(255, 255, 255, 0.6);
  margin-right: 4px;
}

.stat-value {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

.stat-value.modified {
  color: #f9a825;
}

.search-box {
  position: relative;
  margin-right: 16px;
}

.search-input {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  padding: 6px 12px 6px 32px;
  color: rgba(255, 255, 255, 0.9);
  width: 220px;
  font-size: 14px;
  transition: all 0.3s;
}

.search-input:focus {
  outline: none;
  border-color: rgba(102, 126, 234, 0.5);
  background: rgba(255, 255, 255, 0.08);
  width: 260px;
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
  opacity: 0.6;
}

.toggle-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 4px;
  color: rgba(255, 255, 255, 0.7);
  padding: 6px 12px;
  margin-right: 12px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.toggle-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.9);
}

.toggle-btn.active {
  background: rgba(102, 126, 234, 0.2);
  border-color: rgba(102, 126, 234, 0.5);
  color: #667eea;
}

/* Group operation style */
.group-info {
  display: flex;
  align-items: center;
}

.group-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
  margin-right: 16px;
}

.reset-btn {
  background: rgba(244, 67, 54, 0.1);
  border: 1px solid rgba(244, 67, 54, 0.3);
  border-radius: 4px;
  color: #ef5350;
  padding: 4px 8px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.reset-btn:hover:not(:disabled) {
  background: rgba(244, 67, 54, 0.2);
  border-color: rgba(244, 67, 54, 0.5);
}

.reset-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Import/Export action style */
.import-export-actions {
  display: flex;
  gap: 8px;
  margin-right: 16px;
}

.action-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 4px;
  color: rgba(255, 255, 255, 0.8);
  padding: 6px 10px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.95);
  border-color: rgba(255, 255, 255, 0.25);
}
</style>
