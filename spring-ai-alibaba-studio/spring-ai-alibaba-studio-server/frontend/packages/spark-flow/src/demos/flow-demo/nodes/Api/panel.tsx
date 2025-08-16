import $i18n from '@/i18n';
import { Button, IconFont, Radio, Select } from '@spark-ai/design';
import type { INodeDataInputParamItem } from '@spark-ai/flow';
import {
  CustomInputsControl,
  CustomOutputsFormWrap,
  OutputParamsTree,
  SelectWithDesc,
  useNodeDataUpdate,
  VarInputTextArea,
} from '@spark-ai/flow';
import React, { memo, useCallback } from 'react';
import ErrorCatchForm from '../../components/ErrorCatchForm';
import RetryForm from '../../components/RetryForm';
import { IApiNodeData, IApiNodeParam } from '../../types/flow';

const REQUEST_METHODS_OPTIONS = [
  {
    label: 'GET',
    value: 'get',
  },
  {
    label: 'POST',
    value: 'post',
  },
  {
    label: 'PUT',
    value: 'put',
  },
  {
    label: 'DELETE',
    value: 'delete',
  },
  {
    label: 'PATCH',
    value: 'patch',
  },
];

const BODY_TYPE_OPTIONS = [
  {
    label: 'none',
    value: 'none',
  },
  {
    label: 'form-data',
    value: 'form-data',
  },
  {
    label: 'raw',
    value: 'raw',
  },
  {
    label: 'JSON',
    value: 'json',
  },
];

export default memo(function ApiPanel({
  id,
  data,
}: {
  id: string;
  data: IApiNodeData;
}) {
  const { handleNodeDataUpdate } = useNodeDataUpdate();

  const changeNodeParam = useCallback(
    (payload: Partial<IApiNodeParam>) => {
      handleNodeDataUpdate({
        id: id,
        data: {
          node_param: {
            ...data.node_param,
            ...payload,
          },
        },
      });
    },
    [data.node_param],
  );

  return (
    <>
      <div className="spark-flow-panel-form-section">
        <div className="flex-justify-between">
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'spark-flow.demos.spark-flow-1.nodes.Api.panel.apiUrl',
              dm: 'API地址',
            })}
          </div>
          <Button
            className="spark-flow-text-btn"
            size="small"
            type="text"
            icon={<IconFont type="spark-setting-line" />}
          >
            {$i18n.get({
              id: 'spark-flow.demos.spark-flow-1.nodes.Api.panel.authorization',
              dm: '鉴权',
            })}

            {$i18n.get({
              id: 'spark-flow.demos.spark-flow-1.nodes.Api.panel.none',
              dm: '(无)',
            })}
          </Button>
        </div>
        <SelectWithDesc
          value={data.node_param.method}
          onChange={(val) => changeNodeParam({ method: val })}
          options={REQUEST_METHODS_OPTIONS}
        />

        <VarInputTextArea variableList={[]} />
      </div>
      <div className="spark-flow-panel-form-section">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Api.panel.headerSettings',
            dm: 'Header设置',
          })}
        </div>
        <CustomInputsControl
          value={data.node_param.headers}
          onChange={(val) => changeNodeParam({ headers: val })}
        />
      </div>
      <div className="spark-flow-panel-form-section">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Api.panel.bodySettings',
            dm: 'Body设置',
          })}
        </div>
        <Radio.Group
          value={data.node_param.body.type}
          onChange={(e) =>
            changeNodeParam({
              body: {
                ...data.node_param.body,
                type: e.target.value,
                data: e.target.value === 'form-data' ? [] : '',
              },
            })
          }
          options={BODY_TYPE_OPTIONS}
        />

        {data.node_param.body.type === 'none' ? null : data.node_param.body
            .type === 'form-data' ? (
          <CustomInputsControl
            onChange={(val) =>
              changeNodeParam({
                body: {
                  ...data.node_param.body,
                  data: val,
                },
              })
            }
            value={data.node_param.body.data as Array<INodeDataInputParamItem>}
          />
        ) : (
          <VarInputTextArea variableList={[]} />
        )}
      </div>
      <div className="spark-flow-panel-form-section">
        <RetryForm
          value={data.node_param.retry_config}
          onChange={(val) =>
            changeNodeParam({
              retry_config: val,
            })
          }
        />
      </div>
      <div className="spark-flow-panel-form-section">
        <ErrorCatchForm
          value={data.node_param.try_catch_config}
          onChange={(val) =>
            changeNodeParam({
              try_catch_config: val,
            })
          }
        />
      </div>
      <div className="spark-flow-panel-form-section">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.Api.panel.output',
            dm: '输出',
          })}

          <IconFont type="spark-info-line" />
        </div>
        <Select
          value={data.node_param.output_type}
          options={[
            {
              label: $i18n.get({
                id: 'spark-flow.demos.spark-flow-1.nodes.Api.panel.rawData',
                dm: '原始数据',
              }),
              value: 'primitive',
            },
            {
              label: $i18n.get({
                id: 'spark-flow.demos.spark-flow-1.nodes.Api.panel.jsonData',
                dm: 'JSON数据',
              }),
              value: 'json',
            },
          ]}
          onChange={(val) =>
            changeNodeParam({
              output_type: val,
            })
          }
        />

        {data.node_param.output_type === 'primitive' ? (
          <OutputParamsTree data={data.output_params} />
        ) : (
          <CustomOutputsFormWrap
            value={data.output_params}
            onChange={(val) =>
              handleNodeDataUpdate({
                id,
                data: {
                  output_params: val,
                },
              })
            }
          />
        )}
      </div>
    </>
  );
});
