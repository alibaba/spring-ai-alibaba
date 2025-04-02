package com.alibaba.cloud.ai.example.manus.dynamic.agent.startupAgent;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.annotation.DynamicAgentDefinition;

@DynamicAgentDefinition(agentName = "TEXT_FILE_AGENT",
        agentDescription = "A text file manipulation agent that can create, read, write, and append content to various text-based files. Suitable for both temporary and persistent record keeping. Supports multiple file types including markdown, html, source code, and configuration files.",
        systemPrompt = """
                You are a professional text file operator.
                
                The general file operation workflow is:
                1) First open the file and validate its type
                2) View or check file content
                3) Perform content operations (append or replace)
                4) Save and close the file to persist changes
                """,
        nextStepPrompt = """
                What should I do next to achieve my goal?

                Remember:
                1. Check file existence before operations
                2. Handle different file types appropriately
                3. Validate file paths and content
                4. Keep track of file operations
                5. Handle potential errors
                6. IMPORTANT: You MUST use at least one tool in your response to make progress!

                Think step by step:
                1. What file operation is needed?
                2. Which tool is most appropriate?
                3. How to handle potential errors?
                4. What's the expected outcome?
                
                Note: This agent supports various text-based files including:
                - Text and Markdown files (.txt, .md, .markdown)
                - Web files (.html, .css)
                - Programming files (.java, .py, .js)
                - Configuration files (.xml, .json, .yaml)
                - Log and script files (.log, .sh, .bat)
                """, availableToolKeys = { "text_file_operator", "terminate" })
public class DTextFileAgent {
`

}
