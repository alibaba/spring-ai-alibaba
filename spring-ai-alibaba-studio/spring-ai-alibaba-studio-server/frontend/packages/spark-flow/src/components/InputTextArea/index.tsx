import classNames from 'classnames';
import React, { memo, useRef, useState } from 'react';
import CustomIcon from '../CustomIcon';
import VarInputTextArea, {
  IVariableInputReferProps,
} from '../VarInputTextArea';
import { IVarTreeItem } from '../VariableTreeSelect';
import './index.less';

interface IInputTextAreaProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  maxLength?: number;
  showCount?: boolean;
  title?: string;
  onDelete?: () => void;
  disabled?: boolean;
  variableList: IVarTreeItem[];
}

export default memo(function InputTextArea(props: IInputTextAreaProps) {
  const [fold, setFold] = useState(true);
  const editorRef = useRef<IVariableInputReferProps>(null);
  return (
    <div
      className={classNames('spark-flow-input-text-area', {
        ['spark-flow-input-text-area-fold']: fold,
        ['spark-flow-input-text-area-fold-disabled']: props.disabled,
      })}
    >
      <div className="spark-flow-input-text-area-header">
        <div className="spark-flow-input-text-area-header-title">
          {props.title}
        </div>
        <div className="flex gap-[8px] items-center">
          <CustomIcon
            size="small"
            isCursorPointer={!props.disabled}
            onClick={props.disabled ? undefined : props.onDelete}
            type="spark-delete-line"
            className={props.disabled ? 'spark-flow-disabled-icon-btn' : ''}
          />
          <CustomIcon
            size="small"
            isCursorPointer={!props.disabled}
            onClick={
              props.disabled
                ? undefined
                : () => {
                    editorRef.current?.setEditorValue('');
                  }
            }
            type="spark-clear-line"
            className={props.disabled ? 'spark-flow-disabled-icon-btn' : ''}
          />
          <CustomIcon
            size="small"
            isCursorPointer={!props.disabled}
            onClick={props.disabled ? undefined : () => setFold(!fold)}
            type={fold ? 'spark-fold-line' : 'spark-fold-line-2'}
            className={props.disabled ? 'spark-flow-disabled-icon-btn' : ''}
          />
        </div>
      </div>
      <VarInputTextArea
        ref={editorRef}
        variableList={props.variableList}
        disabled={props.disabled}
        value={props.value}
        onChange={(val) => props.onChange(val || '')}
      />
    </div>
  );
});
