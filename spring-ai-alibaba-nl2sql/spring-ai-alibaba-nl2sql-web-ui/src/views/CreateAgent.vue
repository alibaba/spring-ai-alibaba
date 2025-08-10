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
  <div class="create-agent-page">
    <!-- 现代化头部导航 -->
    <header class="page-header">
      <div class="header-content">
        <div class="brand-section">
          <div class="brand-logo" @click="goToHome">
            <i class="bi bi-robot"></i>
            <span class="brand-text">数据智能体</span>
          </div>
          <nav class="header-nav">
            <div class="nav-item" @click="goToAgentList">
              <i class="bi bi-grid-3x3-gap"></i>
              <span>智能体列表</span>
            </div>
            <div class="nav-item" @click="goToWorkspace">
              <i class="bi bi-chat-square-dots"></i>
              <span>智能体工作台</span>
            </div>
          </nav>
        </div>
        <div class="header-actions">
          <button class="btn btn-outline" @click="openHelp">
            <i class="bi bi-question-circle"></i>
            帮助
          </button>
          <button class="btn btn-primary" @click="createAgent" :disabled="loading">
            <i class="bi bi-plus-lg"></i>
            {{ loading ? '创建中...' : '创建智能体' }}
          </button>
        </div>
      </div>
    </header>

    <!-- 页面标题区域 -->
    <div class="title-section">
      <div class="container">
        <div class="title-content">
          <div class="title-icon">
            <i class="bi bi-plus-circle"></i>
          </div>
          <div class="title-info">
            <h1 class="page-title">创建新的智能体</h1>
            <p class="page-subtitle">配置您的专属数据分析智能体，让AI帮助您更好地理解和分析数据</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建表单 -->
    <div class="container">
      <div class="create-form-wrapper">
        <div class="create-form">
          <div class="form-section">
            <div class="section-title">
              <h3>基本信息</h3>
              <p>设置智能体的基本属性</p>
            </div>

            <div class="form-grid">
              <div class="form-group">
                <label for="agentName">智能体名称 *</label>
                <input 
                  type="text" 
                  id="agentName"
                  v-model="agentForm.name" 
                  placeholder="请输入智能体名称"
                  class="form-control"
                  required
                >
              </div>

              <div class="form-group">
                <label for="agentCategory">分类</label>
                <select id="agentCategory" v-model="agentForm.category" class="form-control">
                  <option value="">请选择分类</option>
                  <option value="数据分析">数据分析</option>
                  <option value="业务分析">业务分析</option>
                  <option value="财务分析">财务分析</option>
                  <option value="供应链">供应链</option>
                  <option value="营销">营销</option>
                  <option value="其他">其他</option>
                </select>
              </div>

              <div class="form-group full-width">
                <label for="agentDescription">智能体描述</label>
                <textarea 
                  id="agentDescription"
                  v-model="agentForm.description" 
                  placeholder="请输入智能体的功能描述和使用场景"
                  class="form-control"
                  rows="4"
                ></textarea>
              </div>

              <div class="form-group">
                <label>头像设置</label>
                <div class="avatar-upload">
                  <div class="avatar-preview">
                    <img :src="agentForm.avatar" alt="智能体头像">
                  </div>
                  <div class="avatar-controls">
                    <div class="avatar-buttons">
                      <button type="button" class="btn btn-outline" @click="regenerateAvatar">
                        <i class="bi bi-arrow-clockwise"></i>
                        重新生成
                      </button>
                      <button type="button" class="btn btn-outline">
                        <i class="bi bi-upload"></i>
                        上传图片
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <div class="form-group">
                <label for="agentTags">标签</label>
                <input 
                  type="text" 
                  id="agentTags"
                  v-model="agentForm.tags" 
                  placeholder="请输入标签，用逗号分隔"
                  class="form-control"
                >
                <div class="form-help">例如：数据分析,销售,业务指标</div>
              </div>
            </div>
          </div>

          <div class="form-section">
            <div class="section-title">
              <h3>智能体类型</h3>
              <p>选择适合您业务需求的智能体类型</p>
            </div>

            <div class="template-grid">
              <div class="template-card" @click="useTemplate('data-analyst')">
                <h5>数据分析师</h5>
                <p>专业的数据分析和SQL查询助手</p>
              </div>
              <div class="template-card" @click="useTemplate('report-generator')">
                <h5>报表生成器</h5>
                <p>自动生成各类业务报表</p>
              </div>
            </div>
          </div>

          <div class="form-section">
            <div class="section-title">
              <h3>发布设置</h3>
              <p>选择智能体的初始状态</p>
            </div>

            <div class="publish-options">
              <label class="radio-option">
                <input type="radio" value="draft" v-model="agentForm.status" checked>
                <span class="radio-label">
                  <strong>保存为草稿</strong>
                  <span class="radio-desc">暂不发布，可以继续编辑配置</span>
                </span>
              </label>
              <label class="radio-option">
                <input type="radio" value="published" v-model="agentForm.status">
                <span class="radio-label">
                  <strong>立即发布</strong>
                  <span class="radio-desc">创建完成后立即发布供用户使用</span>
                </span>
              </label>
            </div>
          </div>

          <!-- 表单操作按钮 -->
          <div class="form-actions">
            <button type="button" class="btn btn-outline" @click="goBack">取消</button>
            <button type="button" class="btn btn-secondary" @click="saveDraft">保存草稿</button>
            <button type="button" class="btn btn-primary" @click="createAgent" :disabled="loading">
              <span v-if="loading">创建中...</span>
              <span v-else>创建智能体</span>
            </button>
          </div>
        </div>

        <!-- 预览面板 -->
        <div class="preview-panel">
          <div class="preview-header">
            <h3>预览</h3>
            <p>查看智能体的外观效果</p>
          </div>

          <div class="agent-preview">
            <div class="preview-avatar">
              <img :src="agentForm.avatar" :alt="agentForm.name || '智能体'">
            </div>
            <div class="preview-info">
              <h4>{{ agentForm.name || '智能体名称' }}</h4>
              <p>{{ agentForm.description || '智能体描述...' }}</p>
              <div class="preview-meta">
                <span class="preview-category">{{ agentForm.category || '未分类' }}</span>
                <span class="preview-status" :class="agentForm.status">
                  {{ agentForm.status === 'published' ? '已发布' : '草稿' }}
                </span>
              </div>
              <div class="preview-tags" v-if="agentForm.tags">
                <span v-for="tag in agentForm.tags.split(',')" :key="tag.trim()" class="tag">
                  {{ tag.trim() }}
                </span>
              </div>
            </div>
          </div>


        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { agentApi } from '../utils/api.js'
import { generateRandomAvatar, generateProfessionalAvatar } from '../utils/avatar.js'

export default {
  name: 'CreateAgent',
  setup() {
    const router = useRouter()
    const loading = ref(false)

    const agentForm = reactive({
      name: '',
      description: '',
      avatar: '',
      category: '',
      tags: '',
      prompt: '',
      status: 'draft'
    })

    // 组件挂载时生成随机头像
    onMounted(() => {
      // 直接使用备用头像，确保能正常显示
      agentForm.avatar = generateFallbackAvatar()
      console.log('Generated fallback avatar:', agentForm.avatar)
    })

    // 备用头像生成函数
    const generateFallbackAvatar = () => {
      const colors = ['3B82F6', '8B5CF6', '10B981', 'F59E0B', 'EF4444', '6366F1', 'EC4899', '14B8A6']
      const randomColor = colors[Math.floor(Math.random() * colors.length)]
      const letters = ['AI', '数据', '智能', 'DA', 'BI', 'ML', 'DL', 'NL']
      const randomLetter = letters[Math.floor(Math.random() * letters.length)]
      
      // 使用最简单的 SVG，只有背景色和文字
      const svg = `<svg width="200" height="200" xmlns="http://www.w3.org/2000/svg">
        <rect width="200" height="200" fill="#${randomColor}"/>
        <text x="100" y="120" font-family="Arial, sans-serif" font-size="48" font-weight="bold" text-anchor="middle" fill="white">${randomLetter}</text>
      </svg>`
      
      return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
    }

    // Prompt模板
    const promptTemplates = {
      'data-analyst': `你是一个专业的数据分析助手，具备以下能力：

1. 根据用户的自然语言问题，生成准确的SQL查询语句
2. 分析数据结果并提供业务洞察
3. 解释查询逻辑和数据含义
4. 提供数据可视化建议

请始终保持专业、准确和友好的态度，确保查询结果的正确性和实用性。`,

      'business-advisor': `你是一个资深的业务顾问，专注于：

1. 解答各类业务问题和挑战
2. 提供战略建议和解决方案
3. 分析市场趋势和机会
4. 优化业务流程和效率

请以专业、客观的角度提供建议，确保信息的准确性和实用性。`,

      'report-generator': `你是一个专业的报表生成助手，擅长：

1. 根据数据生成各类业务报表
2. 创建数据汇总和分析报告
3. 提供图表和可视化建议
4. 解释数据趋势和异常

请确保报表格式清晰、数据准确、结论明确。`
    }

    const goBack = () => {
      router.push('/agents')
    }

    const goToAgentList = () => {
      router.push('/agents')
    }

    const goToWorkspace = () => {
      router.push('/workspace')
    }

    const openHelp = () => {
      window.open('https://github.com/alibaba/spring-ai-alibaba/blob/main/spring-ai-alibaba-nl2sql/README.md', '_blank')
    }

    const goToHome = () => {
      router.push('/')
    }

    const useTemplate = (templateKey) => {
      agentForm.prompt = promptTemplates[templateKey]
    }

    const saveDraft = async () => {
      agentForm.status = 'draft'
      await createAgent()
    }

    const regenerateAvatar = () => {
      // 直接使用备用头像生成函数
      agentForm.avatar = generateFallbackAvatar()
      console.log('Regenerated avatar:', agentForm.avatar)
    }

    const createAgent = async () => {
      if (!agentForm.name.trim()) {
        alert('请填写智能体名称')
        return
      }

      try {
        loading.value = true

        const agentData = {
          name: agentForm.name.trim(),
          description: agentForm.description.trim(),
          avatar: agentForm.avatar.trim() || generateRandomAvatar(),
          category: agentForm.category.trim(),
          tags: agentForm.tags.trim(),
          prompt: agentForm.prompt.trim(),
          status: agentForm.status
        }

        const result = await agentApi.create(agentData)
        
        alert(`智能体创建成功！状态：${agentData.status === 'published' ? '已发布' : '草稿'}`)
        router.push(`/agent/${result.id}`)
      } catch (error) {
        console.error('创建智能体失败:', error)
        alert('创建失败，请重试')
      } finally {
        loading.value = false
      }
    }

    return {
      agentForm,
      loading,
      goBack,
      goToAgentList,
      goToWorkspace,
      openHelp,
      goToHome,
      useTemplate,
      saveDraft,
      regenerateAvatar,
      generateFallbackAvatar,
      createAgent
    }
  }
}
</script>

<style scoped>
.create-agent-page {
  min-height: 100vh;
  background: var(--bg-layout);
  font-family: var(--font-family);
}

/* 现代化头部导航 */
.page-header {
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-secondary);
  box-shadow: var(--shadow-xs);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
}

.header-content {
  max-width: 100%;
  margin: 0 auto;
  padding: 0 var(--space-xl);
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
}

.brand-section {
  display: flex;
  align-items: center;
  gap: var(--space-2xl);
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--primary-color);
  cursor: pointer;
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

.brand-logo i {
  font-size: var(--font-size-xl);
  color: var(--accent-color);
}

.brand-text {
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-nav {
  display: flex;
  gap: var(--space-lg);
}

.header-nav .nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-base);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  border-radius: var(--radius-base);
  border: 1px solid transparent;
}

.header-nav .nav-item:hover {
  background: var(--bg-secondary);
  color: var(--text-primary);
  border-color: var(--border-primary);
}

.header-nav .nav-item.active {
  background: var(--primary-light);
  color: var(--primary-color);
  border-color: var(--primary-color);
}

.header-nav .nav-item i {
  font-size: var(--font-size-base);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

/* 页面标题区域 */
.title-section {
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-secondary);
}

.container {
  max-width: 100%;
  margin: 0 auto;
  padding: 0 var(--space-xl);
}

.title-content {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  padding: var(--space-2xl) 0;
}

.title-icon {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--bg-primary);
  font-size: var(--font-size-2xl);
  box-shadow: var(--shadow-md);
  position: relative;
  overflow: hidden;
}

.title-icon::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.2), rgba(255,255,255,0));
  pointer-events: none;
}

.title-info {
  flex: 1;
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-sm) 0;
  background: linear-gradient(135deg, var(--primary-color), var(--accent-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-subtitle {
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.6;
}

/* 表单布局 */
.create-form-wrapper {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: var(--space-2xl);
  margin-top: var(--space-2xl);
  margin-bottom: var(--space-2xl);
}

.create-form {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-2xl);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-secondary);
}

.form-section {
  margin-bottom: var(--space-2xl);
  padding-bottom: var(--space-2xl);
  border-bottom: 1px solid var(--border-tertiary);
}

.form-section:last-of-type {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.section-title {
  margin-bottom: var(--space-xl);
}

.section-title h3 {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-sm) 0;
}

.section-title p {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.5;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-lg);
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-group {
  margin-bottom: var(--space-md);
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-group label {
  display: block;
  margin-bottom: var(--space-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
  font-size: var(--font-size-sm);
}

.form-help {
  margin-top: var(--space-xs);
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  line-height: 1.4;
}

/* 头像上传 */
.avatar-upload {
  display: flex;
  gap: var(--space-xl);
  align-items: flex-start;
}

.avatar-preview {
  width: 100px;
  height: 100px;
  border-radius: var(--radius-xl);
  overflow: hidden;
  border: 3px solid var(--border-secondary);
  flex-shrink: 0;
  background: var(--bg-secondary);
  position: relative;
  box-shadow: var(--shadow-md);
  transition: all var(--transition-base);
}

.avatar-preview:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary-color);
}

.avatar-preview::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(95, 112, 225, 0.05), rgba(145, 125, 254, 0.05));
  pointer-events: none;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-controls {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: var(--space-md);
  min-height: 100px;
}

.avatar-buttons {
  display: flex;
  gap: var(--space-md);
  flex-wrap: wrap;
}

.avatar-buttons .btn {
  flex: 1;
  min-width: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-sm);
  transition: all var(--transition-base);
  white-space: nowrap;
}

.avatar-buttons .btn:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.avatar-buttons .btn i {
  font-size: var(--font-size-base);
}

/* 头像信息区域 */
.avatar-info {
  margin-bottom: var(--space-md);
}

.avatar-desc {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  margin: 0 0 var(--space-xs) 0;
  font-weight: var(--font-weight-medium);
}

.avatar-hint {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.4;
}

/* Prompt 文本区域 */
.prompt-textarea {
  min-height: 160px;
  font-family: var(--font-family-mono);
  font-size: var(--font-size-sm);
  line-height: 1.6;
  resize: vertical;
  background: var(--bg-secondary);
  border: 1px solid var(--border-secondary);
}

.prompt-textarea:focus {
  background: var(--bg-primary);
  border-color: var(--primary-color);
}

/* Prompt 模板 */
.prompt-templates {
  margin-top: var(--space-xl);
  padding-top: var(--space-xl);
  border-top: 1px solid var(--border-tertiary);
}

.prompt-templates h4 {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
  margin: 0 0 var(--space-lg) 0;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-md);
}

.template-card {
  padding: var(--space-lg);
  border: 1px solid var(--border-secondary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  background: var(--bg-primary);
  position: relative;
  overflow: hidden;
}

.template-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--primary-color), var(--accent-color));
  opacity: 0;
  transition: opacity var(--transition-base);
}

.template-card:hover {
  border-color: var(--primary-color);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.template-card:hover::before {
  opacity: 1;
}

.template-card h5 {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-sm) 0;
}

.template-card p {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.4;
}

/* 发布选项 */
.publish-options {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.radio-option {
  display: flex;
  align-items: flex-start;
  gap: var(--space-base);
  padding: var(--space-lg);
  border: 1px solid var(--border-secondary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  background: var(--bg-primary);
}

.radio-option:hover {
  border-color: var(--primary-color);
  background: var(--primary-lighter);
}

.radio-option input[type="radio"] {
  margin-top: 2px;
  accent-color: var(--primary-color);
}

.radio-label {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.radio-label strong {
  color: var(--text-primary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
}

.radio-desc {
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
  line-height: 1.4;
}

/* 表单操作按钮 */
.form-actions {
  display: flex;
  gap: var(--space-md);
  justify-content: flex-end;
  margin-top: var(--space-2xl);
  padding-top: var(--space-xl);
  border-top: 1px solid var(--border-tertiary);
}

/* 预览面板 */
.preview-panel {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-xl);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-secondary);
  height: fit-content;
  position: sticky;
  top: calc(64px + var(--space-xl));
}

.preview-header {
  margin-bottom: var(--space-xl);
  padding-bottom: var(--space-lg);
  border-bottom: 1px solid var(--border-tertiary);
}

.preview-header h3 {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-sm) 0;
}

.preview-header p {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin: 0;
}

.agent-preview {
  display: flex;
  gap: var(--space-lg);
  margin-bottom: var(--space-xl);
  padding: var(--space-lg);
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-tertiary);
}

.preview-avatar {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 2px solid var(--border-secondary);
  flex-shrink: 0;
  background: var(--bg-primary);
}

.preview-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-info {
  flex: 1;
}

.preview-info h4 {
  margin: 0 0 8px 0;
  color: var(--text-primary);
  font-size: var(--font-size-base);
}

.preview-info p {
  margin: 0 0 12px 0;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.5;
}

.preview-meta {
  display: flex;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
  flex-wrap: wrap;
}

.preview-category,
.preview-status {
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.preview-category {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  border: 1px solid var(--border-primary);
}

.preview-status {
  background: var(--success-light);
  color: var(--success-color);
  border: 1px solid rgba(82, 196, 26, 0.2);
}

.preview-status.draft {
  background: var(--warning-light);
  color: var(--warning-color);
  border: 1px solid rgba(250, 173, 20, 0.2);
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
}

.tag {
  padding: var(--space-xs) var(--space-sm);
  background: var(--primary-light);
  color: var(--primary-color);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  border: 1px solid rgba(95, 112, 225, 0.2);
}

.preview-prompt {
  padding-top: var(--space-lg);
  border-top: 1px solid var(--border-tertiary);
}

.preview-prompt h4 {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0 0 var(--space-md) 0;
}

.prompt-preview {
  padding: var(--space-md);
  background: var(--bg-secondary);
  border: 1px solid var(--border-secondary);
  border-radius: var(--radius-sm);
  font-family: var(--font-family-mono);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  line-height: 1.5;
  white-space: pre-wrap;
  max-height: 200px;
  overflow-y: auto;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .create-form-wrapper {
    grid-template-columns: 1fr;
    gap: var(--space-xl);
  }
  
  .preview-panel {
    position: static;
  }
  
  .container {
    padding: 0 var(--space-lg);
  }
}

@media (max-width: 768px) {
  .header-content {
    padding: 0 var(--space-md);
    flex-direction: column;
    height: auto;
    padding-top: var(--space-md);
    padding-bottom: var(--space-md);
    gap: var(--space-md);
  }
  
  .brand-section {
    flex-direction: column;
    gap: var(--space-md);
    align-items: flex-start;
  }
  
  .header-nav {
    flex-wrap: wrap;
  }
  
  .title-content {
    flex-direction: column;
    text-align: center;
    gap: var(--space-md);
  }
  
  .page-title {
    font-size: var(--font-size-2xl);
  }
  
  .container {
    padding: 0 var(--space-md);
  }
  
  .create-form {
    padding: var(--space-lg);
  }
  
  .form-grid {
    grid-template-columns: 1fr;
    gap: var(--space-md);
  }
  
  .avatar-upload {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .template-grid {
    grid-template-columns: 1fr;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .agent-preview {
    flex-direction: column;
    text-align: center;
  }
  
  .preview-avatar {
    align-self: center;
  }
}

@media (max-width: 480px) {
  .title-icon {
    width: 48px;
    height: 48px;
    font-size: var(--font-size-xl);
  }
  
  .create-form {
    padding: var(--space-md);
  }
  
  .preview-panel {
    padding: var(--space-md);
  }
}
</style>
