/**
 * API module - Handle all communication with backend
 */
const ManusAPI = (() => {

    // API base URL
    const BASE_URL = '/api/executor';
    const PLAN_TEMPLATE_URL = '/api/plan-template';

    /**
     * Send message to Manus, get asynchronous processing result
     * @param {string} query - User input query content
     * @returns {Promise<Object>} - Response containing task ID and initial status
     */
    const sendMessage = async (query) => {
        try {
            const response = await fetch(`${BASE_URL}/execute`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ query })
            });

            if (!response.ok) {
                throw new Error(`API request failed: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to send message:', error);
            throw error;
        }
    };
    
    /**
     * Generate execution plan based on input
     * @param {string} query - User input plan requirements
     * @param {string} [existingJson] - Optional existing JSON data string
     * @returns {Promise<Object>} - Response containing complete plan data
     */
    const generatePlan = async (query, existingJson = null) => {
        try {
            const requestBody = { query };
            
            // If existing JSON data exists, add to request
            if (existingJson) {
                requestBody.existingJson = existingJson;
            }
            
            const response = await fetch(`${PLAN_TEMPLATE_URL}/generate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error(`Failed to generate plan: ${response.status}`);
            }

            const responseData = await response.json();
            
            // If response contains planJson field, parse it as JSON object
            if (responseData.planJson) {
                try {
                    responseData.plan = JSON.parse(responseData.planJson);
                } catch (e) {
                    console.warn('Unable to parse plan JSON:', e);
                    responseData.plan = { error: 'Unable to parse plan data' };
                }
            }

            return responseData;
        } catch (error) {
            console.error('Failed to generate plan:', error);
            throw error;
        }
    };
    
    /**
     * Execute generated plan
     * @param {string} planTemplateId - Plan template ID
     * @param {string} [rawParam=null] - Optional execution parameters, string format
     * @returns {Promise<Object>} - Response containing execution status
     */
    const executePlan = async (planTemplateId, rawParam = null) => {
        try {
            let response;
            
            // Build request body object
            const requestBody = { planTemplateId: planTemplateId };
            
            // If rawParam exists, add to request body
            if (rawParam) {
                requestBody.rawParam = rawParam;
            }
            
            // Use POST method uniformly
            response = await fetch(`${PLAN_TEMPLATE_URL}/executePlanByTemplateId`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error(`Failed to execute plan: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to execute plan:', error);
            throw error;
        }
    };
    
    /**
     * Get detailed execution record
     * @param {string} planId - Plan ID
     * @returns {Promise<Object>} - Response containing detailed execution record, returns null if 404
     */
    const getDetails = async (planId) => {
        try {
            const response = await fetch(`${BASE_URL}/details/${planId}`);

            if (response.status === 404) {
                // Silent handling of 404 error, return null
                console.log(`Plan ${planId} does not exist or has been deleted, ignoring this query`);
                return null;
            }
            
            if (!response.ok) {
                throw new Error(`Failed to get details: ${response.status}`);
            }

            // First get original text for inspection
            const rawText = await response.text();
            console.log(`Original response text (planId: ${planId}):`, rawText); // Print original text

            // Try to parse original text
            try {
                return JSON.parse(rawText);
            } catch (jsonParseError) {
                console.error(`JSON parsing of original text failed (planId: ${planId}):`, jsonParseError);
                console.error("There may be problems in the original text, please check for unescaped control characters.");
                // To let the caller know there's an error here, and the error is similar to before, we re-throw this parsing error
                // Or return a specific error object or null as needed
                throw jsonParseError; // or return null; 
            }

        } catch (error) {
            // Log error but don't throw exception
            console.log(`Failed to get details (planId: ${planId}):`, error.message);
            return null;
        }
    };
    
    /**
     * Save plan to server
     * @param {string} planId - Plan ID
     * @param {string} planJson - Plan JSON content
     * @returns {Promise<Object>} - Save result
     */
    const savePlanTemplate = async (planId, planJson) => {
        try {
            const response = await fetch(`${PLAN_TEMPLATE_URL}/save`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    planId: planId,
                    planJson: planJson
                })
            });

            if (!response.ok) {
                throw new Error(`Failed to save plan: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to save plan:', error);
            throw error;
        }
    };
    
    /**
     * Get all versions of a plan
     * @param {string} planId - Plan ID
     * @returns {Promise<Object>} - Response containing version history
     */
    const getPlanVersions = async (planId) => {
        try {
            const response = await fetch(`${PLAN_TEMPLATE_URL}/versions`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ planId })
            });

            if (!response.ok) {
                throw new Error(`Failed to get plan versions: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to get plan versions:', error);
            throw error;
        }
    };
    
    /**
     * Get plan of specific version
     * @param {string} planId - Plan ID
     * @param {number} versionIndex - Version index
     * @returns {Promise<Object>} - Response containing specific version plan
     */
    const getVersionPlan = async (planId, versionIndex) => {
        try {
            const response = await fetch(`${PLAN_TEMPLATE_URL}/get-version`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    planId: planId,
                    versionIndex: versionIndex.toString()
                })
            });

            if (!response.ok) {
                throw new Error(`Failed to get specific version plan: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to get specific version plan:', error);
            throw error;
        }
    };
    
    /**
     * Get all plan template list
     * @returns {Promise<Object>} - Response containing plan template list
     */
    const getAllPlanTemplates = async () => {
        try {
            const url = `${PLAN_TEMPLATE_URL}/list`;
            console.log('Requesting plan template list, URL:', url);
            
            const response = await fetch(url);
            
            if (!response.ok) {
                throw new Error(`Failed to get plan template list: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('Failed to get plan template list:', error);
            throw error;
        }
    };

    /**
     * Update existing plan template
     * @param {string} planId - Plan template ID
     * @param {string} query - User input plan requirements
     * @param {string} existingJson - Existing JSON data string
     * @returns {Promise<Object>} - Response containing updated plan data
     */
    const updatePlanTemplate = async (planId, query, existingJson = null) => {
        try {
            const requestBody = { 
                planId,
                query
            };
            
            // If existing JSON data exists, add to request
            if (existingJson) {
                requestBody.existingJson = existingJson;
            }
            
            const response = await fetch(`${PLAN_TEMPLATE_URL}/update`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error(`Failed to update plan template: ${response.status}`);
            }

            const responseData = await response.json();
            
            // If response contains planJson field, parse it as JSON object
            if (responseData.planJson) {
                try {
                    responseData.plan = JSON.parse(responseData.planJson);
                } catch (e) {
                    console.warn('Unable to parse plan JSON:', e);
                    responseData.plan = { error: 'Unable to parse plan data' };
                }
            }

            return responseData;
        } catch (error) {
            console.error('Failed to update plan template:', error);
            throw error;
        }
    };

    /**
     * Delete plan template
     * @param {string} planId - Plan template ID
     * @returns {Promise<Object>} - Delete result
     */
    const deletePlanTemplate = async (planId) => {
        try {
            const response = await fetch(`${PLAN_TEMPLATE_URL}/delete`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ planId })
            });

            if (!response.ok) {
                throw new Error(`Failed to delete plan template: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Failed to delete plan template:', error);
            throw error;
        }
    };

    /**
     * Submit user form input
     * @param {string} planId - Plan ID
     * @param {Object} formData - User input form data
     * @returns {Promise<Object>} - Submit result
     */
    const submitFormInput = async (planId, formData) => {
        try {
            const response = await fetch(`${BASE_URL}/submit-input/${planId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(formData)
            });

            if (!response.ok) {
                // Try to parse error response body
                let errorData;
                try {
                    errorData = await response.json();
                } catch (e) {
                    errorData = { message: `Failed to submit form input: ${response.status}` };
                }
                console.error('Failed to submit form input:', errorData);
                throw new Error(errorData.message || `Failed to submit form input: ${response.status}`);
            }
            // Even if it's 200 OK, there may be no response body, or the response body may indicate success but have no specific data
            // Check if there's content to parse
            const contentType = response.headers.get("content-type");
            if (contentType && contentType.indexOf("application/json") !== -1) {
                 return await response.json(); // { success: true } or similar
            }
            return { success: true }; // Default successful processing

        } catch (error) {
            console.error('Failed to submit form input:', error);
            throw error; // Re-throw the error so the caller can handle
        }
    };

    // Return public methods
    return {
        BASE_URL,
        PLAN_TEMPLATE_URL,
        sendMessage,
        getDetails,
        generatePlan,
        executePlan,
        savePlanTemplate,
        getPlanVersions,
        updatePlanTemplate,
        getVersionPlan,
        getAllPlanTemplates,
        deletePlanTemplate,
        submitFormInput
    };
})();
