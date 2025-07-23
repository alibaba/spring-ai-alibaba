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
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="modelValue" class="modal-overlay" @click="handleOverlayClick">
        <div class="modal-container" @click.stop>
          <div class="modal-header">
            <h3>{{ $t('cronTask.title') }}</h3>
            <button class="close-btn" @click="$emit('update:modelValue', false)">
              <Icon icon="carbon:close" />
            </button>
          </div>
          <div class="modal-content">
            <!-- Loading state -->
            <div v-if="loading" class="loading-container">
              <Icon icon="carbon:loading" class="loading-icon" />
              <span>{{ $t('common.loading') }}</span>
            </div>

            <!-- Empty state -->
            <div v-else-if="cronTasks.length === 0" class="empty-container">
              <Icon icon="carbon:time" class="empty-icon" />
              <span>{{ $t('cronTask.noTasks') }}</span>
            </div>

            <!-- Task list -->
            <div v-else class="task-list">
              <div v-for="task in cronTasks" :key="task.id || ''" class="task-item" @click="showTaskDetail(task)">
                <div class="task-main">
                  <div class="task-info">
                    <div class="task-header">
                      <div class="task-name">{{ task.cronName}}</div>
                      <div class="task-status-badge" :class="task.status === 0 ? 'active' : 'inactive'">
                        <Icon :icon="task.status === 0 ? 'carbon:checkmark-filled' : 'carbon:pause-filled'" />
                        <span>{{ task.status === 0 ? $t('cronTask.active') : $t('cronTask.inactive') }}</span>
                      </div>
                    </div>
                    <div class="task-description">{{ task.planDesc }}</div>
                    <div class="task-time">
                      <Icon icon="carbon:time" />
                      <span class="cron-readable" style="cursor:pointer" @click.stop="copyCronTime(task.cronTime)">{{ task.cronTime }}</span>
                    </div>
                  </div>
                </div>

                <div class="task-actions" @click.stop>
                  <button
                    class="action-btn execute-btn"
                    @click="executeTask(task.id!)"
                    :disabled="executing === task.id"
                    :title="$t('cronTask.executeOnce')"
                  >
                    <Icon :icon="executing === task.id ? 'carbon:loading' : 'carbon:play-filled'" />
                    {{ $t('cronTask.executeOnce') }}
                  </button>

                  <div class="action-dropdown" :class="{ 'active': activeDropdown === task.id }">
                    <button
                      class="action-btn dropdown-btn"
                      @click="toggleDropdown(task.id!)"
                      :title="$t('cronTask.operations')"
                    >
                      <Icon icon="carbon:overflow-menu-horizontal" />
                      {{ $t('cronTask.operations') }}
                    </button>

                    <div class="dropdown-menu" v-show="activeDropdown === task.id">
                      <button
                        class="dropdown-item edit-btn"
                        @click="showTaskDetail(task)"
                      >
                        <Icon icon="carbon:edit" />
                        {{ $t('cronTask.edit') }}
                      </button>

                      <button
                        class="dropdown-item toggle-btn"
                        @click="toggleTaskStatus(task)"
                        :disabled="toggling === task.id"
                      >
                        <Icon :icon="toggling === task.id ? 'carbon:loading' : (task.status === 0 ? 'carbon:pause-filled' : 'carbon:play-filled')" />
                        {{ task.status === 0 ? $t('cronTask.disable') : $t('cronTask.enable') }}
                      </button>

                      <button
                        class="dropdown-item delete-btn"
                        @click="showDeleteConfirmDialog(task)"
                        :disabled="deleting === task.id"
                      >
                        <Icon :icon="deleting === task.id ? 'carbon:loading' : 'carbon:trash-can'" />
                        {{ $t('cronTask.delete') }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- Task Detail Modal -->
  <TaskDetailModal
    v-model="showDetail"
    :task="selectedTask"
    @save="handleSaveTask"
  />

  <!-- Delete Confirmation Modal -->
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="showDeleteConfirm" class="modal-overlay" @click="cancelDelete">
        <div class="confirm-modal" @click.stop>
          <div class="confirm-header">
            <Icon icon="carbon:warning" class="warning-icon" />
            <h3>{{ $t('cronTask.deleteConfirm') }}</h3>
          </div>
          <div class="confirm-content">
            <p>{{ $t('cronTask.deleteConfirmMessage', { taskName: taskToDelete?.cronName || taskToDelete?.planDesc || '' }) }}</p>
          </div>
          <div class="confirm-actions">
            <button class="confirm-btn cancel-btn" @click="cancelDelete">
              {{ $t('common.cancel') }}
            </button>
            <button
              class="confirm-btn delete-btn"
              @click="handleDeleteTask"
              :disabled="deleting === taskToDelete?.id"
            >
              <Icon :icon="deleting === taskToDelete?.id ? 'carbon:loading' : 'carbon:trash-can'" />
              {{ $t('cronTask.delete') }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { Icon } from '@iconify/vue'
import { useRouter } from 'vue-router'
import { CronApiService } from '@/api/cron-api-service'
import type { CronConfig } from '@/types/cron-task'
import TaskDetailModal from './TaskDetailModal.vue'
import { useTaskStore } from '@/stores/task'
import { useToast } from '@/plugins/useToast'
const router = useRouter()
const taskStore = useTaskStore()
const toast = useToast()

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true,
  },
})

const emit = defineEmits(['update:modelValue'])

const cronTasks = ref<CronConfig[]>([])
const loading = ref(false)
const executing = ref<string | number | null>(null)
const deleting = ref<string | number | null>(null)
const toggling = ref<string | number | null>(null)
const activeDropdown = ref<string | number | null>(null)
const showDetail = ref(false)
const selectedTask = ref<CronConfig | null>(null)
const showDeleteConfirm = ref(false)
const taskToDelete = ref<CronConfig | null>(null)



const handleOverlayClick = (e: MouseEvent) => {
  if (e.target === e.currentTarget) {
    emit('update:modelValue', false)
  }
}

const loadCronTasks = async () => {
  loading.value = true
  try {
    cronTasks.value = await CronApiService.getAllCronTasks()
  } catch (error) {
    console.error('Failed to load cron tasks:', error)
  } finally {
    loading.value = false
  }
}

const executeTask = async (taskId: string | number) => {
  executing.value = taskId
  try {
    // Find the task by ID
    const task = cronTasks.value.find(t => t.id === taskId)
    if (!task) {
      console.error('Task not found:', taskId)
      return
    }

    // Use planDesc as the task content
    const taskContent = task.planDesc || task.cronName || ''

    if (taskContent.trim()) {
      taskStore.setTask(taskContent.trim())
      emit('update:modelValue', false)

      const chatId = Date.now().toString()
      await router.push({
        name: 'direct',
        params: { id: chatId },
      })
    }
  } catch (error) {
    console.error('Failed to execute task:', error)
  } finally {
    executing.value = null
  }
}

const showTaskDetail = (task: CronConfig) => {
  selectedTask.value = { ...task }
  showDetail.value = true
  activeDropdown.value = null
}

const handleSaveTask = async (updatedTask: CronConfig) => {
  try {
    if (updatedTask.id) {
      await CronApiService.updateCronTask(Number(updatedTask.id), updatedTask)
      await loadCronTasks() // Reload the list
    }
    showDetail.value = false
  } catch (error) {
    console.error('Failed to save task:', error)
  }
}

const showDeleteConfirmDialog = (task: CronConfig) => {
  taskToDelete.value = task
  showDeleteConfirm.value = true
}

const handleDeleteTask = async () => {
  if (!taskToDelete.value?.id) return

  deleting.value = taskToDelete.value.id
  try {
    await CronApiService.deleteCronTask(String(taskToDelete.value.id))
    await loadCronTasks() // Reload the list
    showDeleteConfirm.value = false
    taskToDelete.value = null
  } catch (error) {
    console.error('Failed to delete task:', error)
  } finally {
    deleting.value = null
  }
}

const cancelDelete = () => {
  showDeleteConfirm.value = false
  taskToDelete.value = null
}

const toggleDropdown = (taskId: string | number) => {
  activeDropdown.value = activeDropdown.value === taskId ? null : taskId
}

const toggleTaskStatus = async (task: CronConfig) => {
  if (!task.id) return

  toggling.value = task.id
  try {
    const newStatus = task.status === 0 ? 1 : 0
    await CronApiService.updateCronTask(Number(task.id), { ...task, status: newStatus })
    await loadCronTasks() // Reload the list
    activeDropdown.value = null // Close dropdown
  } catch (error) {
    console.error('Failed to toggle task status:', error)
  } finally {
    toggling.value = null
  }
}

const copyCronTime = async (cronTime: string) => {
  try {
    await navigator.clipboard.writeText(cronTime)
    toast.success('成功复制cron表达式')
  } catch (e) {
    toast.error('复制失败')
  }
}

// 点击外部关闭下拉框
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  // 检查点击的元素是否在下拉框内部
  if (!target.closest('.action-dropdown') && !target.closest('.dropdown-menu')) {
    activeDropdown.value = null
  }
}

onMounted(() => {
  // 使用 capture 阶段监听，确保能够捕获到事件
  document.addEventListener('click', handleClickOutside, true)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside, true)
})

watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    loadCronTasks()
  }
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-container {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.15));
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}



.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

.close-btn {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.close-btn:hover {
  color: rgba(255, 255, 255, 0.9);
}

.modal-content {
  padding: 24px;
  min-height: 300px;
}

.loading-container,
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: rgba(255, 255, 255, 0.6);
  gap: 12px;
}

.loading-icon {
  font-size: 24px;
  animation: spin 1s linear infinite;
}

.empty-icon {
  font-size: 48px;
  opacity: 0.5;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-item {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  transition: all 0.3s ease;
  gap: 20px;
  cursor: pointer;
}

.task-item:hover {
  background: rgba(102, 126, 234, 0.15);
  border-color: rgba(102, 126, 234, 0.4);
  transform: translateY(-3px);
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.2);
}

.task-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-header {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.task-name {
  font-size: 20px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.95);
  line-height: 1.3;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.8));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.task-status-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid transparent;
  white-space: nowrap;
}

.task-status-badge.active {
  background: rgba(34, 197, 94, 0.1);
  border-color: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.task-status-badge.inactive {
  background: rgba(156, 163, 175, 0.1);
  border-color: rgba(156, 163, 175, 0.2);
  color: #9ca3af;
}

.task-description {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.5;
  margin: 4px 0;
  max-width: 80%;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.task-time,
.next-execution {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.2s ease;
}

.task-time:hover,
.next-execution:hover {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
}

.cron-readable {
  color: rgba(255, 255, 255, 0.85);
  font-weight: 600;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
}

.task-actions {
  display: flex;
  flex-direction: row;
  gap: 8px;
  min-width: 200px;
  align-items: flex-start;
  margin-left: auto;
  justify-content: flex-end;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  white-space: nowrap;
  border: 1px solid transparent;
}

.execute-btn {
  background: rgba(34, 197, 94, 0.1);
  border-color: rgba(34, 197, 94, 0.2);
  color: #22c55e;
}

.execute-btn:hover:not(:disabled) {
  background: rgba(34, 197, 94, 0.2);
  border-color: rgba(34, 197, 94, 0.3);
}

.execute-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.delete-btn {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.delete-btn:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.3);
}

.delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.dropdown-btn {
  background: rgba(156, 163, 175, 0.1);
  border-color: rgba(156, 163, 175, 0.2);
  color: #9ca3af;
}

.dropdown-btn:hover {
  background: rgba(156, 163, 175, 0.2);
  border-color: rgba(156, 163, 175, 0.3);
}

.action-dropdown {
  position: relative;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: rgba(30, 30, 30, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  z-index: 100;
  min-width: 140px;
  margin-top: 4px;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 12px;
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}

.dropdown-item:first-child {
  border-radius: 8px 8px 0 0;
}

.dropdown-item:last-child {
  border-radius: 0 0 8px 8px;
}

.dropdown-item:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.95);
}

.dropdown-item.edit-btn:hover:not(:disabled) {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.dropdown-item.toggle-btn:hover:not(:disabled) {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.dropdown-item.delete-btn:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.dropdown-item:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Transition animations */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

/* Confirm Modal Styles */
.confirm-modal {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.15));
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  width: 90%;
  max-width: 480px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  overflow: hidden;
}

.confirm-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 24px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.warning-icon {
  font-size: 24px;
  color: #f59e0b;
}

.confirm-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.confirm-content {
  padding: 20px 24px;
}

.confirm-content p {
  margin: 0;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
  font-size: 14px;
}

.confirm-actions {
  display: flex;
  gap: 12px;
  padding: 16px 24px 24px;
  justify-content: flex-end;
}

.confirm-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid transparent;
  min-width: 80px;
  justify-content: center;
}

.confirm-btn.cancel-btn {
  background: rgba(156, 163, 175, 0.1);
  border-color: rgba(156, 163, 175, 0.2);
  color: #9ca3af;
}

.confirm-btn.cancel-btn:hover {
  background: rgba(156, 163, 175, 0.2);
  border-color: rgba(156, 163, 175, 0.3);
}

.confirm-btn.delete-btn {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.confirm-btn.delete-btn:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.3);
}

.confirm-btn.delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>

