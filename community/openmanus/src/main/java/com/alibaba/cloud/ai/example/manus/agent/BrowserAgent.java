package com.alibaba.cloud.ai.example.manus.agent;

import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.llm.ToolBuilder;
import com.alibaba.cloud.ai.example.manus.tool.BrowserUseTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.tool.ToolCallingManager;

import java.util.HashMap;
import java.util.Map;

public class BrowserAgent extends ToolCallAgent {
    private static final Logger log = LoggerFactory.getLogger(BrowserAgent.class);


    public BrowserAgent(LlmService llmService, ToolCallingManager toolCallingManager, ToolBuilder toolBuilder) {
        super(llmService, toolCallingManager, toolBuilder);
    }

    @Override
    public String getSystemPromptTemplate() {
        return """
            You are an AI agent designed to automate browser tasks. Your goal is to accomplish the ultimate task following the rules.
            
            # Input Format
            Task
            Previous steps
            Current URL
            Open Tabs
            Interactive Elements
            [index]<type>text</type>
            - index: Numeric identifier for interaction
            - type: HTML element type (button, input, etc.)
            - text: Element description
            Example:
            [33]<button>Submit Form</button>
            
            - Only elements with numeric indexes in [] are interactive
            - elements without [] provide only context
            
            # Response Rules
            1. RESPONSE FORMAT: You must ALWAYS respond with valid JSON in this exact format:
            \\{"current_state": \\{"evaluation_previous_goal": "Success|Failed|Unknown - Analyze the current elements and the image to check if the previous goals/actions are successful like intended by the task. Mention if something unexpected happened. Shortly state why/why not",
            "memory": "Description of what has been done and what you need to remember. Be very specific. Count here ALWAYS how many times you have done something and how many remain. E.g. 0 out of 10 websites analyzed. Continue with abc and xyz",
            "next_goal": "What needs to be done with the next immediate action"\\},
            "action":[\\{"one_action_name": \\{// action-specific parameter\\}\\}, // ... more actions in sequence]\\}
            
            2. ACTIONS: You can specify multiple actions in a sequence, but one action name per item
            - Form filling: [\\{"input_text": \\{"index": 1, "text": "username"\\}\\}, \\{"click_element": \\{"index": 3\\}\\}]
            - Navigation: [\\{"go_to_url": \\{"url": "https://example.com"\\}\\}, \\{"extract_content": \\{"goal": "names"\\}\\}]
            
            3. ELEMENT INTERACTION:
            - Only use indexed elements
            - Watch for non-interactive elements
            
            4. NAVIGATION & ERROR HANDLING:
            - Try alternative approaches if stuck
            - Handle popups and cookies
            - Use scroll for hidden elements
            - Open new tabs for research
            - Handle captchas or find alternatives
            - Wait for page loads
            
            5. TASK COMPLETION:
            - Track progress in memory
            - Count iterations for repeated tasks
            - Include all findings in results
            - Use done action appropriately
            
            6. VISUAL CONTEXT:
            - Use provided screenshots
            - Reference element indices
            
            7. FORM FILLING:
            - Handle dynamic field changes
            
            8. EXTRACTION:
            - Use extract_content for information gathering
            """;
    }

    @Override
    public String getNextStepPromptTemplate() {
        return """
            What should I do next to achieve my goal?
            
            When you see [Current state starts here], focus on the following:
            - Current URL and page title{url_placeholder}
            - Available tabs{tabs_placeholder}
            - Interactive elements and their indices
            - Content above {content_above_placeholder} or below {content_below_placeholder} the viewport (if indicated)
            - Any action results or errors{results_placeholder}
            
            For browser interactions:
            - To navigate: browser_use with action="go_to_url", url="..."
            - To click: browser_use with action="click_element", index=N
            - To type: browser_use with action="input_text", index=N, text="..."
            - To extract: browser_use with action="extract_content", goal="..."
            - To scroll: browser_use with action="scroll_down" or "scroll_up"
            
            Consider both what's visible and what might be beyond the current viewport.
            Be methodical - remember your progress and what you've learned so far.
            """;
    }

    @Override
    public String getName() {
        return "browser";
    }

    @Override
    public String getDescription() {
        return "A browser agent that can control a browser to accomplish tasks";
    }

    @Override
    Map<String, Object> getData() {
        Map<String, Object> newReturnData = new HashMap<>();
        Map<String,Object> parentData =  super.getData();
        if(parentData != null) {
            newReturnData.putAll(parentData);
        }
        Map<String, Object> browserState = getBrowserState();
        if(browserState != null) {
            newReturnData.putAll(browserState);
        }

        return newReturnData;
    }

    private Map<String, Object> getBrowserState() {
        try {
            BrowserUseTool browserTool = BrowserUseTool.getInstance(toolBuilder.getChromeDriverService());
            if(browserTool == null) {
                log.error("Failed to get browser tool instance");
                return null;
            }
            Map<String, Object> state = browserTool.getCurrentState();
            
            return state;
        } catch (Exception e) {
            log.error("Failed to get browser state", e);
            return null;
        }
    }
}
