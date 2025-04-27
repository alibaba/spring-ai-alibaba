/**
 * 工具组折叠功能
 */
(function() {
    // 用于跟踪已初始化的标题元素
    const initializedHeaders = new Set();
    
    function initGroupCollapsible() {
        console.log('初始化工具组折叠功能');
        
        // 在每个组标题中添加折叠图标
        document.querySelectorAll('.tool-group-header').forEach((header, index) => {
            // 使用自定义属性标记已初始化的元素，避免重复添加
            if (header.hasAttribute('data-collapsible-initialized')) {
                return;
            }
            
            // 创建折叠图标
            const collapseIcon = document.createElement('span');
            collapseIcon.className = 'collapse-icon';
            collapseIcon.textContent = '▼';
            header.appendChild(collapseIcon);
            
            // 获取对应的内容区域
            const contentArea = header.nextElementSibling;
            if (!contentArea) {
                console.warn('折叠组标题没有对应的内容区域:', header);
                return;
            }
            
            // 默认情况下，只有第一个组是展开的，其他都折叠
            if (index > 0) {
                collapseIcon.classList.add('collapsed');
                contentArea.classList.add('collapsed');
            }
            
            // 添加点击事件，用于切换折叠状态
            header.addEventListener('click', function(event) {
                // 避免点击选择全部或启用全部时触发折叠
                if (event.target.closest('.group-actions')) {
                    return;
                }
                
                // 切换折叠图标状态
                collapseIcon.classList.toggle('collapsed');
                
                // 切换内容区域的折叠状态
                contentArea.classList.toggle('collapsed');
            });
            
            // 标记此标题元素已初始化
            header.setAttribute('data-collapsible-initialized', 'true');
            initializedHeaders.add(header);
        });
        
    }

    // 初始化函数，同时支持直接调用和动态加载后调用
    function init() {
        // 如果DOM已加载完成，立即初始化
        if (document.readyState === 'complete' || document.readyState === 'interactive') {
            setTimeout(initGroupCollapsible, 100); // 稍微延迟一点，确保DOM已完全加载
        } else {
            // 否则等待DOM加载完成
            document.addEventListener('DOMContentLoaded', initGroupCollapsible);
        }
        
        // 为了处理动态生成的对话框，添加MutationObserver监听DOM变化
        const observer = new MutationObserver(function(mutations) {
            let dialogFound = false;
            
            mutations.forEach(function(mutation) {
                if (mutation.addedNodes.length) {
                    // 检查是否添加了工具选择对话框
                    const dialog = document.querySelector('.tool-selection-dialog[style*="display: block"]');
                    if (dialog && !dialogFound) {
                        dialogFound = true; // 防止在同一批变更中多次处理
                        console.log('检测到工具选择对话框显示');
                        // 延迟一点执行，确保DOM完全渲染
                        setTimeout(initGroupCollapsible, 200);
                    }
                }
            });
        });
        
        // 监听整个body的变化
        observer.observe(document.body, { childList: true, subtree: true });
    }
    
    // 立即执行初始化
    init();
    
})();
