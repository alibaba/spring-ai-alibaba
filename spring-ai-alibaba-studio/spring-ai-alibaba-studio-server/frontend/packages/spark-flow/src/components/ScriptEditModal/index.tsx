import $i18n from '@/i18n';
import {
  INodeDataInputParamItem,
  INodeDataOutputParamItem,
} from '@/types/work-flow';
import { Modal } from '@spark-ai/design';
import { useSetState } from 'ahooks';
import { Segmented } from 'antd';
import React from 'react';
import ScriptCodeMirror from '../ScriptCodeMirror';
import './index.less';

export const SCRIPT_TYPE_OPTIONS = [
  { label: 'Python', value: 'python' },
  { label: 'JavaScript', value: 'javascript' },
];

export const CODE_DEMO_MAP = {
  python: `def main():
  ret = {
      "output": params['input1'] + params['input2'] 
  }
  return ret`,
  javascript: `function main() {
  const ret = {
      "output": params.input1 + params.input2
  };
  return ret;
}`,
};

export interface IScriptEditModalProps {
  language: 'python' | 'javascript';
  value: string;
  inputParams: INodeDataInputParamItem[];
  outputParams: INodeDataOutputParamItem[];
  onClose: () => void;
  onOk: (val: {
    language: IScriptEditModalProps['language'];
    value: IScriptEditModalProps['value'];
  }) => void;
  disabled?: boolean;
  codeDemoMap?: Record<string, string>;
  scriptTypeOptions?: { label: string; value: string }[];
}

export default function ScriptEditModal(props: IScriptEditModalProps) {
  const {
    codeDemoMap = CODE_DEMO_MAP,
    scriptTypeOptions = SCRIPT_TYPE_OPTIONS,
  } = props;
  const [state, setState] = useSetState({
    language: props.language,
    value: props.value,
  });

  const handleChangeLanguage = (val: 'python' | 'javascript') => {
    setState({ language: val, value: codeDemoMap[val] });
  };

  return (
    <Modal
      open
      width={960}
      className="spark-flow-script-edit-modal"
      onOk={() => props.onOk({ language: state.language, value: state.value })}
      onCancel={props.onClose}
      okButtonProps={{ disabled: props.disabled }}
      title={
        <>
          <span>
            {$i18n.get({
              id: 'spark-flow.ScriptEditModal.index.scriptConversionCodeEditor',
              dm: '脚本转换：代码编辑',
            })}
          </span>
          <Segmented
            disabled={props.disabled}
            value={state.language}
            options={scriptTypeOptions}
            onChange={(val) =>
              handleChangeLanguage(val as IScriptEditModalProps['language'])
            }
          />
        </>
      }
    >
      <div className="spark-flow-script-edit-modal-editor">
        <ScriptCodeMirror
          disabled={props.disabled}
          inputParams={props.inputParams}
          outputParams={props.outputParams}
          value={state.value}
          onChange={(val) => setState({ value: val })}
          language={state.language}
        />
      </div>
    </Modal>
  );
}
