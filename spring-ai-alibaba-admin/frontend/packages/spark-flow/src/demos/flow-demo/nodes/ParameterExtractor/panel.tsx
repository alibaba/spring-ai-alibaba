import $i18n from '@/i18n';
import { Button, IconFont, Select } from '@spark-ai/design';
import {
  OutputParamsTree,
  VarInputTextArea,
  VariableTreeSelect,
  useNodeDataUpdate,
  useStore,
} from '@spark-ai/flow';
import { useSetState } from 'ahooks';
import React, { memo, useCallback } from 'react';
import ExtractParamEditModal from '../../components/ExtractParamEditModal';
import ExtractParamItem from '../../components/ExtractParamItem';
import {
  IParameterExtractorNodeData,
  IParameterExtractorNodeParam,
} from '../../types/flow';

export default memo(function ParameterExtractorPanel({
  id,
  data,
}: {
  id: string;
  data: IParameterExtractorNodeData;
}) {
  const variableTree = useStore((store) => store.variableTree);
  const { handleNodeDataUpdate } = useNodeDataUpdate();
  const [state, setState] = useSetState({
    showExtractParamEditModal: false,
    editExtractParam: undefined as
      | IParameterExtractorNodeParam['extract_params'][number]
      | undefined,
  });

  const changeNodeParam = useCallback(
    (payload: Partial<IParameterExtractorNodeParam>) => {
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
    [id, data.node_param],
  );

  const changeExtractParam = useCallback(
    (payload: IParameterExtractorNodeParam['extract_params']) => {
      changeNodeParam({
        extract_params: payload,
      });
    },
    [changeNodeParam, data.node_param],
  );

  const handleAdd = useCallback(() => {
    setState({
      showExtractParamEditModal: true,
    });
  }, []);

  const handleEditExtractParams = useCallback(
    (item: IParameterExtractorNodeParam['extract_params'][number]) => {
      setState({
        showExtractParamEditModal: true,
        editExtractParam: item,
      });
    },
    [],
  );

  const handleDeleteExtractParams = useCallback(
    (key: string) => {
      changeExtractParam(
        data.node_param.extract_params.filter((item) => item.key !== key),
      );
    },
    [id, data],
  );

  const changeExtractParamItem = useCallback(
    (payload: IParameterExtractorNodeParam['extract_params'][number]) => {
      const { editExtractParam } = state;
      if (editExtractParam) {
        changeExtractParam(
          data.node_param.extract_params.map((item) =>
            item.key === editExtractParam.key ? payload : item,
          ),
        );
      } else {
        changeExtractParam([...data.node_param.extract_params, payload]);
      }
      setState({
        showExtractParamEditModal: false,
        editExtractParam: undefined,
      });
    },
    [changeExtractParam, data.node_param, state.editExtractParam],
  );

  return (
    <>
      <div className="spark-flow-panel-form-section">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.ParameterExtractor.panel.input',
            dm: '输入',
          })}

          <IconFont type="spark-info-line" />
        </div>
        <VariableTreeSelect options={variableTree}>
          <Select open={false} />
        </VariableTreeSelect>
      </div>
      <div className="spark-flow-panel-form-section">
        <div className="flex-justify-between">
          <div className="spark-flow-panel-form-title">
            {$i18n.get({
              id: 'spark-flow.demos.spark-flow-1.nodes.ParameterExtractor.panel.extractParameters',
              dm: '提取参数',
            })}

            <IconFont type="spark-info-line" />
          </div>
          <Button
            type="link"
            onClick={handleAdd}
            size="small"
            className="self-start spark-flow-text-btn"
            icon={<IconFont type="spark-plus-line" />}
          >
            {$i18n.get({
              id: 'spark-flow.demos.spark-flow-1.nodes.ParameterExtractor.panel.addVariable',
              dm: '添加变量',
            })}
          </Button>
        </div>
        {data.node_param.extract_params.map((item) => (
          <ExtractParamItem
            onEdit={() => handleEditExtractParams(item)}
            key={item.key}
            data={item}
            onDelete={() => handleDeleteExtractParams(item.key)}
          />
        ))}
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.ParameterExtractor.panel.prompt',
            dm: '提示词',
          })}
        </div>
        <VarInputTextArea variableList={[]} />
      </div>
      <div className="spark-flow-panel-form-section">
        <div className="spark-flow-panel-form-title">
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.ParameterExtractor.panel.output',
            dm: '输出',
          })}
        </div>
        <OutputParamsTree data={data.output_params} />
      </div>
      {state.showExtractParamEditModal && (
        <ExtractParamEditModal
          onCancel={() => setState({ showExtractParamEditModal: false })}
          onOk={changeExtractParamItem}
          extractParams={data.node_param.extract_params}
          initialValues={state.editExtractParam}
        />
      )}
    </>
  );
});
