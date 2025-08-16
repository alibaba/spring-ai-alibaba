import $i18n from '@/i18n';
import { Button, IconFont } from '@spark-ai/design';
import type { INodeDataOutputParamItem } from '@spark-ai/flow';
import {
  CustomInputsControl,
  CustomOutputsFormWrap,
  useNodeDataUpdate,
} from '@spark-ai/flow';
import ReactCodeMirror from '@uiw/react-codemirror';
import React, { memo, useCallback } from 'react';
import ErrorCatchForm from '../../components/ErrorCatchForm';
import RetryForm from '../../components/RetryForm';
import {
  IScriptNodeData,
  IScriptNodeParam,
  ITryCatchConfig,
} from '../../types/flow';

export const mergeTryCatchDefaultValue = (
  params: INodeDataOutputParamItem[],
  defaultValue: ITryCatchConfig['default_values'],
) => {
  return params.map((item) => {
    const target = defaultValue?.find(
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
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Script.panel.input',
            dm: '输入',
          })}

          <IconFont type="spark-info-line" />
        </div>
        <CustomInputsControl
          value={props.data.input_params}
          onChange={(val) =>
            handleNodeDataUpdate({
              id: props.id,
              data: {
                input_params: val,
              },
            })
          }
        />
      </div>
      <div className="spark-flow-panel-form-section">
        <div className="flex-justify-between">
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'spark-flow.demos.spark-flow-1.nodes.Script.panel.code',
              dm: '代码',
            })}

            <IconFont type="spark-info-line" />
          </div>
          <Button icon={<IconFont type="spark-fullscreen-line" />}>
            {$i18n.get({
              id: 'spark-flow.demos.spark-flow-1.nodes.Script.panel.fullScreenEditing',
              dm: '全屏编辑',
            })}
          </Button>
        </div>
        <ReactCodeMirror />
      </div>
      <div className="spark-flow-panel-form-section">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Script.panel.outputVariables',
            dm: '输出变量',
          })}

          <IconFont type="spark-info-line" />
        </div>
        <CustomOutputsFormWrap
          value={props.data.output_params}
          onChange={changeOutputParams}
        />
      </div>
      <div className="spark-flow-panel-form-section">
        <RetryForm
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
          value={props.data.node_param.try_catch_config}
          onChange={(val) =>
            changeNodeParam({
              try_catch_config: val,
            })
          }
        />
      </div>
    </>
  );
});
