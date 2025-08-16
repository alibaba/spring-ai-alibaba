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
  <div class="init-container">
    <div class="init-card">
      <!-- Header -->
      <div class="init-header">
        <div class="logo">
          <h1><Icon icon="carbon:bot" class="logo-icon" /> JManus</h1>
        </div>
        <h2>{{ currentStep === 1 ? $t('init.welcomeStep') : $t('init.welcome') }}</h2>
        <p class="description">{{ currentStep === 1 ? $t('init.languageStepDescription') : $t('init.description') }}</p>
      </div>

      <!-- Step Indicator -->
      <div class="step-indicator">
        <div class="step" :class="{ active: currentStep >= 1, completed: currentStep > 1 }">
          <span class="step-number">1</span>
          <span class="step-label">{{ $t('init.stepLanguage') }}</span>
        </div>
        <div class="step-divider"></div>
        <div class="step" :class="{ active: currentStep >= 2, completed: currentStep > 2 }">
          <span class="step-number">2</span>
          <span class="step-label">{{ $t('init.stepModel') }}</span>
        </div>
      </div>

      <!-- Step 1: Language Selection -->
      <div v-if="currentStep === 1" class="init-form language-selection">
        <div class="form-group">
          <label class="form-label">{{ $t('init.selectLanguageLabel') }}</label>
          <div class="language-options">
            <label class="language-option" :class="{ active: selectedLanguage === 'zh' }">
              <input
                type="radio"
                v-model="selectedLanguage"
                value="zh"
              />
              <span class="language-content">
                <span class="language-flag">
                  <Icon icon="circle-flags:cn" />
                </span>
                <span class="language-text">
                  <strong>{{ $t('language.zh') }}</strong>
                  <small>{{ $t('init.simplifiedChinese') }}</small>
                </span>
              </span>
            </label>
            <label class="language-option" :class="{ active: selectedLanguage === 'en' }">
              <input
                type="radio"
                v-model="selectedLanguage"
                value="en"
              />
              <span class="language-content">
                <span class="language-flag">
                  <Icon icon="circle-flags:us" />
                </span>
                <span class="language-text">
                  <strong>English</strong>
                  <small>English (US)</small>
                </span>
              </span>
            </label>
          </div>
        </div>

        <div class="form-actions single">
          <button
            type="button"
            class="submit-btn"
            :disabled="!selectedLanguage"
            @click="goToNextStep"
          >
            {{ $t('init.continueToModel') }}
          </button>
        </div>
      </div>

      <!-- Step 2: Model Configuration -->
      <div v-if="currentStep === 2" class="init-form">
        <form @submit.prevent="handleSubmit">
          <!-- Configuration Mode Selection -->
          <div class="form-group">
            <label class="form-label">{{ $t('init.configModeLabel') }}</label>
            <div class="config-mode-selection">
              <label class="radio-option" :class="{ active: form.configMode === 'dashscope' }">
                <input
                  type="radio"
                  v-model="form.configMode"
                  value="dashscope"
                  @change="onConfigModeChange"
                />
                <span class="radio-text">
                  <strong>{{ $t('init.dashscopeMode') }}</strong>
                  <small>{{ $t('init.dashscopeModeDesc') }}</small>
                </span>
              </label>
              <label class="radio-option" :class="{ active: form.configMode === 'custom' }">
                <input
                  type="radio"
                  v-model="form.configMode"
                  value="custom"
                  @change="onConfigModeChange"
                />
                <span class="radio-text">
                  <strong>{{ $t('init.customMode') }}</strong>
                  <small>{{ $t('init.customModeDesc') }}</small>
                </span>
              </label>
            </div>
          </div>

          <!-- DashScope API Key Mode -->
          <div v-if="form.configMode === 'dashscope'" class="form-group">
            <label for="apiKey" class="form-label">
              {{ $t('init.apiKeyLabel') }}
              <span class="required">*</span>
            </label>
            <div class="api-key-input-container">
              <input
                id="apiKey"
                v-model="form.apiKey"
                :type="showDashscopeApiKey ? 'text' : 'password'"
                class="form-input"
                :placeholder="$t('init.apiKeyPlaceholder')"
                :disabled="loading"
                required
              />
              <button
                type="button"
                class="api-key-toggle-btn"
                @click="showDashscopeApiKey = !showDashscopeApiKey"
                :title="showDashscopeApiKey ? $t('init.hideApiKey') : $t('init.showApiKey')"
              >
                <Icon v-if="showDashscopeApiKey" icon="carbon:view" />
                <Icon v-else icon="carbon:view-off" />
              </button>
            </div>
            <div class="form-hint">
              {{ $t('init.apiKeyHint') }}
              <a
                href="https://bailian.console.aliyun.com/?tab=model#/api-key"
                target="_blank"
                class="help-link"
              >
                {{ $t('init.getApiKey') }}
              </a>
            </div>
          </div>

          <!-- Custom OpenAI Compatible Mode -->
          <div v-if="form.configMode === 'custom'" class="custom-config-section">
            <div class="form-group">
              <label for="baseUrl" class="form-label">
                {{ $t('init.baseUrlLabel') }}
                <span class="required">*</span>
              </label>
              <input
                id="baseUrl"
                v-model="form.baseUrl"
                type="url"
                class="form-input"
                :placeholder="$t('init.baseUrlPlaceholder')"
                :disabled="loading"
                required
              />
              <div class="form-hint">{{ $t('init.baseUrlHint') }}</div>
            </div>

            <div class="form-group">
              <label for="customApiKey" class="form-label">
                {{ $t('init.customApiKeyLabel') }}
                <span class="required">*</span>
              </label>
              <div class="api-key-input-container">
                <input
                  id="customApiKey"
                  v-model="form.apiKey"
                  :type="showCustomApiKey ? 'text' : 'password'"
                  class="form-input"
                  :placeholder="$t('init.customApiKeyPlaceholder')"
                  :disabled="loading"
                  required
                />
                <button
                  type="button"
                  class="api-key-toggle-btn"
                  @click="showCustomApiKey = !showCustomApiKey"
                  :title="showCustomApiKey ? $t('init.hideApiKey') : $t('init.showApiKey')"
                >
                  <Icon v-if="showCustomApiKey" icon="carbon:view" />
                  <Icon v-else icon="carbon:view-off" />
                </button>
              </div>
            </div>

            <div class="form-group">
              <label for="modelName" class="form-label">
                {{ $t('init.modelNameLabel') }}
                <span class="required">*</span>
              </label>
              <input
                id="modelName"
                v-model="form.modelName"
                type="text"
                class="form-input"
                :placeholder="$t('init.modelNamePlaceholder')"
                :disabled="loading"
                required
              />
              <div class="form-hint">{{ $t('init.modelNameHint') }}</div>
            </div>

            <div class="form-group">
              <label for="modelDisplayName" class="form-label">{{ $t('init.modelDisplayNameLabel') }}</label>
              <input
                id="modelDisplayName"
                v-model="form.modelDisplayName"
                type="text"
                class="form-input"
                :placeholder="$t('init.modelDisplayNamePlaceholder')"
                :disabled="loading"
              />
            </div>

            <div class="form-group">
              <label for="completionsPath" class="form-label">{{ $t('init.completionsPath') }}</label>
              <input
                  id="modelDisplayName"
                  v-model="form.completionsPath"
                  type="text"
                  class="form-input"
                  :placeholder="$t('init.completionsPathPlaceholder')"
                  :disabled="loading"
              />
            </div>
          </div>

          <div class="form-actions">
            <button
              type="button"
              class="back-btn"
              @click="goToPreviousStep"
              :disabled="loading"
            >
              {{ $t('init.back') }}
            </button>
            <button
              type="submit"
              class="submit-btn"
              :disabled="loading || !isFormValid"
            >
              <span v-if="loading" class="loading-spinner"></span>
              {{ loading ? $t('init.saving') : $t('init.saveAndContinue') }}
            </button>
          </div>
        </form>
      </div>

      <!-- Error message -->
      <transition name="error-fade">
        <div v-if="error" class="error-message">
          {{ error }}
        </div>
      </transition>

      <!-- Success message -->
      <transition name="success-fade">
        <div v-if="success" class="success-message">
          {{ $t('init.successMessage') }}
        </div>
      </transition>
    </div>

    <!-- Background animation -->
    <div class="background-animation">
      <div class="floating-shape" v-for="i in 6" :key="i"></div>
    </div>

    <!-- Background effects -->
    <div class="background-effects">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'
import { LlmCheckService } from '@/utils/llm-check'
import { changeLanguageWithAgentReset, LOCAL_STORAGE_LOCALE } from '@/base/i18n'

const { t, locale } = useI18n()
const router = useRouter()

// Step management
const currentStep = ref(1)
const selectedLanguage = ref(locale.value || 'en')

// Form state
const form = ref({
  configMode: 'dashscope', // Default to DashScope mode
  apiKey: '',
  baseUrl: '',
  modelName: '',
  modelDisplayName: '',
  completionsPath: ''
})

const loading = ref(false)
const error = ref('')
const success = ref(false)

// API key visibility state
const showDashscopeApiKey = ref(false)
const showCustomApiKey = ref(false)

// Computed properties
const isFormValid = computed(() => {
  if (!form.value.apiKey.trim()) {
    return false
  }

  if (form.value.configMode === 'custom') {
    return form.value.baseUrl.trim() && form.value.modelName.trim()
  }

  return true
})

// Methods
const goToNextStep = async () => {
  if (selectedLanguage.value) {
    try {
      loading.value = true

      // Use changeLanguageWithAgentReset function to switch language and reset agents
      await changeLanguageWithAgentReset(selectedLanguage.value)

      // Move to next step
      currentStep.value = 2
    } catch (err: any) {
      console.warn('Failed to switch language:', err)
      // Continue to next step even if language switch fails, don't block user flow
      currentStep.value = 2
    } finally {
      loading.value = false
    }
  }
}

const goToPreviousStep = () => {
  currentStep.value = 1
}

const onConfigModeChange = () => {
  // Clear related fields when configuration mode changes
  form.value.apiKey = ''
  form.value.baseUrl = ''
  form.value.modelName = ''
  form.value.modelDisplayName = ''
  form.value.completionsPath = ''
  error.value = ''
  // Reset API key visibility
  showDashscopeApiKey.value = false
  showCustomApiKey.value = false
}

const validateForm = () => {
  if (!form.value.apiKey.trim()) {
    error.value = t('init.apiKeyRequired')
    return false
  }

  if (form.value.configMode === 'custom') {
    if (!form.value.baseUrl.trim()) {
      error.value = t('init.baseUrlRequired')
      return false
    }
    if (!form.value.modelName.trim()) {
      error.value = t('init.modelNameRequired')
      return false
    }
  }

  return true
}

const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }

  try {
    loading.value = true
    error.value = ''

    const requestBody: any = {
      configMode: form.value.configMode,
      apiKey: form.value.apiKey.trim()
    }

    if (form.value.configMode === 'custom') {
      requestBody.baseUrl = form.value.baseUrl.trim()
      requestBody.modelName = form.value.modelName.trim()
      requestBody.modelDisplayName = form.value.modelDisplayName.trim() || form.value.modelName.trim()
      requestBody.completionsPath = form.value.completionsPath.trim()
    }

    const response = await fetch('/api/init/save', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(requestBody)
    })

    const result = await response.json()

    if (result.success) {
      success.value = true

      // Save initialization status to localStorage
      localStorage.setItem('hasInitialized', 'true')
      localStorage.setItem('hasVisitedHome', 'true')

      // Clear LLM check cache to make configuration take effect immediately
      LlmCheckService.clearCache()

      if (result.requiresRestart) {
        // If restart is required, show restart prompt
        setTimeout(() => {
          if (confirm(t('init.restartRequired'))) {
            // User confirms restart, reload the page
            window.location.reload()
          } else {
            // User chooses to restart later, navigate to home page
            router.push('/home')
          }
        }, 2000)
      } else {
        // No restart needed, navigate directly
        setTimeout(() => {
          router.push('/home')
        }, 2000)
      }
    } else {
      error.value = result.error || t('init.saveFailed')
    }
  } catch (err) {
    console.error('Save config failed:', err)
    error.value = t('init.networkError')
  } finally {
    loading.value = false
  }
}

// Check if already initialized
const checkInitStatus = async () => {
  try {
    const response = await fetch('/api/init/status')
    const result = await response.json()

    if (result.success && result.initialized) {
      // If already initialized, navigate to home page
      localStorage.setItem('hasInitialized', 'true')
      router.push('/home')
    }
  } catch (err) {
    console.error('Check init status failed:', err)
  }
}

onMounted(() => {
  // Check for saved language preference using the same key as i18n system
  const savedLanguage = localStorage.getItem(LOCAL_STORAGE_LOCALE)
  if (savedLanguage && (savedLanguage === 'zh' || savedLanguage === 'en')) {
    selectedLanguage.value = savedLanguage
    locale.value = savedLanguage
  }

  checkInitStatus()
})
</script>

<style scoped>
.init-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0a0a0a;
  position: relative;
  padding: 40px 20px;
}

.init-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  padding: 40px;
  width: 100%;
  max-width: 480px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 10;
  margin: auto;
  max-height: none;
}

.init-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo h1 {
  font-size: 48px;
  margin: 0 0 16px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.logo-icon {
  font-size: 48px !important;
  color: #667eea !important;
  background: none !important;
  -webkit-text-fill-color: #667eea !important;
}

.init-header h2 {
  font-size: 28px;
  color: #ffffff;
  margin: 0 0 12px 0;
  font-weight: 600;
}

.description {
  color: #888888;
  font-size: 16px;
  line-height: 1.6;
  margin: 0;
}

.init-form {
  margin-bottom: 24px;
}

/* Step Indicator Styles */
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 32px 0;
  padding: 0 20px;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  opacity: 0.5;
  transition: all 0.3s ease;
}

.step.active {
  opacity: 1;
}

.step.completed {
  opacity: 1;
}

.step-number {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  border: 2px solid rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #ffffff;
  transition: all 0.3s ease;
}

.step.active .step-number {
  background: #667eea;
  border-color: #667eea;
  color: #ffffff;
}

.step.completed .step-number {
  background: #4ade80;
  border-color: #4ade80;
  color: #ffffff;
}

.step-label {
  font-size: 14px;
  color: #888888;
  text-align: center;
  transition: all 0.3s ease;
}

.step.active .step-label {
  color: #ffffff;
  font-weight: 500;
}

.step.completed .step-label {
  color: #4ade80;
}

.step-divider {
  width: 60px;
  height: 2px;
  background: rgba(255, 255, 255, 0.2);
  margin: 0 20px;
}

/* Language Selection Styles */
.language-selection {
  margin-bottom: 0;
}

.language-options {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.language-option {
  display: flex;
  align-items: center;
  padding: 20px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.02);
}

.language-option:hover {
  border-color: rgba(102, 126, 234, 0.4);
  background: rgba(255, 255, 255, 0.05);
  transform: translateY(-2px);
}

.language-option.active {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.1);
  transform: translateY(-2px);
}

.language-option input[type="radio"] {
  margin: 0 16px 0 0;
  width: 20px;
  height: 20px;
  accent-color: #667eea;
}

.language-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.language-flag {
  font-size: 32px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.language-flag .iconify {
  font-size: 32px !important;
  width: 32px !important;
  height: 32px !important;
}

.language-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.language-text strong {
  color: #ffffff;
  font-size: 18px;
  font-weight: 600;
}

.language-text small {
  color: #888888;
  font-size: 14px;
}

.form-group {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-weight: 500;
  color: #ffffff;
  margin-bottom: 8px;
  font-size: 14px;
}

.required {
  color: #e53e3e;
  margin-left: 4px;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  font-size: 16px;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
  background: rgba(255, 255, 255, 0.08);
}

.form-input:disabled {
  background: rgba(255, 255, 255, 0.02);
  cursor: not-allowed;
  opacity: 0.6;
}

.form-input::placeholder {
  color: #666666;
}

.api-key-input-container {
  position: relative;
  display: flex;
  align-items: center;
}

.api-key-input-container .form-input {
  padding-right: 50px;
}

.api-key-toggle-btn {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
}

.api-key-toggle-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.api-key-toggle-btn:focus {
  outline: none;
  background: rgba(255, 255, 255, 0.15);
}

.api-key-toggle-btn .iconify {
  font-size: 16px !important;
  width: 16px !important;
  height: 16px !important;
  color: #ffffff !important;
}

.config-mode-selection {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.radio-option {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.02);
}

.radio-option:hover {
  border-color: rgba(102, 126, 234, 0.4);
  background: rgba(255, 255, 255, 0.05);
}

.radio-option.active {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.1);
}

.radio-option input[type="radio"] {
  margin: 4px 12px 0 0;
  width: 16px;
  height: 16px;
  accent-color: #667eea;
}

.radio-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.radio-text strong {
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
}

.radio-text small {
  color: #888888;
  font-size: 14px;
  line-height: 1.4;
}

.custom-config-section {
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.02);
  margin-top: 8px;
}

.form-hint {
  font-size: 13px;
  color: #888888;
  margin-top: 6px;
  line-height: 1.4;
}

.help-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.help-link:hover {
  text-decoration: underline;
  color: #8b9cf0;
}

.form-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  align-items: center;
}

.form-actions.single {
  justify-content: center;
}

.back-btn {
  padding: 12px 32px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  background: transparent;
  color: #ffffff;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 120px;
}

.back-btn:hover:not(:disabled) {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.1);
  transform: translateY(-2px);
}

.back-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.submit-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 14px 32px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  min-width: 180px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.3);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-message {
  background: #fed7d7;
  color: #c53030;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid #feb2b2;
  font-size: 14px;
  margin-top: 16px;
}

.success-message {
  background: #c6f6d5;
  color: #2f855a;
  padding: 12px 16px;
  border-radius: 8px;
  border: 1px solid #9ae6b4;
  font-size: 14px;
  margin-top: 16px;
}

.error-fade-enter-active,
.error-fade-leave-active,
.success-fade-enter-active,
.success-fade-leave-active {
  transition: all 0.3s ease;
}

.error-fade-enter-from,
.error-fade-leave-to,
.success-fade-enter-from,
.success-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Background animation */
.background-animation {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 1;
}

.floating-shape {
  position: absolute;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border-radius: 50%;
  animation: float 20s infinite linear;
}

.floating-shape:nth-child(1) {
  width: 80px;
  height: 80px;
  left: 10%;
  animation-duration: 25s;
  animation-delay: 0s;
}

.floating-shape:nth-child(2) {
  width: 120px;
  height: 120px;
  left: 20%;
  animation-duration: 30s;
  animation-delay: 5s;
}

.floating-shape:nth-child(3) {
  width: 60px;
  height: 60px;
  left: 70%;
  animation-duration: 22s;
  animation-delay: 10s;
}

.floating-shape:nth-child(4) {
  width: 100px;
  height: 100px;
  left: 80%;
  animation-duration: 28s;
  animation-delay: 15s;
}

.floating-shape:nth-child(5) {
  width: 40px;
  height: 40px;
  left: 40%;
  animation-duration: 35s;
  animation-delay: 20s;
}

.floating-shape:nth-child(6) {
  width: 90px;
  height: 90px;
  left: 60%;
  animation-duration: 24s;
  animation-delay: 8s;
}

@keyframes float {
  0% {
    transform: translateY(100vh) rotate(0deg);
    opacity: 0;
  }
  10% {
    opacity: 0.1;
  }
  90% {
    opacity: 0.1;
  }
  100% {
    transform: translateY(-100px) rotate(360deg);
    opacity: 0;
  }
}

/* Background effects */
.background-effects {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  opacity: 0.1;
  animation: orbit 20s infinite linear;
}

.orb-1 {
  width: 400px;
  height: 400px;
  top: -200px;
  left: -200px;
  animation-duration: 25s;
}

.orb-2 {
  width: 300px;
  height: 300px;
  bottom: -150px;
  right: -150px;
  animation-duration: 30s;
  animation-direction: reverse;
}

.orb-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-duration: 35s;
}

@keyframes orbit {
  0% {
    transform: rotate(0deg) scale(1);
  }
  50% {
    transform: rotate(180deg) scale(1.1);
  }
  100% {
    transform: rotate(360deg) scale(1);
  }
}

/* Responsive design */
@media (max-height: 800px) {
  .init-container {
    align-items: flex-start;
    padding: 20px 0 40px 0;
  }

  .init-card {
    margin: 20px auto;
  }

  .init-header {
    margin-bottom: 30px;
  }

  .logo h1 {
    font-size: 36px;
  }

  .logo-icon {
    font-size: 36px !important;
  }

  .init-header h2 {
    font-size: 24px;
  }
}

@media (max-width: 768px) {
  .init-container {
    padding: 20px;
  }

  .init-card {
    padding: 30px 24px;
  }

  .logo h1 {
    font-size: 40px;
  }

  .logo-icon {
    font-size: 40px !important;
  }

  .init-header h2 {
    font-size: 24px;
  }

  .description {
    font-size: 15px;
  }

  .step-indicator {
    margin: 24px 0;
    padding: 0 10px;
  }

  .step-divider {
    width: 40px;
    margin: 0 15px;
  }

  .language-flag {
    font-size: 28px;
  }

  .language-flag .iconify {
    font-size: 28px !important;
    width: 28px !important;
    height: 28px !important;
  }

  .language-text strong {
    font-size: 16px;
  }

  .form-actions {
    flex-direction: column;
  }

  .back-btn,
  .submit-btn {
    width: 100%;
    min-width: auto;
  }
}

@media (max-width: 640px) {
  .init-container {
    padding: 15px;
  }

  .init-card {
    padding: 24px;
    margin: 0;
    border-radius: 16px;
  }

  .logo h1 {
    font-size: 32px;
  }

  .logo-icon {
    font-size: 32px !important;
  }

  .init-header h2 {
    font-size: 22px;
  }

  .description {
    font-size: 14px;
  }

  .step-indicator {
    margin: 20px 0;
    padding: 0 5px;
  }

  .step-number {
    width: 36px;
    height: 36px;
    font-size: 14px;
  }

  .step-label {
    font-size: 12px;
  }

  .step-divider {
    width: 30px;
    margin: 0 10px;
  }

  .language-option {
    padding: 16px;
  }

  .language-flag {
    font-size: 24px;
  }

  .language-flag .iconify {
    font-size: 24px !important;
    width: 24px !important;
    height: 24px !important;
  }

  .language-text strong {
    font-size: 16px;
  }

  .language-text small {
    font-size: 13px;
  }
}
</style>
