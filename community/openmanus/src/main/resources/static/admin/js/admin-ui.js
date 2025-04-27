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
        
        // 复制工具列表以便于排序
        const toolsCopy = [...availableTools];
        
        // 按服务组对工具进行分组
        const groupedTools = this.groupToolsByServiceGroup(toolsCopy);
        
        // 渲染分组后的工具选择列表
        this.renderToolSelectionList(toolListContainer, groupedTools);
        
        // 排序功能
        const handleSort = (e) => {
            const sortMethod = e.target.value;
            let sortedTools = [...toolsCopy];
            
            switch(sortMethod) {
                case 'name':
                    sortedTools.sort((a, b) => (a.name || a.key).localeCompare(b.name || b.key));
                    break;
                case 'enabled':
                    sortedTools.sort((a, b) => (b.enabled ? 1 : 0) - (a.enabled ? 1 : 0));
                    break;
                case 'group':
                default:
                    // 默认按服务组排序，在 groupToolsByServiceGroup 中处理
                    break;
            }
            
            // 更新当前工具列表
            const filteredGroupedTools = this.groupToolsByServiceGroup(sortedTools);
            
            // 重新渲染工具列表
            this.renderToolSelectionList(toolListContainer, filteredGroupedTools);
            
            // 重新绑定事件（因为DOM已重新生成）
            addToolListEventListeners();
        };
        
        // 搜索功能
        const handleSearch = (e) => {
            const searchText = e.target.value.toLowerCase();
            const filteredTools = toolsCopy.filter(tool => 
                tool.key.toLowerCase().includes(searchText) || 
                (tool.name && tool.name.toLowerCase().includes(searchText)) ||
                (tool.description && tool.description.toLowerCase().includes(searchText)) ||
                (tool.serviceGroup && tool.serviceGroup.toLowerCase().includes(searchText))
            );
            
            // 重新分组过滤后的工具
            const filteredGroupedTools = this.groupToolsByServiceGroup(filteredTools);
            
            // 渲染过滤后的分组工具
            this.renderToolSelectionList(toolListContainer, filteredGroupedTools);
            
            // 重新绑定事件（因为DOM已重新生成）
            addToolListEventListeners();
        };
        
        // 处理工具启用/禁用状态
        const handleToolEnableToggle = (e) => {
            if (!e.target.classList.contains('tool-enable-checkbox')) return;
            
            const toolItem = e.target.closest('.tool-selection-item');
            if (!toolItem) return;
            
            const toolKey = toolItem.dataset.toolKey;
            const tool = toolsCopy.find(t => t.key === toolKey);
            
            if (tool) {
                tool.enabled = e.target.checked;
            }
        };
        
        // 处理组级别启用/禁用
        const handleGroupEnableToggle = (e) => {
            if (!e.target.classList.contains('group-enable-checkbox')) return;
            
            const groupHeader = e.target.closest('.tool-group-header');
            if (!groupHeader) return;
            
            const groupName = groupHeader.dataset.group;
            const isEnabled = e.target.checked;
            
            // 更新该组中所有工具的启用状态
            toolsCopy.forEach(tool => {
                if ((tool.serviceGroup || '未分组') === groupName) {
                    tool.enabled = isEnabled;
                }
            });
            
            // 更新UI中该组所有工具的复选框状态
            const groupContent = groupHeader.nextElementSibling;
            const checkboxes = groupContent.querySelectorAll('.tool-enable-checkbox');
            checkboxes.forEach(checkbox => {
                checkbox.checked = isEnabled;
            });
        };
        
        // 显示对话框和遮罩
        overlay.style.display = 'block';
        dialog.style.display = 'block';
        setTimeout(() => overlay.classList.add('show'), 10);
        
        // 处理工具选择
        let selectedTools = [];
        const handleToolSelection = (e) => {
            // 忽略启用/禁用复选框点击导致的事件冒泡
            if (e.target.classList.contains('tool-enable-checkbox')) return;
            
            // 处理单个工具选择
            const toolItem = e.target.closest('.tool-selection-item');
            if (toolItem) {
                const isSelected = toolItem.classList.toggle('selected');
                const toolKey = toolItem.dataset.toolKey;
                
                if (isSelected) {
                    // 添加到已选工具列表
                    const tool = toolsCopy.find(t => t.key === toolKey);
                    if (tool && !selectedTools.some(t => t.key === toolKey)) {
                        selectedTools.push(tool);
                    }
                } else {
                    // 从已选工具列表中移除
                    selectedTools = selectedTools.filter(t => t.key !== toolKey);
                }
            }
        };
        
        // 处理组标题点击（全选/取消全选）
        const handleGroupToggle = (e) => {
            if (!e.target.classList.contains('group-toggle')) return;
            
            const groupHeader = e.target.closest('.tool-group-header');
            if (!groupHeader) return;
            
            const groupName = groupHeader.dataset.group;
            const groupContent = groupHeader.nextElementSibling;
            const groupItems = groupContent.querySelectorAll('.tool-selection-item');
            
            // 检查当前组内是否所有工具都被选中
            const allSelected = Array.from(groupItems).every(item => item.classList.contains('selected'));
            
            // 根据当前状态进行全选或取消全选
            groupItems.forEach(item => {
                const toolKey = item.dataset.toolKey;
                
                if (allSelected) {
                    // 取消全选
                    item.classList.remove('selected');
                    selectedTools = selectedTools.filter(t => t.key !== toolKey);
                } else {
                    // 全选
                    item.classList.add('selected');
                    
                    if (!selectedTools.some(t => t.key === toolKey)) {
                        const tool = toolsCopy.find(t => t.key === toolKey);
                        if (tool) {
                            selectedTools.push(tool);
                        }
                    }
                }
            });
        };
        
        // 为工具列表添加事件监听器
        const addToolListEventListeners = () => {
            // 绑定排序事件
            const sortSelect = toolListContainer.querySelector('.tool-sort-select');
            if (sortSelect) {
                sortSelect.addEventListener('change', handleSort);
            }
            
            // 绑定工具选择事件
            const toolItems = toolListContainer.querySelectorAll('.tool-selection-item');
            toolItems.forEach(item => {
                item.addEventListener('click', handleToolSelection);
            });
            
            // 绑定工具启用状态切换事件
            const toolEnableCheckboxes = toolListContainer.querySelectorAll('.tool-enable-checkbox');
            toolEnableCheckboxes.forEach(checkbox => {
                checkbox.addEventListener('change', handleToolEnableToggle);
            });
            
            // 绑定组切换事件
            const groupToggles = toolListContainer.querySelectorAll('.group-toggle');
            groupToggles.forEach(toggle => {
                toggle.addEventListener('click', handleGroupToggle);
            });
            
            // 绑定组启用状态切换事件
            const groupEnableCheckboxes = toolListContainer.querySelectorAll('.group-enable-checkbox');
            groupEnableCheckboxes.forEach(checkbox => {
                checkbox.addEventListener('change', handleGroupEnableToggle);
            });
        };
        
        // 初始绑定事件
        searchInput.addEventListener('input', handleSearch);
        addToolListEventListeners();
        
        // 确认按钮
        const handleConfirm = () => {
            if (selectedTools.length > 0) {
                // 调用回调函数，传递所有选中的工具
                selectedTools.forEach(tool => onSelect(tool));
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
                dialog.querySelector('.confirm-btn').removeEventListener('click', handleConfirm);
                dialog.querySelector('.cancel-btn').removeEventListener('click', handleCancel);
                document.removeEventListener('keydown', handleKeyDown);
            }, 300);
        };
    }
    
    /**
     * 按服务组对工具进行分组
     * @param {Array} tools - 工具列表
     * @returns {Object} - 按serviceGroup分组后的工具对象
     */
    groupToolsByServiceGroup(tools) {
        const groupedTools = {};
        
        // 将工具按照serviceGroup分组
        tools.forEach(tool => {
            const group = tool.serviceGroup || '未分组';
            if (!groupedTools[group]) {
                groupedTools[group] = [];
            }
            groupedTools[group].push(tool);
        });
        
        return groupedTools;
    }

    /**
     * 渲染工具选择列表
     * @private
     */
    renderToolSelectionList(container, groupedTools) {
        let html = '';
        
        // 添加排序选项
        html += `
            <div class="tool-sort-options">
                <label>排序方式：</label>
                <select class="tool-sort-select">
                    <option value="group">按服务组排序</option>
                    <option value="name">按名称排序</option>
                    <option value="enabled">按启用状态排序</option>
                </select>
            </div>
        `;
        
        // 遍历每个组
        Object.keys(groupedTools).sort().forEach(group => {
            const tools = groupedTools[group];
            
            // 添加组标题
            html += `
                <div class="tool-group">
                    <div class="tool-group-header" data-group="${group}">
                        <span class="group-name">${group}</span>
                        <span class="group-count">(${tools.length})</span>
                        <div class="group-actions">
                            <span class="group-toggle">选择全部</span>
                            <label class="group-enable-all">
                                <input type="checkbox" class="group-enable-checkbox"> 启用全部
                            </label>
                        </div>
                    </div>
                    <div class="tool-group-content">
            `;
            
            // 添加该组下的所有工具
            tools.forEach(tool => {
                html += `
                    <div class="tool-selection-item" data-tool-key="${tool.key}" data-group="${group}">
                        <div class="tool-info">
                            <div class="tool-selection-name">${tool.name || tool.key}</div>
                            ${tool.description ? `<div class="tool-selection-desc">${tool.description}</div>` : ''}
                        </div>
                        <div class="tool-actions">
                            <label class="tool-enable-switch">
                                <input type="checkbox" class="tool-enable-checkbox" ${tool.enabled ? 'checked' : ''}>
                                <span class="tool-enable-slider"></span>
                            </label>
                        </div>
                    </div>
                `;
            });
            
            // 关闭组容器
            html += `
                    </div>
                </div>
            `;
        });
        
        container.innerHTML = html;
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
