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
  <div class="language-switcher">
    <button
      class="language-btn"
      @click="toggleDropdown"
      :title="$t('language.switch')"
    >
      <Icon icon="carbon:translate" width="18" />
      <span class="current-lang">{{ currentLanguageLabel }}</span>
      <Icon :icon="showDropdown ? 'carbon:chevron-up' : 'carbon:chevron-down'" width="14" class="chevron" />
    </button>

    <div v-if="showDropdown" class="language-dropdown" @click.stop>
      <div class="dropdown-header">
        <span>{{ $t('language.switch') }}</span>
        <button class="close-btn" @click="showDropdown = false">
          <Icon icon="carbon:close" width="16" />
        </button>
      </div>
      <div class="language-options">
        <button
          v-for="option in languageOptions"
          :key="option.value"
          class="language-option"
          :class="{
            active: currentLocale === option.value,
            loading: isChangingLanguage && currentLocale !== option.value
          }"
          :disabled="isChangingLanguage"
          @click="selectLanguage(option.value)"
        >
          <span class="lang-code">{{ option.value.toUpperCase() }}</span>
          <span class="lang-name">{{ option.title }}</span>
          <Icon
            v-if="isChangingLanguage && currentLocale !== option.value"
            icon="carbon:circle-dash"
            width="16"
            class="loading-icon"
          />
          <Icon
            v-else-if="currentLocale === option.value"
            icon="carbon:checkmark"
            width="16"
            class="check-icon"
          />
        </button>
      </div>
    </div>

    <!-- Backdrop -->
    <div
      v-if="showDropdown"
      class="backdrop"
      @click="showDropdown = false"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Icon } from '@iconify/vue'
import { localeConfig, changeLanguage } from '@/base/i18n'

const { locale } = useI18n()

const showDropdown = ref(false)

const currentLocale = computed(() => locale.value)

const languageOptions = computed(() => localeConfig.opts)

const currentLanguageLabel = computed(() => {
  const current = languageOptions.value.find(opt => opt.value === currentLocale.value)
  return current ? current.title : 'Unknown'
})

const toggleDropdown = () => {
  showDropdown.value = !showDropdown.value
}

const isChangingLanguage = ref(false)

const selectLanguage = async (lang: string) => {
  if (isChangingLanguage.value || currentLocale.value === lang) return

  try {
    isChangingLanguage.value = true
    await changeLanguage(lang)
    showDropdown.value = false
  } catch (error) {
    console.error('Failed to change language:', error)
    // Close dropdown even if failed
    showDropdown.value = false
  } finally {
    isChangingLanguage.value = false
  }
}

// Close dropdown when clicking outside
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.language-switcher')) {
    showDropdown.value = false
  }
}

// Close dropdown on escape key
const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    showDropdown.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.language-switcher {
  position: relative;
  display: inline-block;
}

.language-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: transparent;
  border: 1.5px solid #667eea;
  border-radius: 8px;
  color: #8da2fb;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 600;
  outline: none;
}

.language-btn:hover {
  background: rgba(102, 126, 234, 0.15);
  border-color: #7c9eff;
  color: #a3bffa;
  box-shadow: 0 0 15px rgba(102, 126, 234, 0.2);
}

.language-btn:focus {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.5);
}

.current-lang {
  color: inherit;
  font-weight: 600;
  min-width: 40px;
  text-align: left;
  text-shadow: none;
}

.chevron {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0.9;
  filter: none;
}

.language-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  z-index: 9999;
  margin-top: 4px;
  background: linear-gradient(135deg, rgba(40, 40, 50, 0.95), rgba(30, 30, 40, 0.95));
  backdrop-filter: blur(16px);
  border: 1px solid rgba(102, 126, 234, 0.3);
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4), 0 0 0 1px rgba(102, 126, 234, 0.2);
  min-width: 200px;
  animation: slideDown 0.2s ease;
}

.dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(102, 126, 234, 0.2);
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(102, 126, 234, 0.05));
}

.close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.8);
}

.language-options {
  padding: 8px 0;
}

.language-option {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px 16px;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.language-option:hover {
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.9);
}

.language-option.active {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.2), rgba(102, 126, 234, 0.1));
  color: #7c9eff;
  border-left: 3px solid #667eea;
  padding-left: 13px;
}

.language-option.loading {
  opacity: 0.6;
  cursor: not-allowed;
}

.language-option:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.lang-code {
  display: inline-block;
  min-width: 24px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  opacity: 0.8;
}

.lang-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
}

.check-icon {
  color: #667eea;
  opacity: 0.8;
}

.loading-icon {
  color: #667eea;
  opacity: 0.8;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9998;
  background: transparent;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-8px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* Responsive design */
@media (max-width: 768px) {
  .language-dropdown {
    right: -8px;
    left: -8px;
    width: auto;
    min-width: auto;
  }

  .language-btn {
    padding: 6px 10px;
    font-size: 13px;
  }

  .current-lang {
    min-width: 35px;
  }
}

/* Dark theme adjustments */
@media (prefers-color-scheme: light) {
  .language-dropdown {
    background: rgba(255, 255, 255, 0.95);
    border-color: rgba(0, 0, 0, 0.1);
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  }

  .dropdown-header {
    color: rgba(0, 0, 0, 0.8);
    border-bottom-color: rgba(0, 0, 0, 0.1);
  }

  .close-btn {
    color: rgba(0, 0, 0, 0.6);
  }

  .close-btn:hover {
    background: rgba(0, 0, 0, 0.1);
    color: rgba(0, 0, 0, 0.8);
  }

  .language-option {
    color: rgba(0, 0, 0, 0.7);
  }

  .language-option:hover {
    background: rgba(0, 0, 0, 0.05);
    color: rgba(0, 0, 0, 0.9);
  }
}
</style>
