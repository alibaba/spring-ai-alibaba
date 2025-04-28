/**
 * 计划模板辅助函数 - 提供操作计划模板列表的功能
 */

/**
 * 将时间戳转换为相对时间字符串
 * @param {Date} date - 日期对象
 * @returns {string} - 相对时间字符串
 */
function getRelativeTimeString(date) {
    const now = new Date();
    const diff = now - date;
    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    
    if (days > 30) {
        return date.toLocaleDateString('zh-CN');
    } else if (days > 0) {
        return `${days}天前`;
    } else if (hours > 0) {
        return `${hours}小时前`;
    } else if (minutes > 0) {
        return `${minutes}分钟前`;
    } else {
        return '刚刚';
    }
}

/**
 * 截断文本至指定长度
 * @param {string} text - 文本内容
 * @param {number} maxLength - 最大长度
 * @returns {string} - 截断后的文本
 */
function truncateText(text, maxLength) {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substr(0, maxLength - 3) + '...';
}

/**
 * 处理计划模板项点击事件
 * @param {Object} template - 计划模板对象
 */
async function handlePlanTemplateClick(template) {
    if (isGenerating || isExecuting) {
        return;
    }
    
    try {
        // 设置当前计划模板ID
        currentPlanTemplateId = template.id;
        
        // 获取计划模板的最新版本
        const versionsResponse = await ManusAPI.getPlanVersions(template.id);
        planVersions = versionsResponse.versions || [];
        
        if (planVersions.length > 0) {
            // 设置当前版本为最新版本
            currentVersionIndex = planVersions.length - 1;
            const latestVersion = planVersions[currentVersionIndex];
            
            // 解析JSON并显示
            try {
                currentPlanData = JSON.parse(latestVersion);
                jsonEditor.value = JSON.stringify(currentPlanData, null, 2);
            } catch (e) {
                console.warn('无法解析计划JSON:', e);
                jsonEditor.value = latestVersion;
            }
            
            // 更新API URL
            apiUrlElement.textContent = `http://your-domain/api/plan-template/execute/${template.id}`;
        }
        
        // 直接调用updateUIState来更新UI状态，确保按钮文本正确显示
        updateUIState();
        
    } catch (error) {
        console.error('加载计划模板失败:', error);
        alert('加载计划模板失败: ' + error.message);
    }
}
