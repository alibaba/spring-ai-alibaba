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
  <div class="search-box" :class="{ focused: isFocused, loading: isLoading }">
    <div class="search-input-container">
      <i class="search-icon bi bi-search"></i>
      <input
        ref="inputRef"
        type="text"
        class="search-input"
        :placeholder="placeholder"
        :value="modelValue"
        :disabled="disabled"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
        @keydown="handleKeydown"
        @keyup.enter="handleSearch"
      />
      <div v-if="isLoading" class="loading-spinner">
        <div class="spinner"></div>
      </div>
      <button
        v-if="modelValue && clearable"
        class="clear-button"
        @click="handleClear"
        type="button"
      >
        <i class="bi bi-x"></i>
      </button>
      <button
        v-if="showSearchButton"
        class="search-button"
        :disabled="disabled || isLoading"
        @click="handleSearch"
        type="button"
      >
        <i class="bi bi-search"></i>
      </button>
    </div>

    <!-- 搜索建议下拉框 -->
    <Transition name="dropdown">
      <div v-if="showSuggestions && suggestions.length > 0" class="suggestions-dropdown">
        <div
          v-for="(suggestion, index) in suggestions"
          :key="index"
          class="suggestion-item"
          :class="{ active: index === activeSuggestionIndex }"
          @click="selectSuggestion(suggestion)"
          @mouseenter="activeSuggestionIndex = index"
        >
          <i class="suggestion-icon bi bi-clock-history"></i>
          <span class="suggestion-text">{{ suggestion }}</span>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useDebounce } from '../utils/debounce.js'

export default {
  name: 'SearchBox',
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: '请输入搜索内容...'
    },
    disabled: {
      type: Boolean,
      default: false
    },
    clearable: {
      type: Boolean,
      default: true
    },
    showSearchButton: {
      type: Boolean,
      default: false
    },
    suggestions: {
      type: Array,
      default: () => []
    },
    debounceDelay: {
      type: Number,
      default: 300
    },
    isLoading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:modelValue', 'search', 'input', 'focus', 'blur', 'clear'],
  setup(props, { emit }) {
    const inputRef = ref(null)
    const isFocused = ref(false)
    const activeSuggestionIndex = ref(-1)

    const showSuggestions = computed(() => {
      return isFocused.value && props.modelValue && props.suggestions.length > 0
    })

    // 防抖处理输入
    const debouncedInput = useDebounce((value) => {
      emit('input', value)
    }, props.debounceDelay)

    const handleInput = (event) => {
      const value = event.target.value
      emit('update:modelValue', value)
      debouncedInput(value)
      activeSuggestionIndex.value = -1
    }

    const handleFocus = () => {
      isFocused.value = true
      emit('focus')
    }

    const handleBlur = () => {
      // 延迟隐藏建议，以便点击建议项
      setTimeout(() => {
        isFocused.value = false
        activeSuggestionIndex.value = -1
        emit('blur')
      }, 200)
    }

    const handleKeydown = (event) => {
      if (!showSuggestions.value) return

      switch (event.key) {
        case 'ArrowDown':
          event.preventDefault()
          activeSuggestionIndex.value = Math.min(
            activeSuggestionIndex.value + 1,
            props.suggestions.length - 1
          )
          break
        case 'ArrowUp':
          event.preventDefault()
          activeSuggestionIndex.value = Math.max(
            activeSuggestionIndex.value - 1,
            -1
          )
          break
        case 'Enter':
          event.preventDefault()
          if (activeSuggestionIndex.value >= 0) {
            selectSuggestion(props.suggestions[activeSuggestionIndex.value])
          } else {
            handleSearch()
          }
          break
        case 'Escape':
          isFocused.value = false
          activeSuggestionIndex.value = -1
          inputRef.value?.blur()
          break
      }
    }

    const handleSearch = () => {
      if (props.disabled || props.isLoading) return
      emit('search', props.modelValue)
      isFocused.value = false
    }

    const handleClear = () => {
      emit('update:modelValue', '')
      emit('clear')
      nextTick(() => {
        inputRef.value?.focus()
      })
    }

    const selectSuggestion = (suggestion) => {
      emit('update:modelValue', suggestion)
      emit('search', suggestion)
      isFocused.value = false
      activeSuggestionIndex.value = -1
    }

    const focus = () => {
      inputRef.value?.focus()
    }

    const blur = () => {
      inputRef.value?.blur()
    }

    // 点击外部关闭建议
    const handleClickOutside = (event) => {
      if (!inputRef.value?.contains(event.target)) {
        isFocused.value = false
        activeSuggestionIndex.value = -1
      }
    }

    onMounted(() => {
      document.addEventListener('click', handleClickOutside)
    })

    onUnmounted(() => {
      document.removeEventListener('click', handleClickOutside)
    })

    return {
      inputRef,
      isFocused,
      activeSuggestionIndex,
      showSuggestions,
      handleInput,
      handleFocus,
      handleBlur,
      handleKeydown,
      handleSearch,
      handleClear,
      selectSuggestion,
      focus,
      blur
    }
  }
}
</script>

<style scoped>
.search-box {
  position: relative;
  width: 100%;
}

.search-input-container {
  position: relative;
  display: flex;
  align-items: center;
  background: white;
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  transition: all 0.3s ease;
  overflow: hidden;
}

.search-box.focused .search-input-container {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.search-box.loading .search-input-container {
  border-color: #1890ff;
}

.search-icon {
  position: absolute;
  left: 12px;
  color: #8c8c8c;
  font-size: 16px;
  z-index: 1;
  transition: color 0.3s ease;
}

.search-box.focused .search-icon {
  color: #1890ff;
}

.search-input {
  flex: 1;
  padding: 12px 16px 12px 40px;
  border: none;
  outline: none;
  font-size: 14px;
  background: transparent;
  color: #262626;
}

.search-input::placeholder {
  color: #bfbfbf;
}

.search-input:disabled {
  background: #f5f5f5;
  color: #bfbfbf;
  cursor: not-allowed;
}

.loading-spinner {
  position: absolute;
  right: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #f0f0f0;
  border-top-color: #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.clear-button,
.search-button {
  position: absolute;
  right: 8px;
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  color: #8c8c8c;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.clear-button:hover,
.search-button:hover {
  background: #f5f5f5;
  color: #262626;
}

.search-button {
  background: #1890ff;
  color: white;
  right: 4px;
}

.search-button:hover:not(:disabled) {
  background: #40a9ff;
}

.search-button:disabled {
  background: #f5f5f5;
  color: #bfbfbf;
  cursor: not-allowed;
}

.suggestions-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #d9d9d9;
  border-top: none;
  border-radius: 0 0 8px 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  max-height: 200px;
  overflow-y: auto;
}

.suggestion-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s ease;
  border-bottom: 1px solid #f0f0f0;
}

.suggestion-item:last-child {
  border-bottom: none;
}

.suggestion-item:hover,
.suggestion-item.active {
  background: #f5f5f5;
}

.suggestion-icon {
  color: #8c8c8c;
  margin-right: 8px;
  font-size: 14px;
}

.suggestion-text {
  flex: 1;
  color: #262626;
  font-size: 14px;
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-input {
    padding: 10px 14px 10px 36px;
    font-size: 16px; /* 防止iOS缩放 */
  }
  
  .search-icon {
    left: 10px;
  }
}
</style>
