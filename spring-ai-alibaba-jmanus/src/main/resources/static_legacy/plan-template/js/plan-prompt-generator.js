/**
 * Plan prompt generator class
 * Handles plan prompt input, parameter processing and plan generation functionality
 */
class PlanPromptGenerator {
    constructor() {

        // Current plan state
        this.currentPlanTemplateId = null;
        this.currentPlanData = null;
        this.isGenerating = false;

        // DOM element references - only includes plan prompt generation related elements
        this.planPromptInput = null;
        this.generatePlanBtn = null;
        this.clearParamBtn = null;
        this.clearBtn = null; // Clear all data button
        this.apiUrlElement = null;

        // Cached state data to reduce event queries
        this.cachedExecutionState = false;
    }

    /**
     * Initialize plan prompt generator
     */
    init() {
        // Get DOM elements
        this.planPromptInput = document.getElementById('plan-prompt');
        this.generatePlanBtn = document.getElementById('generatePlanBtn');
        this.clearParamBtn = document.getElementById('clearParamBtn');
        this.clearBtn = document.getElementById('clearBtn');
        this.apiUrlElement = document.querySelector('.api-url');

        // Bind event listeners
        this.bindEventListeners();
        this.bindUIEvents();

        // Initialize UI state
        this.updateUIState();

        console.log('PlanPromptGenerator initialization completed');
    }

    /**
     * Bind UI event listeners
     */
    bindUIEvents() {
        // Listen for execution state changes
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.EXECUTION_STATE_CHANGED, (data) => {
            this.cachedExecutionState = data.isExecuting;
            this.updateUIState();
        });

        // Listen for current plan template changes
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.CURRENT_PLAN_TEMPLATE_CHANGED, (data) => {
            this.currentPlanTemplateId = data.templateId ; // Compatibility handling
            this.currentPlanData = data.planData;
            if (data.planData && this.planPromptInput) {
                this.planPromptInput.value = data.planData.prompt || '';
            }
            this.updateUIState();
        });

       
    }

    /**
     * Bind event listeners
     */
    bindEventListeners() {
        // Generate plan button event
        if (this.generatePlanBtn) {
            this.generatePlanBtn.addEventListener('click', this.handleGeneratePlan.bind(this));
        }

        // Clear parameters button event
        if (this.clearParamBtn) {
            this.clearParamBtn.addEventListener('click', () => {
                console.log('Clear parameters button click event removed');
            });
        }

        // Clear all data button event
        if (this.clearBtn) {
            this.clearBtn.addEventListener('click', this.handleClearInput.bind(this));
        }
    }

    /**
     * Generate plan
     */
    async handleGeneratePlan() {
        const query = this.planPromptInput.value.trim();
        if (!query) {
            alert('Please enter plan requirement description');
            return;
        }
        if (this.isGenerating) return;

        // Read jsonContent from plan-json-editor field, if not empty add to parameters
        const planJsonEditor = document.getElementById('plan-json-editor');
        const jsonContent = planJsonEditor && planJsonEditor.value.trim() ? planJsonEditor.value.trim() : null;

        this.isGenerating = true;
        this.updateUIState();
        
        // Publish generation state change event
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.GENERATION_STATE_CHANGED, {
            isGenerating: true
        });

        try {

            let response;
            if (this.currentPlanTemplateId) {
                console.log('Updating existing plan template:', this.currentPlanTemplateId);
                response = await ManusAPI.updatePlanTemplate(
                    this.currentPlanTemplateId, 
                    query, 
                    jsonContent || null
                );
            } else {
                console.log('Creating new plan template');
                response = await ManusAPI.generatePlan(
                    query, 
                    jsonContent || null
                );
            }
            
            // Handle API response data structure
            console.log('API response data:', response);
            
            // Extract data based on actual API response structure
            const planJson = response.planJson || (response.plan && response.plan.json) || null;
            const planTemplateId = response.planTemplateId || (response.plan && response.plan.id) || null;
            const planData = response.plan || null;

            if (planJson) {
                // Save current plan data
                this.currentPlanData = {
                    json: planJson,
                    id: planTemplateId,
                    prompt: query,
                    plan: planData
                };
                
                // Set JSON content through event
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.JSON_CONTENT_SET, {
                    content: planJson
                });
                
                // Update current template ID
                this.currentPlanTemplateId = planTemplateId;
                
                // Update Prompt input box
                this.planPromptInput.value = query;
                
                // Publish plan template change event
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CURRENT_PLAN_TEMPLATE_CHANGED, {
                    templateId: this.currentPlanTemplateId,
                    planData: this.currentPlanData
                });
                
                // Check if content is duplicate and show corresponding prompt
                if (response.duplicate) {
                    console.log('Generated plan content is same as existing version');
                    // Can add a less intrusive prompt here, such as toast notification
                    // Temporarily use console.log for recording, without interrupting user experience
                } else if (response.saved) {
                    console.log('New version saved:', response.saveMessage);
                }
                
                console.log('Plan generation successful, template ID:', this.currentPlanTemplateId);
            } else {
                console.warn('API response data structure abnormal:', response);
                alert('Plan generation or update failed to return valid JSON data.');
            }

        } catch (error) {
            console.error('Plan generation failed:', error);
            alert('Plan generation failed: ' + error.message);
        } finally {
            this.isGenerating = false;
            this.updateUIState();
            
            // Publish generation state change event, including success information
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.GENERATION_STATE_CHANGED, {
                isGenerating: false,
                success: this.currentPlanTemplateId !== null,
                templateId: this.currentPlanTemplateId,
                planData: this.currentPlanData
            });
        }
    }


    /**
     * Update UI state (button disable/enable etc.)
     */
    updateUIState() {
        if (this.generatePlanBtn) {
            this.generatePlanBtn.disabled = this.isGenerating || this.cachedExecutionState;
            
            if (this.isGenerating) {
                            this.generatePlanBtn.textContent = 'Generating...';
        } else {
            this.generatePlanBtn.textContent = this.currentPlanTemplateId ? 'Update Plan' : 'Generate Plan';
            }
        }
    }

    /**
     * Clear prompt-related input and state
     */
    clearPromptData() {
        if (this.planPromptInput) {
            this.planPromptInput.value = '';
        }
        this.currentPlanTemplateId = null;
        this.currentPlanData = null;
        this.isGenerating = false;
        this.updateUIState();
        console.log('Plan prompt data cleared');
    }

    /**
     * Handle clearing all input operations
     */
    handleClearInput() {
        // Clear own prompt data
        this.clearPromptData();
        
        console.log('All input cleared');
    }

    // Getter methods
    getIsGenerating() {
        return this.isGenerating;
    }

    getCurrentPlanTemplateId() {
        return this.currentPlanTemplateId;
    }

    getCurrentPlanData() {
        return this.currentPlanData;
    }

    getPlanParams() {
        return null;
    }

    getPlanPrompt() {
        if (this.planPromptInput) {
            return this.planPromptInput.value.trim();
        }
        return null;
    }

    // Setter methods
    setCurrentPlanTemplateId(id) {
        this.currentPlanTemplateId = id;
        this.updateUIState();
    }

    setCurrentPlanData(data) {
        this.currentPlanData = data;
        if (data && this.planPromptInput) {
            this.planPromptInput.value = data.prompt || '';
        }
    }

    setPlanParams(params) {
        console.warn('setPlanParams method has been removed');
    }
}
