import { DEFAULT_NAME } from '@/constants';

export default {
  site: {
    title: DEFAULT_NAME,
  },
  router: {
    home: '首页',
    agent: '智能体',
    chatbot: '聊天机器人',
    workspace: '工作空间',
    projects: '项目管理',
    graph: '流程图',
  },
  design: {
    toolbar: '工具条',
  },
  page: {
    graph: {
      sn: '序号',
      num: '序号',
      graphName: '流程名称',
      version: '版本',
      graphDesc: '流程描述',
      createTime: '创建时间',
      updateTime: '更新时间',
      addNew: '新增',
      genCode: '生成代码',
      genProject: '生成工程',
      option: '操作',
      delete: '删除',
      editMeta: '编辑',
      design: '设计',
      search: '搜索',
      map: {
        home: '主页',
      },
      toolbar: {
        'import-dsl': '导入DSL',
        'export-dsl': '导出DSL',
      },
      contextMenu: {
        'add-node': '新增节点',
        'import-dsl': '导入DSL',
        'export-dsl': '导出DSL',
      },
    },
  },
};
