/**
 * admin-utils.js - 管理界面实用工具
 * 提供管理界面中通用的工具函数
 */

export class AdminUtils {
    /**
     * 显示通知消息
     * @param {string} message - 消息内容
     * @param {string} type - 消息类型：'success', 'error', 'warning', 'info'
     * @param {number} duration - 显示时长(毫秒)
     */
    static showNotification(message, type = 'info', duration = 3000) {
        // 检查是否已存在通知容器
        let container = document.querySelector('.notification-container');
        if (!container) {
            // 创建通知容器
            container = document.createElement('div');
            container.className = 'notification-container';
            document.body.appendChild(container);
        }
        
        // 创建通知元素
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.innerHTML = `
            <div class="notification-content">
                <span class="notification-icon">${this.getNotificationIcon(type)}</span>
                <span class="notification-message">${message}</span>
            </div>
            <button class="notification-close">×</button>
        `;
        
        // 添加关闭按钮事件
        notification.querySelector('.notification-close').addEventListener('click', () => {
            this.closeNotification(notification);
        });
        
        // 添加到容器
        container.appendChild(notification);
        
        // 添加动画类
        setTimeout(() => {
            notification.classList.add('show');
        }, 10);
        
        // 自动关闭
        if (duration > 0) {
            setTimeout(() => {
                this.closeNotification(notification);
            }, duration);
        }
        
        return notification;
    }
    
    /**
     * 关闭通知
     * @param {HTMLElement} notification - 通知元素
     */
    static closeNotification(notification) {
        notification.classList.remove('show');
        notification.classList.add('hide');
        
        // 移除元素
        setTimeout(() => {
            notification.remove();
        }, 300);
    }
    
    /**
     * 获取通知类型对应的图标
     * @param {string} type - 通知类型
     * @returns {string} 图标HTML
     */
    static getNotificationIcon(type) {
        switch (type) {
            case 'success':
                return '✓';
            case 'error':
                return '✗';
            case 'warning':
                return '⚠';
            case 'info':
            default:
                return 'ℹ';
        }
    }
    
    /**
     * 确认对话框
     * @param {string} message - 确认消息
     * @returns {Promise<boolean>} 用户确认结果
     */
    static confirmDialog(message) {
        return new Promise((resolve) => {
            // 创建对话框元素
            const overlay = document.createElement('div');
            overlay.className = 'dialog-overlay';
            
            const dialog = document.createElement('div');
            dialog.className = 'confirm-dialog';
            dialog.innerHTML = `
                <div class="dialog-content">
                    <p class="dialog-message">${message}</p>
                    <div class="dialog-buttons">
                        <button class="dialog-btn cancel-btn">取消</button>
                        <button class="dialog-btn confirm-btn">确认</button>
                    </div>
                </div>
            `;
            
            overlay.appendChild(dialog);
            document.body.appendChild(overlay);
            
            // 添加动画，使用 requestAnimationFrame 确保 DOM 更新后再添加动画类
            requestAnimationFrame(() => {
                overlay.classList.add('show');
                dialog.classList.add('show');
            });
            
            // 添加按钮事件
            dialog.querySelector('.confirm-btn').addEventListener('click', () => {
                this.closeDialog(overlay);
                resolve(true);
            });
            
            dialog.querySelector('.cancel-btn').addEventListener('click', () => {
                this.closeDialog(overlay);
                resolve(false);
            });
        });
    }
    
    /**
     * 关闭对话框
     * @param {HTMLElement} overlay - 对话框遮罩元素
     */
    static closeDialog(overlay) {
        const dialog = overlay.querySelector('.confirm-dialog');
        overlay.classList.remove('show');
        dialog.classList.remove('show');
        
        // 移除元素
        setTimeout(() => {
            overlay.remove();
        }, 300);
    }
    
    /**
     * 格式化日期时间
     * @param {Date|string|number} date - 日期对象或时间戳
     * @param {string} format - 格式化模板，默认：'YYYY-MM-DD HH:mm:ss'
     * @returns {string} 格式化后的日期字符串
     */
    static formatDate(date, format = 'YYYY-MM-DD HH:mm:ss') {
        const d = new Date(date);
        
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        const hours = String(d.getHours()).padStart(2, '0');
        const minutes = String(d.getMinutes()).padStart(2, '0');
        const seconds = String(d.getSeconds()).padStart(2, '0');
        
        return format
            .replace('YYYY', year)
            .replace('MM', month)
            .replace('DD', day)
            .replace('HH', hours)
            .replace('mm', minutes)
            .replace('ss', seconds);
    }
    
    /**
     * 防抖函数
     * @param {Function} func - 要执行的函数
     * @param {number} wait - 等待时间(毫秒)
     * @returns {Function} 防抖处理后的函数
     */
    static debounce(func, wait = 300) {
        let timeout;
        return function(...args) {
            clearTimeout(timeout);
            timeout = setTimeout(() => {
                func.apply(this, args);
            }, wait);
        };
    }
    
    /**
     * 检查是否有未保存的更改
     * @param {Object} originalData - 原始数据
     * @param {Object} currentData - 当前数据
     * @returns {boolean} 是否有未保存的更改
     */
    static hasUnsavedChanges(originalData, currentData) {
        return JSON.stringify(originalData) !== JSON.stringify(currentData);
    }
}

