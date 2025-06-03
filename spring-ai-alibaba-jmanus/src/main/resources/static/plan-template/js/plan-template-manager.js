

const PlanTemplatePollingManager = (() => {
    let currentPlanId = null;
    let currentPlanData = null;
    let pollTimer = null;
    const POLL_INTERVAL = 3000; // From original plan-template.js
    let internalIsPolling = false;
    let internalIsGenerating = false;

    let onPlanDataReceivedCallback = (planData) => {
        console.warn('PlanTemplatePollingManager: onPlanDataReceivedCallback not set.');
    };
    let onStatusChangeCallback = (status) => {
        console.warn('PlanTemplatePollingManager: onStatusChangeCallback not set.');
    };

    const init = (config) => {
        if (config && config.onPlanDataReceived) {
            onPlanDataReceivedCallback = config.onPlanDataReceived;
        }
        if (config && config.onStatusChange) {
            onStatusChangeCallback = config.onStatusChange;
        }
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.DIALOG_ROUND_START, (eventData) => {
            if (eventData && eventData.planId) {
                console.log(`PlanTemplatePollingManager: Received DIALOG_ROUND_START event for planId: ${eventData.planId}`);
                // Assuming the 'query' field in DIALOG_ROUND_START indicates a new execution round requiring polling.
                // The `true` for initialGeneratingState might need adjustment based on actual event semantics.
                startPolling(eventData.planId, true);
            } else {
                console.warn('PlanTemplatePollingManager: DIALOG_ROUND_START event received without planId.');
            }
        });

        console.log('PlanTemplatePollingManager initialized.');
    };

    const updateStatus = () => {
        if (typeof onStatusChangeCallback === 'function') {
            onStatusChangeCallback({
                isGenerating: internalIsGenerating,
                isPolling: !!pollTimer
            });
        }
    };

    const setGenerating = (generating) => {
        if (internalIsGenerating !== generating) {
            internalIsGenerating = generating;
            updateStatus();
        }
    };

    const pollPlanStatus = async () => {
        if (!currentPlanId) {
            console.warn("PlanTemplatePollingManager: currentPlanId not set for polling.");
            setGenerating(false);
            stopPolling();
            return;
        }
        if (internalIsPolling) {
            console.log("PlanTemplatePollingManager: Poll already in progress, skipping.");
            return;
        }

        internalIsPolling = true;

        try {
            // IMPORTANT: Verify this API endpoint for polling plan status.
            const response = await fetch(`/api/plan-template/status/${currentPlanId}`);

            if (!response.ok) {
                console.error(`PlanTemplatePollingManager: HTTP error polling plan ${currentPlanId}: ${response.status}`);
                const errorData = { error: `Failed to poll plan status: ${response.status}`, planId: currentPlanId, status: 'ERROR' };
                onPlanDataReceivedCallback(errorData);
                setGenerating(false);
                stopPolling();
                return;
            }

            const planData = await response.json();
            currentPlanData = planData;
            onPlanDataReceivedCallback(planData);

            if (planData.status === 'COMPLETED' || planData.status === 'FAILED' || planData.status === 'ERROR' || planData.completed) {
                setGenerating(false);
                stopPolling();
            }
        } catch (error) {
            console.error('PlanTemplatePollingManager: Error in pollPlanStatus:', error);
            const errorData = { error: `Exception during polling: ${error.message}`, planId: currentPlanId, status: 'ERROR' };
            onPlanDataReceivedCallback(errorData);
            setGenerating(false);
            stopPolling();
        } finally {
            internalIsPolling = false;
        }
    };

    const startPolling = (planIdToPoll, initialGeneratingState = true) => {
        if (pollTimer) {
            clearInterval(pollTimer);
        }
        currentPlanId = planIdToPoll;
        internalIsGenerating = initialGeneratingState;
        console.log(`PlanTemplatePollingManager: Starting polling for planId: ${currentPlanId}`);

        pollPlanStatus();
        pollTimer = setInterval(pollPlanStatus, POLL_INTERVAL);
        updateStatus();
    };

    const stopPolling = () => {
        if (pollTimer) {
            clearInterval(pollTimer);
            pollTimer = null;
            console.log('PlanTemplatePollingManager: Polling stopped.');
            // internalIsGenerating is typically set by the logic calling stopPolling (e.g., completion/error)
            // Call updateStatus to ensure UI reflects that polling is no longer active.
            updateStatus();
        }
    };

    const getCurrentPlanId = () => currentPlanId;

    return {
        init,
        startPolling,
        stopPolling,
        setGenerating,
        isCurrentlyGenerating: () => internalIsGenerating,
        isCurrentlyPolling: () => !!pollTimer,
        getCurrentPlanId
    };
})();

// Ensure PlanTemplatePollingManager is accessible, e.g., by making it global if not using ES modules
if (typeof window !== 'undefined') {
    window.PlanTemplatePollingManager = PlanTemplatePollingManager;
}
