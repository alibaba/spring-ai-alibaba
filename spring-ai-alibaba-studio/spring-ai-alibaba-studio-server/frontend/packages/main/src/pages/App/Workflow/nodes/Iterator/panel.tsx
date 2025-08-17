import $i18n from '@/i18n';
import { SliderSelector } from '@spark-ai/design';
import {
  CustomInputsControl,
  filterVarItemsByType,
  IValueType,
  IVarItem,
  IVarTreeItem,
  JudgeForm,
  useNodeDataUpdate,
  useNodesOutputParams,
  useNodesReadOnly,
  useReactFlowStore,
} from '@spark-ai/flow';
import { Flex, Select } from 'antd';
import { memo, useCallback, useMemo } from 'react';
import InfoIcon from '../../components/InfoIcon';
import IteratorVariableForm from '../../components/IteratorVariableForm';
import { IIteratorNodeData, IIteratorNodeParam } from '../../types';

const options = [
  {
    label: $i18n.get({
      id: 'main.pages.App.Workflow.nodes.Iterator.panel.useArrayLoop',
      dm: '使用数组循环',
    }),
    value: 'byArray',
  },
  {
    label: $i18n.get({
      id: 'main.pages.App.Workflow.nodes.Iterator.panel.specifyLoopCount',
      dm: '指定循环次数',
    }),
    value: 'byCount',
  },
];

export default memo(function IteratorPanel(props: {
  id: string;
  data: IIteratorNodeData;
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
    (payload: Partial<IIteratorNodeParam>) => {
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

  const variableParameters = useMemo(() => {
    const params: IVarItem[] = [];
    props.data.node_param.variable_parameters?.forEach((item) => {
      if (!item.value) return;
      params.push({
        label: item.key,
        value: `\${${props.id}.${item.key}}`,
        type: item.type as IValueType,
      });
    });
    if (!params.length) return [];
    return [
      {
        label: props.data.label,
        nodeId: props.id,
        nodeType: 'Iterator',
        children: params,
      },
    ];
  }, [props.data.node_param.variable_parameters, props.data.label, props.id]);

  return (
    <>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.Iterator.panel.loopType',
              dm: '循环类型',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Iterator.panel.twoTypesOfLoops',
                dm: '分为指定循环次数和数组循环两种，区别在循环的轮次，前者为固定轮次，后者为数组长度。',
              })}
            />
          </div>
          <Select
            disabled={nodesReadOnly}
            options={options}
            value={props.data.node_param.iterator_type}
            onChange={(val) => changeNodeParam({ iterator_type: val })}
          />
        </Flex>

        {props.data.node_param.iterator_type === 'byArray' ? (
          <Flex vertical gap={12}>
            <div className="spark-flow-panel-form-title">
              {$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Iterator.panel.loopArray',
                dm: '循环数组',
              })}

              <InfoIcon
                tip={$i18n.get({
                  id: 'main.pages.App.Workflow.nodes.Iterator.panel.loopBodyInput',
                  dm: '循环体的输入，必须是数组（List）类型数据，循环会按照数组的索引顺序执行。',
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
              defaultType="Array<String>"
              variableList={inputVariableList}
              disabledTypes={[
                'String',
                'Boolean',
                'Number',
                'File',
                'Object',
                'Array<File>',
              ]}
              value={props.data.input_params}
            />
          </Flex>
        ) : (
          <Flex vertical gap={12}>
            <div className="spark-flow-panel-form-title">
              {$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Iterator.panel.loopCount',
                dm: '循环次数',
              })}
            </div>
            <SliderSelector
              disabled={nodesReadOnly}
              value={props.data.node_param.count_limit}
              onChange={(val) =>
                changeNodeParam({ count_limit: val as number })
              }
              min={1}
              max={500}
              step={1}
              inputNumberWrapperStyle={{ width: 54 }}
            />
          </Flex>
        )}
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.Iterator.panel.middleVariable',
              dm: '中间变量',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Iterator.panel.variableUsedInLoopBody',
                dm: '循环体中用到的变量，可以用于循环体中。',
              })}
            />
          </div>
          <IteratorVariableForm
            disabled={nodesReadOnly}
            value={props.data.node_param.variable_parameters}
            onChange={(val) => changeNodeParam({ variable_parameters: val })}
            variableList={flowVariableList}
          />
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <div>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.Iterator.panel.terminationCondition',
              dm: '终止条件',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Iterator.panel.setTerminationCondition',
                dm: '用户自行设定的循环退出条件，通过变量设置节点更新循环体中间变量达成用户提前设置的终止条件的方式，让循环体提前结束循环，当未设置循环终止条件时，会根据循环类型判断退出。',
              })}
            />
          </div>
          <JudgeForm
            areaStyle={{ padding: '20px 0' }}
            disabled={nodesReadOnly}
            value={props.data.node_param.terminations}
            onChange={(val) => changeNodeParam({ terminations: val })}
            leftVariableList={variableParameters}
            rightVariableList={flowVariableList}
          />
        </div>
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.Iterator.panel.outputVariable',
              dm: '输出变量',
            })}
          </div>
          <CustomInputsControl
            disabled={nodesReadOnly}
            value={props.data.output_params}
            disabledValueFrom
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
    </>
  );
});
