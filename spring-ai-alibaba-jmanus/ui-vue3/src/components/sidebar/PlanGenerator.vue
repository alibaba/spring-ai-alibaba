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
      <!-- Plan Type Selection -->
      <div class="plan-type-selection">
        <label class="form-label">{{ t('sidebar.planType') }}</label>
        <select v-model="selectedPlanType" class="form-select">
          <option value="simple">{{ t('sidebar.simplePlan') }}</option>
          <option value="dynamic_agent">{{ t('sidebar.dynamicAgentPlan') }}</option>
        </select>
      </div>
      
      <!-- Dynamic Agent Plan - Show readonly instruction textarea -->
      <div v-if="selectedPlanType === 'dynamic_agent'" class="dynamic-agent-instruction">
        <textarea
          readonly
          class="instruction-textarea"
          rows="2"
          :value="t('sidebar.dynamicAgentInstruction')"
        ></textarea>
      </div>
      
      <!-- Simple Plan - Show editable prompt and actions -->
      <template v-else>
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
      </template>
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
  planType?: string
}

const props = withDefaults(defineProps<Props>(), {
  planType: 'dynamic_agent'
})

// Emits
const emit = defineEmits<{
  generatePlan: []
  updatePlan: []
  updateGeneratorPrompt: [prompt: string]
  updatePlanType: [planType: string]
}>()

// Local state
const generatorPrompt = ref(props.generatorPrompt)
const selectedPlanType = ref(props.planType)

// Watch for changes in generator prompt
watch(() => props.generatorPrompt, (newValue) => {
  generatorPrompt.value = newValue
})

watch(generatorPrompt, (newValue) => {
  emit('updateGeneratorPrompt', newValue)
})

// Watch for changes in plan type
watch(() => props.planType, (newValue) => {
  selectedPlanType.value = newValue
})

watch(selectedPlanType, (newValue) => {
  emit('updatePlanType', newValue)
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
  gap: 12px;
}

.plan-type-selection {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 10px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.form-select {
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.3);
  color: white;
  font-size: 11px;
  font-family: inherit;
  transition: all 0.2s ease;
  cursor: pointer;
}

.form-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.form-select option {
  background: rgba(0, 0, 0, 0.8);
  color: white;
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

.dynamic-agent-instruction {
  .instruction-textarea {
    width: 100%;
    background: rgba(102, 126, 234, 0.1);
    border: 1px solid rgba(102, 126, 234, 0.3);
    border-radius: 6px;
    color: rgba(255, 255, 255, 0.9);
    font-size: 12px;
    font-family: inherit;
    padding: 12px;
    resize: none;
    cursor: default;
    line-height: 1.5;

    &:focus {
      outline: none;
      border-color: rgba(102, 126, 234, 0.5);
    }
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
