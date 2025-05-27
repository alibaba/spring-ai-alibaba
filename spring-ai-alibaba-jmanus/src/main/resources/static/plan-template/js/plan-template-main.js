const PlanTemplateManager = (() => {
    let planTemplateManagerOldInstance = null; // Keep instance here
    let planTemplateListUIHandler = null; // Keep instance here

    /**
     * Initializes PlanTemplateManager and sets up ChatHandler.
     */
    const init = async () => {
        console.log('初始化');
        let chatAreaContainer = document.querySelector('.chat-area');

        if (!chatAreaContainer) {
            console.error('PlanTemplateManager: Main chat area container (.chat-area) not found. ChatHandler will not be initialized.');
            return;
        }
        try {
           
            // 1. 初始化 UI 模块（包含基础事件系统）
            // PlanExecutionManagerController is a class, we need to create an instance.
            const planExecutionManager = new PlanExecutionManagerController();
            planExecutionManager.init();
            console.log('UI 模块初始化完成');

            const chatHandler = new ChatHandler(planExecutionManager); // Pass planExecutionManager instance
            console.log('PlanTemplateManager: ChatHandler initialized.');


            // 3. 初始化右侧边栏
            // Assuming RightSidebarController is the class name and is globally available
            const rightSidebar = new RightSidebarController();
            rightSidebar.init(); // init is synchronous
            console.log('右侧边栏初始化完成 (from PlanTemplateManager)');

            planTemplateManagerOldInstance = new PlanTemplateManagerOld();
            await planTemplateManagerOldInstance.init(); // planTemplateManagerOldInstance.init is now async

            const runPlanButtonHandler = new RunPlanButtonHandler();
            // Assuming runPlanButtonHandler.init is not async, if it is, add await
            runPlanButtonHandler.init(planTemplateManagerOldInstance);
            console.log('RunPlanButtonHandler 初始化完成');

            planTemplateListUIHandler = new PlanTemplateListUIHandler(planTemplateManagerOldInstance);
            planTemplateListUIHandler.init(); // This will call updatePlanTemplateListUI internally
            // planTemplateManagerOldInstance.setPlanTemplateListUIHandler(planTemplateListUIHandler); // No longer needed
            console.log('PlanTemplateListUIHandler 初始化完成 from main');

        } catch (e) {
            console.error('PlanTemplateManager: Error during initialization:', e);
        }

    };


    // Expose public methods and potentially instances if needed by other modules globally
    return {
        init,
        getPlanTemplateManagerOldInstance: () => planTemplateManagerOldInstance,
        getPlanTemplateListUIHandler: () => planTemplateListUIHandler
    };
})();

document.addEventListener('DOMContentLoaded', () => {

    PlanTemplateManager.init(); // Correct: Call init directly on the object
    console.log('PlanTemplateManager 初始化完成');
});
