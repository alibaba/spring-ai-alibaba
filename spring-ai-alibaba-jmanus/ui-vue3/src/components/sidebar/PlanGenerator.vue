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
  <div class="config-section">
    <div class="section-header">
      <Icon icon="carbon:generate" width="16" />
      <span>{{ t('sidebar.planGenerator') }}</span>
    </div>
    <div class="generator-content">
      <textarea
        v-model="generatorPrompt"
        class="prompt-input"
        :placeholder="t('sidebar.generatorPlaceholder')"
        rows="3"
      ></textarea>
      <div class="generator-actions">
        <button
          class="btn btn-primary btn-sm"
          @click="handleGeneratePlan"
          :disabled="isGenerating || !generatorPrompt.trim()"
        >
          <Icon
            :icon="isGenerating ? 'carbon:circle-dash' : 'carbon:generate'"
            width="14"
            :class="{ spinning: isGenerating }"
          />
          {{ isGenerating ? t('sidebar.generating') : t('sidebar.generatePlan') }}
        </button>
        <button
          class="btn btn-secondary btn-sm"
          @click="handleUpdatePlan"
          :disabled="
            isGenerating ||
            !generatorPrompt.trim() ||
            !jsonContent.trim()
          "
        >
          <Icon icon="carbon:edit" width="14" />
          {{ t('sidebar.updatePlan') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Icon } from '@iconify/vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

// Props
interface Props {
  generatorPrompt: string
  jsonContent: string
  isGenerating: boolean
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  generatePlan: []
  updatePlan: []
  updateGeneratorPrompt: [prompt: string]
}>()

// Local state
const generatorPrompt = ref(props.generatorPrompt)

// Watch for changes in generator prompt
watch(() => props.generatorPrompt, (newValue) => {
  generatorPrompt.value = newValue
})

watch(generatorPrompt, (newValue) => {
  emit('updateGeneratorPrompt', newValue)
})

// Methods
const handleGeneratePlan = () => {
  emit('generatePlan')
}

const handleUpdatePlan = () => {
  emit('updatePlan')
}

// Expose methods for parent component
defineExpose({
  generatorPrompt
})
</script>

<style scoped>
.config-section {
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px;
}

.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  color: #667eea;
  font-size: 13px;
  font-weight: 600;
  gap: 8px;
}

.generator-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.prompt-input {
  width: 100%;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  color: white;
  font-size: 12px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  padding: 8px;
  resize: vertical;
  min-height: 100px;

  &:focus {
    outline: none;
    border-color: #667eea;
    box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
  }

  &::placeholder {
    color: rgba(255, 255, 255, 0.4);
  }
}

.generator-actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s ease;

  &.btn-sm {
    padding: 4px 8px;
    font-size: 11px;
  }

  &.btn-primary {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
    }
  }

  &.btn-secondary {
    background: rgba(255, 255, 255, 0.1);
    color: rgba(255, 255, 255, 0.8);
    border: 1px solid rgba(255, 255, 255, 0.2);

    &:hover:not(:disabled) {
      background: rgba(255, 255, 255, 0.2);
      color: white;
    }
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none !important;
    box-shadow: none !important;
  }

  .spinning {
    animation: spin 1s linear infinite;
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
