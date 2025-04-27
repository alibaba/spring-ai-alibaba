/**
 * mcp-ui.js - MCP配置界面UI交互
 */

class McpUI {
    constructor() {
        // MCP配置面板元素
        this.mcpConfigPanel = document.getElementById('mcp-config');
        // MCP表格容器
        this.mcpTableContainer = null;
        // 添加MCP服务器表单
        this.addMcpForm = null;
    }

    /**
     * 初始化UI
     */
    async init() {
        // 初始化MCP配置面板
        this.initMcpPanel();
        
        // 加载MCP服务器列表
        await this.loadMcpServers();
        
        // 绑定事件
        this.bindEvents();
    }

    /**
     * 初始化MCP配置面板
     */
    initMcpPanel() {
        // 清空当前内容
        this.mcpConfigPanel.innerHTML = '';
        
        // 添加标题
        const titleElement = document.createElement('h2');
        titleElement.className = 'panel-title';
        titleElement.textContent = 'MCP配置';
        this.mcpConfigPanel.appendChild(titleElement);
        
        // 创建MCP服务器列表容器
        const mcpLayout = document.createElement('div');
        mcpLayout.className = 'mcp-layout';
        this.mcpConfigPanel.appendChild(mcpLayout);
        
        // 创建表格容器
        this.mcpTableContainer = document.createElement('div');
        this.mcpTableContainer.className = 'mcp-table-container';
        mcpLayout.appendChild(this.mcpTableContainer);
        
        // 创建添加MCP服务器表单容器
        const addMcpContainer = document.createElement('div');
        addMcpContainer.className = 'add-mcp-container';
        mcpLayout.appendChild(addMcpContainer);
        
        // 添加标题
        const addMcpHeader = document.createElement('div');
        addMcpHeader.className = 'add-mcp-header';
        addMcpContainer.appendChild(addMcpHeader);
        
        const addMcpTitle = document.createElement('h3');
        addMcpTitle.className = 'add-mcp-title';
        addMcpTitle.textContent = '添加MCP服务器';
        addMcpHeader.appendChild(addMcpTitle);
        
        // 创建添加表单
        const addMcpForm = document.createElement('div');
        addMcpForm.className = 'mcp-form';
        addMcpContainer.appendChild(addMcpForm);
        
        // 添加表单内容
        addMcpForm.innerHTML = `
            <div class="mcp-form-group">
                <label for="mcp-connection-type">连接类型：</label>
                <div class="connection-type-options">
                    <div class="connection-type-option">
                        <input type="radio" id="mcp-connection-type-studio" name="mcp-connection-type" value="STUDIO" checked>
                        <label for="mcp-connection-type-studio">STUDIO</label>
                        <div class="connection-type-desc">本地mcp server，目前市面上主流的是这个</div>
                    </div>
                    <div class="connection-type-option">
                        <input type="radio" id="mcp-connection-type-sse" name="mcp-connection-type" value="SSE">
                        <label for="mcp-connection-type-sse">SSE</label>
                        <div class="connection-type-desc">一种远程mcp server 链接协议，目前推荐</div>
                    </div>
                </div>
            </div>
            <div class="mcp-form-group">
                <label for="mcp-config-input">mcp json配置：</label>
                <textarea id="mcp-config-input" rows="12" placeholder="请输入MCP服务器配置JSON。

        例如：
        {
        &quot;mcpServers&quot;: {
            &quot;github&quot;: {
            &quot;command&quot;: &quot;npx&quot;,
            &quot;args&quot;: [
                &quot;-y&quot;,
                &quot;@modelcontextprotocol/server-github&quot;
            ],
            &quot;env&quot;: {
                &quot;GITHUB_PERSONAL_ACCESS_TOKEN&quot;: &quot;<YOUR_TOKEN>&quot;
            }
            }
        }
        }"></textarea>
            </div>
            <div class="mcp-form-actions">
                <button class="submit-mcp-btn" id="submit-mcp-btn">提交</button>
            </div>
            <div class="mcp-form-instructions">
                <h3>使用说明：</h3>
                <ol>
                    <li>找到你要用的mcp server的配置json：
                        <ul class="indented-list">
                            <li><strong>本地(STDIO)</strong>：可以在<a href="https://mcp.so" target="_blank">mcp.so</a>上找到，需要你有Node.js环境并理解你要配置的json里面的每一个项，
                                做对应调整比如配置ak</li>
                            <li><strong>远程服务(SSE)</strong>：<a href="https://mcp.higress.ai/" target="_blank">mcp.higress.ai/</a>上可以找到，
                                有SSE和STREAMING两种，目前SSE协议更完备一些</li>
                        </ul>
                    </li>
                    <li>将json配置复制到上面的输入框，本地选STUDIO，远程选STREAMING或SSE，提交</li>
                    <li>这样mcp tools就注册成功了。默认会放在DEFAULT_AGENT下面，如果tools过多，对上下文有压力，多出的tools会被忽略</li>
                    <li>推荐在Agent配置里面，新建一个agent，然后增加指定的tools，这样可以极大减少冲突，增强tools被agent选择的准确性</li>
                </ol>
            </div>
        `;
        
        this.addMcpForm = {
            configInput: addMcpForm.querySelector('#mcp-config-input'),
            submitButton: addMcpForm.querySelector('#submit-mcp-btn')
        };
    }
    
    /**
     * 加载MCP服务器列表
     */
    async loadMcpServers() {
        try {
            // 显示加载中状态
            this.mcpTableContainer.innerHTML = '<div class="loading">加载中...</div>';
            
            // 获取所有MCP服务器配置
            const mcpServers = await McpAPI.getAllMcpServers();
            
            // 渲染MCP服务器列表
            this.renderMcpServers(mcpServers);
        } catch (error) {
            console.error('加载MCP服务器列表失败:', error);
            this.mcpTableContainer.innerHTML = `<div class="error-message">加载MCP服务器列表失败: ${error.message}</div>`;
        }
    }
    
    /**
     * 渲染MCP服务器列表
     * @param {Array} mcpServers - MCP服务器配置数组
     */
    renderMcpServers(mcpServers) {
        // 清空容器
        this.mcpTableContainer.innerHTML = '';
        
        // 如果没有MCP服务器，显示空状态
        if (!mcpServers || mcpServers.length === 0) {
            const emptyState = document.createElement('div');
            emptyState.className = 'empty-state';
            emptyState.innerHTML = `
                <div class="empty-state-icon">📂</div>
                <div class="empty-state-text">暂无MCP服务器配置</div>
            `;
            this.mcpTableContainer.appendChild(emptyState);
            return;
        }
        
        // 创建表格
        const table = document.createElement('table');
        table.className = 'mcp-table';
        
        // 创建表头
        const thead = document.createElement('thead');
        thead.innerHTML = `
            <tr>
                <th>ID</th>
                <th>服务器名称</th>
                <th>连接类型</th>
                <th>连接配置</th>
                <th>操作</th>
            </tr>
        `;
        table.appendChild(thead);
        
        // 创建表格内容
        const tbody = document.createElement('tbody');
        mcpServers.forEach(server => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${server.id}</td>
                <td class="mcp-server-name">${server.mcpServerName}</td>
                <td><span class="connection-type-badge">${server.connectionType}</span></td>
                <td>${this.formatConfig(server.connectionConfig)}</td>
                <td class="mcp-action-cell">
                    <button class="action-btn delete-mcp-btn" data-id="${server.id}">删除</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
        table.appendChild(tbody);
        
        // 添加表格到容器
        this.mcpTableContainer.appendChild(table);
    }
    
    /**
     * 格式化配置信息，截断长文本
     * @param {string} config - 配置信息
     * @returns {string} - 格式化后的配置信息
     */
    formatConfig(config) {
        if (!config) return '';
        
        // 如果配置信息太长，截断显示
        if (config.length > 50) {
            return config.substring(0, 50) + '...';
        }
        
        return config;
    }
    
    /**
     * 绑定事件
     */
    bindEvents() {
        // 提交按钮点击事件
        if (this.addMcpForm && this.addMcpForm.submitButton) {
            this.addMcpForm.submitButton.addEventListener('click', async () => {
                await this.handleAddMcpServer();
            });
        }
        
        // 删除按钮点击事件委托
        if (this.mcpTableContainer) {
            this.mcpTableContainer.addEventListener('click', async (event) => {
                if (event.target.classList.contains('delete-mcp-btn')) {
                    const id = event.target.dataset.id;
                    if (id) {
                        await this.handleRemoveMcpServer(id);
                    }
                }
            });
        }
    }
    
    /**
     * 处理添加MCP服务器
     */
    async handleAddMcpServer() {
        // 获取配置输入
        const configInput = this.addMcpForm.configInput.value.trim();
        if (!configInput) {
            this.showMessage('请输入MCP服务器配置', 'error');
            return;
        }
        
        // 获取选择的连接类型
        const connectionTypeRadios = document.getElementsByName('mcp-connection-type');
        let selectedConnectionType = 'STUDIO'; // 默认值
        for (const radio of connectionTypeRadios) {
            if (radio.checked) {
                selectedConnectionType = radio.value;
                break;
            }
        }
        
        try {
            
            // 创建请求数据，只包含原始JSON和连接类型
            const requestData = {
                connectionType: selectedConnectionType,
                configJson: configInput // 传递原始JSON字符串
            };
            
            // 调用API添加MCP服务器
            const result = await McpAPI.addMcpServer(requestData);
            
            if (result.success) {
                this.showMessage(result.message, 'success');
                // 清空输入
                this.addMcpForm.configInput.value = '';
                // 重新加载MCP服务器列表
                await this.loadMcpServers();
            } else {
                this.showMessage(result.message, 'error');
            }
        } catch (error) {
            console.error('添加MCP服务器失败:', error);
            this.showMessage('添加MCP服务器失败: ' + error.message, 'error');
        }
    }
    
    /**
     * 处理删除MCP服务器
     * @param {number} id - MCP服务器ID
     */
    async handleRemoveMcpServer(id) {
        // 确认删除
        if (!confirm('确定要删除这个MCP服务器配置吗？此操作不可恢复。')) {
            return;
        }
        
        try {
            // 删除MCP服务器
            const result = await McpAPI.removeMcpServer(id);
            
            if (result.success) {
                this.showMessage(result.message, 'success');
                // 重新加载MCP服务器列表
                await this.loadMcpServers();
            } else {
                this.showMessage(result.message, 'error');
            }
        } catch (error) {
            console.error('删除MCP服务器失败:', error);
            this.showMessage('删除MCP服务器失败: ' + error.message, 'error');
        }
    }
    
    /**
     * 显示消息
     * @param {string} message - 消息内容
     * @param {string} type - 消息类型：success, error, info
     */
    showMessage(message, type = 'info') {
        // 此处可以实现消息提示，根据实际情况选择合适的方式
        alert(`${type.toUpperCase()}: ${message}`);
    }
}

// 导出 UI 类，使其在其他文件中可用
window.McpUI = McpUI;

// 初始化MCP配置
document.addEventListener('DOMContentLoaded', () => {
    // 创建MCP配置UI实例
    window.mcpUI = new McpUI();
    
    // 当用户点击MCP配置导航时初始化UI
    document.querySelector('.category-item[data-category="mcp"]').addEventListener('click', () => {
        window.mcpUI.init();
    });
});
