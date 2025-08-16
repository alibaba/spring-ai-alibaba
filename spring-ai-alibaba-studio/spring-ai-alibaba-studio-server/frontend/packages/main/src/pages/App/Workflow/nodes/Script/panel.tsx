import $i18n from '@/i18n';
import { Button, IconFont, Select } from '@spark-ai/design';
import type { INodeDataOutputParamItem } from '@spark-ai/flow';
import {
  CODE_DEMO_MAP,
  CustomInputsControl,
  CustomOutputsFormWrap,
  ScriptCodeMirror,
  ScriptEditModal,
  useNodeDataUpdate,
  useNodesOutputParams,
  useNodesReadOnly,
  useReactFlowStore,
} from '@spark-ai/flow';
import { Flex } from 'antd';
import { memo, useCallback, useMemo, useState } from 'react';
import ErrorCatchForm from '../../components/ErrorCatchForm';
import InfoIcon from '../../components/InfoIcon';
import RetryForm from '../../components/RetryForm';
import { useWorkflowAppStore } from '../../context/WorkflowAppProvider';
import {
  IScriptNodeData,
  IScriptNodeParam,
  ITryCatchConfig,
} from '../../types';
import { getDefaultValueSchemaFromOutputParams } from '../APINode/panel';
import styles from './index.module.less';

export const SCRIPT_TYPE_OPTIONS = [
  { label: 'Python', value: 'python' },
  { label: 'JavaScript', value: 'javascript' },
];

export const mergeTryCatchDefaultValue = (
  params: INodeDataOutputParamItem[],
  defaultValues: ITryCatchConfig['default_values'],
) => {
  return params.map((item) => {
    const target = defaultValues?.find(
      (v) => v.type === item.type && v.key === item.key,
    );
    return {
      type: item.type,
      key: item.key,
      value: target?.value,
    };
  });
};

export default memo((props: { id: string; data: IScriptNodeData }) => {
  const { handleNodeDataUpdate } = useNodeDataUpdate();
  const { getVariableList } = useNodesOutputParams();
  const nodes = useReactFlowStore((store) => store.nodes);
  const edges = useReactFlowStore((store) => store.edges);
  const globalVariableList = useWorkflowAppStore(
    (state) => state.globalVariableList,
  );
  const [fullScreen, setFullScreen] = useState(false);
  const { nodesReadOnly } = useNodesReadOnly();

  const flowVariableList = useMemo(() => {
    return getVariableList({
      nodeId: props.id,
    });
  }, [props.id, nodes, edges]);

  const variableList = useMemo(() => {
    return [...globalVariableList, ...flowVariableList];
  }, [globalVariableList, flowVariableList]);

  const changeNodeParam = useCallback(
    (payload: Partial<IScriptNodeParam>) => {
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

  const changeOutputParams = useCallback(
    (val: INodeDataOutputParamItem[]) => {
      handleNodeDataUpdate({
        id: props.id,
        data: {
          output_params: val,
          node_param: {
            ...props.data.node_param,
            try_catch_config: {
              ...props.data.node_param.try_catch_config,
              default_values: mergeTryCatchDefaultValue(
                val,
                props.data.node_param.try_catch_config.default_values,
              ),
            },
          },
        },
      });
    },
    [props.data],
  );

  return (
    <>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.Script.panel.input',
              dm: '输入',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Script.panel.parametersOrConstants',
                dm: '脚本需要引用的入参或常量。',
              })}
            />
          </div>
          <CustomInputsControl
            disabled={nodesReadOnly}
            value={props.data.input_params}
            variableList={variableList}
            onChange={(val) =>
              handleNodeDataUpdate({
                id: props.id,
                data: {
                  input_params: val,
                },
              })
            }
          />
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="flex-justify-between">
            <div className="spark-flow-panel-form-title">
              {$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Script.panel.code',
                dm: '代码',
              })}
            </div>
            <Flex align="center" gap={8}>
              <Select
                disabled={nodesReadOnly}
                value={props.data.node_param.script_type}
                popupMatchSelectWidth={false}
                onChange={(val) =>
                  changeNodeParam({
                    script_type: val,
                    script_content: CODE_DEMO_MAP[val],
                  })
                }
                options={SCRIPT_TYPE_OPTIONS}
              />

              <Button
                onClick={() => setFullScreen(true)}
                icon={<IconFont type="spark-fullscreen-line" />}
              >
                {$i18n.get({
                  id: 'main.pages.App.Workflow.nodes.Script.panel.fullScreenEdit',
                  dm: '全屏编辑',
                })}
              </Button>
            </Flex>
          </div>
          <div className={styles['script-code-mirror-container']}>
            <ScriptCodeMirror
              disabled={nodesReadOnly}
              value={props.data.node_param.script_content}
              onChange={(val) =>
                changeNodeParam({
                  script_content: val,
                })
              }
              inputParams={props.data.input_params}
              outputParams={props.data.output_params}
              language={props.data.node_param.script_type}
            />
          </div>
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.VariableHandle.panel.output',
              dm: '输出',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.Script.panel.output',
                dm: '脚本的输出内容以及结构。',
              })}
            />
          </div>
          <CustomOutputsFormWrap
            readyOnly={nodesReadOnly}
            value={props.data.output_params}
            onChange={changeOutputParams}
          />
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <RetryForm
          disabled={nodesReadOnly}
          value={props.data.node_param.retry_config}
          onChange={(val) =>
            changeNodeParam({
              retry_config: val,
            })
          }
        />
      </div>
      <div className="spark-flow-panel-form-section">
        <ErrorCatchForm
          disabled={nodesReadOnly}
          nodeId={props.id}
          value={props.data.node_param.try_catch_config}
          onChange={(val) =>
            changeNodeParam({
              try_catch_config: val,
            })
          }
          onChangeType={(type) => {
            const params =
              type === 'failBranch'
                ? {
                    default_values: getDefaultValueSchemaFromOutputParams(
                      props.data.output_params,
                    ),
                  }
                : {};
            changeNodeParam({
              try_catch_config: {
                ...props.data.node_param.try_catch_config,
                strategy: type,
                ...params,
              },
            });
          }}
        />
      </div>
      {fullScreen && (
        <ScriptEditModal
          disabled={nodesReadOnly}
          language={props.data.node_param.script_type}
          value={props.data.node_param.script_content}
          inputParams={props.data.input_params}
          outputParams={props.data.output_params}
          onClose={() => setFullScreen(false)}
          onOk={({ language, value }) => {
            changeNodeParam({ script_type: language, script_content: value });
            setFullScreen(false);
          }}
        />
      )}
    </>
  );
});
