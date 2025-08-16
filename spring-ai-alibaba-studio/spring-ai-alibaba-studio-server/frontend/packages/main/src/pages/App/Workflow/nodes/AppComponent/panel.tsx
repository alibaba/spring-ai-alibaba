import $i18n from '@/i18n';
import { getAppComponentInputAndOutputParams } from '@/services/appComponent';
import { IconFont } from '@spark-ai/design';
import {
  CustomInputsControl,
  OutputParamsTree,
  useNodeDataUpdate,
  useNodesOutputParams,
  useNodesReadOnly,
  useReactFlowStore,
} from '@spark-ai/flow';
import { useSetState } from 'ahooks';
import { Flex, Spin, Switch, Typography } from 'antd';
import classNames from 'classnames';
import { memo, useCallback, useEffect, useMemo } from 'react';
import InfoIcon from '../../components/InfoIcon';
import ShortMemoryForm from '../../components/ShortMemoryForm';
import { useWorkflowAppStore } from '../../context/WorkflowAppProvider';
import { IAppComponentNodeData, IAppComponentNodeParam } from '../../types';
import styles from './index.module.less';

export default memo(function AppComponentPanel(props: {
  id: string;
  data: IAppComponentNodeData;
}) {
  const { handleNodeDataUpdate } = useNodeDataUpdate();
  const globalVariableList = useWorkflowAppStore(
    (state) => state.globalVariableList,
  );
  const { getVariableList } = useNodesOutputParams();
  const nodes = useReactFlowStore((store) => store.nodes);
  const edges = useReactFlowStore((store) => store.edges);
  const { nodesReadOnly } = useNodesReadOnly();
  const [state, setState] = useSetState({
    loading: true,
  });

  const flowVariableList = useMemo(() => {
    return getVariableList({
      nodeId: props.id,
    });
  }, [props.id, nodes, edges]);

  const variableList = useMemo(() => {
    return [...globalVariableList, ...flowVariableList];
  }, [globalVariableList, flowVariableList]);

  const changeNodeParam = useCallback(
    (payload: Partial<IAppComponentNodeParam>) => {
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

  useEffect(() => {
    if (!props.data.node_param.code) {
      setState({
        loading: false,
      });
      return;
    }
    setState({
      loading: true,
    });

    getAppComponentInputAndOutputParams(props.data.node_param.code)
      .then((res) => {
        handleNodeDataUpdate({
          id: props.id,
          data: {
            input_params: res.input.map((item) => {
              const targetOldValue =
                props.data.input_params.find((i) => i.key === item.alias) || {};
              return {
                key: item.alias,
                type: item.type,
                value_from: 'refer',
                value: void 0,
                ...targetOldValue,
              };
            }),
            output_params: res.output.map((item) => ({
              key: item.field,
              type: item.type,
              desc: item.description,
            })),
          },
        });
        setState({
          loading: false,
        });
      })
      .finally(() => {
        setState({
          loading: false,
        });
      });
  }, [props.data.node_param.code]);

  if (state.loading) {
    return <Spin spinning className="loading-center" />;
  }

  return (
    <>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.AppComponent.panel.component',
              dm: '组件',
            })}
          </div>
          <Flex
            className={styles['app-component-item']}
            gap={4}
            justify="space-between"
            align="center"
          >
            <IconFont size="small" type="spark-agent-line" />
            <Typography.Text
              ellipsis={{ tooltip: props.data.node_param.name }}
              className={classNames(styles['app-component-name'])}
            >
              {props.data.node_param.name}
            </Typography.Text>
          </Flex>
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex vertical gap={12}>
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.AppComponent.panel.inputVariables',
              dm: '输入变量',
            })}
            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.AppComponent.panel.inputVariablesForThisNode',
                dm: '输入本节点需要处理的变量，用于识别需要处理的内容，支持引用前置。',
              })}
            />
          </div>
          <CustomInputsControl
            disabledKey
            disabled={nodesReadOnly}
            onChange={(payload) => {
              handleNodeDataUpdate({
                id: props.id,
                data: {
                  input_params: payload,
                },
              });
            }}
            value={props.data.input_params}
            variableList={variableList}
          />
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <Flex justify="space-between">
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'main.pages.App.Workflow.nodes.AppComponent.panel.streamSwitch',
              dm: '流式开关',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.AppComponent.panel.outputInStream',
                dm: '本节点模型输出的内容会以流式的方式输出。',
              })}
            />
          </div>
          <Switch
            disabled={nodesReadOnly}
            checked={props.data.node_param.stream_switch}
            onChange={(val) =>
              changeNodeParam({
                stream_switch: val,
              })
            }
          />
        </Flex>
      </div>
      <div className="spark-flow-panel-form-section">
        <ShortMemoryForm
          variableList={variableList}
          value={props.data.node_param.short_memory!}
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
              id: 'main.pages.App.Workflow.nodes.AppComponent.panel.output',
              dm: '输出',
            })}

            <InfoIcon
              tip={$i18n.get({
                id: 'main.pages.App.Workflow.nodes.AppComponent.panel.outputVariables',
                dm: '输出本节点处理结果的变量，用于后续节点识别和处理本节点的处理结果。',
              })}
            />
          </div>
          <OutputParamsTree data={props.data.output_params} />
        </Flex>
      </div>
    </>
  );
});
