const ChatAreaManager = (() => {

    /**
     * Initializes ChatAreaManager and sets up ChatHandler.
     */
    const init = () => {
        let chatAreaContainer = document.querySelector('.chat-area');

        if (!chatAreaContainer) {
            console.error('ChatAreaManager: Main chat area container (.chat-area) not found. ChatHandler will not be initialized.');
            return;
        }
        try {
            ChatHandler.init(); // ChatHandler uses '.chat-area' internally
            console.log('ChatAreaManager: ChatHandler initialized.');
        } catch (e) {
            console.error('ChatAreaManager: Error during ChatHandler.init():', e);
        }

    };


    // Expose public methods
    return {
        init
    };
})();
