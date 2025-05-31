<template>
  <div class="config-panel">
    <h2>基础配置</h2>

    <div class="config-group">
      <h3>智能体设置</h3>
      <div class="form-item">
        <label>智能体执行最大步数</label>
        <input type="number" v-model="config.maxSteps" min="1" max="100" />
      </div>
      <div class="form-item">
        <Flex align="center" justify="space-between">
          <span>重置所有agent</span>
          <Switch :enabled="config.resetAllAgents" label="" />
        </Flex>
      </div>
    </div>

    <div class="config-group">
      <h3>浏览器设置</h3>
      <div class="form-item">
        <Flex align="center" justify="space-between">
          <span>是否使用无头浏览器模式</span>
          <Switch :enabled="config.headlessBrowser" label="" />
        </Flex>
      </div>
      <div class="form-item">
        <label>浏览器请求超时时间(秒)</label>
        <input type="number" v-model="config.browserTimeout" min="1" max="600" />
      </div>
      <div class="form-item">
        <Flex align="center" justify="space-between">
          <span>浏览器debug模式</span>
          <Switch :enabled="config.browserDebug" label="" />
        </Flex>
      </div>
    </div>

    <div class="config-group">
      <h3>交互设置</h3>
      <div class="form-item">
        <Flex align="center" justify="space-between">
          <span>启动时自动打开浏览器</span>
          <Switch :enabled="config.autoOpenBrowser" label="" />
        </Flex>
      </div>
      <div class="form-item">
        <Flex align="center" justify="space-between">
          <span>启用控制台交互模式</span>
          <Switch :enabled="config.consoleInteractive" label="" />
        </Flex>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import Switch from '@/components/switch/index.vue'
import Flex from '@/components/flex/index.vue'

const config = reactive({
  // 智能体设置
  maxSteps: 20,
  resetAllAgents: false,

  // 浏览器设置
  headlessBrowser: false,
  browserTimeout: 180,
  browserDebug: false,

  // 交互设置
  autoOpenBrowser: true,
  consoleInteractive: false,
})
</script>

<style scoped>
.config-panel h2 {
  margin-bottom: 24px;
  font-size: 20px;
  font-weight: 500;
}

.config-group {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
}

.config-group h3 {
  margin-bottom: 16px;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
}

.form-item {
  margin-bottom: 16px;
}

.form-item:last-child {
  margin-bottom: 0;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  color: rgba(255, 255, 255, 0.8);
}

.form-item.checkbox label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 0;
  cursor: pointer;
}

.form-item input[type='checkbox'] {
  width: 16px;
  height: 16px;
  margin: 0;
  cursor: pointer;
}

.form-item input[type='number'] {
  width: 100%;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: #fff;
  transition: all 0.3s;
}

.form-item input[type='number']:focus {
  border-color: #667eea;
  outline: none;
}
</style>
