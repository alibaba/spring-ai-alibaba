/**
 * 这个对应 输入框 和发送按钮
 */
class ChatInputHandler {
    #inputField;
    #sendButton;

    constructor() {
        this.#inputField = document.querySelector('.input-area input');
        this.#sendButton = document.querySelector('.send-btn');

        if (!this.#inputField || !this.#sendButton) {
            console.error('ChatInputHandler: 未找到输入框或发送按钮!');
            return;
        }
        this.#attachEventListeners();
        this.#subscribeToEvents();
        console.log('ChatInputHandler 初始化完成');
    }

    #attachEventListeners() {
        const handleSend = () => {
            const query = this.#inputField.value.trim();
            if (query) {
                // 发布用户请求发送消息的事件
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.USER_MESSAGE_SEND_REQUESTED, { query });
            }
        };

        this.#sendButton.addEventListener('click', handleSend);
        this.#inputField.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                handleSend();
            }
        });
    }

    #subscribeToEvents() {
        // 订阅外部事件来清空输入和更新状态
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_CLEAR, this.clearInput.bind(this));
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_UPDATE_STATE, (data) => this.updateState(data.enabled, data.placeholder));
    }

    /**
     * 获取当前输入框的值
     * @returns {string} 当前输入框的文本值 (已去除首尾空格)
     */
    getQuery() {
        return this.#inputField ? this.#inputField.value.trim() : '';
    }

    /**
     * 清空输入框
     */
    clearInput() {
        if (this.#inputField) {
            this.#inputField.value = '';
        }
    }

    /**
     * 更新输入区域的状态（启用/禁用）
     * @param {boolean} enabled - 是否启用输入
     * @param {string} [placeholder='向 JTaskPilot 发送消息'] - 启用时的占位文本
     */
    updateState(enabled, placeholder = '向 JTaskPilot 发送消息') {
        if (this.#inputField) {
            this.#inputField.disabled = !enabled;
            this.#inputField.placeholder = enabled ? placeholder : '等待任务完成...'; // 更具体的禁用提示
            this.#inputField.classList.toggle('disabled', !enabled);
        }
        if (this.#sendButton) {
            this.#sendButton.disabled = !enabled;
            this.#sendButton.classList.toggle('disabled', !enabled);
        }
    }
}
