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
  // 基础导航
  conversation: '对话',
  plan: '计划执行',
  backHome: '返回首页',
  noPageTip: '您访问的页面不存在。',

  // 通用按钮和操作
  common: {
    cancel: '取消',
    confirm: '确认',
    delete: '删除',
    edit: '编辑',
    save: '保存',
    reset: '重置',
    close: '关闭',
    add: '添加',
    create: '创建',
    update: '更新',
    submit: '提交',
    clear: '清空',
    search: '搜索',
    loading: '加载中...',
    success: '成功',
    error: '错误',
    warning: '警告',
    info: '信息',
    yes: '是',
    no: '否',
    enable: '启用',
    disable: '禁用',
    copy: '复制',
    paste: '粘贴',
    cut: '剪切',
    undo: '撤销',
    redo: '重做',
    select: '选择',
    selectAll: '全选',
    deselectAll: '取消全选',
    previous: '上一步',
    next: '下一步',
    finish: '完成',
    retry: '重试',
    refresh: '刷新',
    import: '导入',
    export: '导出',
    upload: '上传',
    download: '下载',
    preview: '预览',
    expand: '展开',
    collapse: '收起',
    maximize: '最大化',
    minimize: '最小化',
    fullscreen: '全屏',
    exitFullscreen: '退出全屏',
    parameters: '参数',
    thinking: '思考',
    input: '输入'
  },

  // 配置相关
  config: {
    title: '配置管理',
    loading: '正在加载配置...',
    notFound: '未找到配置项',
    reset: '重置',
    resetGroupConfirm: '重置该组所有配置为默认值',
    modified: '已修改',
    saved: '配置已保存',
    saveFailed: '保存失败',
    search: '搜索配置项...',
    mcpSearch: '搜索MCP服务器...',
    mcpConfigPlaceholder: '请输入MCP服务器的配置(JSON格式)...',
    types: {
      string: '字符串',
      text: '文本',
      number: '数值',
      boolean: '布尔值',
      select: '选择',
      textarea: '多行',
      checkbox: '复选框'
    },
    range: '范围',
    min: '最小值',
    max: '最大值',
    categories: {
      basic: '基础配置',
      agent: 'Agent配置',
      model: 'Model配置',
      mcp: 'Tools/MCP配置'
    },
    // Agent配置页面
    agentConfig: {
      title: 'Agent配置',
      import: '导入',
      export: '导出',
      configuredAgents: '已配置的Agent',
      agentCount: '个',
      noAgent: '暂无Agent配置',
      createNew: '新建Agent',
      selectAgentHint: '请选择一个Agent进行配置',
      newAgent: '新建Agent',
      agentName: 'Agent名称',
      agentNamePlaceholder: '输入Agent名称',
      description: '描述',
      descriptionPlaceholder: '描述这个Agent的功能和用途',
      nextStepPrompt: 'Agent提示词（人设，要求，以及下一步动作的指导）',
      nextStepPromptPlaceholder: '设置Agent的人设、要求以及下一步动作的指导...',
      modelConfiguration: '模型配置',
      modelChoose: '模型选择',
      controlledByPlan: '由计划控制（根据Model配置，由计划控制模型调用）',
      modelName: '模型名称',
      modelNamePlaceholder: '输入Model名称（为空时调用默认Model）',
      toolConfiguration: '工具配置',
      assignedTools: '已分配工具',
      noAssignedTools: '暂无分配的工具',
      addRemoveTools: '添加/删除工具',
      deleteConfirm: '删除确认',
      deleteConfirmText: '确定要删除',
      deleteWarning: '此操作不可恢复。',
      requiredFields: '请填写必要的字段',
      createSuccess: 'Agent创建成功',
      createFailed: '创建Agent失败',
      saveSuccess: 'Agent保存成功',
      saveFailed: '保存Agent失败',
      deleteSuccess: 'Agent删除成功',
      deleteFailed: '删除Agent失败',
      importSuccess: 'Agent导入成功',
      importFailed: '导入Agent失败',
      exportSuccess: 'Agent导出成功',
      exportFailed: '导出Agent失败',
      loadDataFailed: '加载数据失败',
      loadDetailsFailed: '加载Agent详情失败',
      invalidFormat: 'Agent配置格式不正确：缺少必要字段'
    },
    // Model配置页面
    modelConfig: {
      title: 'Model配置',
      import: '导入',
      export: '导出',
      configuredModels: '已配置的Model',
      modelCount: '个',
      noModel: '暂无Model配置',
      createNew: '新建Model',
      selectModelHint: '请选择一个Model进行配置',
      newModel: '新建Model',
      modelName: 'Model名称',
      modelNamePlaceholder: '输入Model名称',
      description: '描述',
      descriptionPlaceholder: '描述这个Model及适用场景',
      deleteConfirm: '删除确认',
      deleteConfirmText: '确定要删除',
      deleteWarning: '此操作不可恢复。',
      requiredFields: '请填写必要的字段',
      createSuccess: 'Model创建成功',
      createFailed: '创建Model失败',
      saveSuccess: 'Model保存成功',
      saveFailed: '保存Model失败',
      deleteSuccess: 'Model删除成功',
      deleteFailed: '删除Model失败',
      importSuccess: 'Model导入成功',
      importFailed: '导入Model失败',
      exportSuccess: 'Model导出成功',
      exportFailed: '导出Model失败',
      loadDataFailed: '加载数据失败',
      loadDetailsFailed: '加载Model详情失败',
      invalidFormat: 'Model配置格式不正确：缺少必要字段'
    },
    // MCP配置页面
    mcpConfig: {
      title: 'MCP服务器配置',
      mcpServers: 'MCP服务器',
      addMcpServer: '添加MCP服务器',
      serverList: '服务器列表',
      noServers: '暂无MCP服务器配置',
      connectionType: '连接类型',
      configJsonLabel: 'mcp json配置：',
      configJsonPlaceholder: '请输入MCP服务器的配置(JSON格式)...',
      instructions: '使用说明：',
      instructionStep1: '找到你要用的mcp server的配置json：',
      instructionStep1Local: '本地(STDIO)',
      instructionStep1LocalDesc: '可以在 mcp.so 上找到，需要你有Node.js环境并理解你要配置的json里面的每一个项，做对应调整比如配置ak',
      instructionStep1Remote: '远程服务(SSE)',
      instructionStep1RemoteDesc: 'mcp.higress.ai/ 上可以找到，有SSE和STREAMING两种，目前SSE协议更完备一些',
      instructionStep2: '将json配置复制到上面的输入框，本地选STUDIO，远程选STREAMING或SSE，提交',
      instructionStep3: '这样mcp tools就注册成功了。',
      instructionStep4: '然后需要在Agent配置里面，新建一个agent，然后增加指定你刚才添加的mcp tools，这样可以极大减少冲突，增强tools被agent选择的准确性',
      configRequired: '请输入MCP服务器配置',
      invalidJson: '配置JSON格式不正确，请检查语法',
      addFailed: '添加MCP服务器失败，请重试',
      deleteFailed: '删除MCP服务器失败，请重试',
      studioExample: '请输入MCP服务器配置JSON。\n\n例如：\n{\n  "mcpServers": {\n    "github": {\n      "command": "npx",\n      "args": [\n        "-y",\n        "@modelcontextprotocol/server-github"\n      ],\n      "env": {\n        "GITHUB_PERSONAL_ACCESS_TOKEN": "<YOUR_TOKEN>"\n      }\n    }\n  }\n}',
      sseExample: '请输入SSE MCP服务器配置JSON。\n\n例如：\n{\n  "mcpServers": {\n    "remote-server": {\n      "url": "https://example.com/mcp",\n      "headers": {\n        "Authorization": "Bearer <YOUR_TOKEN>"\n      }\n    }\n  }\n}'
    },
    // 基础配置
    basicConfig: {
      title: '基础配置',
      requestTimeout: '请求超时时间(秒)',
      browserTimeout: '浏览器请求超时时间(秒)',
      loadConfigFailed: '加载配置失败，请刷新重试',
      saveFailed: '保存失败，请重试',
      resetFailed: '重置失败，请重试',
      importFailed: '导入失败，请检查文件格式'
    }
  },

  // Agent 配置
  agent: {
    title: 'Agent 配置',
    name: 'Agent名称',
    description: '描述',
    prompt: 'Agent提示词（人设，要求，以及下一步动作的指导）',
    tools: '工具',
    addAgent: '新建Agent',
    editAgent: '编辑Agent',
    deleteAgent: '删除Agent',
    deleteConfirm: '确定要删除吗？',
    deleteWarning: '此操作不可恢复。',
    namePlaceholder: '输入Agent名称',
    descriptionPlaceholder: '描述这个Agent的功能和用途',
    promptPlaceholder: '设置Agent的人设、要求以及下一步动作的指导...',
    toolSelection: '工具选择',
    availableTools: '可用工具',
    selectedTools: '已选工具',
    selectTools: '选择工具',
    required: '*',
    saveSuccess: 'Agent保存成功',
    saveFailed: 'Agent保存失败',
    deleteSuccess: 'Agent删除成功',
    deleteFailed: 'Agent删除失败'
  },

  // Model 配置
  model: {
    title: 'Model 配置',
    name: 'Model名称',
    description: '描述',
    addModel: '新建Model',
    editModel: '编辑Model',
    deleteModel: '删除Model',
    deleteConfirm: '确定要删除吗？',
    deleteWarning: '此操作不可恢复。',
    namePlaceholder: '输入Model名称',
    descriptionPlaceholder: '描述这个Model及适用场景',
    required: '*',
    saveSuccess: 'Model保存成功',
    saveFailed: 'Model保存失败',
    deleteSuccess: 'Model删除成功',
    deleteFailed: 'Model删除失败'
  },

  // 计划模板配置
  planTemplate: {
    title: '计划模板配置',
    generator: '计划生成器',
    execution: '计划执行',
    prompt: '生成提示',
    promptPlaceholder: '描述您想要生成的计划...',
    generating: '生成中...',
    generate: '生成计划',
    updatePlan: '更新计划',
    executing: '执行中...',
    execute: '执行计划',
    executionParams: '执行参数',
    executionParamsPlaceholder: '输入执行参数（可选）...',
    apiUrl: 'API 调用地址',
    clearParams: '清空参数',
    versionControl: '版本控制',
    rollback: '回滚',
    restore: '恢复',
    currentVersion: '当前版本',
    saveTemplate: '保存模板',
    loadTemplate: '加载模板',
    templateSaved: '模板已保存',
    templateLoaded: '模板已加载',
    executionSuccess: '执行成功',
    executionFailed: '执行失败',
    generationSuccess: '生成成功',
    generationFailed: '生成失败'
  },

  // 聊天组件
  chat: {
    botName: 'TaskPilot:',
    thinkingLabel: 'TaskPilot 思考/处理',
    processing: '处理中...',
    step: '步骤',
    stepNumber: '步骤 {number}',
    stepExecutionDetails: '步骤执行详情',
    status: {
      executing: '执行中',
      completed: '已完成',
      pending: '待执行',
      failed: '失败'
    },
    userInput: {
      message: '请输入所需信息:',
      submit: '提交'
    },
    thinking: '正在思考...',
    thinkingAnalyzing: '正在分析任务需求...',
    thinkingExecuting: '正在执行: {title}',
    thinkingResponse: '正在组织语言回复您...',
    thinkingProcessing: '正在处理您的请求...',
    preparingExecution: '准备执行计划...',
    preparing: '准备执行...',
    response: '回复',
    retry: '重试',
    regenerate: '重新生成',
    copy: '复制',
    scrollToBottom: '滚动到底部',
    waitingDecision: '等待决策中',
    executionCompleted: '执行完成',
    noTool: '无工具',
    noToolParameters: '无工具参数',
    executionError: '执行出现错误',
    newMessage: '新消息',
    networkError: '网络连接有问题，请检查您的网络连接后再试一次',
    authError: '访问权限出现了问题，请联系管理员或稍后再试',
    formatError: '请求格式可能有些问题，能否请您重新表述一下您的需求？',
    unknownError: '处理您的请求时遇到了一些问题，请稍后再试',
    thinkingOutput: '思考输出'
  },

  // 输入组件
  input: {
    placeholder: '向 JTaskPilot 发送消息',
    send: '发送',
    planMode: '计划模式',
    waiting: '等待任务完成...',
    maxLength: '最大长度',
    charactersRemaining: '剩余字符'
  },

  // 侧边栏
  sidebar: {
    title: '计划模板',
    templateList: '模板列表',
    configuration: '配置',
    newPlan: '新建计划',
    loading: '加载中...',
    retry: '重试',
    noTemplates: '没有可用的计划模板',
    unnamedPlan: '未命名计划',
    noDescription: '无描述',
    deleteTemplate: '删除此计划模板',
    jsonTemplate: 'JSON 模板',
    rollback: '回滚',
    restore: '恢复',
    jsonPlaceholder: '输入 JSON 计划模板...',
    planGenerator: '计划生成器',
    generatorPlaceholder: '描述您想要生成的计划...',
    generating: '生成中...',
    generatePlan: '生成计划',
    updatePlan: '更新计划',
    executionController: '执行控制器',
    executionParams: '执行参数',
    executionParamsPlaceholder: '输入执行参数...',
    clearParams: '清空参数',
    apiUrl: 'API URL',
    executing: '执行中...',
    executePlan: '执行计划',
    newTemplate: '新建模板',
    templateName: '模板名称',
    templateDescription: '模板描述',
    lastModified: '最后修改',
    createTime: '创建时间',
    expand: '展开',
    collapse: '收起',
    pin: '固定',
    unpin: '取消固定',
    favorite: '收藏',
    unfavorite: '取消收藏',
    share: '分享',
    duplicate: '复制',
    rename: '重命名',
    move: '移动',
    archive: '归档',
    unarchive: '取消归档',
    selectTemplateFailed: '选择计划模板失败',
    confirmDelete: '确定要删除计划模板 "{name}" 吗？此操作不可恢复。',
    templateDeleted: '计划模板已删除。',
    deleteTemplateFailed: '删除计划模板失败',
    saveCompleted: '保存完成：{message}\n\n当前版本数：{versionCount}',
    saveSuccess: '保存成功：{message}\n\n当前版本数：{versionCount}',
    saveStatus: '保存状态：{message}',
    saveFailed: '保存计划修改失败',
    generateSuccess: '计划生成成功！模板ID: {templateId}',
    generateFailed: '生成计划失败',
    updateSuccess: '计划更新成功！',
    updateFailed: '更新计划失败',
    executeFailed: '执行计划失败',
    unknown: '未知'
  },

  // 模态框
  modal: {
    close: '关闭',
    cancel: '取消',
    confirm: '确认',
    save: '保存',
    delete: '删除',
    edit: '编辑'
  },

  // 编辑器
  editor: {
    format: '格式化',
    undo: '撤销',
    redo: '重做',
    find: '查找',
    replace: '替换',
    gotoLine: '跳转到行',
    selectAll: '全选',
    toggleWordWrap: '切换自动换行',
    toggleMinimap: '切换迷你地图',
    increaseFontSize: '增大字体',
    decreaseFontSize: '减小字体',
    resetFontSize: '重置字体大小'
  },

  // 语言切换
  language: {
    switch: '切换语言',
    current: '当前语言',
    zh: '中文',
    en: 'English'
  },

  // 主题
  theme: {
    switch: '切换主题',
    light: '浅色主题',
    dark: '深色主题',
    auto: '跟随系统'
  },

  // 错误页面
  error: {
    notFound: '页面未找到',
    notFoundDescription: '抱歉，您访问的页面不存在',
    serverError: '服务器错误',
    serverErrorDescription: '服务器出现了一些问题，请稍后再试',
    networkError: '网络错误',
    networkErrorDescription: '网络连接失败，请检查您的网络设置',
    backToHome: '返回首页',
    retry: '重试'
  },

  // 表单验证
  validation: {
    required: '此字段为必填项',
    email: '请输入有效的邮箱地址',
    phone: '请输入有效的手机号码',
    url: '请输入有效的网址',
    minLength: '至少需要 {min} 个字符',
    maxLength: '最多只能输入 {max} 个字符',
    min: '值不能小于 {min}',
    max: '值不能大于 {max}',
    pattern: '格式不正确',
    confirmation: '两次输入不一致'
  },

  // 时间相关
  time: {
    now: '刚刚',
    minuteAgo: '{count} 分钟前',
    hourAgo: '{count} 小时前',
    dayAgo: '{count} 天前',
    weekAgo: '{count} 周前',
    monthAgo: '{count} 个月前',
    yearAgo: '{count} 年前',
    today: '今天',
    yesterday: '昨天',
    tomorrow: '明天',
    thisWeek: '本周',
    lastWeek: '上周',
    nextWeek: '下周',
    thisMonth: '本月',
    lastMonth: '上月',
    nextMonth: '下月',
    thisYear: '今年',
    lastYear: '去年',
    nextYear: '明年'
  },

  // 数据统计
  stats: {
    total: '总计',
    count: '数量',
    percentage: '百分比',
    average: '平均',
    median: '中位数',
    min: '最小值',
    max: '最大值',
    sum: '总和',
    growth: '增长',
    decline: '下降',
    noData: '暂无数据',
    loading: '数据加载中...',
    error: '数据加载失败'
  },

  // 首页
  home: {
    welcomeTitle: '欢迎使用 JTaskPilot！',
    welcomeSubtitle: '您的 Java AI 智能助手，帮助您构建和完成各种任务。',
    tagline: 'Java AI 智能体',
    inputPlaceholder: '描述您想构建或完成的内容...',
    examples: {
      stockPrice: {
        title: '查询股价',
        description: '获取今天阿里巴巴的最新股价（Agent可以使用浏览器工具）',
        prompt: '用浏览器基于百度，查询今天阿里巴巴的股价，并返回最新股价'
      },
      novel: {
        title: '生成一个中篇小说',
        description: '帮我生成一个中篇小说（Agent可以生成更长的内容）',
        prompt: '请帮我写一个关于机器人取代人类的小说。20000字。 使用TEXT_FILE_AGENT ，先生成提纲，然后，完善和丰满整个提纲的内容为一篇通顺的小说，最后再全局通顺一下语法'
      },
      weather: {
        title: '查询天气',
        description: '获取北京今天的天气情况（Agent可以使用MCP工具服务）',
        prompt: '用浏览器，基于百度，查询北京今天的天气'
      }
    }
  },

  // 右侧面板
  rightPanel: {
    stepExecutionDetails: '步骤执行详情',
    noStepSelected: '未选择执行步骤',
    selectStepHint: '请在左侧聊天区域选择一个执行步骤查看详情',
    stepExecuting: '步骤正在执行中，请稍候...',
    step: '步骤',
    executingAgent: '执行智能体',
    description: '描述',
    request: '请求',
    callingModel: '调用模型',
    executionResult: '执行结果',
    executing: '执行中...',
    thinkAndActionSteps: '思考与行动步骤',
    thinking: '思考',
    action: '行动',
    input: '输入',
    output: '输出',
    tool: '工具',
    toolParameters: '工具参数',
    noStepDetails: '暂无详细步骤信息',
    scrollToBottom: '滚动到底部',
    // 步骤状态
    status: {
      completed: '已完成',
      executing: '执行中',
      waiting: '等待执行'
    },
    // Tab 标签
    tabs: {
      details: '步骤执行详情',
      chat: 'Chat',
      code: 'Code'
    },
    // 示例 chatBubbles 数据
    chatBubbles: {
      analyzeRequirements: {
        title: '分析需求',
        content: '将您的请求分解为可操作的步骤：1) 创建用户实体，2) 实现用户服务，3) 构建 REST 端点，4) 添加验证和错误处理。'
      },
      generateCode: {
        title: '生成代码',
        content: '创建具有用户管理 CRUD 操作的 Spring Boot REST API。包括正确的 HTTP 状态代码和错误处理。'
      },
      codeGenerated: {
        title: '代码已生成',
        content: '成功生成具有所有 CRUD 操作的 UserController。代码包含正确的 REST 约定、错误处理，并遵循 Spring Boot 最佳实践。'
      }
    },
    // 时间显示
    timeAgo: {
      justNow: '刚刚',
      minutesAgo: '{n} 分钟前',
      hoursAgo: '{n} 小时前',
      daysAgo: '{n} 天前'
    },
    // 默认步骤标题
    defaultStepTitle: '步骤 {number}'
  },

  // 直接页面
  direct: {
    configuration: '配置',
    panelResizeHint: '拖拽调整面板大小，双击重置',
    aboutExecutionDetails: '关于集成执行详情'
  }
}

export default words
