/**
 * 计划模板页面的主要JavaScript文件
 * 负责处理用户输入、发送API请求、展示计划结果等功能
 */

// 全局变量，保存当前计划状态
let currentPlanTemplateId = null; // 存储计划模板ID
let currentPlanId = null; // 存储计划执行ID
let currentPlanData = null;
let isGenerating = false;
let isExecuting = false;
let planTemplateList = []; // 存储计划模板列表

// 版本控制相关变量
let planVersions = []; // 存储所有版本的计划JSON
let currentVersionIndex = -1; // 当前版本索引

// DOM 元素引用
let planPromptInput;
let generatePlanBtn;
let jsonEditor;
let runPlanBtn;
let modifyPlanBtn;
let clearBtn;
let apiUrlElement;

// 执行确认对话框元素
let executionDialog;
let planSummaryElement;
let stepCountElement;
let cancelExecutionBtn;
let confirmExecutionBtn;
let dialogCloseBtn;

// 轮询相关变量
let pollTimer = null;
let lastSequenceSize = 0;
const POLL_INTERVAL = 3000; // 3秒轮询间隔
let isPolling = false;

/**
 * 初始化函数，设置事件监听器
 */
function init() {
    // 获取DOM元素
    planPromptInput = document.getElementById('plan-prompt');
    generatePlanBtn = document.getElementById('generatePlanBtn');
    jsonEditor = document.getElementById('plan-json-editor');
    runPlanBtn = document.getElementById('runPlanBtn');
    modifyPlanBtn = document.getElementById('modifyPlanBtn');
    clearBtn = document.getElementById('clearBtn');
    apiUrlElement = document.querySelector('.api-url');

    // 执行确认对话框元素
    executionDialog = document.getElementById('executionDialog');
    planSummaryElement = document.getElementById('plan-summary');
    stepCountElement = document.getElementById('step-count');
    cancelExecutionBtn = document.getElementById('cancelExecutionBtn');
    confirmExecutionBtn = document.getElementById('confirmExecutionBtn');
    dialogCloseBtn = document.querySelector('#executionDialog .close-btn');
    
    // 绑定按钮事件
    generatePlanBtn.addEventListener('click', handleGeneratePlan);
    runPlanBtn.addEventListener('click', handleRunPlanClick);
    modifyPlanBtn.addEventListener('click', handleModifyPlan);
    clearBtn.addEventListener('click', handleClearInput);
    
    // 绑定版本控制按钮事件
    document.getElementById('rollbackJsonBtn').addEventListener('click', handleRollbackJson);
    document.getElementById('restoreJsonBtn').addEventListener('click', handleRestoreJson);
    document.getElementById('compareJsonBtn').addEventListener('click', handleCompareJson);
    
    // 对话框事件
    cancelExecutionBtn.addEventListener('click', closeExecutionDialog);
    confirmExecutionBtn.addEventListener('click', executePlan);
    dialogCloseBtn.addEventListener('click', closeExecutionDialog);
    
    // 初始状态
    updateUIState();
    
    // 加载计划模板列表
    loadPlanTemplateList();
    
    console.log('计划模板页面初始化完成');
}

/**
 * 加载计划模板列表并更新左侧边栏
 */
async function loadPlanTemplateList() {
    try {
        // 调用API获取计划模板列表
        const response = await ManusAPI.getAllPlanTemplates();
        planTemplateList = response.templates || [];
        
        // 更新左侧边栏
        updatePlanTemplateListUI();
    } catch (error) {
        console.error('加载计划模板列表失败:', error);
    }
}

/**
 * 更新左侧边栏的计划模板列表
 */
function updatePlanTemplateListUI() {
    const taskListEl = document.querySelector('.task-list');
    if (!taskListEl) {
        console.error('找不到任务列表元素');
        return;
    }
    
    // 清空现有列表
    taskListEl.innerHTML = '';
    
    if (planTemplateList.length === 0) {
        // 如果没有计划模板，显示提示信息
        const emptyItem = document.createElement('li');
        emptyItem.className = 'task-item empty';
        emptyItem.textContent = '没有可用的计划模板';
        taskListEl.appendChild(emptyItem);
        return;
    }
    
    // 按更新时间排序，最新的在前面
    const sortedTemplates = [...planTemplateList].sort((a, b) => {
        const timeA = new Date(a.updateTime || a.createTime);
        const timeB = new Date(b.updateTime || b.createTime);
        return timeB - timeA; // 降序排序
    });
    
    // 添加计划模板项
    sortedTemplates.forEach(template => {
        const listItem = document.createElement('li');
        listItem.className = 'task-item';
        if (template.id === currentPlanTemplateId) {
            listItem.classList.add('selected');
        }
        
        // 计算相对时间
        const updateTime = new Date(template.updateTime || template.createTime);
        const relativeTime = getRelativeTimeString(updateTime);
        
        // 创建HTML结构
        listItem.innerHTML = `
            <div class="task-icon">[📄]</div>
            <div class="task-details">
                <div class="task-title">${template.title || '未命名计划'}</div>
                <div class="task-preview">${truncateText(template.description || '', 40)}</div>
            </div>
            <div class="task-time">${relativeTime}</div>
        `;
        
        // 添加点击事件
        listItem.addEventListener('click', () => handlePlanTemplateClick(template));
        
        taskListEl.appendChild(listItem);
    });
    
    // 更新新建计划按钮文本
    const newTaskBtn = document.querySelector('.new-task-btn');
    if (newTaskBtn) {
        newTaskBtn.innerHTML = '<span class="icon-add"></span> 新建计划 <span class="shortcut">⌘ K</span>';
        newTaskBtn.addEventListener('click', handleClearInput);
    }
}

/**
 * 生成计划
 */
async function handleGeneratePlan() {
    // 获取用户输入
    const query = planPromptInput.value.trim();
    if (!query) {
        alert('请输入计划需求描述');
        return;
    }
    
    // 避免重复提交
    if (isGenerating) {
        return;
    }
    
    try {
        isGenerating = true;
        updateUIState();
        
        // 获取可能存在的JSON数据
        let existingJson = null;
        if (jsonEditor.value.trim()) {
            try {
                // 尝试解析JSON数据
                JSON.parse(jsonEditor.value.trim());
                existingJson = jsonEditor.value.trim();
            } catch (e) {
                console.log('现有JSON数据格式无效，将不使用它');
            }
        }
        
        let response;
        
        // 检查是否有当前计划模板ID，决定是更新还是创建新计划
        if (currentPlanTemplateId && (currentPlanData || existingJson)) {
            console.log('正在更新现有计划模板:', currentPlanTemplateId);
            // 调用更新接口
            response = await ManusAPI.updatePlanTemplate(currentPlanTemplateId, query, existingJson);
            console.log('计划模板更新成功');
        } else {
            console.log('正在创建新计划模板');
            // 调用创建接口
            response = await ManusAPI.generatePlan(query, existingJson);
            // 更新计划模板ID
            currentPlanTemplateId = response.planTemplateId;
            console.log('创建新计划模板成功:', currentPlanTemplateId);
        }
        
        // 更新计划数据
        currentPlanData = response.plan;
        
        // 直接显示计划数据
        if (currentPlanData) {
            // 显示计划JSON数据
            const jsonString = JSON.stringify(currentPlanData, null, 2);
            jsonEditor.value = jsonString;
            
            // 保存此版本到版本历史
            saveToVersionHistory(jsonString);
            
            // 更新API URL
            if (currentPlanTemplateId) {
                apiUrlElement.textContent = `http://your-domain/api/plan-template/execute/${currentPlanTemplateId}`;
            }
        } else if (response.planJson) {
            // 如果plan对象解析失败但有原始JSON字符串，直接显示原始JSON
            jsonEditor.value = response.planJson;
            // 保存此版本到版本历史
            saveToVersionHistory(response.planJson);
        }
        
        // 计划生成完成
        isGenerating = false;
        updateUIState();
        
    } catch (error) {
        console.error('生成计划出错:', error);
        alert('生成计划失败: ' + error.message);
        isGenerating = false;
        updateUIState();
    }
}

/**
 * 开始轮询计划状态
 */
function startPolling() {
    if (pollTimer) {
        clearInterval(pollTimer);
    }
    
    // 立即执行一次
    pollPlanStatus();
    
    // 设置定时轮询
    pollTimer = setInterval(pollPlanStatus, POLL_INTERVAL);
}

/**
 * 停止轮询
 */
function stopPolling() {
    if (pollTimer) {
        clearInterval(pollTimer);
        pollTimer = null;
    }
}

/**
 * 轮询计划状态
 */
async function pollPlanStatus() {
    if (!currentPlanTemplateId || isPolling) {
        return;
    }
    
    try {
        isPolling = true;
        
        // 调用获取计划详情的API
        const planData = await ManusAPI.getDetails(currentPlanTemplateId);
        
        // 如果planData为null（可能404或其他错误），继续轮询
        if (!planData) {
            isPolling = false;
            return;
        }
        
        // 处理计划数据
        handlePlanData(planData);
        
        // 如果计划仍在生成中，继续轮询
        if (!planData.completed && planData.steps && planData.steps.length > 0) {
            isPolling = false;
        } else {
            // 计划生成完成
            isGenerating = false;
            updateUIState();
            stopPolling();
        }
    } catch (error) {
        console.error('轮询计划状态出错:', error);
        isPolling = false;
    }
}

/**
 * 处理计划数据
 */
function handlePlanData(planData) {
    // 保存当前计划数据
    currentPlanData = planData;
    
    // 显示计划JSON数据
    const jsonString = JSON.stringify(planData, null, 2);
    jsonEditor.value = jsonString;
    
    // 保存此版本到版本历史
    saveToVersionHistory(jsonString);
    
    // 更新API URL
    if (currentPlanTemplateId) {
        apiUrlElement.textContent = `http://your-domain/api/plan-template/execute/${currentPlanTemplateId}`;
    }
    
    // 更新UI状态
    updateUIState();
    
    // 如果有新的智能体执行记录，且sequence size增加了，更新UI
    if (planData.agentExecutionSequence) {
        const currentSize = planData.agentExecutionSequence.length;
        if (currentSize > lastSequenceSize) {
            // 只处理新增的记录
            lastSequenceSize = currentSize;
        }
    }
    
    isPolling = false;
}

/**
 * 处理执行计划按钮点击
 */
function handleRunPlanClick() {
    // 检查是否正在执行
    if (isExecuting) {
        return;
    }
    
    // 检查是否有计划模板ID和JSON数据可以执行
    if (!currentPlanTemplateId) {
        alert('没有可执行的计划模板');
        return;
    }
    
    let jsonContent = jsonEditor.value.trim();
    if (!jsonContent) {
        alert('计划数据不能为空');
        return;
    }
    
    try {
        // 尝试解析JSON
        const planData = JSON.parse(jsonContent);
        
        // 显示执行确认对话框
        if (planData) {
            planSummaryElement.textContent = planData.title || '未命名计划';
            stepCountElement.textContent = (planData.steps && planData.steps.length) || 0;
        } else {
            planSummaryElement.textContent = '计划详情不可用';
            stepCountElement.textContent = '?';
        }
        
        openExecutionDialog();
    } catch (e) {
        console.error('JSON解析错误', e);
        alert('无效的JSON格式: ' + e.message);
    }
}

/**
 * 执行计划
 */
async function executePlan() {
    if (isExecuting) {
        closeExecutionDialog();
        return;
    }
    
    if (!currentPlanTemplateId) {
        alert('没有可执行的计划模板');
        closeExecutionDialog();
        return;
    }
    
    try {
        isExecuting = true;
        updateUIState();
        closeExecutionDialog();
        
        let jsonContent = jsonEditor.value.trim();
        let response;
        
        // 检查JSON内容是否已修改
        let isModified = true;
        if (currentVersionIndex >= 0 && planVersions.length > 0) {
            const latestVersion = planVersions[currentVersionIndex];
            isModified = jsonContent !== latestVersion;
        }
        
        if (isModified) {
            // JSON已修改，先保存新版本
            // 保存此版本到版本历史
            saveToVersionHistory(jsonContent);
            
            // 保存到服务器
            await savePlanToServer(currentPlanTemplateId, jsonContent);
            
            console.log('修改后的JSON已保存，使用计划模板ID执行');
        }
        
        // 使用现有计划模板ID执行
        response = await ManusAPI.executePlan(currentPlanTemplateId);
        
        // 更新当前计划ID
        currentPlanId = response.planId;
        
        // 提示用户
        alert('计划执行请求已提交，可以在右侧边栏查看执行进度');
        
        // 更新API URL
        if (currentPlanTemplateId) {
            apiUrlElement.textContent = `http://your-domain/api/plan-template/execute/${currentPlanTemplateId}`;
        }
        
        // 开始轮询执行状态
        lastSequenceSize = 0; // 重置序列大小，以便接收所有执行记录
        startPolling();
        
    } catch (error) {
        console.error('执行计划出错:', error);
        alert('执行计划失败: ' + error.message);
    } finally {
        isExecuting = false;
        updateUIState();
    }
}

/**
 * 修改计划
 */
function handleModifyPlan() {
    if (!currentPlanId) {
        alert('没有计划可以保存');
        return;
    }
    
    // 尝试解析当前JSON编辑器中的内容
    let jsonContent = jsonEditor.value.trim();
    try {
        if (jsonContent) {
            // 验证JSON格式是否正确
            JSON.parse(jsonContent);
            
            // 检查是否与最新版本相同
            let isModified = true;
            if (currentVersionIndex >= 0 && planVersions.length > 0) {
                const latestVersion = planVersions[currentVersionIndex];
                isModified = jsonContent !== latestVersion;
            }
            
            if (isModified) {
                // 保存当前版本到历史记录
                saveToVersionHistory(jsonContent);
                
                // 如果有当前计划ID，保存修改到后端
                savePlanToServer(currentPlanId, jsonContent);
                
                alert('计划已保存');
            } else {
                console.log('计划未修改，忽略保存操作');
                // 可选：轻量级提示
                alert('计划未修改，无需保存');
            }
        } else {
            alert('计划内容不能为空');
        }
    } catch (e) {
        console.error('JSON解析错误', e);
        alert('无效的JSON格式: ' + e.message);
    }
}

/**
 * 保存计划到服务器
 * @param {string} planId - 计划ID
 * @param {string} jsonContent - JSON内容
 */
async function savePlanToServer(planId, jsonContent) {
    try {
        // 使用api.js中提供的ManusAPI.savePlan函数
        const result = await ManusAPI.savePlan(planId, jsonContent);
        return result;
    } catch (error) {
        console.error('保存计划失败:', error);
        throw error;
    }
}

/**
 * 清空输入
 */
function handleClearInput() {
    planPromptInput.value = '';
    jsonEditor.value = '';
    currentPlanId = null;
    currentPlanData = null;
    updateUIState();
}

/**
 * 打开执行确认对话框
 */
function openExecutionDialog() {
    executionDialog.classList.add('show');
}

/**
 * 关闭执行确认对话框
 */
function closeExecutionDialog() {
    executionDialog.classList.remove('show');
}

/**
 * 更新UI状态
 */
function updateUIState() {
    // 更新按钮状态
    generatePlanBtn.disabled = isGenerating;
    runPlanBtn.disabled = !currentPlanId || isGenerating || isExecuting;
    modifyPlanBtn.disabled = isGenerating || isExecuting || !currentPlanId;
    
    // 更新按钮文本
    if (isGenerating) {
        generatePlanBtn.innerHTML = '<span class="icon-loader"></span> 生成中...';
    } else {
        // 当计划模板ID不为空且有数据时，显示"优化计划"而不是"生成计划"
        if (currentPlanTemplateId && (currentPlanData || jsonEditor.value.trim())) {
            generatePlanBtn.innerHTML = '<span class="icon-placeholder"></span> 优化计划';
        } else {
            generatePlanBtn.innerHTML = '<span class="icon-placeholder"></span> 生成计划';
        }
    }
    
    if (isExecuting) {
        runPlanBtn.innerHTML = '<span class="icon-loader"></span> 执行中...';
    } else {
        runPlanBtn.innerHTML = '<span class="icon-run"></span> 执行计划';
    }
    
    // 更新版本控制按钮状态
    const rollbackBtn = document.getElementById('rollbackJsonBtn');
    const restoreBtn = document.getElementById('restoreJsonBtn');
    
    if (rollbackBtn && restoreBtn) {
        rollbackBtn.disabled = planVersions.length <= 1 || currentVersionIndex <= 0;
        restoreBtn.disabled = planVersions.length <= 1 || currentVersionIndex >= planVersions.length - 1;
    }
}

/**
 * 保存当前JSON到版本历史
 * @param {string} jsonText - JSON文本内容
 */
function saveToVersionHistory(jsonText) {
    try {
        // 如果内容与当前版本相同，则不保存
        if (currentVersionIndex >= 0 && 
            planVersions[currentVersionIndex] === jsonText) {
            return;
        }
        
        // 如果用户从历史版本回滚后修改，则清除该版本之后的所有版本
        if (currentVersionIndex >= 0 && currentVersionIndex < planVersions.length - 1) {
            planVersions = planVersions.slice(0, currentVersionIndex + 1);
        }
        
        // 添加新版本
        planVersions.push(jsonText);
        currentVersionIndex = planVersions.length - 1;
        
        console.log(`保存版本 ${currentVersionIndex + 1}/${planVersions.length}`);
    } catch (e) {
        console.error('保存版本失败', e);
    }
}

/**
 * 处理回滚JSON按钮点击
 */
function handleRollbackJson() {
    if (planVersions.length <= 1 || currentVersionIndex <= 0) {
        alert('没有更早的版本可回滚');
        return;
    }
    
    currentVersionIndex--;
    jsonEditor.value = planVersions[currentVersionIndex];
    console.log(`已回滚到版本 ${currentVersionIndex + 1}/${planVersions.length}`);
    // 更新UI状态以反映版本变化
    updateUIState();
}

/**
 * 处理恢复JSON按钮点击
 */
function handleRestoreJson() {
    if (planVersions.length <= 1 || currentVersionIndex >= planVersions.length - 1) {
        alert('没有更新的版本可恢复');
        return;
    }
    
    currentVersionIndex++;
    jsonEditor.value = planVersions[currentVersionIndex];
    console.log(`已恢复到版本 ${currentVersionIndex + 1}/${planVersions.length}`);
    // 更新UI状态以反映版本变化
    updateUIState();
}

/**
 * 处理对比JSON按钮点击
 */
function handleCompareJson() {
    if (planVersions.length <= 1) {
        alert('没有多个版本可供对比');
        return;
    }
    
    // 创建一个简单的版本选择对话框
    const currentVersion = planVersions[currentVersionIndex];
    const versionOptions = planVersions.map((_, i) => 
        `<option value="${i}" ${i === currentVersionIndex ? 'selected' : ''}>版本 ${i + 1}</option>`).join('');
    
    const dialog = document.createElement('div');
    dialog.className = 'dialog-overlay show';
    dialog.innerHTML = `
        <div class="dialog-container">
            <div class="dialog-header">
                <h3>版本对比</h3>
                <button class="close-btn" id="closeCompareDialog">&times;</button>
            </div>
            <div class="dialog-content">
                <div style="display: flex; margin-bottom: 15px;">
                    <div style="flex: 1; margin-right: 10px;">
                        <label>对比版本: </label>
                        <select id="compareVersionSelect">
                            ${versionOptions}
                        </select>
                    </div>
                    <div style="flex: 1;">
                        <label>与版本: </label>
                        <select id="targetVersionSelect">
                            ${versionOptions}
                        </select>
                    </div>
                </div>
                <div style="display: flex; height: 300px;">
                    <textarea readonly style="flex: 1; margin-right: 5px; font-family: monospace;" id="compareVersionText"></textarea>
                    <textarea readonly style="flex: 1; font-family: monospace;" id="targetVersionText"></textarea>
                </div>
            </div>
            <div class="dialog-footer">
                <button class="secondary-btn" id="closeCompareBtn">关闭</button>
            </div>
        </div>
    `;
    
    document.body.appendChild(dialog);
    
    // 获取元素引用
    const compareVersionSelect = document.getElementById('compareVersionSelect');
    const targetVersionSelect = document.getElementById('targetVersionSelect');
    const compareVersionText = document.getElementById('compareVersionText');
    const targetVersionText = document.getElementById('targetVersionText');
    const closeCompareBtn = document.getElementById('closeCompareBtn');
    const closeCompareDialog = document.getElementById('closeCompareDialog');
    
    // 设置左侧默认显示前一个版本（当前版本-1）
    if (currentVersionIndex > 0) {
        compareVersionSelect.value = currentVersionIndex - 1;
    }
    
    // 设置初始内容
    updateCompareContent();
    
    // 绑定事件
    compareVersionSelect.addEventListener('change', updateCompareContent);
    targetVersionSelect.addEventListener('change', updateCompareContent);
    closeCompareBtn.addEventListener('click', () => document.body.removeChild(dialog));
    closeCompareDialog.addEventListener('click', () => document.body.removeChild(dialog));
    
    // 更新对比内容
    function updateCompareContent() {
        const compareIndex = parseInt(compareVersionSelect.value, 10);
        const targetIndex = parseInt(targetVersionSelect.value, 10);
        
        compareVersionText.value = planVersions[compareIndex] || '';
        targetVersionText.value = planVersions[targetIndex] || '';
        
        console.log(`对比版本 ${compareIndex + 1} 和版本 ${targetIndex + 1}`);
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', init);
