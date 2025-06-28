/**
 * This corresponds to the input field and send button
 */
class ChatInputHandler {
    #inputField;
    #sendButton;

    constructor() {
        this.#inputField = document.querySelector('.input-area input');
        this.#sendButton = document.querySelector('.send-btn');

        if (!this.#inputField || !this.#sendButton) {
            console.error('ChatInputHandler: Input field or send button not found!');
            return;
        }
        this.#attachEventListeners();
        this.#subscribeToEvents();
        console.log('ChatInputHandler initialization completed');
    }

    #attachEventListeners() {
        const handleSend = () => {
            const query = this.#inputField.value.trim();
            if (query) {
                // Publish event for user request send message
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
        // Subscribe to external events to clear input and update state
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_CLEAR, this.clearInput.bind(this));
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_UPDATE_STATE, (data) => this.updateState(data.enabled, data.placeholder));
    }

    /**
     * Get current input field value
     * @returns {string} Current input field text value (trimmed)
     */
    getQuery() {
        return this.#inputField ? this.#inputField.value.trim() : '';
    }

    /**
     * Clear input field
     */
    clearInput() {
        if (this.#inputField) {
            this.#inputField.value = '';
        }
    }

    /**
     * Update input area state (enable/disable)
     * @param {boolean} enabled - Whether to enable input
     * @param {string} [placeholder='Send message to JTaskPilot'] - Placeholder text when enabled
     */
    updateState(enabled, placeholder = 'Send message to JTaskPilot') {
        if (this.#inputField) {
            this.#inputField.disabled = !enabled;
            this.#inputField.placeholder = enabled ? placeholder : 'Waiting for task completion...'; // More specific disable hint
            this.#inputField.classList.toggle('disabled', !enabled);
        }
        if (this.#sendButton) {
            this.#sendButton.disabled = !enabled;
            this.#sendButton.classList.toggle('disabled', !enabled);
        }
    }
}
