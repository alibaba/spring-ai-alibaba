import $i18n from '@/i18n';
import { Select } from '@spark-ai/design';
import {
  CustomInputsControl,
  useNodeDataUpdate,
  useNodesOutputParams,
  useNodesReadOnly,
  useReactFlowStore,
  VarInputTextArea,
} from '@spark-ai/flow';
import { Switch } from 'antd';
import React, { memo, useCallback, useMemo } from 'react';
import InfoIcon from '../../components/InfoIcon';
import { END_NODE_OUTPUT_PARAMS_DEFAULT } from '../../constant';
import { IEndNodeData, IEndNodeParam } from '../../types/flow';

export default memo((props: { id: string; data: IEndNodeData }) => {
  const { handleNodeDataUpdate } = useNodeDataUpdate();
  const { getVariableList } = useNodesOutputParams();
  const nodes = useReactFlowStore((store) => store.nodes);
  const edges = useReactFlowStore((store) => store.edges);
  const { nodesReadOnly } = useNodesReadOnly();

  const variableList = useMemo(() => {
    return getVariableList({
      nodeId: props.id,
    });
  }, [props.id, nodes, edges]);

  const changeNodeParam = useCallback(
    (payload: Partial<IEndNodeParam>) => {
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

  return (
    <div className="spark-flow-panel-form-section">
      <div className="spark-flow-panel-form-title">
        {$i18n.get({
          id: 'main.pages.App.Workflow.nodes.End.panel.outputMode',
          dm: '输出模式',
        })}
      </div>
      <Select
        disabled={nodesReadOnly}
        value={props.data.node_param.output_type}
        options={[
          {
            label: $i18n.get({
              id: 'main.pages.App.Workflow.nodes.End.panel.textOutput',
              dm: '文本输出',
            }),
            value: 'text',
          },
          {
            label: $i18n.get({
              id: 'main.pages.App.Workflow.nodes.End.panel.jsonOutput',
              dm: 'JSON输出',
            }),
            value: 'json',
          },
        ]}
        onChange={(val) =>
          changeNodeParam({
            output_type: val,
            json_params: END_NODE_OUTPUT_PARAMS_DEFAULT,
            text_template: '',
            stream_switch: false,
          })
        }
      />

      {props.data.node_param.output_type === 'text' ? (
        <VarInputTextArea
          disabled={nodesReadOnly}
          variableList={variableList}
          value={props.data.node_param.text_template}
          onChange={(val) => {
            changeNodeParam({
              text_template: val,
            });
          }}
        />
      ) : (
        <CustomInputsControl
          disabled={nodesReadOnly}
          onChange={(val) =>
            changeNodeParam({
              json_params: val,
            })
          }
          disabledValueFrom
          variableList={variableList}
          value={props.data.node_param.json_params}
        />
      )}
      {props.data.node_param.output_type === 'text' && (
        <div className="flex-justify-between w-full">
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.End.panel.streamOutput',
              dm: '流式输出',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.End.panel.outputContentInStream',
                dm: '开启后，节点的来源于大模型的输出内容将用流式呈现。',
              })}
            />
          </div>
          <Switch
            disabled={nodesReadOnly}
            checked={props.data.node_param.stream_switch}
            onChange={(val) => {
              changeNodeParam({
                stream_switch: val,
              });
            }}
          />
        </div>
      )}
    </div>
  );
});
