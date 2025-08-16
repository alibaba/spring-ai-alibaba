import $i18n from '@/i18n';
import { IMCPTool } from '@/types/mcp';
import {
  getParentInputParams,
  IBranchItem,
  INodeDataInputParamItem,
  IValueType,
  IVarTreeItem,
  IWorkFlowNode,
} from '@spark-ai/flow';
import {
  IIteratorNodeParam,
  ISelectedModelParams,
  IShortMemoryConfig,
  ITryCatchConfig,
} from '../types';

export function getMCPNodeInputParams(
  tool: IMCPTool,
  inputParams: INodeDataInputParamItem[] = [],
) {
  const inputSchema = tool.input_schema;
  return Object.keys(inputSchema.properties)
    .sort((now) => ((inputSchema.required || []).includes(now) ? -1 : 1))
    .map((itemKey) => {
      const targetInput =
        inputParams.find((item) => item.key === itemKey) || {};
      return {
        key: itemKey,
        value_from: 'refer',
        value: void 0,
        ...targetInput,
      } as INodeDataInputParamItem;
    });
}

export const transformInputParams = (
  inputParams: INodeDataInputParamItem[] = [],
  variableMap: Record<string, boolean> = {},
) => {
  inputParams.forEach((item) => {
    if (!item.value || item.value_from === 'input' || variableMap[item.value])
      return;
    variableMap[item.value] = true;
  });
};

export function getVariablesFromText(
  text = '',
  variableMap: Record<string, boolean> = {},
) {
  const regex = /\${(.*?)}/g;
  const matches = text.match(regex);

  matches?.forEach((match) => {
    if (variableMap[match]) return;
    variableMap[match] = true;
    match.substring(2, match.length - 1);
  });
}

export function transformBranchVariables(
  branches: IBranchItem[] = [],
  variableMap: Record<string, boolean> = {},
  { disableShowLeft = false } = {},
) {
  branches.forEach((item) => {
    item.conditions?.forEach((condition) => {
      const { left, right } = condition;
      if (
        left.value_from === 'refer' &&
        !!left.value &&
        !disableShowLeft &&
        !variableMap[left.value]
      ) {
        variableMap[left.value] = true;
      }
      if (
        right.value_from === 'refer' &&
        !!right.value &&
        !variableMap[right.value]
      ) {
        variableMap[right.value] = true;
      }
    });
  });
}

export function checkShortMemory(
  shortMemory: IShortMemoryConfig,
  list: { label: string; error: string }[],
) {
  const { enabled, type, param } = shortMemory;
  if (!enabled || type === 'self') return;
  if (!param.value) {
    list.push({
      label: $i18n.get({
        id: 'main.pages.App.Workflow.utils.index.memory',
        dm: '记忆',
      }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.utils.index.contextVariableRequired',
        dm: '上下文变量不能为空',
      }),
    });
  }
}

export function checkTryCatchConfig(
  tryCatchConfig: ITryCatchConfig,
  list: { label: string; error: string }[],
) {
  if (
    tryCatchConfig.strategy === 'defaultValue' &&
    !tryCatchConfig.default_values?.[0]?.value
  ) {
    list.push({
      label: $i18n.get({
        id: 'main.pages.App.Workflow.utils.index.exceptionHandling',
        dm: '异常处理',
      }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.utils.index.defaultValueRequired',
        dm: '默认值不能为空',
      }),
    });
  }
}

export function checkLLMData(
  val: ISelectedModelParams,
  list: { label: string; error: string }[],
) {
  if (!val?.model_id) {
    list.push({
      label: $i18n.get({
        id: 'main.pages.App.Workflow.utils.index.modelSelection',
        dm: '模型选择',
      }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.utils.index.notNull',
        dm: '不能为空',
      }),
    });
  }
}

// check the input parameters of the start/input node
export function checkInputParams(
  inputParams: { key: string; value?: string }[] = [],
  list: { label: string; error: string }[] = [],
  options: {
    label?: string;
    checkValue?: boolean;
    disableCheckEmptyList?: boolean;
  } = {},
) {
  const keys = new Set<string>();
  let hasEmptyKey = false;
  let hasInvalidKey = false;
  let hasDuplicateKey = false;
  let hasEmptyValue = false;
  if (!inputParams.length && !options.disableCheckEmptyList) {
    list.push({
      label:
        options.label ||
        $i18n.get({
          id: 'main.pages.App.Workflow.utils.index.input',
          dm: '输入',
        }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.utils.index.notNull',
        dm: '不能为空',
      }),
    });
  }

  for (const item of inputParams) {
    // check whether the key is empty
    if (!item.key && !hasEmptyKey) {
      hasEmptyKey = true;
      continue;
    }

    if (options.checkValue && !item.value && !hasEmptyValue) {
      hasEmptyValue = true;
      continue;
    }

    // check whether the key is a valid variable name
    if (!/^[a-zA-Z0-9]*$/.test(item.key) && !hasInvalidKey) {
      hasInvalidKey = true;
      continue;
    }

    // check whether the key is duplicated
    if (keys.has(item.key) && !hasDuplicateKey) {
      hasDuplicateKey = true;
      continue;
    }
    keys.add(item.key);
  }

  if (hasDuplicateKey) {
    list.push({
      label:
        options.label ||
        $i18n.get({
          id: 'main.pages.App.Workflow.utils.index.input',
          dm: '输入',
        }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.utils.index.variableNameDuplicate',
        dm: '变量名不能重复',
      }),
    });
  }

  if (hasEmptyValue) {
    list.push({
      label:
        options.label ||
        $i18n.get({
          id: 'main.pages.App.Workflow.utils.index.input',
          dm: '输入',
        }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.utils.index.valueNotNull',
        dm: '值不能为空',
      }),
    });
  }

  // add other error information
  if (hasEmptyKey) {
    list.push({
      label:
        options.label ||
        $i18n.get({
          id: 'main.pages.App.Workflow.utils.index.input',
          dm: '输入',
        }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.utils.index.variableNameRequired',
        dm: '变量名不能为空',
      }),
    });
  }

  if (hasInvalidKey) {
    list.push({
      label:
        options.label ||
        $i18n.get({
          id: 'main.pages.App.Workflow.utils.index.input',
          dm: '输入',
        }),
      error: $i18n.get({
        id: 'main.pages.App.Workflow.utils.index.variableNameAlphanumeric',
        dm: '变量名只能包含字母和数字',
      }),
    });
  }
}

export const getParentNodeVariableList = (
  parentNode: IWorkFlowNode,
  options: { disableShowVariableParameters?: boolean } = {},
) => {
  const indexParam = {
    label: 'index',
    type: 'Number',
    value: `\${${parentNode.id}.index}`,
  };
  switch (parentNode.type) {
    case 'Iterator':
      const iteratorParam = [
        {
          label: parentNode.data.label,
          nodeId: parentNode.id,
          nodeType: parentNode.type,
          children: [indexParam],
        },
      ] as IVarTreeItem[];
      if (
        (parentNode.data.node_param as IIteratorNodeParam).iterator_type ===
        'byArray'
      ) {
        iteratorParam[0].children = [
          ...iteratorParam[0].children,
          ...getParentInputParams(parentNode),
        ];
      }

      if (!options.disableShowVariableParameters) {
        (
          parentNode.data.node_param as IIteratorNodeParam
        ).variable_parameters?.forEach((item) => {
          iteratorParam[0].children.push({
            label: item.key,
            value: `\${${parentNode.id}.${item.key}}`,
            type: item.type as IValueType,
          });
        });
      }

      return iteratorParam;
    case 'Parallel':
      return [
        {
          label: parentNode.data.label,
          nodeId: parentNode.id,
          nodeType: parentNode.type,
          children: [indexParam, ...getParentInputParams(parentNode)],
        },
      ] as IVarTreeItem[];
    default:
      return [];
  }
};
