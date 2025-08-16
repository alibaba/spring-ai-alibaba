import $i18n from '@/i18n';
import { Button, IconFont } from '@spark-ai/design';
import type { IVarItem, IVarTreeItem } from '@spark-ai/flow';
import {
  InputTextArea,
  OutputParamsTree,
  SelectWithDesc,
  VarInputTextArea,
  VariableSelector,
  filterVarItemsByType,
  uniqueId,
  useNodeDataUpdate,
  useNodesOutputParams,
  useNodesReadOnly,
  useReactFlowStore,
} from '@spark-ai/flow';
import { Flex } from 'antd';
import { memo, useCallback, useMemo } from 'react';
import InfoIcon from '../../components/InfoIcon';
import ModelConfigFormWrap from '../../components/ModelConfigFormWrap';
import ShortMemoryForm from '../../components/ShortMemoryForm';
import { useWorkflowAppStore } from '../../context/WorkflowAppProvider';
import { IClassifierNodeData, IClassifierNodeParam } from '../../types';

const MAX_LEN = 10;

const modeSwitchOpts = [
  {
    label: $i18n.get({
      id: 'main.pages.App.Workflow.nodes.Classifier.panel.fastMode',
      dm: '快速模式',
    }),
    value: 'efficient',
    desc: $i18n.get({
      id: 'main.pages.App.Workflow.nodes.Classifier.panel.classificationModelAvoidsThinkingProcess',
      dm: '分类模型会避免输出思考过程，提升速度，适用于简单场景',
    }),
  },
  {
    label: $i18n.get({
      id: 'main.pages.App.Workflow.nodes.Classifier.panel.efficiencyMode',
      dm: '效果模式',
    }),
    value: 'advanced',
    desc: $i18n.get({
      id: 'main.pages.App.Workflow.nodes.Classifier.panel.classificationModelThinksStepByStep',
      dm: '分类模型会一步一步的思考，更精准的匹配对应的分类',
    }),
  },
];

export default memo(function ClassifyPanel(props: {
  id: string;
  data: IClassifierNodeData;
}) {
  const { handleNodeDataUpdate } = useNodeDataUpdate();
  const { getVariableList } = useNodesOutputParams();
  const globalVariableList = useWorkflowAppStore(
    (state) => state.globalVariableList,
  );
  const nodes = useReactFlowStore((store) => store.nodes);
  const edges = useReactFlowStore((store) => store.edges);
  const { nodesReadOnly } = useNodesReadOnly();

  const flowVariableList = useMemo(() => {
    return getVariableList({
      nodeId: props.id,
    });
  }, [props.id, nodes, edges]);

  const variableList = useMemo(() => {
    return [...globalVariableList, ...flowVariableList];
  }, [globalVariableList, flowVariableList]);

  const variableListByArrayType = useMemo(() => {
    const list: IVarTreeItem[] = [];
    variableList.forEach((item) => {
      const params = filterVarItemsByType(item.children, [
        'Array<String>',
        'Array<Object>',
      ]);
      if (!params.length) return;
      list.push({
        ...item,
        children: params,
      });
    });
    return list;
  }, [variableList]);

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
    (val: Partial<IVarItem>) => {
      handleNodeDataUpdate({
        id: props.id,
        data: {
          input_params: [
            {
              key: 'input',
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

  const inputVariableList = useMemo(() => {
    const list: IVarTreeItem[] = [];
    variableList.forEach((item) => {
      const subList = filterVarItemsByType(item.children, ['String']);
      if (subList.length > 0) {
        list.push({
          ...item,
          children: subList,
        });
      }
    });
    return list;
  }, [variableList]);

  const fileVariableList = useMemo(() => {
    const list: IVarTreeItem[] = [];
    variableList.forEach((item) => {
      const subList = filterVarItemsByType(item.children, [
        'File',
        'Array<File>',
      ]);
      if (subList.length > 0) {
        list.push({
          ...item,
          children: subList,
        });
      }
    });
    return list;
  }, [variableList]);

  return (
    <>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            <span>
              {$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Classifier.panel.inputVariables',
                dm: '输入变量',
              })}
            </span>
            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Classifier.panel.inputContentForIntentionJudgment',
                dm: '输入需要用做意图判断的内容。',
              })}
            />
          </div>
          <VariableSelector
            disabled={nodesReadOnly}
            onChange={changeInputVariable}
            value={props.data.input_params[0]}
            variableList={inputVariableList}
            prefix="String"
          />
        </Flex>
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.Classifier.panel.modelSelection',
              dm: '模型选择',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Classifier.panel.selectAppropriateModel',
                dm: '按需选合适模型辅助识别。',
              })}
            />
          </div>
          <ModelConfigFormWrap
            disabled={nodesReadOnly}
            variableList={fileVariableList}
            value={props.data.node_param.model_config}
            onChange={(val) => changeNodeParam({ model_config: val })}
          />
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            <span>
              {$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Classifier.panel.intentionClassification',
                dm: '意图分类',
              })}
            </span>
            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Classifier.panel.configureIntentionBranches',
                dm: '配置需要模型来判断的意图分支。',
              })}
            />
          </div>
          {props.data.node_param.conditions.map((item, index) => {
            if (item.id === 'default') return null;
            return (
              <InputTextArea
                disabled={nodesReadOnly}
                variableList={variableList}
                key={index}
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
            disabled={nodesReadOnly}
          >
            {$i18n.get(
              {
                id: 'main.pages.App.Workflow.nodes.Classifier.panel.addIntention',
                dm: '添加意图（{var1}/{var2}）',
              },
              {
                var1: props.data.node_param.conditions.length - 1,
                var2: MAX_LEN,
              },
            )}
          </Button>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.Classifier.panel.otherIntention',
              dm: '其他意图',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Classifier.panel.executeThisBranchWhenNoIntentionMatches',
                dm: '当模型判断所有意图均不满足时执行该分支。',
              })}
            />
          </div>
        </Flex>
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.Classifier.panel.thinkingMode',
              dm: '思考模式',
            })}
          </div>
          <SelectWithDesc
            disabled={nodesReadOnly}
            options={modeSwitchOpts}
            value={props.data.node_param.mode_switch}
            onChange={(val) => changeNodeParam({ mode_switch: val })}
          />
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <ShortMemoryForm
          disabled={nodesReadOnly}
          variableList={variableListByArrayType}
          value={props.data.node_param.short_memory}
          onChange={(val) =>
            changeNodeParam({
              short_memory: val,
            })
          }
        />
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.Classifier.panel.prompt',
              dm: '提示词',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Classifier.panel.provideAdditionalRequirements',
                dm: '为意图识别模型提供额外的要求或约束。',
              })}
            />
          </div>
          <VarInputTextArea
            disabled={nodesReadOnly}
            variableList={variableList}
            value={props.data.node_param.instruction}
            onChange={(val) =>
              changeNodeParam({
                instruction: val,
              })
            }
            maxLength={Number.MAX_SAFE_INTEGER}
          />
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.Classifier.panel.output',
              dm: '输出',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Classifier.panel.outputSpecificResult',
                dm: '输出具体的识别结果，效果模式下会输出详细的思考过程。',
              })}
            />
          </div>
          <OutputParamsTree data={props.data.output_params} />
        </Flex>
      </div>
    </>
  );
});
