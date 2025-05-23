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
           
            const chatHandler = new ChatHandler();
            console.log('PlanTemplateManager: ChatHandler initialized.');


            // 3. 初始化右侧边栏
            await RightSidebar.init();
            console.log('右侧边栏初始化完成');

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

            // 1. 初始化 UI 模块（包含基础事件系统）
            await PlanExecutionManager.init();
            console.log('UI 模块初始化完成');

        } catch (e) {
            console.error('PlanTemplateManager: Error during ChatHandler.init():', e);
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
