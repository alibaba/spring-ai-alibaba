/**
 * admin-events.js - Admin interface event handling
 */
import { AdminUtils } from './admin-utils.js';

class AdminEvents {
    constructor() {
        // Agent list related elements
        this.agentListContainer = document.querySelector('.agent-list-container');
        this.addAgentBtn = document.querySelector('.add-agent-btn');
        
        // Agent details related elements
        this.saveAgentBtn = document.querySelector('.save-agent-btn');
        this.addToolBtn = document.querySelector('.add-tool-btn');
        this.deleteAgentBtn = document.querySelector('.delete-agent-btn');
        
        // Add import/export buttons
        this.importAgentsBtn = document.querySelector('.import-agents-btn');
        this.exportAgentsBtn = document.querySelector('.export-agents-btn');
        
        this.currentAgentId = null;
        this.bindEvents();
    }

    /**
     * Bind all event listeners
     */
    bindEvents() {
        // Agent list events
        this.agentListContainer.addEventListener('click', (e) => this.handleAgentListClick(e));
        this.addAgentBtn.addEventListener('click', () => this.handleAddAgent());

        // Agent details events
        this.saveAgentBtn.addEventListener('click', () => this.handleSaveAgent());
        this.addToolBtn.addEventListener('click', () => this.handleAddTool());
        this.deleteAgentBtn.addEventListener('click', () => this.handleDeleteAgent());

        // Import/export events
        this.importAgentsBtn.addEventListener('click', () => this.handleImportAgents());
        this.exportAgentsBtn.addEventListener('click', () => this.handleExportAgents());

        // Tool list event delegation
        document.querySelector('.tool-list').addEventListener('click', (e) => this.handleToolListClick(e));
    }

    /**
     * Handle Agent list click events
     */
    async handleAgentListClick(event) {
        const agentItem = event.target.closest('.agent-item');
        if (!agentItem) return;

        const agentId = agentItem.dataset.agentId;
        if (event.target.closest('.expand-btn')) {
            // Expand/collapse Agent details
            agentItem.classList.toggle('expanded');
        } else {
            // Load Agent details
            try {
                const agent = await agentConfigModel.loadAgentDetails(agentId);
                this.currentAgentId = agentId;
                adminUI.showAgentDetails(agent);
            } catch (error) {
                adminUI.showError('Failed to load Agent details');
            }
        }
    }

    /**
     * Handle adding new Agent
     */
    handleAddAgent() {
        this.currentAgentId = null;
        adminUI.clearAgentDetails();
    }

    /**
     * Handle saving Agent
     */
    async handleSaveAgent() {
        try {
            const formData = adminUI.collectFormData();
            if (this.currentAgentId) {
                formData.id = this.currentAgentId;
            }

            const savedAgent = await agentConfigModel.saveAgent(formData);
            this.currentAgentId = savedAgent.id;
            
            // Reload Agent list
            await adminUI.loadAgents();
            adminUI.showSuccess('Agent saved successfully');
        } catch (error) {
            adminUI.showError('Failed to save Agent');
        }
    }

    /**
     * Handle adding tools
     */
    handleAddTool() {
        const availableTools = agentConfigModel.availableTools;
        // currentTools are the keys/names of tools currently displayed in the agent's tool list
        const currentTools = Array.from(document.querySelectorAll('.tool-list .tool-item .tool-name'))
            .map(item => item.textContent);
            
        if (availableTools && availableTools.length > 0) {
            // The onSelect callback now receives the complete list of tools that should be active.
            adminUI.showToolSelectionDialog(availableTools, currentTools, (finalSelectedToolsArray) => {
                const toolList = document.querySelector('.tool-list');
                toolList.innerHTML = ''; // Clear the current list

                finalSelectedToolsArray.forEach(selectedTool => {
                    // Assuming selectedTool is an object and has a 'key' property for its name/identifier.
                    // Adjust 'selectedTool.key' if the actual property name is different (e.g., name, id).
                    const toolName = selectedTool.key || selectedTool.name; // Use key, fallback to name
                    if (toolName) {
                        const toolHtml = `
                            <div class="tool-item">
                                <span class="tool-name">${toolName}</span>
                            </div>
                        `;
                        toolList.insertAdjacentHTML('beforeend', toolHtml);
                    }
                });
            });
        } else {
            adminUI.showError('Tool list not loaded or empty');
        }
    }


    /**
     * Handle deleting Agent
     */
    async handleDeleteAgent() {
        if (!this.currentAgentId) {
            adminUI.showError('No Agent selected');
            return;
        }

        const confirmed = await AdminUtils.confirmDialog('Are you sure you want to delete this Agent?');
        if (!confirmed) return;

        try {
            await agentConfigModel.deleteAgent(this.currentAgentId);
            this.currentAgentId = null;
            await adminUI.loadAgents();
            adminUI.clearAgentDetails();
            adminUI.showSuccess('Agent deleted successfully');
        } catch (error) {
            adminUI.showError('Failed to delete Agent');
        }
    }

    /**
     * Handle importing Agents configuration
     */
    handleImportAgents() {
        // Create hidden file input element
        const fileInput = document.createElement('input');
        fileInput.type = 'file';
        fileInput.accept = '.json';
        fileInput.style.display = 'none';
        document.body.appendChild(fileInput);

        fileInput.addEventListener('change', async (e) => {
            const file = e.target.files[0];
            if (!file) return;

            try {
                const reader = new FileReader();
                reader.onload = async (event) => {
                    try {
                        const agentConfigs = JSON.parse(event.target.result);
                        for (const config of agentConfigs) {
                            await agentConfigModel.saveAgent(config,true);
                        }
                        await adminUI.loadAgents();
                        adminUI.showSuccess('Agent configuration imported successfully');
                    } catch (error) {
                        adminUI.showError('Import failed: Invalid configuration file format');
                    }
                };
                reader.readAsText(file);
            } catch (error) {
                adminUI.showError('Import failed');
            } finally {
                document.body.removeChild(fileInput);
            }
        });

        fileInput.click();
    }

    /**
     * Handle exporting Agents configuration
     */
    async handleExportAgents() {
        try {
            const agents = await agentConfigModel.loadAgents();
            const exportData = JSON.stringify(agents, null, 2);
            
            // Create download link
            const blob = new Blob([exportData], { type: 'application/json' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `agents-config-${new Date().toISOString().split('T')[0]}.json`;
            document.body.appendChild(a);
            a.click();
            
            // Cleanup
            setTimeout(() => {
                document.body.removeChild(a);
                URL.revokeObjectURL(url);
            }, 100);
            
            adminUI.showSuccess('Agent configuration exported successfully');
        } catch (error) {
            adminUI.showError('Export failed');
        }
    }
}

// Initialize after DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.adminEvents = new AdminEvents();
    window.adminUI.init();
});
