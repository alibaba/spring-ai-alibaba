/**
 * Plan template helper functions - Provides functionality for operating plan template lists
 */

/**
 * Convert timestamp to relative time string
 * @param {Date} date - Date object
 * @returns {string} - Relative time string
 */
function getRelativeTimeString(date) {
    const now = new Date();
    const diff = now - date;
    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    
    if (days > 30) {
        return date.toLocaleDateString('zh-CN');
    } else if (days > 0) {
        return `${days} days ago`;
    } else if (hours > 0) {
        return `${hours} hours ago`;
    } else if (minutes > 0) {
        return `${minutes} minutes ago`;
    } else {
        return 'Just now';
    }
}

/**
 * Truncate text to specified length
 * @param {string} text - Text content
 * @param {number} maxLength - Maximum length
 * @returns {string} - Truncated text
 */
function truncateText(text, maxLength) {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substr(0, maxLength - 3) + '...';
}

/**
 * Handle plan template item click event
 * @param {Object} template - Plan template object
 */
async function handlePlanTemplateClick(template) {
    if (isGenerating || isExecuting) {
        return;
    }
    
    try {
        // Set current plan template ID
        currentPlanTemplateId = template.id;
        
        // Get latest version of plan template
        const versionsResponse = await ManusAPI.getPlanVersions(template.id);
        planVersions = versionsResponse.versions || [];
        
        if (planVersions.length > 0) {
            // Set current version to latest version
            currentVersionIndex = planVersions.length - 1;
            const latestVersion = planVersions[currentVersionIndex];
            
            // Parse JSON and display
            try {
                currentPlanData = JSON.parse(latestVersion);
                jsonEditor.value = JSON.stringify(currentPlanData, null, 2);
            } catch (e) {
                console.warn('Unable to parse plan JSON:', e);
                jsonEditor.value = latestVersion;
            }
            
            // Update API URL
            apiUrlElement.textContent = `http://your-domain/api/plan-template/execute/${template.id}`;
        }
        
        // Directly call updateUIState to update UI state, ensuring button text is displayed correctly
        updateUIState();
        
    } catch (error) {
        console.error('Failed to load plan template:', error);
        alert('Failed to load plan template: ' + error.message);
    }
}
