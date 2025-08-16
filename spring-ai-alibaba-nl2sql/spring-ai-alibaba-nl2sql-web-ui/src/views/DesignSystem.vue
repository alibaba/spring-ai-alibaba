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
  <div class="design-system-page">
    <div class="container">
      <div class="page-header">
        <h1>现代化设计系统展示</h1>
        <p>Spring AI Alibaba NL2SQL 智能体管理系统设计规范</p>
      </div>

      <!-- 色彩系统 -->
      <section class="design-section">
        <h2>🎨 现代化色彩系统</h2>
        <p class="section-desc">基于现代简约 + 科技轻盈感的配色方案，营造专业而友好的用户体验</p>
        
        <div class="color-palette">
          <div class="color-group">
            <h3>主色系 - 淡蓝科技感</h3>
            <div class="color-swatches">
              <div class="color-swatch primary">
                <div class="color-preview" style="background-color: #5F70E1"></div>
                <div class="color-info">
                  <span class="color-name">Primary</span>
                  <span class="color-value">#5F70E1</span>
                  <span class="color-desc">主品牌色</span>
                </div>
              </div>
              <div class="color-swatch">
                <div class="color-preview" style="background-color: #3A7AFE"></div>
                <div class="color-info">
                  <span class="color-name">Primary Hover</span>
                  <span class="color-value">#3A7AFE</span>
                  <span class="color-desc">悬停状态</span>
                </div>
              </div>
              <div class="color-swatch">
                <div class="color-preview" style="background-color: #E8EBFF"></div>
                <div class="color-info">
                  <span class="color-name">Primary Light</span>
                  <span class="color-value">#E8EBFF</span>
                  <span class="color-desc">浅色背景</span>
                </div>
              </div>
            </div>
          </div>

          <div class="color-group">
            <h3>辅助色 - 浅紫活泼点缀</h3>
            <div class="color-swatches">
              <div class="color-swatch">
                <div class="color-preview" style="background-color: #917DFE"></div>
                <div class="color-info">
                  <span class="color-name">Accent</span>
                  <span class="color-value">#917DFE</span>
                  <span class="color-desc">强调色</span>
                </div>
              </div>
              <div class="color-swatch">
                <div class="color-preview" style="background-color: #A794FF"></div>
                <div class="color-info">
                  <span class="color-name">Accent Hover</span>
                  <span class="color-value">#A794FF</span>
                  <span class="color-desc">悬停状态</span>
                </div>
              </div>
              <div class="color-swatch">
                <div class="color-preview" style="background-color: #F0EEFF"></div>
                <div class="color-info">
                  <span class="color-name">Accent Light</span>
                  <span class="color-value">#F0EEFF</span>
                  <span class="color-desc">浅色背景</span>
                </div>
              </div>
            </div>
          </div>

          <div class="color-group">
            <h3>功能色系</h3>
            <div class="color-swatches">
              <div class="color-swatch">
                <div class="color-preview" style="background-color: var(--success-color)"></div>
                <div class="color-info">
                  <span class="color-name">Success</span>
                  <span class="color-value">#52c41a</span>
                </div>
              </div>
              <div class="color-swatch">
                <div class="color-preview" style="background-color: var(--warning-color)"></div>
                <div class="color-info">
                  <span class="color-name">Warning</span>
                  <span class="color-value">#faad14</span>
                </div>
              </div>
              <div class="color-swatch">
                <div class="color-preview" style="background-color: var(--error-color)"></div>
                <div class="color-info">
                  <span class="color-name">Error</span>
                  <span class="color-value">#ff4d4f</span>
                </div>
              </div>
            </div>
          </div>

          <div class="color-group">
            <h3>中性色系</h3>
            <div class="color-swatches">
              <div class="color-swatch">
                <div class="color-preview" style="background-color: var(--text-primary)"></div>
                <div class="color-info">
                  <span class="color-name">Text Primary</span>
                  <span class="color-value">#262626</span>
                </div>
              </div>
              <div class="color-swatch">
                <div class="color-preview" style="background-color: var(--text-secondary)"></div>
                <div class="color-info">
                  <span class="color-name">Text Secondary</span>
                  <span class="color-value">#595959</span>
                </div>
              </div>
              <div class="color-swatch">
                <div class="color-preview" style="background-color: var(--text-tertiary)"></div>
                <div class="color-info">
                  <span class="color-name">Text Tertiary</span>
                  <span class="color-value">#8c8c8c</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 按钮系统 -->
      <section class="design-section">
        <h2>按钮系统</h2>
        <div class="component-showcase">
          <div class="component-group">
            <h3>按钮类型</h3>
            <div class="button-examples">
              <button class="btn btn-primary">主要按钮</button>
              <button class="btn btn-secondary">次要按钮</button>
              <button class="btn btn-success">成功按钮</button>
              <button class="btn btn-warning">警告按钮</button>
              <button class="btn btn-danger">危险按钮</button>
              <button class="btn btn-outline">轮廓按钮</button>
              <button class="btn btn-text">文字按钮</button>
            </div>
          </div>

          <div class="component-group">
            <h3>按钮尺寸</h3>
            <div class="button-examples">
              <button class="btn btn-primary btn-xs">超小按钮</button>
              <button class="btn btn-primary btn-sm">小按钮</button>
              <button class="btn btn-primary">默认按钮</button>
              <button class="btn btn-primary btn-lg">大按钮</button>
              <button class="btn btn-primary btn-xl">超大按钮</button>
            </div>
          </div>

          <div class="component-group">
            <h3>按钮状态</h3>
            <div class="button-examples">
              <button class="btn btn-primary">正常状态</button>
              <button class="btn btn-primary" disabled>禁用状态</button>
              <button class="btn btn-primary">
                <div class="spinner spinner-sm"></div>
                加载中
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- 表单系统 -->
      <section class="design-section">
        <h2>表单系统</h2>
        <div class="component-showcase">
          <div class="form-examples">
            <div class="form-group">
              <label class="form-label required">用户名</label>
              <input type="text" class="form-control" placeholder="请输入用户名">
            </div>
            <div class="form-group">
              <label class="form-label">邮箱地址</label>
              <input type="email" class="form-control is-valid" value="user@example.com">
              <div class="valid-feedback">邮箱格式正确</div>
            </div>
            <div class="form-group">
              <label class="form-label">密码</label>
              <input type="password" class="form-control is-invalid">
              <div class="invalid-feedback">密码长度至少8位</div>
            </div>
            <div class="form-group">
              <label class="form-label">描述</label>
              <textarea class="form-control" rows="3" placeholder="请输入描述信息"></textarea>
            </div>
            <div class="form-group">
              <label class="form-label">选择类型</label>
              <select class="form-control">
                <option>选项一</option>
                <option>选项二</option>
                <option>选项三</option>
              </select>
            </div>
            <div class="checkbox-group">
              <input type="checkbox" id="agree" class="form-checkbox">
              <label for="agree" class="checkbox-label">我同意服务条款</label>
            </div>
          </div>
        </div>
      </section>

      <!-- 卡片系统 -->
      <section class="design-section">
        <h2>卡片系统</h2>
        <div class="component-showcase">
          <div class="card-examples">
            <div class="card">
              <div class="card-header">
                <h4 class="card-title">基础卡片</h4>
              </div>
              <div class="card-body">
                <p class="card-text">这是一个基础卡片的示例，展示了卡片的基本结构和样式。</p>
                <button class="btn btn-primary btn-sm">操作按钮</button>
              </div>
            </div>

            <div class="card">
              <div class="card-body">
                <h4 class="card-title">智能体卡片</h4>
                <p class="card-subtitle">数据分析专家</p>
                <p class="card-text">专门用于数据分析和报表生成的智能体，支持多种数据源连接。</p>
                <div class="d-flex gap-sm">
                  <span class="badge badge-success">已发布</span>
                  <span class="badge badge-secondary">SQL</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 徽章系统 -->
      <section class="design-section">
        <h2>徽章系统</h2>
        <div class="component-showcase">
          <div class="badge-examples">
            <span class="badge badge-primary">Primary</span>
            <span class="badge badge-secondary">Secondary</span>
            <span class="badge badge-success">Success</span>
            <span class="badge badge-warning">Warning</span>
            <span class="badge badge-error">Error</span>
            <span class="badge badge-info">Info</span>
          </div>
          <div class="badge-examples">
            <span class="status-badge active">已激活</span>
            <span class="status-badge inactive">未激活</span>
            <span class="status-badge published">已发布</span>
            <span class="status-badge draft">草稿</span>
            <span class="status-badge offline">已下线</span>
          </div>
        </div>
      </section>

      <!-- 表格系统 -->
      <section class="design-section">
        <h2>表格系统</h2>
        <div class="component-showcase">
          <table class="table">
            <thead>
              <tr>
                <th>智能体名称</th>
                <th>类型</th>
                <th>状态</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>销售数据分析师</td>
                <td>数据分析</td>
                <td><span class="status-badge published">已发布</span></td>
                <td>2024-01-15</td>
                <td>
                  <button class="btn btn-text btn-sm">编辑</button>
                  <button class="btn btn-text btn-sm text-error">删除</button>
                </td>
              </tr>
              <tr>
                <td>客户行为分析师</td>
                <td>行为分析</td>
                <td><span class="status-badge draft">草稿</span></td>
                <td>2024-01-16</td>
                <td>
                  <button class="btn btn-text btn-sm">编辑</button>
                  <button class="btn btn-text btn-sm text-error">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- 消息提示 -->
      <section class="design-section">
        <h2>消息提示</h2>
        <div class="component-showcase">
          <div class="message-examples">
            <div class="message-toast success">
              <div class="message-content">
                <i class="bi bi-check-circle-fill"></i>
                <span>操作成功完成！</span>
              </div>
            </div>
            <div class="message-toast warning">
              <div class="message-content">
                <i class="bi bi-exclamation-triangle-fill"></i>
                <span>请注意检查输入信息</span>
              </div>
            </div>
            <div class="message-toast error">
              <div class="message-content">
                <i class="bi bi-x-circle-fill"></i>
                <span>操作失败，请重试</span>
              </div>
            </div>
            <div class="message-toast info">
              <div class="message-content">
                <i class="bi bi-info-circle-fill"></i>
                <span>这是一条信息提示</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 加载动画 -->
      <section class="design-section">
        <h2>加载动画</h2>
        <div class="component-showcase">
          <div class="spinner-examples">
            <div class="spinner spinner-sm"></div>
            <div class="spinner"></div>
            <div class="spinner spinner-lg"></div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DesignSystem'
}
</script>

<style scoped>
.design-system-page {
  min-height: 100vh;
  background-color: var(--bg-layout);
  padding: var(--space-xl) 0;
}

.container {
  max-width: 100%;
  margin: 0 auto;
  padding: 0 var(--space-xl);
}

.page-header {
  text-align: center;
  margin-bottom: var(--space-3xl);
}

.page-header h1 {
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin-bottom: var(--space-sm);
}

.page-header p {
  font-size: var(--font-size-lg);
  color: var(--text-secondary);
  margin: 0;
}

.design-section {
  background-color: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-2xl);
  margin-bottom: var(--space-xl);
  box-shadow: var(--shadow-base);
  border: 1px solid var(--border-secondary);
}

.design-section h2 {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin-bottom: var(--space-xl);
  padding-bottom: var(--space-sm);
  border-bottom: 2px solid var(--primary-light);
}

.design-section h3 {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
  margin-bottom: var(--space-md);
}

/* 色彩系统样式 */
.color-palette {
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
}

.color-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.color-swatches {
  display: flex;
  gap: var(--space-md);
  flex-wrap: wrap;
}

.color-swatch {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
  min-width: 120px;
}

.color-preview {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-base);
  border: 1px solid var(--border-tertiary);
}

.color-info {
  text-align: center;
}

.color-name {
  display: block;
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
  font-size: var(--font-size-sm);
}

.color-value {
  display: block;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  color: var(--text-tertiary);
  font-size: var(--font-size-xs);
}

/* 组件展示样式 */
.component-showcase {
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
}

.component-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.button-examples {
  display: flex;
  gap: var(--space-md);
  flex-wrap: wrap;
  align-items: center;
}

.form-examples {
  max-width: 400px;
}

.card-examples {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: var(--space-lg);
}

.badge-examples {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
  margin-bottom: var(--space-md);
}

.message-examples {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  max-width: 400px;
}

.message-examples .message-toast {
  position: static;
  animation: none;
}

.spinner-examples {
  display: flex;
  gap: var(--space-lg);
  align-items: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .container {
    padding: 0 var(--space-md);
  }
  
  .design-section {
    padding: var(--space-lg);
  }
  
  .color-swatches {
    justify-content: center;
  }
  
  .button-examples {
    justify-content: center;
  }
  
  .card-examples {
    grid-template-columns: 1fr;
  }
}
</style>
