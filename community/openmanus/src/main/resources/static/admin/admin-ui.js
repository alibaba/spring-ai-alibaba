/**
 * admin-ui.js - 管理界面UI交互
 */
import { AdminUtils } from './admin-utils.js';

class AdminUI {
    constructor() {
        // Agent列表容器
        this.agentListContainer = document.querySelector('.agent-list-container');
        // Agent详情表单元素
        this.agentDetailForm = {
            name: document.getElementById('agent-detail-name'),
            description: document.getElementById('agent-detail-desc'),
            systemPrompt: document.getElementById('agent-think-prompt'),
            nextStepPrompt: document.getElementById('agent-next-prompt'),
            toolList: document.querySelector('.tool-list')
        };
        // 基础配置表单元素
        this.basicConfigForm = {
            systemName: document.getElementById('system-name'),
            language: document.getElementById('system-language'),
            maxThreads: document.getElementById('max-threads'),
            timeoutSeconds: document.getElementById('timeout-seconds')
        };
    }

    /**
     * 初始化UI
     */
    async init() {
        // 初始化分类导航
        this.initCategories();
        
        // 加载基础配置 - 两种方式
        await this.loadBasicConfig(); // 默认配置
        await this.loadBasicConfigs(); // manus 组配置

        // 加载Agent相关数据
        await this.loadAgents();
        await this.loadAvailableTools();
    }

    /**
     * 初始化分类导航
     */
    initCategories() {
        const categories = document.querySelectorAll('.category-item');
        categories.forEach(category => {
            category.addEventListener('click', () => {
                // 点击逻辑由admin-core.js处理
            });
        });
    }

    /**
     * 加载基础配置(对象模式)
     */
    async loadBasicConfig() {
        try {
            // 从 configModel 获取基础配置
            const basicConfig = configModel.config.basic;
            this.renderBasicConfig(basicConfig);
            this.bindBasicConfigEvents();
        } catch (error) {
            this.showError('加载基础配置失败');
        }
    }

    /**
     * 渲染基础配置(对象模式)
     */
    renderBasicConfig(config) {
        if (!config) return;

        // 设置系统名称
        if (this.basicConfigForm.systemName) {
            this.basicConfigForm.systemName.value = config.systemName;
        }

        // 设置语言选择
        if (this.basicConfigForm.language) {
            this.basicConfigForm.language.value = config.language;
        }

        // 设置性能参数
        if (config.performance) {
            if (this.basicConfigForm.maxThreads) {
                this.basicConfigForm.maxThreads.value = config.performance.maxThreads;
            }
            if (this.basicConfigForm.timeoutSeconds) {
                this.basicConfigForm.timeoutSeconds.value = config.performance.timeoutSeconds;
            }
        }
    }

    /**
     * 绑定基础配置事件(对象模式)
     */
    bindBasicConfigEvents() {
        // 系统名称变更
        if (this.basicConfigForm.systemName) {
            this.basicConfigForm.systemName.addEventListener('change', (e) => {
                configModel.config.basic.systemName = e.target.value;
            });
        }

        // 语言选择变更
        if (this.basicConfigForm.language) {
            this.basicConfigForm.language.addEventListener('change', (e) => {
                configModel.config.basic.language = e.target.value;
            });
        }

        // 最大线程数变更
        if (this.basicConfigForm.maxThreads) {
            this.basicConfigForm.maxThreads.addEventListener('change', (e) => {
                configModel.config.basic.performance.maxThreads = parseInt(e.target.value, 10);
            });
        }

        // 超时时间变更
        if (this.basicConfigForm.timeoutSeconds) {
            this.basicConfigForm.timeoutSeconds.addEventListener('change', (e) => {
                configModel.config.basic.performance.timeoutSeconds = parseInt(e.target.value, 10);
            });
        }
    }

    /**
     * 加载基础配置(manus组)
     */
    async loadBasicConfigs() {
        try {
            const manusConfigs = await configModel.loadConfigByGroup('manus');
            this.renderManusConfigs(manusConfigs);
        } catch (error) {
            console.error('加载基础配置失败:', error);
            this.showError('加载配置失败，请重试');
        }
    }
    
    /**
     * 渲染manus组的配置
     * @param {Array} configs - 配置数组
     */
    renderManusConfigs(configs) {
        // 获取基础配置面板
        const basicPanel = document.getElementById('basic-config');
        if (!basicPanel) {
            console.error('未找到基础配置面板');
            return;
        }
        
        // 备份并解析当前HTML (用于保留静态配置区域)
        const currentHtml = basicPanel.innerHTML;
        const titleEndIndex = currentHtml.indexOf('</h2>') + 5;
        const titleHtml = currentHtml.substring(0, titleEndIndex);
        
        // 清空面板（保留标题）
        basicPanel.innerHTML = titleHtml;
        
        // 按config_sub_group分组
        const groupedConfigs = {};
        configs.forEach(config => {
            if (!groupedConfigs[config.configSubGroup]) {
                groupedConfigs[config.configSubGroup] = [];
            }
            groupedConfigs[config.configSubGroup].push(config);
        });
        
        // 按子组首字母排序
        const sortedGroups = Object.keys(groupedConfigs).sort();
        
        // 为每个子组创建配置区域
        sortedGroups.forEach(groupName => {
            // 创建子组容器
            const groupContainer = document.createElement('div');
            groupContainer.className = 'config-group';
            
            // 创建子组标题
            const groupTitle = document.createElement('h3');
            groupTitle.className = 'group-title';
            groupTitle.textContent = this.formatSubGroupName(groupName);
            groupContainer.appendChild(groupTitle);
            
            // 创建配置项列表
            const configList = document.createElement('div');
            configList.className = 'config-list';
            
            // 为每个配置项创建UI元素
            groupedConfigs[groupName].forEach(config => {
                const configItem = this.createConfigItem(config);
                configList.appendChild(configItem);
            });
            
            groupContainer.appendChild(configList);
            basicPanel.appendChild(groupContainer);
        });
        
        // 添加保存按钮
        this.addSaveButton(basicPanel, 'manus');
    }
    
    /**
     * 格式化子组名称，使其更加易读
     * @param {string} subGroup - 子组名称
     * @returns {string} - 格式化后的名称
     */
    formatSubGroupName(subGroup) {
        // 子组名称映射表，可以根据需要扩展
        const subGroupNameMap = {
            'browser': '浏览器设置',
            'agent': '智能体设置',
            'interaction': '交互设置'
        };
        
        return subGroupNameMap[subGroup] || subGroup.charAt(0).toUpperCase() + subGroup.slice(1);
    }
    
    /**
     * 创建单个配置项的UI元素
     * @param {Object} config - 配置对象
     * @returns {HTMLElement} - 配置项UI元素
     */
    createConfigItem(config) {
        const item = document.createElement('div');
        item.className = 'config-item';
        
        // 创建配置项标签
        const label = document.createElement('label');
        label.setAttribute('for', `config-${config.id}`);
        label.textContent = config.description;
        item.appendChild(label);
        
        // 根据配置类型创建输入元素
        let inputElem;
        switch (config.inputType) {
            case 'CHECKBOX':
                inputElem = document.createElement('input');
                inputElem.type = 'checkbox';
                inputElem.checked = config.configValue === 'true';
                break;
                
            case 'NUMBER':
                inputElem = document.createElement('input');
                inputElem.type = 'number';
                inputElem.value = config.configValue;
                break;
                
            case 'SELECT':
                inputElem = document.createElement('select');
                try {
                    const options = JSON.parse(config.optionsJson || '[]');
                    options.forEach(option => {
                        const optionElem = document.createElement('option');
                        optionElem.value = option.value;
                        optionElem.textContent = option.label;
                        optionElem.selected = option.value === config.configValue;
                        inputElem.appendChild(optionElem);
                    });
                } catch (e) {
                    console.error('解析选项JSON失败:', e);
                }
                break;
                
            case 'TEXTAREA':
                inputElem = document.createElement('textarea');
                inputElem.value = config.configValue;
                inputElem.rows = 3;
                break;
                
            default: // TEXT或其他类型
                inputElem = document.createElement('input');
                inputElem.type = 'text';
                inputElem.value = config.configValue;
                break;
        }
        
        // 设置通用属性
        inputElem.id = `config-${config.id}`;
        inputElem.className = 'config-input';
        inputElem.setAttribute('data-config-id', config.id);
        inputElem.setAttribute('data-config-type', config.inputType);
        
        // 添加事件处理
        inputElem.addEventListener('change', (e) => {
            const value = config.inputType === 'BOOLEAN' 
                ? e.target.checked.toString() 
                : e.target.value;
            
            // 更新配置模型中的值
            configModel.updateGroupConfigValue('manus', config.id, value);
        });
        
        item.appendChild(inputElem);
        return item;
    }
    
    /**
     * 添加保存按钮
     * @param {HTMLElement} panel - 面板元素
     * @param {string} groupName - 配置组名
     */
    addSaveButton(panel, groupName) {
        // 创建按钮容器
        const buttonContainer = document.createElement('div');
        buttonContainer.className = 'button-container';
        
        // 创建保存按钮
        const saveButton = document.createElement('button');
        saveButton.className = 'save-button';
        saveButton.textContent = '保存配置';
        
        // 添加保存事件处理
        saveButton.addEventListener('click', async () => {
            // 禁用按钮，防止重复点击
            saveButton.disabled = true;
            saveButton.textContent = '保存中...';
            
            try {
                // 保存配置
                const result = await configModel.saveGroupConfig(groupName);
                
                // 显示保存结果
                this.showSuccess(
                    result.success ? '配置保存成功' : result.message
                );
            } catch (error) {
                console.error('保存配置失败:', error);
                this.showError('保存失败: ' + (error.message || '未知错误'));
            } finally {
                // 恢复按钮状态
                saveButton.disabled = false;
                saveButton.textContent = '保存配置';
            }
        });
        
        buttonContainer.appendChild(saveButton);
        panel.appendChild(buttonContainer);
    }

    /**
     * 加载并渲染Agent列表
     */
    async loadAgents() {
        try {
            const agents = await agentConfigModel.loadAgents();
            this.renderAgentList(agents);
        } catch (error) {
            this.showError('加载Agent列表失败');
        }
    }

    /**
     * 渲染Agent列表
     */
    renderAgentList(agents) {
        if (!this.agentListContainer) return;
        this.agentListContainer.innerHTML = agents.map(agent => this.createAgentListItem(agent)).join('');
    }

    /**
     * 创建Agent列表项HTML
     */
    createAgentListItem(agent) {
        return `
            <div class="agent-item" data-agent-id="${agent.id}">
                <div class="agent-item-header">
                    <span class="agent-name">${agent.name}</span>
                    <button class="expand-btn"><span class="icon-play"></span></button>
                </div>
                <div class="agent-item-content">
                    <div class="agent-desc">${agent.description}</div>
                    <div class="agent-tools">
                        ${agent.availableTools.map(tool => `
                            <span class="tool-tag">${tool}</span>
                        `).join('')}
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * 显示Agent详情
     */
    showAgentDetails(agent) {
        this.agentDetailForm.name.value = agent.name || '';
        this.agentDetailForm.description.value = agent.description || '';
        this.agentDetailForm.systemPrompt.value = agent.systemPrompt || '';
        this.agentDetailForm.nextStepPrompt.value = agent.nextStepPrompt || '';
        this.renderAgentToolList(agent.availableTools || []);
    }

    /**
     * 渲染主工具列表
     */
    renderAgentToolList(tools) {
        this.agentDetailForm.toolList.innerHTML = tools.map(tool => `
            <div class="tool-item">
                <span class="tool-name">${tool}</span>
                <button class="delete-tool-btn" data-tool="${tool}">×</button>
            </div>
        `).join('');
    }

    /**
     * 显示工具选择对话框
     */
    showToolSelectionDialog(availableTools, onSelect) {
        // 创建遮罩层
        const overlay = document.createElement('div');
        overlay.className = 'dialog-overlay';
        document.body.appendChild(overlay);

        // 获取或创建对话框
        let dialog = document.querySelector('.tool-selection-dialog');
        if (!dialog) {
            dialog = document.createElement('div');
            dialog.className = 'tool-selection-dialog';
            dialog.innerHTML = `
                <div class="dialog-header">
                    <h3>选择工具</h3>
                    <input type="text" class="tool-search" placeholder="搜索工具...">
                </div>
                <div class="tool-list-container"></div>
                <div class="dialog-footer">
                    <button class="cancel-btn">取消</button>
                    <button class="confirm-btn">确认</button>
                </div>
            `;
            document.body.appendChild(dialog);
        }
        
        const toolListContainer = dialog.querySelector('.tool-list-container');
        const searchInput = dialog.querySelector('.tool-search');
        
        // 渲染工具选择列表
        this.renderToolSelectionList(toolListContainer, availableTools);
        
        // 搜索功能
        const handleSearch = (e) => {
            const searchText = e.target.value.toLowerCase();
            const filteredTools = availableTools.filter(tool => 
                tool.key.toLowerCase().includes(searchText) || 
                (tool.description && tool.description.toLowerCase().includes(searchText))
            );
            this.renderToolSelectionList(toolListContainer, filteredTools);
        };
        
        searchInput.addEventListener('input', handleSearch);
        
        // 显示对话框和遮罩
        overlay.style.display = 'block';
        dialog.style.display = 'block';
        setTimeout(() => overlay.classList.add('show'), 10);
        
        // 处理工具选择
        let selectedTool = null;
        const handleToolClick = (e) => {
            const item = e.target.closest('.tool-selection-item');
            if (item) {
                toolListContainer.querySelectorAll('.tool-selection-item').forEach(el => {
                    el.classList.remove('selected');
                });
                item.classList.add('selected');
                selectedTool = availableTools.find(tool => tool.key === item.dataset.toolKey);
            }
        };

        toolListContainer.addEventListener('click', handleToolClick);
        
        // 确认按钮
        const handleConfirm = () => {
            if (selectedTool) {
                onSelect(selectedTool);
            }
            closeDialog();
        };
        
        // 取消按钮
        const handleCancel = () => {
            closeDialog();
        };
        
        // ESC键关闭
        const handleKeyDown = (e) => {
            if (e.key === 'Escape') {
                closeDialog();
            }
        };
        
        dialog.querySelector('.confirm-btn').addEventListener('click', handleConfirm);
        dialog.querySelector('.cancel-btn').addEventListener('click', handleCancel);
        document.addEventListener('keydown', handleKeyDown);
        
        // 关闭对话框并清理事件监听器
        const closeDialog = () => {
            overlay.classList.remove('show');
            setTimeout(() => {
                dialog.style.display = 'none';
                overlay.style.display = 'none';
                overlay.remove();
                
                // 清理事件监听器
                searchInput.removeEventListener('input', handleSearch);
                toolListContainer.removeEventListener('click', handleToolClick);
                dialog.querySelector('.confirm-btn').removeEventListener('click', handleConfirm);
                dialog.querySelector('.cancel-btn').removeEventListener('click', handleCancel);
                document.removeEventListener('keydown', handleKeyDown);
            }, 300);
        };
    }

    /**
     * 渲染工具选择列表
     * @private
     */
    renderToolSelectionList(container, tools) {
        container.innerHTML = tools.map(tool => `
            <div class="tool-selection-item" data-tool-key="${tool.key}">
                <div class="tool-info">
                    <div class="tool-selection-name">${tool.key}</div>
                    ${tool.description ? `<div class="tool-selection-desc">${tool.description}</div>` : ''}
                </div>
            </div>
        `).join('');
    }

    /**
     * 清空Agent详情表单
     */
    clearAgentDetails() {
        this.agentDetailForm.name.value = '';
        this.agentDetailForm.description.value = '';
        this.agentDetailForm.systemPrompt.value = '';
        this.agentDetailForm.nextStepPrompt.value = '';
        this.agentDetailForm.toolList.innerHTML = '';
    }

    /**
     * 收集表单数据
     */
    collectFormData() {
        const tools = Array.from(this.agentDetailForm.toolList.querySelectorAll('.tool-item'))
            .map(item => item.querySelector('.tool-name').textContent);

        return {
            name: this.agentDetailForm.name.value,
            description: this.agentDetailForm.description.value,
            systemPrompt: this.agentDetailForm.systemPrompt.value,
            nextStepPrompt: this.agentDetailForm.nextStepPrompt.value,
            availableTools: tools
        };
    }

    /**
     * 加载可用工具列表
     */
    async loadAvailableTools() {
        try {
            await agentConfigModel.loadAvailableTools();
        } catch (error) {
            this.showError('加载工具列表失败');
        }
    }

    /**
     * 显示错误消息
     */
    showError(message) {
        AdminUtils.showNotification(message, 'error');
    }

    /**
     * 显示成功消息
     */
    showSuccess(message) {
        AdminUtils.showNotification(message, 'success');
    }
}

// 创建全局UI实例
window.adminUI = new AdminUI();

// 在DOMContentLoaded事件中初始化UI
document.addEventListener('DOMContentLoaded', () => {
    adminUI.init();
});
