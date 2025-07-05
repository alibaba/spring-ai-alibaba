/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import type { I18nType } from './type.ts'

const words: I18nType = {
  // Basic navigation
  conversation: 'Conversation',
  plan: 'Plan Execution',
  backHome: 'Back to Home',
  noPageTip: 'The page you are looking for does not exist.',

  // Common buttons and actions
  common: {
    cancel: 'Cancel',
    confirm: 'Confirm',
    delete: 'Delete',
    edit: 'Edit',
    save: 'Save',
    reset: 'Reset',
    close: 'Close',
    add: 'Add',
    create: 'Create',
    update: 'Update',
    submit: 'Submit',
    clear: 'Clear',
    search: 'Search',
    loading: 'Loading...',
    success: 'Success',
    error: 'Error',
    warning: 'Warning',
    info: 'Info',
    yes: 'Yes',
    no: 'No',
    enable: 'Enable',
    disable: 'Disable',
    copy: 'Copy',
    paste: 'Paste',
    cut: 'Cut',
    undo: 'Undo',
    redo: 'Redo',
    select: 'Select',
    selectAll: 'Select All',
    deselectAll: 'Deselect All',
    previous: 'Previous',
    next: 'Next',
    finish: 'Finish',
    retry: 'Retry',
    refresh: 'Refresh',
    import: 'Import',
    export: 'Export',
    upload: 'Upload',
    download: 'Download',
    preview: 'Preview',
    expand: 'Expand',
    collapse: 'Collapse',
    maximize: 'Maximize',
    minimize: 'Minimize',
    fullscreen: 'Fullscreen',
    exitFullscreen: 'Exit Fullscreen',
    parameters: 'Parameters',
    thinking: 'Thinking',
    input: 'Input'
  },

  // Configuration related
  config: {
    title: 'Configuration Management',
    loading: 'Loading configuration...',
    notFound: 'No configuration items found',
    reset: 'Reset',
    resetGroupConfirm: 'Reset all configurations in this group to default values',
    modified: 'Modified',
    saved: 'Configuration saved',
    saveFailed: 'Save failed',
    search: 'Search configuration items...',
    mcpSearch: 'Search MCP servers...',
    mcpConfigPlaceholder: 'Please enter MCP server configuration (JSON format)...',
    types: {
      string: 'String',
      text: 'Text',
      number: 'Number',
      boolean: 'Boolean',
      select: 'Select',
      textarea: 'Textarea',
      checkbox: 'Checkbox'
    },
    range: 'Range',
    min: 'Minimum',
    max: 'Maximum',
    categories: {
      basic: 'Basic Configuration',
      agent: 'Agent Configuration',
      model: 'Model Configuration',
      mcp: 'Tools/MCP Configuration'
    },
    // Agent configuration page
    agentConfig: {
      title: 'Agent Configuration',
      import: 'Import',
      export: 'Export',
      configuredAgents: 'Configured Agents',
      agentCount: 'agents',
      noAgent: 'No agent configuration',
      createNew: 'Create New Agent',
      selectAgentHint: 'Please select an agent to configure',
      newAgent: 'New Agent',
      agentName: 'Agent Name',
      agentNamePlaceholder: 'Enter agent name',
      description: 'Description',
      descriptionPlaceholder: 'Describe the function and purpose of this agent',
      nextStepPrompt: 'Agent Prompt (personality, requirements, and next step guidance)',
      nextStepPromptPlaceholder: 'Set the agent\'s personality, requirements, and next step guidance...',
      toolConfiguration: 'Tool Configuration',
      assignedTools: 'Assigned Tools',
      noAssignedTools: 'No assigned tools',
      addRemoveTools: 'Add/Remove Tools',
      deleteConfirm: 'Delete Confirmation',
      deleteConfirmText: 'Are you sure you want to delete',
      deleteWarning: 'This operation cannot be undone.',
      requiredFields: 'Please fill in the required fields',
      createSuccess: 'Agent created successfully',
      createFailed: 'Failed to create agent',
      saveSuccess: 'Agent saved successfully',
      saveFailed: 'Failed to save agent',
      deleteSuccess: 'Agent deleted successfully',
      deleteFailed: 'Failed to delete agent',
      importSuccess: 'Agent imported successfully',
      importFailed: 'Failed to import agent',
      exportSuccess: 'Agent exported successfully',
      exportFailed: 'Failed to export agent',
      loadDataFailed: 'Failed to load data',
      loadDetailsFailed: 'Failed to load agent details',
      invalidFormat: 'Invalid agent configuration format: missing required fields'
    },
    // MCP configuration page
    mcpConfig: {
      title: 'MCP Server Configuration',
      mcpServers: 'MCP Servers',
      addMcpServer: 'Add MCP Server',
      serverList: 'Server List',
      noServers: 'No MCP server configuration',
      connectionType: 'Connection Type',
      configJsonLabel: 'MCP JSON Configuration:',
      configJsonPlaceholder: 'Please enter MCP server configuration (JSON format)...',
      instructions: 'Instructions:',
      instructionStep1: 'Find the configuration JSON for your MCP server:',
      instructionStep1Local: 'Local (STDIO)',
      instructionStep1LocalDesc: 'Available at mcp.so, requires Node.js environment and understanding of each item in the configuration JSON for proper adjustments like setting access keys',
      instructionStep1Remote: 'Remote Service (SSE)',
      instructionStep1RemoteDesc: 'Available at mcp.higress.ai/, offers SSE and STREAMING types, currently SSE protocol is more complete',
      instructionStep2: 'Copy the JSON configuration to the input box above, select STUDIO for local, STREAMING or SSE for remote, then submit',
      instructionStep3: 'This will successfully register the MCP tools.',
      instructionStep4: 'Then you need to create a new agent in Agent Configuration, and add the specific MCP tools you just added, which can greatly reduce conflicts and enhance the accuracy of tools being selected by agents',
      configRequired: 'Please enter MCP server configuration',
      invalidJson: 'Configuration JSON format is incorrect, please check syntax',
      addFailed: 'Failed to add MCP server, please try again',
      deleteFailed: 'Failed to delete MCP server, please try again',
      studioExample: 'Please enter MCP server configuration JSON.\n\nExample:\n{\n  "mcpServers": {\n    "github": {\n      "command": "npx",\n      "args": [\n        "-y",\n        "@modelcontextprotocol/server-github"\n      ],\n      "env": {\n        "GITHUB_PERSONAL_ACCESS_TOKEN": "<YOUR_TOKEN>"\n      }\n    }\n  }\n}',
      sseExample: 'Please enter SSE MCP server configuration JSON.\n\nExample:\n{\n  "mcpServers": {\n    "remote-server": {\n      "url": "https://example.com/mcp",\n      "headers": {\n        "Authorization": "Bearer <YOUR_TOKEN>"\n      }\n    }\n  }\n}'
    },
    // Basic configuration
    basicConfig: {
      title: 'Basic Configuration',
      requestTimeout: 'Request timeout (seconds)',
      browserTimeout: 'Browser request timeout (seconds)',
      loadConfigFailed: 'Failed to load configuration, please refresh and try again',
      saveFailed: 'Save failed, please try again',
      resetFailed: 'Reset failed, please try again',
      importFailed: 'Import failed, please check file format'
    }
  },

  // Agent configuration
  agent: {
    title: 'Agent Configuration',
    name: 'Agent Name',
    description: 'Description',
    prompt: 'Agent Prompt (personality, requirements, and next step guidance)',
    tools: 'Tools',
    addAgent: 'Add Agent',
    editAgent: 'Edit Agent',
    deleteAgent: 'Delete Agent',
    deleteConfirm: 'Are you sure you want to delete?',
    deleteWarning: 'This action cannot be undone.',
    namePlaceholder: 'Enter agent name',
    descriptionPlaceholder: 'Describe the function and purpose of this agent',
    promptPlaceholder: 'Set the agent\'s personality, requirements, and next step guidance...',
    toolSelection: 'Tool Selection',
    availableTools: 'Available Tools',
    selectedTools: 'Selected Tools',
    selectTools: 'Select Tools',
    required: '*',
    saveSuccess: 'Agent saved successfully',
    saveFailed: 'Failed to save agent',
    deleteSuccess: 'Agent deleted successfully',
    deleteFailed: 'Failed to delete agent'
  },

  // Model 配置
  model: {
    title: 'Model Configuration',
    name: 'Model Name',
    description: 'Description',
    addModel: 'Add Model',
    editModel: 'Edit Model',
    deleteModel: 'Delete Model',
    deleteConfirm: 'Are you sure you want to delete?',
    deleteWarning: 'This action cannot be undone.',
    namePlaceholder: 'Enter model name',
    descriptionPlaceholder: 'Describe the function and use cases of this model',
    required: '*',
    saveSuccess: 'Model saved successfully',
    saveFailed: 'Failed to save model',
    deleteSuccess: 'Model deleted successfully',
    deleteFailed: 'Failed to delete model'
  },

  // Plan template configuration
  planTemplate: {
    title: 'Plan Template Configuration',
    generator: 'Plan Generator',
    execution: 'Plan Execution',
    prompt: 'Generation Prompt',
    promptPlaceholder: 'Describe the plan you want to generate...',
    generating: 'Generating...',
    generate: 'Generate Plan',
    updatePlan: 'Update Plan',
    executing: 'Executing...',
    execute: 'Execute Plan',
    executionParams: 'Execution Parameters',
    executionParamsPlaceholder: 'Enter execution parameters (optional)...',
    apiUrl: 'API Call URL',
    clearParams: 'Clear Parameters',
    versionControl: 'Version Control',
    rollback: 'Rollback',
    restore: 'Restore',
    currentVersion: 'Current Version',
    saveTemplate: 'Save Template',
    loadTemplate: 'Load Template',
    templateSaved: 'Template saved',
    templateLoaded: 'Template loaded',
    executionSuccess: 'Execution successful',
    executionFailed: 'Execution failed',
    generationSuccess: 'Generation successful',
    generationFailed: 'Generation failed'
  },

  // Chat component
  chat: {
    botName: 'TaskPilot:',
    thinkingLabel: 'TaskPilot Thinking/Processing',
    processing: 'Processing...',
    step: 'Step',
    stepNumber: 'Step {number}',
    stepExecutionDetails: 'Step Execution Details',
    status: {
      executing: 'Executing',
      completed: 'Completed',
      pending: 'Pending',
      failed: 'Failed'
    },
    userInput: {
      message: 'Please enter the required information:',
      submit: 'Submit'
    },
    thinking: 'Thinking...',
    thinkingAnalyzing: 'Analyzing task requirements...',
    thinkingExecuting: 'Executing: {title}',
    thinkingResponse: 'Organizing response for you...',
    thinkingProcessing: 'Processing your request...',
    preparingExecution: 'Preparing to execute plan...',
    preparing: 'Preparing...',
    response: 'Response',
    retry: 'Retry',
    regenerate: 'Regenerate',
    copy: 'Copy',
    scrollToBottom: 'Scroll to Bottom',
    waitingDecision: 'Waiting for Decision',
    executionCompleted: 'Execution Completed',
    noTool: 'No Tool',
    noToolParameters: 'No Tool Parameters',
    executionError: 'Execution Error',
    newMessage: 'New Message',
    networkError: 'Network connection issue, please check your network connection and try again',
    authError: 'Access permission issue, please contact administrator or try later',
    formatError: 'Request format might be incorrect, could you please rephrase your request?',
    unknownError: 'Encountered some issues while processing your request, please try again later',
    thinkingOutput: 'Thinking Output'
  },

  // Input component
  input: {
    placeholder: 'Send a message to JTaskPilot',
    send: 'Send',
    planMode: 'Plan Mode',
    waiting: 'Waiting for task completion...',
    maxLength: 'Max Length',
    charactersRemaining: 'Characters Remaining'
  },

  // Sidebar
  sidebar: {
    title: 'Plan Templates',
    templateList: 'Template List',
    configuration: 'Configuration',
    newPlan: 'New Plan',
    loading: 'Loading...',
    retry: 'Retry',
    noTemplates: 'No available plan templates',
    unnamedPlan: 'Unnamed Plan',
    noDescription: 'No description',
    deleteTemplate: 'Delete this plan template',
    jsonTemplate: 'JSON Template',
    rollback: 'Rollback',
    restore: 'Restore',
    jsonPlaceholder: 'Enter JSON plan template...',
    planGenerator: 'Plan Generator',
    generatorPlaceholder: 'Describe the plan you want to generate...',
    generating: 'Generating...',
    generatePlan: 'Generate Plan',
    updatePlan: 'Update Plan',
    executionController: 'Execution Controller',
    executionParams: 'Execution Parameters',
    executionParamsPlaceholder: 'Enter execution parameters...',
    clearParams: 'Clear Parameters',
    apiUrl: 'API URL',
    executing: 'Executing...',
    executePlan: 'Execute Plan',
    newTemplate: 'New Template',
    templateName: 'Template Name',
    templateDescription: 'Template Description',
    lastModified: 'Last Modified',
    createTime: 'Create Time',
    expand: 'Expand',
    collapse: 'Collapse',
    pin: 'Pin',
    unpin: 'Unpin',
    favorite: 'Favorite',
    unfavorite: 'Unfavorite',
    share: 'Share',
    duplicate: 'Duplicate',
    rename: 'Rename',
    move: 'Move',
    archive: 'Archive',
    unarchive: 'Unarchive',
    selectTemplateFailed: 'Failed to select plan template',
    confirmDelete: 'Are you sure you want to delete plan template "{name}"? This action cannot be undone.',
    templateDeleted: 'Plan template has been deleted.',
    deleteTemplateFailed: 'Failed to delete plan template',
    saveCompleted: 'Save completed: {message}\n\nCurrent version count: {versionCount}',
    saveSuccess: 'Save successful: {message}\n\nCurrent version count: {versionCount}',
    saveStatus: 'Save status: {message}',
    saveFailed: 'Failed to save plan modifications',
    generateSuccess: 'Plan generated successfully! Template ID: {templateId}',
    generateFailed: 'Failed to generate plan',
    updateSuccess: 'Plan updated successfully!',
    updateFailed: 'Failed to update plan',
    executeFailed: 'Failed to execute plan',
    unknown: 'Unknown'
  },

  // Modal
  modal: {
    close: 'Close',
    cancel: 'Cancel',
    confirm: 'Confirm',
    save: 'Save',
    delete: 'Delete',
    edit: 'Edit'
  },

  // Editor
  editor: {
    format: 'Format',
    undo: 'Undo',
    redo: 'Redo',
    find: 'Find',
    replace: 'Replace',
    gotoLine: 'Go to Line',
    selectAll: 'Select All',
    toggleWordWrap: 'Toggle Word Wrap',
    toggleMinimap: 'Toggle Minimap',
    increaseFontSize: 'Increase Font Size',
    decreaseFontSize: 'Decrease Font Size',
    resetFontSize: 'Reset Font Size'
  },

  // Language switching
  language: {
    switch: 'Switch Language',
    current: 'Current Language',
    zh: '中文',
    en: 'English'
  },

  // Theme
  theme: {
    switch: 'Switch Theme',
    light: 'Light Theme',
    dark: 'Dark Theme',
    auto: 'Follow System'
  },

  // Error pages
  error: {
    notFound: 'Page Not Found',
    notFoundDescription: 'Sorry, the page you are looking for does not exist',
    serverError: 'Server Error',
    serverErrorDescription: 'Server encountered some issues, please try again later',
    networkError: 'Network Error',
    networkErrorDescription: 'Network connection failed, please check your network settings',
    backToHome: 'Back to Home',
    retry: 'Retry'
  },

  // Form validation
  validation: {
    required: 'This field is required',
    email: 'Please enter a valid email address',
    phone: 'Please enter a valid phone number',
    url: 'Please enter a valid URL',
    minLength: 'At least {min} characters required',
    maxLength: 'Maximum {max} characters allowed',
    min: 'Value cannot be less than {min}',
    max: 'Value cannot be greater than {max}',
    pattern: 'Invalid format',
    confirmation: 'The two inputs do not match'
  },

  // Time related
  time: {
    now: 'Just now',
    minuteAgo: '{count} minutes ago',
    hourAgo: '{count} hours ago',
    dayAgo: '{count} days ago',
    weekAgo: '{count} weeks ago',
    monthAgo: '{count} months ago',
    yearAgo: '{count} years ago',
    today: 'Today',
    yesterday: 'Yesterday',
    tomorrow: 'Tomorrow',
    thisWeek: 'This Week',
    lastWeek: 'Last Week',
    nextWeek: 'Next Week',
    thisMonth: 'This Month',
    lastMonth: 'Last Month',
    nextMonth: 'Next Month',
    thisYear: 'This Year',
    lastYear: 'Last Year',
    nextYear: 'Next Year'
  },

  // Data statistics
  stats: {
    total: 'Total',
    count: 'Count',
    percentage: 'Percentage',
    average: 'Average',
    median: 'Median',
    min: 'Minimum',
    max: 'Maximum',
    sum: 'Sum',
    growth: 'Growth',
    decline: 'Decline',
    noData: 'No data',
    loading: 'Loading data...',
    error: 'Failed to load data'
  },

  // Home page
  home: {
    welcomeTitle: 'Welcome to JTaskPilot!',
    welcomeSubtitle: 'Your Java AI intelligent assistant, helping you build and complete various tasks.',
    tagline: 'Java AI Agent',
    inputPlaceholder: 'Describe what you want to build or accomplish...',
    examples: {
      stockPrice: {
        title: 'Query Stock Price',
        description: 'Get today\'s latest stock price for Alibaba (Agent can use browser tools)',
        prompt: 'Use browser based on Baidu to query today\'s Alibaba stock price and return the latest stock price'
      },
      novel: {
        title: 'Generate a Novella',
        description: 'Help me generate a novella (Agent can generate longer content)',
        prompt: 'Please help me write a novel about robots replacing humans. 20,000 words. Use TEXT_FILE_AGENT, first generate an outline, then improve and enrich the entire outline content into a coherent novel, and finally smooth out the grammar globally'
      },
      weather: {
        title: 'Query Weather',
        description: 'Get today\'s weather in Beijing (Agent can use MCP tool services)',
        prompt: 'Use browser, based on Baidu, to query today\'s weather in Beijing'
      }
    }
  },

  // Right panel
  rightPanel: {
    stepExecutionDetails: 'Step Execution Details',
    noStepSelected: 'No Step Selected',
    selectStepHint: 'Please select an execution step in the left chat area to view details',
    stepExecuting: 'Step is executing, please wait...',
    step: 'Step',
    executingAgent: 'Executing Agent',
    description: 'Description',
    request: 'Request',
    executionResult: 'Execution Result',
    executing: 'Executing...',
    thinkAndActionSteps: 'Think & Action Steps',
    thinking: 'Thinking',
    action: 'Action',
    input: 'Input',
    output: 'Output',
    tool: 'Tool',
    toolParameters: 'Tool Parameters',
    noStepDetails: 'No detailed step information available',
    scrollToBottom: 'Scroll to Bottom',
    // Step status
    status: {
      completed: 'Completed',
      executing: 'Executing',
      waiting: 'Waiting'
    },
    // Tab labels
    tabs: {
      details: 'Step Execution Details',
      chat: 'Chat',
      code: 'Code'
    },
    // Sample chatBubbles data
    chatBubbles: {
      analyzeRequirements: {
        title: 'Analyze Requirements',
        content: 'Breaking down your request into actionable steps: 1) Create user entity, 2) Implement user service, 3) Build REST endpoints, 4) Add validation and error handling.'
      },
      generateCode: {
        title: 'Generate Code',
        content: 'Creating Spring Boot REST API with user management CRUD operations. Including proper HTTP status codes and error handling.'
      },
      codeGenerated: {
        title: 'Code Generated',
        content: 'Successfully generated UserController with all CRUD operations. The code includes proper REST conventions, error handling, and follows Spring Boot best practices.'
      }
    },
    // Time display
    timeAgo: {
      justNow: 'Just now',
      minutesAgo: '{n} minutes ago',
      hoursAgo: '{n} hours ago',
      daysAgo: '{n} days ago'
    },
    // Default step title
    defaultStepTitle: 'Step {number}'
  },

  // Direct page
  direct: {
    configuration: 'Configuration',
    panelResizeHint: 'Drag to resize panel, double-click to reset',
    aboutExecutionDetails: 'About Integrated Execution Details'
  }
}

export default words
