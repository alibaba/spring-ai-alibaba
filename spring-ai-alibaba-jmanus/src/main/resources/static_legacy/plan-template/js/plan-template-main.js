

/**
 * Plan Template Manager - Responsible for coordinating and initializing all plan template related components
 */
class PlanTemplateManager {
    constructor() {
        this.planTemplateListUIHandler = null;
        this.planPromptGenerator = null;
        this.planTemplateHandler = null;
    }

    /**
     * Initialize PlanTemplateManager and set up all components
     */
    async init() {
        console.log('Initializing');
        let chatAreaContainer = document.querySelector('.chat-area');

        if (!chatAreaContainer) {
            console.error('PlanTemplateManager: Main chat area container (.chat-area) not found. ChatHandler will not be initialized.');
            return;
        }
        try {
           
            // 1. Initialize UI module (including basic event system)
            const planExecutionManager = new PlanExecutionManagerController();
            planExecutionManager.init();
            console.log('UI module initialization completed');

            const chatHandler = new ChatHandler(planExecutionManager);
            console.log('PlanTemplateManager: ChatHandler initialized.');

            // 3. Initialize sidebar manager
            const sidebarManager = new SidebarManager();
            sidebarManager.init();
            console.log('Sidebar manager initialization completed');

            // 4. Initialize right sidebar
            const rightSidebar = new RightSidebarController();
            rightSidebar.init();
            console.log('Right sidebar initialization completed (from PlanTemplateManager)');

            // 6. Initialize plan prompt generator
            this.planPromptGenerator = new PlanPromptGenerator();
            this.planPromptGenerator.init();
            console.log('PlanPromptGenerator initialization completed');

            // 7. Initialize handler
            this.planTemplateHandler = new PlanTemplateHandler();
            this.planTemplateHandler.init();
            console.log('PlanTemplateHandler initialization completed');

            const planTemplateExecutionController = new PlanTemplateExecutionController();
            planTemplateExecutionController.init();
            console.log('PlanTemplateExecutionController initialization completed');

            this.planTemplateListUIHandler = new PlanTemplateListUIHandler();
            this.planTemplateListUIHandler.init();
            console.log('PlanTemplateListUIHandler initialization completed from main');

        } catch (e) {
            console.error('PlanTemplateManager: Error during initialization:', e);
        }
    }

    /**
     * Get plan prompt generator instance
     * @returns {PlanPromptGenerator} Plan prompt generator instance
     */
    getPlanPromptGenerator() {
        return this.planPromptGenerator;
    }

    /**
     * Get plan template handler instance
     * @returns {PlanTemplateHandler} Plan template handler instance
     */
    getPlanTemplateHandler() {
        return this.planTemplateHandler;
    }

    /**
     * Get plan template list UI handler instance
     * @returns {PlanTemplateListUIHandler} Plan template list UI handler instance
     */
    getPlanTemplateListUIHandler() {
        return this.planTemplateListUIHandler;
    }
}

/**
 * Sidebar Manager - Responsible for left and right sidebar collapse/expand functionality
 */
class SidebarManager {
    constructor() {
        this.toggleLeftSidebarBtn = null;
        this.leftSidebar = null;
    }

    /**
     * Initialize sidebar manager - Only handles left sidebar, right sidebar is handled by RightSidebarController
     */
    init() {
        // Get left sidebar toggle button and sidebar elements
        this.toggleLeftSidebarBtn = document.getElementById('toggleLeftSidebarBtn');
        this.leftSidebar = document.getElementById('leftSidebar');

        // Bind left sidebar toggle button event
        if (this.toggleLeftSidebarBtn && this.leftSidebar) {
            this.toggleLeftSidebarBtn.addEventListener('click', this.handleToggleLeftSidebar.bind(this));
            console.log('Left sidebar toggle button event binding successful');
        } else {
            console.warn('SidebarManager: Left sidebar or toggle button not found');
        }

        console.log('SidebarManager initialization completed (only handles left sidebar)');
    }

    /**
     * Handle left sidebar collapse/expand
     */
    handleToggleLeftSidebar() {
        if (!this.leftSidebar) {
            console.warn('SidebarManager: Left sidebar element not found');
            return;
        }
        
        this.leftSidebar.classList.toggle('collapsed');
        
        // Can adjust main content area margins or width as needed
        const mainContent = document.getElementById('mainContent') || document.querySelector('.main-content-wrapper');
        if (mainContent) {
            mainContent.classList.toggle('left-collapsed');
        }
    }
}
document.addEventListener('DOMContentLoaded', () => {
    // Create PlanTemplateManager instance and initialize
    const planTemplateManager = new PlanTemplateManager();
    planTemplateManager.init();
    console.log('PlanTemplateManager initialization completed');
    
    // Expose instance to global scope so other scripts can access it
    if (typeof window !== 'undefined') {
        window.planTemplateManager = planTemplateManager;
    }
});
