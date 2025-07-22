import $i18n from '@/i18n';
import { Button, IconFont } from '@spark-ai/design';
import type { INodeDataInputParamItem } from '@spark-ai/flow';
import {
  InputTextArea,
  OutputParamsTree,
  SelectWithDesc,
  VarInputTextArea,
  VariableSelector,
  uniqueId,
  useNodeDataUpdate,
  useNodesOutputParams,
  useReactFlowStore,
} from '@spark-ai/flow';
import React, { memo, useCallback, useMemo } from 'react';
import ShortMemoryForm from '../../components/ShortMemoryForm';
import { IClassifierNodeData, IClassifierNodeParam } from '../../types/flow';

const MAX_LEN = 10;

const modeSwitchOpts = [
  {
    label: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.nodes.Classify.panel.fastMode',
      dm: '快速模式',
    }),
    value: 'efficient',
    desc: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.nodes.Classify.panel.classificationModelAvoidsThinkingProcessToImproveSpeed适用于SimpleScenarios',
      dm: '分类模型会避免输出思考过程，提升速度，适用于简单场景',
    }),
  },
  {
    label: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.nodes.Classify.panel.efficiencyMode',
      dm: '效果模式',
    }),
    value: 'advanced',
    desc: $i18n.get({
      id: 'spark-flow.demos.spark-flow-1.nodes.Classify.panel.classificationModelThinksStepByStepToMatchCorrespondingClassificationMoreAccurately',
      dm: '分类模型会一步一步的思考，更精准的匹配对应的分类',
    }),
  },
];

export default memo(function ClassifyPanel(props: {
  id: string;
  data: IClassifierNodeData;
}) {
  const { handleNodeDataUpdate } = useNodeDataUpdate();
  const nodes = useReactFlowStore((store) => store.nodes);
  const edges = useReactFlowStore((store) => store.edges);
  const { getVariableList } = useNodesOutputParams();

  const variableList = useMemo(() => {
    const list = getVariableList({
      nodeId: props.id,
    });
    return list;
  }, [props.id, nodes, edges]);

  const changeNodeParam = useCallback(
    (payload: Partial<IClassifierNodeParam>) => {
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
    [props.data.node_param],
  );
  const changeConditionItem = useCallback(
    (ind: number, subject: string) => {
      changeNodeParam({
        conditions: props.data.node_param.conditions.map((item, index) => ({
          ...item,
          subject: index === ind ? subject : item.subject,
        })),
      });
    },
    [props.data.node_param],
  );

  const deleteConditionItem = useCallback(
    (ind: number) => {
      changeNodeParam({
        conditions: props.data.node_param.conditions.filter(
          (_, index) => index !== ind,
        ),
      });
    },
    [props.data.node_param],
  );

  const addConditionItem = useCallback(() => {
    if (props.data.node_param.conditions.length >= MAX_LEN + 1) return;
    changeNodeParam({
      conditions: [
        ...props.data.node_param.conditions,
        { id: uniqueId(4), subject: '' },
      ],
    });
  }, [props.data.node_param]);

  const changeInputVariable = useCallback(
    (val: Partial<INodeDataInputParamItem>) => {
      handleNodeDataUpdate({
        id: props.id,
        data: {
          input_params: [
            {
              key: 'content',
              value_from: 'refer',
              type: 'String',
              ...val,
            },
          ],
        },
      });
    },
    [props.id, props.data.input_params],
  );

  return (
    <>
      <div className="spark-flow-panel-form-section">
        <div className="spark-flow-panel-form-section-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Classify.panel.input',
            dm: '输入',
          })}
        </div>
        <VariableSelector
          onChange={changeInputVariable}
          value={props.data.input_params[0]}
          variableList={variableList}
          prefix={props.data.input_params[0].type}
        />
      </div>
      <div className="spark-flow-panel-form-section">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Classify.panel.intentionClassification',
            dm: '意图分类',
          })}
        </div>
        {props.data.node_param.conditions.map((item, index) => {
          if (item.id === 'default') return null;
          return (
            <InputTextArea
              variableList={variableList}
              key={index}
              disabled
              value={item.subject}
              onChange={(val) => changeConditionItem(index, val)}
              onDelete={() => deleteConditionItem(index)}
            />
          );
        })}
        <Button
          onClick={addConditionItem}
          type="link"
          size="small"
          className="self-start spark-flow-text-btn"
          icon={<IconFont type="spark-plus-line" />}
        >
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Classify.panel.addIntention(',
            dm: '添加意图（',
          })}
          {props.data.node_param.conditions.length - 1}/{MAX_LEN}）
        </Button>
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Classify.panel.otherIntention',
            dm: '其他意图',
          })}

          <IconFont type="spark-info-line" />
        </div>
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Classify.panel.thinkingMode',
            dm: '思考模式',
          })}
        </div>
        <SelectWithDesc
          options={modeSwitchOpts}
          value={props.data.node_param.mode_switch}
          onChange={(val) => changeNodeParam({ mode_switch: val })}
        />
      </div>
      <div className="spark-flow-panel-form-section">
        <ShortMemoryForm
          value={props.data.node_param.short_memory}
          onChange={(val) =>
            changeNodeParam({
              short_memory: val,
            })
          }
        />
      </div>
      <div className="spark-flow-panel-form-section">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Classify.panel.prompt',
            dm: '提示词',
          })}

          <IconFont type="spark-info-line" />
        </div>
        <VarInputTextArea variableList={[]} />
      </div>
      <div className="spark-flow-panel-form-section">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Classify.panel.output',
            dm: '输出',
          })}

          <IconFont type="spark-info-line" />
        </div>
        <OutputParamsTree data={props.data.output_params} />
      </div>
    </>
  );
});
