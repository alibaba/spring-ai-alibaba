const ChatInputHandler = (() => {
    let inputField;
    let sendButton;

    /**
     * 初始化聊天输入组件
     */
    const init = () => {
        inputField = document.querySelector('.input-area input');
        sendButton = document.querySelector('.send-btn');

        if (!inputField || !sendButton) {
            console.error('ChatInputHandler: 未找到输入框或发送按钮!');
            return;
        }

        const handleSend = () => {
            const query = inputField.value.trim();
            if (query) {
                // 发布用户请求发送消息的事件
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.USER_MESSAGE_SEND_REQUESTED, { query });
            }
        };

        sendButton.addEventListener('click', handleSend);
        inputField.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault(); 
                handleSend();
            }
        });

        // 订阅外部事件来清空输入和更新状态
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_CLEAR, clearInput);
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_UPDATE_STATE, (data) => updateState(data.enabled, data.placeholder));


        console.log('ChatInputHandler 初始化完成');
    };

    /**
     * 获取当前输入框的值
     * @returns {string} 当前输入框的文本值 (已去除首尾空格)
     */
    const getQuery = () => {
        return inputField ? inputField.value.trim() : '';
    };

    /**
     * 清空输入框
     */
    const clearInput = () => {
        if (inputField) {
            inputField.value = '';
        }
    };

    /**
     * 更新输入区域的状态（启用/禁用）
     * @param {boolean} enabled - 是否启用输入
     * @param {string} [placeholder='向 JTaskPilot 发送消息'] - 启用时的占位文本
     */
    const updateState = (enabled, placeholder = '向 JTaskPilot 发送消息') => {
        if (inputField) {
            inputField.disabled = !enabled;
            inputField.placeholder = enabled ? placeholder : '等待任务完成...'; // 更具体的禁用提示
            inputField.classList.toggle('disabled', !enabled);
        }
        if (sendButton) {
            sendButton.disabled = !enabled;
            sendButton.classList.toggle('disabled', !enabled);
        }
    };

    // 公开接口
    return {
        init
    };
})();
