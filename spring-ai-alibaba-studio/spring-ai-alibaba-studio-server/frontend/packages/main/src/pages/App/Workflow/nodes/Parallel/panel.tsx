import $i18n from '@/i18n';
import {
  CustomInputsControl,
  filterVarItemsByType,
  IVarTreeItem,
  useNodeDataUpdate,
  useNodesOutputParams,
  useNodesReadOnly,
  useReactFlowStore,
} from '@spark-ai/flow';
import { Flex, Select } from 'antd';
import { memo, useCallback, useMemo } from 'react';
import InfoIcon from '../../components/InfoIcon';
import ParallelConfigForm from '../../components/ParallelConfigForm';
import { IParallelNodeData, IParallelNodeParam } from '../../types';

const options = [
  {
    label: $i18n.get({
      id: 'main.pages.App.Workflow.nodes.Parallel.panel.terminateOnError',
      dm: '错误时终止',
    }),
    value: 'terminated',
  },
  {
    label: $i18n.get({
      id: 'main.pages.App.Workflow.nodes.Parallel.panel.ignoreErrorContinue',
      dm: '忽略错误并继续',
    }),
    value: 'continueOnError',
  },
  {
    label: $i18n.get({
      id: 'main.pages.App.Workflow.nodes.Parallel.panel.removeErrorOutput',
      dm: '移除错误输出',
    }),
    value: 'removeErrorOutput',
  },
];

export default memo(function ParallelPanel(props: {
  id: string;
  data: IParallelNodeData;
}) {
  const nodes = useReactFlowStore((store) => store.nodes);
  const edges = useReactFlowStore((store) => store.edges);
  const { handleNodeDataUpdate } = useNodeDataUpdate();
  const { getSubNodesVariables, getVariableList } = useNodesOutputParams();
  const { nodesReadOnly } = useNodesReadOnly();

  const flowVariableList = useMemo(() => {
    return getVariableList({
      nodeId: props.id,
    });
  }, [props.id, nodes, edges]);

  const inputVariableList = useMemo(() => {
    const list: IVarTreeItem[] = [];
    flowVariableList.forEach((item) => {
      const subList = filterVarItemsByType(item.children, [
        'Array<String>',
        'Array<Boolean>',
        'Array<File>',
        'Array<Number>',
        'Array<String>',
      ]);
      if (subList.length > 0) {
        list.push({
          ...item,
          children: subList,
        });
      }
    });
    return list;
  }, [props.id, flowVariableList]);

  const subNodesVariables = useMemo(() => {
    return getSubNodesVariables(props.id);
  }, [props.id, nodes]);

  const changeNodeParam = useCallback(
    (payload: Partial<IParallelNodeParam>) => {
      handleNodeDataUpdate({
        id: props.id,
        data: {
          node_param: {
            ...props.data.node_param,
            ...payload,
          },
        },
      });
    },
    [props.data.node_param, handleNodeDataUpdate],
  );

  return (
    <>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.Parallel.panel.batchSettings',
              dm: '批处理设置',
            })}
          </div>
          <ParallelConfigForm
            value={props.data.node_param}
            onChange={changeNodeParam}
          />
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.Parallel.panel.batchArray',
              dm: '批处理数组',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Parallel.panel.batchInput',
                dm: '批处理的输入必须是数组（List）类型数据，流程会按照数组的索引顺序执行。',
              })}
            />
          </div>
          <CustomInputsControl
            disabled={nodesReadOnly}
            onChange={(val) =>
              handleNodeDataUpdate({
                id: props.id,
                data: {
                  input_params: val,
                },
              })
            }
            variableList={inputVariableList}
            value={props.data.input_params}
          />
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.Parallel.panel.outputVariable',
              dm: '输出变量',
            })}
          </div>
          <CustomInputsControl
            disabled={nodesReadOnly}
            disabledValueFrom
            value={props.data.output_params}
            variableList={subNodesVariables}
            onChange={(val) => {
              handleNodeDataUpdate({
                id: props.id,
                data: {
                  output_params: val,
                },
              });
            }}
          />
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.Parallel.panel.errorResponseMethod',
              dm: '错误响应方法',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Parallel.panel.errorHandling',
                dm: '当批处理中某一个子节点运行失败时，根据用户选择的方式进行处理。',
              })}
            />
          </div>
          <Select
            disabled={nodesReadOnly}
            value={props.data.node_param.error_strategy}
            onChange={(val) =>
              changeNodeParam({
                error_strategy: val as IParallelNodeParam['error_strategy'],
              })
            }
            options={options}
            placeholder={$i18n.get({
              id: 'main.pages.App.Workflow.nodes.Parallel.panel.selectErrorMethod',
              dm: '请选择错误响应方法',
            })}
          />
        </Flex>
      </div>
    </>
  );
});
