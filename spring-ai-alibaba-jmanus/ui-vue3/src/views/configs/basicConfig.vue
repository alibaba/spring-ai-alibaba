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
        <h2>{{ t('config.basicConfig.title') }}</h2>
        <div class="config-stats">
          <span class="stat-item">
            <span class="stat-label">总配置项:</span>
            <span class="stat-value">{{ configStats.total }}</span>
          </span>
          <span class="stat-item" v-if="configStats.modified > 0">
            <span class="stat-label">已修改:</span>
            <span class="stat-value modified">{{ configStats.modified }}</span>
          </span>
        </div>
      </div>
      <div class="header-right">
        <div class="import-export-actions">
          <button @click="exportConfigs" class="action-btn" title="导出配置">
            📤
          </button>
          <label class="action-btn" title="导入配置">
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

    <!-- 加载状态 -->
    <div v-if="initialLoading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>{{ $t('config.loading') }}</p>
    </div>

    <!-- 配置组 -->
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
        
        <!-- 子组 -->
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
                <h4 class="sub-group-title">{{ subGroup.displayName }}</h4>
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
                <!-- 布尔类型配置项 (CHECKBOX/BOOLEAN) -->
                <template v-if="item.inputType === 'BOOLEAN' || item.inputType === 'CHECKBOX'">
                  <div class="config-item-content vertical-layout">
                    <div class="config-item-info">
                      <div class="config-item-header">
                        <label class="config-label">
                          {{ item.description || item.displayName }}
                          <span class="type-badge boolean">{{ item.inputType === 'CHECKBOX' ? $t('config.types.checkbox') : $t('config.types.boolean') }}</span>
                          <span v-if="originalConfigValues.get(item.id) !== item.configValue" class="modified-badge">{{ $t('config.modified') }}</span>
                        </label>
                        <span class="config-key" :title="item.configKey">{{ item.configKey }}</span>
                      </div>
                    </div>
                    <div class="config-control">
                      <!-- 如果有定义 options，显示为选择框 -->
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
                      <!-- 否则显示为开关 -->
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

                <!-- 选择类型配置项 -->
                <template v-else-if="item.inputType === 'SELECT'">
                  <div class="config-item-content vertical-layout">
                    <div class="config-item-info">
                      <div class="config-item-header">
                        <label class="config-label">
                          {{ item.description || item.displayName }}
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

                <!-- 多行文本类型配置项 -->
                <template v-else-if="item.inputType === 'TEXTAREA'">
                  <div class="config-item-content vertical-layout">
                    <div class="config-item-info">
                      <div class="config-item-header">
                        <label class="config-label">
                          {{ item.description || item.displayName }}
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

                <!-- 数值类型配置项 -->
                <template v-else-if="item.inputType === 'NUMBER'">
                  <div class="config-item-content vertical-layout">
                    <div class="config-item-info">
                      <div class="config-item-header">
                        <label class="config-label">
                          {{ item.description || item.displayName }}
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

                <!-- 字符串类型配置项 (STRING/TEXT) -->
                <template v-else>
                  <div class="config-item-content vertical-layout">
                    <div class="config-item-info">
                      <div class="config-item-header">
                        <label class="config-label">
                          {{ item.description || item.displayName }}
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

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <p>{{ $t('config.notFound') }}</p>
    </div>

    <!-- 消息提示 -->
    <transition name="message-fade">
      <div v-if="message.show" :class="['message-toast', message.type]">
        {{ message.text }}
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import Switch from '@/components/switch/index.vue'
import Flex from '@/components/flex/index.vue'
import { AdminApiService, type ConfigItem } from '@/api/admin-api-service'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

// 定义扩展的配置项接口
interface ExtendedConfigItem extends ConfigItem {
  displayName: string
  min?: number
  max?: number
}

// 定义配置子组接口
interface ConfigSubGroup {
  name: string
  displayName: string
  items: ExtendedConfigItem[]
}

// 定义配置组接口
interface ConfigGroup {
  name: string
  displayName: string
  subGroups: ConfigSubGroup[]
}

// 响应式数据
const initialLoading = ref(true)
const loading = ref(false)
const configGroups = ref<ConfigGroup[]>([])
const originalConfigValues = ref<Map<number, string>>(new Map())

// 子组折叠状态
const collapsedSubGroups = ref<Set<string>>(new Set())

// 消息提示
const message = reactive({
  show: false,
  text: '',
  type: 'success' as 'success' | 'error'
})

// 搜索过滤状态
const searchQuery = ref('')



// 配置项显示名称映射
const CONFIG_DISPLAY_NAMES: Record<string, string> = {
  // 智能体设置
  'maxSteps': '智能体执行最大步数',
  'resetAllAgents': '重置所有agent',
  'maxMemory': '能记住的最大消息数',
  
  // 浏览器设置
  'headlessBrowser': '是否使用无头浏览器模式',
  'browserTimeout': t('config.basicConfig.browserTimeout'),
  'browserDebug': '浏览器debug模式',
  
  // 交互设置
  'autoOpenBrowser': '启动时自动打开浏览器',
  'consoleInteractive': '启用控制台交互模式',
  
  // 系统设置
  'systemName': '系统名称',
  'language': '默认语言',
  'maxThreads': '最大线程数',
  'timeoutSeconds': t('config.basicConfig.requestTimeout')
}

// 组显示名称映射
const GROUP_DISPLAY_NAMES: Record<string, string> = {
  'manus': '智能体设置',
  'browser': '浏览器设置', 
  'interaction': '交互设置',
  'system': '系统设置',
  'performance': '性能设置'
}

// 组图标映射
const GROUP_ICONS: Record<string, string> = {
  'manus': '🤖',
  'browser': '🌐',
  'interaction': '🖥️',
  'system': '⚙️',
  'performance': '⚡'
}

// 子组显示名称映射
const SUB_GROUP_DISPLAY_NAMES: Record<string, string> = {
  'agent': '智能体设置',
  'browser': '浏览器设置',
  'interaction': '交互设置',
  'system': '系统设置',
  'performance': '性能设置',
  'general': '常规设置'
}

// 计算属性：是否有修改
const hasChanges = computed(() => {
  return configGroups.value.some(group => 
    group.subGroups.some(subGroup =>
      subGroup.items.some(item => 
        originalConfigValues.value.get(item.id) !== item.configValue
      )
    )
  )
})

// 工具函数：获取布尔值
const getBooleanValue = (value: string): boolean => {
  return value === 'true'
}

// 工具函数：获取数值
const getNumberValue = (value: string): number => {
  return parseFloat(value) || 0
}

// 工具函数：获取配置项的最小值
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

// 工具函数：获取配置项的最大值  
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

// 工具函数：获取选项值
const getOptionValue = (option: string | { value: string; label: string }): string => {
  return typeof option === 'string' ? option : option.value
}

// 工具函数：获取选项标签
const getOptionLabel = (option: string | { value: string; label: string }): string => {
  return typeof option === 'string' ? option : option.label
}

// 工具函数：处理布尔值更新（支持选项映射）
const handleBooleanUpdate = (item: ExtendedConfigItem, newValue: string | boolean): string => {
  // 如果是直接的布尔值（来自开关）
  if (typeof newValue === 'boolean') {
    return newValue.toString()
  }
  
  // 如果是字符串（来自选择框）
  if (typeof newValue === 'string') {
    // 处理可能的选项映射（例如 "是" -> "true", "否" -> "false"）
    if (item.options && item.options.length > 0) {
      // 查找匹配的选项
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
  
  // fallback 处理
  return String(newValue)
}

// 更新配置值
const updateConfigValue = (item: ExtendedConfigItem, value: any, autoSave: boolean = false) => {
  let stringValue: string
  
  // 根据输入类型处理值
  if (item.inputType === 'BOOLEAN' || item.inputType === 'CHECKBOX') {
    stringValue = handleBooleanUpdate(item, value)
  } else {
    stringValue = String(value)
  }
  
  if (item.configValue !== stringValue) {
    item.configValue = stringValue
    item._modified = true
    
    // 如果是非文本输入类型（如switch、select），自动保存
    if (autoSave || item.inputType === 'BOOLEAN' || item.inputType === 'CHECKBOX' || item.inputType === 'SELECT') {
      debouncedSave()
    }
  }
}

// 防抖保存
let saveTimeout: number | null = null
const debouncedSave = () => {
  if (saveTimeout) {
    clearTimeout(saveTimeout)
  }
  saveTimeout = window.setTimeout(() => {
    saveAllConfigs()
  }, 500)
}

// 显示消息
const showMessage = (text: string, type: 'success' | 'error' = 'success') => {
  message.text = text
  message.type = type
  message.show = true
  
  setTimeout(() => {
    message.show = false
  }, 3000)
}

// 加载所有配置组
const loadAllConfigs = async () => {
  try {
    initialLoading.value = true
    
    // 定义已知的配置组（避免依赖后端的 getAllGroups 接口）
    const knownGroups = ['manus', 'browser', 'interaction', 'system', 'performance']
    
    // 加载每个组的配置
    const groupPromises = knownGroups.map(async (groupName: string) => {
      try {
        const items = await AdminApiService.getConfigsByGroup(groupName)
        
        // 如果该组没有配置项，跳过
        if (items.length === 0) {
          return null
        }
        
        // 为每个配置项设置显示名称（优先使用description）
        const processedItems: ExtendedConfigItem[] = items.map(item => ({
          ...item,
          displayName: item.description || CONFIG_DISPLAY_NAMES[item.configKey] || item.configKey,
          min: getConfigMin(item.configKey),
          max: getConfigMax(item.configKey)
        }))
        
        // 缓存原始值
        processedItems.forEach(item => {
          originalConfigValues.value.set(item.id, item.configValue)
        })
        
        // 按子组分组
        const subGroupsMap = new Map<string, ExtendedConfigItem[]>()
        
        processedItems.forEach(item => {
          const subGroupName = item.configSubGroup || 'general'
          if (!subGroupsMap.has(subGroupName)) {
            subGroupsMap.set(subGroupName, [])
          }
          subGroupsMap.get(subGroupName)!.push(item)
        })
        
        // 转换为子组数组
        const subGroups: ConfigSubGroup[] = Array.from(subGroupsMap.entries()).map(([name, items]) => ({
          name,
          displayName: SUB_GROUP_DISPLAY_NAMES[name] || name,
          items
        }))
        
        return {
          name: groupName,
          displayName: GROUP_DISPLAY_NAMES[groupName] || groupName,
          subGroups
        }
      } catch (error) {
        console.warn(`加载配置组 ${groupName} 失败，跳过:`, error)
        return null
      }
    })
    
    const results = await Promise.all(groupPromises)
    
    // 过滤掉空的配置组
    configGroups.value = results.filter(group => group !== null) as ConfigGroup[]
    
    console.log('配置加载完成:', configGroups.value)
  } catch (error) {
    console.error('加载配置失败:', error)
    showMessage(t('config.basicConfig.loadConfigFailed'), 'error')
  } finally {
    initialLoading.value = false
  }
}

// 保存所有配置
const saveAllConfigs = async () => {
  if (loading.value || !hasChanges.value) return
  
  try {
    loading.value = true
    
    // 收集所有修改的配置项
    const allModifiedConfigs: ConfigItem[] = []
    
    configGroups.value.forEach(group => {
      group.subGroups.forEach(subGroup => {
        const modifiedItems = subGroup.items.filter(item => item._modified)
        allModifiedConfigs.push(...modifiedItems)
      })
    })
    
    if (allModifiedConfigs.length === 0) {
      showMessage('没有需要保存的修改')
      return
    }
    
    // 批量保存
    const result = await AdminApiService.batchUpdateConfigs(allModifiedConfigs)
    
    if (result.success) {
      // 更新原始值缓存
      allModifiedConfigs.forEach(item => {
        originalConfigValues.value.set(item.id, item.configValue)
        item._modified = false
      })
      
      showMessage('配置保存成功')
    } else {
      showMessage(result.message || '保存失败', 'error')
    }
  } catch (error) {
    console.error('保存配置失败:', error)
    showMessage(t('config.basicConfig.saveFailed'), 'error')
  } finally {
    loading.value = false
  }
}

// 重置组配置
const resetGroupConfigs = async (groupName: string) => {
  const confirmed = confirm(`确定要重置 "${GROUP_DISPLAY_NAMES[groupName] || groupName}" 组的所有配置吗？`)
  if (!confirmed) return
  
  try {
    loading.value = true
    
    // 找到目标组
    const targetGroup = configGroups.value.find(g => g.name === groupName)
    if (!targetGroup) return
    
    // 收集该组的所有配置项
    const groupConfigs: ConfigItem[] = []
    targetGroup.subGroups.forEach(subGroup => {
      subGroup.items.forEach(item => {
        // 这里应该调用API获取默认值，现在先简单处理
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
      showMessage('该组配置已是默认值')
      return
    }
    
    // 批量更新
    const result = await AdminApiService.batchUpdateConfigs(groupConfigs)
    
    if (result.success) {
      // 重新加载配置
      await loadAllConfigs()
      showMessage(`成功重置 ${groupConfigs.length} 项配置`)
    } else {
      showMessage(result.message || '重置失败', 'error')
    }
  } catch (error) {
    console.error('重置组配置失败:', error)
    showMessage(t('config.basicConfig.resetFailed'), 'error')
  } finally {
    loading.value = false
  }
}

// 获取配置项的默认值
const getDefaultValueForKey = (configKey: string): string => {
  // 这里应该有一个默认值映射表，现在先返回基本默认值
  const defaults: Record<string, string> = {
    'systemName': 'JTaskPilot',
    'language': 'zh-CN',
    'maxThreads': '8',
    'timeoutSeconds': '60',
    'autoOpenBrowser': 'false',
    'headlessBrowser': 'true',
    'maxMemory': '1000',
    // 可以根据需要添加更多默认值
  }
  
  return defaults[configKey] || ''
}

// 子组折叠切换
const toggleSubGroup = (groupName: string, subGroupName: string) => {
  const key = `${groupName}-${subGroupName}`
  if (collapsedSubGroups.value.has(key)) {
    collapsedSubGroups.value.delete(key)
  } else {
    collapsedSubGroups.value.add(key)
  }
}

// 检查子组是否折叠
const isSubGroupCollapsed = (groupName: string, subGroupName: string): boolean => {
  return collapsedSubGroups.value.has(`${groupName}-${subGroupName}`)
}

// 计算配置统计
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

// 过滤配置组
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



// 导出配置
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
    
    showMessage('配置导出成功')
  } catch (error) {
    console.error('导出配置失败:', error)
    showMessage('导出失败', 'error')
  }
}

// 导入配置
const importConfigs = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  
  if (!file) return
  
  const reader = new FileReader()
  reader.onload = async (e) => {
    try {
      const importData = JSON.parse(e.target?.result as string)
      
      if (!importData.configs) {
        throw new Error('无效的配置文件格式')
      }
      
      const confirmed = confirm(`确定要导入配置吗？这将覆盖当前配置。`)
      if (!confirmed) return
      
      loading.value = true
      
      // 准备要更新的配置项
      const configsToUpdate: ConfigItem[] = []
      
      configGroups.value.forEach(group => {
        group.subGroups.forEach(subGroup => {
          subGroup.items.forEach(item => {
            if (importData.configs.hasOwnProperty(item.configKey)) {
              configsToUpdate.push({
                ...item,
                configValue: importData.configs[item.configKey]
              })
            }
          })
        })
      })
      
      if (configsToUpdate.length === 0) {
        showMessage('没有找到可导入的配置项')
        return
      }
      
      // 批量更新
      const result = await AdminApiService.batchUpdateConfigs(configsToUpdate)
      
      if (result.success) {
        await loadAllConfigs()
        showMessage(`成功导入 ${configsToUpdate.length} 项配置`)
      } else {
        showMessage(result.message || '导入失败', 'error')
      }
    } catch (error) {
      console.error('导入配置失败:', error)
      showMessage(t('config.basicConfig.importFailed'), 'error')
    } finally {
      loading.value = false
      // 清空输入框
      input.value = ''
    }
  }
  
  reader.readAsText(file)
}

// 组件挂载时加载配置
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

/* 垂直布局样式 */
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

/* 垂直布局中的标签样式 */
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

/* 垂直布局中的配置键样式 */
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

/* 垂直布局中的输入控件样式调整 */
.vertical-layout .config-control {
  min-width: auto;
  max-width: 400px; /* 限制最大宽度，避免输入框过宽 */
}

/* 输入框样式增强 */
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

/* 子组样式 */
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

/* 头部样式增强 */
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

/* 组操作样式 */
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

/* 导入/导出动作样式 */
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
