import $i18n from '@/i18n';
import { Button, IconFont, InputNumber, Radio, Select } from '@spark-ai/design';
import type {
  INodeDataInputParamItem,
  INodeDataOutputParamItem,
} from '@spark-ai/flow';
import {
  CustomInputsControl,
  CustomOutputsFormWrap,
  OutputParamsTree,
  SelectWithDesc,
  useNodeDataUpdate,
  useNodesOutputParams,
  useNodesReadOnly,
  useReactFlowStore,
  VarInputTextArea,
} from '@spark-ai/flow';
import { Flex } from 'antd';
import { memo, useMemo, useState } from 'react';
import AuthConfigFormModal from '../../components/AuthConfigFormModal';
import ErrorCatchForm from '../../components/ErrorCatchForm';
import RetryForm from '../../components/RetryForm';
import { useWorkflowAppStore } from '../../context/WorkflowAppProvider';
import { IApiNodeData, IApiNodeParam, ITryCatchConfig } from '../../types';
import { API_OUTPUT_DEFAULT_PARAMS } from './schema';

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

export const getDefaultValueSchemaFromOutputParams = (
  outputParams: INodeDataOutputParamItem[],
) => {
  const list: ITryCatchConfig['default_values'] = [];
  outputParams.forEach((item) => {
    if (!item.key && !item.type) return;
    list.push({
      key: item.key,
      value: void 0,
      type: item.type,
    });
  });
  return list;
};

export default memo(function ApiPanel({
  id,
  data,
}: {
  id: string;
  data: IApiNodeData;
}) {
  const { handleNodeDataUpdate } = useNodeDataUpdate();
  const { getVariableList } = useNodesOutputParams();
  const globalVariableList = useWorkflowAppStore(
    (state) => state.globalVariableList,
  );
  const nodes = useReactFlowStore((store) => store.nodes);
  const edges = useReactFlowStore((store) => store.edges);
  const [showAuthConfigModal, setShowAuthConfigModal] = useState(false);
  const { nodesReadOnly: disabled } = useNodesReadOnly();

  const flowVariableList = useMemo(() => {
    return getVariableList({
      nodeId: id,
    });
  }, [id, nodes, edges]);

  const variableList = useMemo(() => {
    return [...globalVariableList, ...flowVariableList];
  }, [globalVariableList, flowVariableList]);

  const changeNodeParam = (payload: Partial<IApiNodeParam>) => {
    handleNodeDataUpdate({
      id: id,
      data: {
        node_param: {
          ...data.node_param,
          ...payload,
        },
      },
    });
  };

  const memoAuthorization = useMemo(() => {
    switch (data.node_param.authorization.auth_type) {
      case 'BearerAuth':
        return 'Bearer Token';
      case 'ApiKeyAuth':
        return 'Api Key';
      default:
        return $i18n.get({
          id: 'main.pages.App.Workflow.nodes.APINode.panel.none',
          dm: '无',
        });
    }
  }, [data.node_param.authorization]);

  return (
    <>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="flex-justify-between">
            <div className="spark-flow-panel-form-title">
              {$i18n.get({
                id: 'main.pages.App.Workflow.nodes.APINode.panel.apiAddress',
                dm: 'API地址',
              })}
            </div>
            <Button
              disabled={disabled}
              className="spark-flow-text-btn"
              size="small"
              onClick={() => {
                setShowAuthConfigModal(true);
              }}
              type="text"
              icon={<IconFont type="spark-setting-line" />}
            >
              {$i18n.get({
                id: 'main.pages.App.Workflow.nodes.APINode.panel.authentication',
                dm: '鉴权',
              })}

              {`(${memoAuthorization})`}
            </Button>
          </div>
          <SelectWithDesc
            disabled={disabled}
            value={data.node_param.method}
            onChange={(val) => changeNodeParam({ method: val })}
            options={REQUEST_METHODS_OPTIONS}
          />

          <VarInputTextArea
            disabled={disabled}
            variableList={variableList}
            value={data.node_param.url}
            onChange={(val) => changeNodeParam({ url: val })}
            maxLength={Number.MAX_SAFE_INTEGER}
          />
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.APINode.panel.headerSetting',
              dm: 'Header设置',
            })}
          </div>
          <CustomInputsControl
            disabled={disabled}
            value={data.node_param.headers}
            variableList={variableList}
            onChange={(val) => changeNodeParam({ headers: val })}
          />
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.APINode.panel.paramSetting',
              dm: 'Param设置',
            })}
          </div>
          <CustomInputsControl
            disabled={disabled}
            variableList={variableList}
            value={data.node_param.params}
            onChange={(val) => changeNodeParam({ params: val })}
          />
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.APINode.panel.bodySetting',
              dm: 'Body设置',
            })}
          </div>
          <Radio.Group
            disabled={disabled}
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
              disabled={disabled}
              onChange={(val) =>
                changeNodeParam({
                  body: {
                    ...data.node_param.body,
                    data: val,
                  },
                })
              }
              value={
                data.node_param.body.data as Array<INodeDataInputParamItem>
              }
            />
          ) : (
            <VarInputTextArea
              disabled={disabled}
              variableList={variableList}
              value={data.node_param.body.data as string}
              onChange={(val) =>
                changeNodeParam({
                  body: { ...data.node_param.body, data: val },
                })
              }
              maxLength={Number.MAX_SAFE_INTEGER}
            />
          )}
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.APINode.panel.timeoutSetting',
              dm: '超时设置（秒）',
            })}
          </div>
          <InputNumber
            disabled={disabled}
            className="w-full"
            onChange={(val) =>
              changeNodeParam({ timeout: { read: val as number } })
            }
            placeholder={$i18n.get({
              id: 'main.pages.App.Workflow.nodes.APINode.panel.enterTimeoutTime',
              dm: '请输入超时时间',
            })}
            max={120}
            min={1}
            value={data.node_param.timeout.read}
            step={1}
          ></InputNumber>
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.APINode.panel.output',
              dm: '输出',
            })}
          </div>
          <Select
            disabled={disabled}
            value={data.node_param.output_type}
            options={[
              {
                label: $i18n.get({
                  id: 'main.pages.App.Workflow.nodes.APINode.panel.rawData',
                  dm: '原始数据',
                }),
                value: 'primitive',
              },
              {
                label: $i18n.get({
                  id: 'main.pages.App.Workflow.nodes.APINode.panel.jsonData',
                  dm: 'JSON数据',
                }),
                value: 'json',
              },
            ]}
            onChange={(val) => {
              handleNodeDataUpdate({
                id,
                data: {
                  output_params: API_OUTPUT_DEFAULT_PARAMS,
                  node_param: {
                    ...data.node_param,
                    output_type: val,
                    try_catch_config: {
                      ...data.node_param.try_catch_config,
                      default_values: getDefaultValueSchemaFromOutputParams(
                        API_OUTPUT_DEFAULT_PARAMS,
                      ),
                    },
                  },
                },
              });
            }}
          />
        </Flex>
        {data.node_param.output_type === 'primitive' ? (
          <OutputParamsTree data={data.output_params} />
        ) : (
          <CustomOutputsFormWrap
            readyOnly={disabled}
            value={data.output_params}
            onChange={(val) =>
              handleNodeDataUpdate({
                id,
                data: {
                  output_params: val,
                  node_param: {
                    ...data.node_param,
                    try_catch_config: {
                      ...data.node_param.try_catch_config,
                      default_values:
                        getDefaultValueSchemaFromOutputParams(val),
                    },
                  },
                },
              })
            }
          />
        )}
      </div>
      <div className="spark-flow-panel-form-section">
        <RetryForm
          disabled={disabled}
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
          disabled={disabled}
          nodeId={id}
          value={data.node_param.try_catch_config}
          onChangeType={(type) => {
            const params =
              type === 'failBranch'
                ? {
                    default_values: getDefaultValueSchemaFromOutputParams(
                      data.output_params,
                    ),
                  }
                : {};
            changeNodeParam({
              try_catch_config: {
                ...data.node_param.try_catch_config,
                strategy: type,
                ...params,
              },
            });
          }}
          onChange={(val) =>
            changeNodeParam({
              try_catch_config: val,
            })
          }
        />
      </div>
      {showAuthConfigModal && (
        <AuthConfigFormModal
          variableList={variableList}
          value={data.node_param.authorization}
          onClose={() => setShowAuthConfigModal(false)}
          onOk={(val) => {
            changeNodeParam({ authorization: val });
            setShowAuthConfigModal(false);
          }}
        />
      )}
    </>
  );
});
