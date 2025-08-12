<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements.  See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache License, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License.  You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
-->
<template>
  <div class="kb-management">
    <!-- <div class="header">
      <h2>知识管理</h2>
      <a-space>

      </a-space>
    </div> -->

    <a-card class="list-card">
      <a-button type="primary" @click="openAdd" class="add-btn">
        <template #icon><PlusOutlined /></template>
        新增知识库
      </a-button>
        <a-divider />
      <a-table 
        :data-source="dataSource" 
        :columns="columns" 
        row-key="kb_id" 
        bordered
        :scroll="{ x: 1000 }">
        <template #emptyText>
          <div style="text-align: center; padding: 60px 0; color: #8c8c8c">
            <FileTextOutlined style="font-size: 64px; margin-bottom: 16px; opacity: 0.5" />
            <div style="font-size: 16px">暂无知识库数据</div>
            <div style="font-size: 14px; margin-top: 8px; color: #bfbfbf">
              点击右上角"新增知识库"按钮创建您的第一个知识库
            </div>
          </div>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'kb_name'">
            <a-typography-link @click="openFiles(record)" class="kb-name-link">
              <FolderOpenOutlined style="margin-right: 8px; color: #667eea" />
              {{ record.kb_name }}
            </a-typography-link>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button size="small" @click="editKb(record)" class="action-btn">
                <EditOutlined />
                编辑
              </a-button>
              <a-popconfirm 
                title="确定删除该知识库？" 
                @confirm="removeKb(record.kb_id)"
                ok-text="确定"
                cancel-text="取消">
                <a-button size="small" danger class="action-btn">
                  <DeleteOutlined />
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal 
      v-model:open="editVisible" 
      :title="editForm.kb_id ? '编辑知识库' : '新增知识库'" 
      @ok="saveKb"
      :width="520"
      :ok-button-props="{ style: { background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)', border: 'none' } }">
      <a-form :model="editForm" :label-col="{ span: 5 }" :wrapper-col="{ span: 19 }" layout="vertical">
        <a-form-item label="知识库名称" required>
          <a-input 
            v-model:value="editForm.kb_name" 
            placeholder="请输入知识库名称"
            :max-length="50"
            show-count>
            <template #prefix>
              <FolderOpenOutlined style="color: #667eea" />
            </template>
          </a-input>
        </a-form-item>
        <a-form-item label="描述信息">
          <a-textarea 
            v-model:value="editForm.kb_description" 
            placeholder="请输入知识库描述信息"
            :rows="3"
            :max-length="200"
            show-count />
        </a-form-item>
        <a-form-item label="分类标签">
          <a-select 
            v-model:value="editForm.category" 
            placeholder="请选择分类标签"
            allow-clear>
            <a-select-option v-for="option in categoryOptions" :key="option.value" :value="option.value">
              <span class="category-option">
                <span class="category-dot" :style="{ background: option.color }"></span>
                {{ option.label }}
              </span>
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 文件列表和上传弹窗 -->
    <a-modal 
      v-model:open="filesVisible" 
      :title="currentKb?.kb_name + ' - 文件管理'" 
      width="800px" 
      :footer="null"
      :body-style="{ padding: '0' }">
      <div class="files-header">
        <div class="stats-container">
          <a-statistic 
            title="文件总数" 
            :value="currentKb?.files?.length || 0" 
            :value-style="{ color: '#667eea', fontWeight: 600 }" />
          <a-statistic 
            title="总大小" 
            :value="formatTotalSize(currentKb?.files)" 
            :value-style="{ color: '#52c41a', fontWeight: 600 }" />
        </div>
        <a-upload
          multiple
          :before-upload="beforeUpload"
          :custom-request="handleBatchUpload"
          :show-upload-list="false"
        >
          <a-button type="primary">
            <template #icon><UploadOutlined /></template>
            批量上传
          </a-button>
        </a-upload>
      </div>

      <div style="padding: 24px">
        <a-list 
          :data-source="currentKb?.files || []" 
          item-layout="horizontal" 
          :locale="{ emptyText: '暂无文件' }">
          <template #renderItem="{ item }">
            <a-list-item class="file-item">
              <a-list-item-meta>
                <template #title>
                  <div style="display: flex; align-items: center; gap: 8px">
                    <FileTextOutlined style="color: #667eea" />
                    <span style="font-weight: 600; color: #1a1a1a">{{ item.name }}</span>
                  </div>
                </template>
                <template #description>
                  <div style="color: #6c757d; font-size: 13px">
                    大小: {{ formatFileSize(item.size) }} · 上传时间: {{ formatTime(item.uploadTime) }}
                  </div>
                </template>
              </a-list-item-meta>
              <template #actions>
                <a-space>
                  <a-tag 
                    :color="getStatusColor(item.status)" 
                    style="font-weight: 500">
                    {{ statusText(item.status) }}
                  </a-tag>
                  <a-popconfirm 
                    title="确定删除该文件？" 
                    @confirm="removeFile(item.id)"
                    ok-text="确定"
                    cancel-text="取消">
                    <a-button type="text" size="small" danger>
                      <template #icon><DeleteOutlined /></template>
                    </a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </a-list-item>
          </template>
        </a-list>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, h } from 'vue'
import { message } from 'ant-design-vue'
import { 
  FolderOpenOutlined, 
  EditOutlined, 
  DeleteOutlined,
  PlusOutlined,
  UploadOutlined,
  FileTextOutlined
} from '@ant-design/icons-vue'
import service from '@/utils/request'
import { useKnowledgeStore, type KnowledgeBase, type KnowledgeFile } from '@/store/KnowledgeStore'

const store = useKnowledgeStore()

// 分类选项配置
const categoryOptions = [
  { value: '产品管理', label: '产品管理', color: '#1890ff' },
  { value: '技术开发', label: '技术开发', color: '#722ed1' },
  { value: '用户支持', label: '用户支持', color: '#52c41a' }
]

const columns = [
  { 
    title: 'ID', 
    dataIndex: 'kb_id', 
    key: 'kb_id', 
    width: 260,
    ellipsis: true
  },
  { 
    title: '名称', 
    dataIndex: 'kb_name', 
    key: 'kb_name',
    ellipsis: true
  },
  { 
    title: '描述', 
    dataIndex: 'kb_description', 
    key: 'kb_description',
    ellipsis: true
  },
  { 
    title: '分类', 
    dataIndex: 'category', 
    key: 'category', 
    width: 140,
    render: (text: string) => {
      const colors: Record<string, string> = {
        '产品管理': 'blue',
        '技术开发': 'purple',
        '用户支持': 'green'
      }
      return h('a-tag', { 
        color: colors[text] || 'default',
        style: { fontWeight: 500, borderRadius: '12px' }
      }, () => text)
    }
  },
  { 
    title: '文件数', 
    key: 'fileCount', 
    width: 100,
    render: (text: any, record: KnowledgeBase) => {
      return h('span', { 
        style: { 
          color: '#667eea', 
          fontWeight: 600,
          fontSize: '14px'
        }
      }, record.files?.length || 0)
    }
  },
  { 
    title: '操作', 
    key: 'action', 
    width: 180,
    fixed: 'right' as const
  },
]

const dataSource = computed(() => store.knowledgeBases)

const editVisible = ref(false)
const editForm = reactive<{ kb_id?: string; kb_name: string; kb_description: string; category: string }>({
  kb_name: '',
  kb_description: '',
  category: '',
})

function openAdd() {
  Object.assign(editForm, { kb_id: undefined, kb_name: '', kb_description: '', category: '' })
  editVisible.value = true
}

function editKb(record: KnowledgeBase) {
  Object.assign(editForm, record)
  editVisible.value = true
}

function saveKb() {
  if (!editForm.kb_name) {
    message.warning('请输入名称')
    return
  }
  
  const action = editForm.kb_id ? '更新' : '新增'
  const params = {
    kb_name: editForm.kb_name,
    kb_description: editForm.kb_description,
    category: editForm.category || '未分类',
  }
  
  if (editForm.kb_id) {
    store.updateKnowledgeBase(editForm.kb_id, params)
  } else {
    store.addKnowledgeBase(params)
  }
  
  message.success(`${action}成功`)
  editVisible.value = false
}

function removeKb(id: string) {
  if (store.deleteKnowledgeBase(id)) {
    message.success('删除成功')
  }
}

// 文件管理
const filesVisible = ref(false)
const currentKb = ref<KnowledgeBase | null>(null)

function openFiles(record: KnowledgeBase) {
  currentKb.value = record
  filesVisible.value = true
}

function beforeUpload(file: File) {
  // 检查文件类型
  const allowedTypes = ['application/pdf', 'text/plain', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
  if (!allowedTypes.includes(file.type)) {
    message.error('只支持 PDF、TXT、DOC、DOCX 格式的文件')
    return false
  }
  
  // 检查文件大小（限制为 10MB）
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    message.error('文件大小不能超过 10MB')
    return false
  }
  
  // 使用自定义上传
  return true
}

const statusMap = {
  success: { text: '已完成', color: 'success' },
  error: { text: '失败', color: 'error' },
  processing: { text: '上传中', color: 'processing' },
  uploading: { text: '上传中', color: 'processing' }
}

function statusText(status: KnowledgeFile['status']) {
  return statusMap[status]?.text || '未知'
}

function getStatusColor(status: KnowledgeFile['status']) {
  return statusMap[status]?.color || 'default'
}

function formatTime(time: string) {
  return new Date(time).toLocaleString()
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function formatTotalSize(files?: KnowledgeFile[]): string {
  if (!files || files.length === 0) return '0 B'
  const total = files.reduce((sum, file) => sum + file.size, 0)
  return formatFileSize(total)
}

async function handleBatchUpload(options: any) {
  if (!currentKb.value) {
    message.error('请先选择知识库')
    return
  }
  
  const file = options.file as File
  const kb = currentKb.value
  
  // 添加文件到本地状态，初始状态为上传中
  const added = store.addFileToKnowledgeBase(kb.kb_id, {
    name: file.name,
    size: file.size,
    uploadTime: new Date().toISOString()
  })
  
  if (!added) {
    message.error('添加文件失败')
    options.onError?.(new Error('添加文件失败'))
    return
  }
  
  try {
    // 构建表单数据
    const formData = new FormData()
    formData.append('kb_id', kb.kb_id)
    formData.append('kb_name', kb.kb_name)
    formData.append('kb_description', kb.kb_description || '')
    formData.append('category', kb.category || '未分类')
    formData.append('files', file, file.name)

    // 调试信息
    console.log('上传文件信息:', file)

    // 发送上传请求
    const res = await service.post('/api/rag/professional-kb/batch-upload', formData, {
      timeout: 30000 // 30秒超时
    })
    
    // 更新文件状态为成功
    store.updateFileStatus(kb.kb_id, added.id, 'success')
    message.success(`${file.name} 上传成功`)
    options.onSuccess?.(res)
    
  } catch (error: any) {
    console.error('文件上传失败:', error)
    
    // 更新文件状态为失败
    store.updateFileStatus(kb.kb_id, added.id, 'error')
    
    // 根据错误类型显示不同的错误信息
    let errorMessage = `${file.name} 上传失败`
    if (error.code === 'ECONNABORTED') {
      errorMessage += '：请求超时'
    } else if (error.response?.status === 413) {
      errorMessage += '：文件过大'
    } else if (error.response?.status === 415) {
      errorMessage += '：不支持的文件格式'
    } else if (error.response?.data?.message) {
      errorMessage += `：${error.response.data.message}`
    }
    
    message.error(errorMessage)
    options.onError?.(error)
  }
}

function removeFile(fileId: string) {
  if (!currentKb.value) return
  if (store.removeFileFromKnowledgeBase(currentKb.value.kb_id, fileId)) {
    message.success('删除成功')
  }
}

onMounted(() => {
  store.initSampleData()
})
</script>

<style lang="less" scoped>
.category-option {
  display: flex;
  align-items: center;
}

.category-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
}

.kb-management {
  padding: 24px;
  height: 100%;
  width: 100%;
  overflow: auto;
  background: #f5f5f5;
  min-height: 100vh;

  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 24px;
    padding: 16px 24px;
    background: #fff;
    border-radius: 8px;
    border: 1px solid #f0f0f0;

    h2 {
      color: #262626;
      margin: 0;
      font-size: 20px;
      font-weight: 500;
    }
  }

  .list-card {
    background: #fff;
    border-radius: 8px;
    border: 1px solid #f0f0f0;
    overflow: hidden;

    :deep(.ant-table) {
      .ant-table-thead > tr > th {
        background: #fafafa;
        color: #262626;
        font-weight: 500;
        font-size: 14px;
        border-bottom: 1px solid #f0f0f0;
        padding: 12px 16px;
      }

      .ant-table-tbody > tr > td {
        padding: 12px 16px;
        border-bottom: 1px solid #f0f0f0;
      }

      .ant-table-tbody > tr:hover > td {
        background-color: #fafafa;
      }
    }
  }


  .add-btn {
    border-radius: 6px;
  }
}

  .files-header {
    margin-bottom: 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 24px;
    background: #fafafa;
    border-radius: 8px;
    border: 1px solid #f0f0f0;

    .stats-container {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

// 全局样式增强
:deep(.ant-modal-content) {
  border-radius: 8px;
}

:deep(.ant-modal-header) {
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  padding: 16px 24px;

  .ant-modal-title {
    color: #262626;
    font-weight: 500;
    font-size: 16px;
  }
}

:deep(.ant-list-item) {
  padding: 12px 16px;
  border-radius: 6px;
  margin-bottom: 8px;
  background: #fff;
  border: 1px solid #f0f0f0;
}

:deep(.ant-btn) {
  border-radius: 6px;
}
</style>
