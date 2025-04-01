/**
 * admin-ui.js - 管理界面UI处理模块
 * 负责渲染和更新管理界面
 */

/**
 * 管理UI类
 * 处理配置界面的所有UI操作
 */
class AdminUI {
    /**
     * 初始化管理界面
     */
    static async initialize() {
        // 初始化其他UI元素
        this.initCategories();
        
        // 默认加载基础配置(manus组)
        await this.loadBasicConfigs();
    }
    
    /**
     * 初始化分类导航
     */
    static initCategories() {
        const categories = document.querySelectorAll('.category-item');
        categories.forEach(category => {
            category.addEventListener('click', () => {
                // 移除所有分类的active类
                categories.forEach(item => item.classList.remove('active'));
                
                // 添加当前分类的active类
                category.classList.add('active');
                
                // 获取分类数据
                const categoryId = category.getAttribute('data-category');
                
                // 显示对应的配置面板
                this.showConfigPanel(categoryId);
                
                // 如果是基础配置面板，加载manus组配置
                if (categoryId === 'basic') {
                    this.loadBasicConfigs();
                }
            });
        });
    }
    
    /**
     * 显示指定的配置面板
     * @param {string} panelId - 面板ID
     */
    static showConfigPanel(panelId) {
        // 隐藏所有配置面板
        const panels = document.querySelectorAll('.config-panel');
        panels.forEach(panel => panel.classList.remove('active'));
        
        // 显示指定的配置面板
        const targetPanel = document.getElementById(`${panelId}-config`);
        if (targetPanel) {
            targetPanel.classList.add('active');
        }
    }
    
    /**
     * 加载基础配置(manus组)
     */
    static async loadBasicConfigs() {
        try {
            const manusConfigs = await configModel.loadConfigByGroup('manus');
            this.renderManusConfigs(manusConfigs);
        } catch (error) {
            console.error('加载基础配置失败:', error);
            this.showNotification('加载配置失败，请重试', 'error');
        }
    }
    
    /**
     * 渲染manus组的配置
     * @param {Array} configs - 配置数组
     */
    static renderManusConfigs(configs) {
        // 获取基础配置面板
        const basicPanel = document.getElementById('basic-config');
        if (!basicPanel) {
            console.error('未找到基础配置面板');
            return;
        }
        
        // 清空面板（保留标题）
        basicPanel.innerHTML = '<h2 class="panel-title">基础配置</h2>';
        
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
    static formatSubGroupName(subGroup) {
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
    static createConfigItem(config) {
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
            case 'BOOLEAN':
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
    static addSaveButton(panel, groupName) {
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
                this.showNotification(
                    result.success ? '配置保存成功' : result.message,
                    result.success ? 'success' : 'error'
                );
            } catch (error) {
                console.error('保存配置失败:', error);
                this.showNotification('保存失败: ' + (error.message || '未知错误'), 'error');
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
     * 显示通知消息
     * @param {string} message - 通知消息
     * @param {string} type - 消息类型 (success, error, warning, info)
     */
    static showNotification(message, type = 'info') {
        // 如果已存在通知，先移除
        const existingNotification = document.querySelector('.notification');
        if (existingNotification) {
            existingNotification.remove();
        }
        
        // 创建通知元素
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.textContent = message;
        
        // 添加到文档
        document.body.appendChild(notification);
        
        // 3秒后自动移除
        setTimeout(() => {
            notification.classList.add('hide');
            setTimeout(() => notification.remove(), 500);
        }, 3000);
    }
}

// 在DOMContentLoaded事件中初始化UI
document.addEventListener('DOMContentLoaded', () => {
    AdminUI.initialize();
});
