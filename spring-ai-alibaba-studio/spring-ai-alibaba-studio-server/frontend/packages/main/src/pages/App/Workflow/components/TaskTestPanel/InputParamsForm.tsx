import { VariableBaseInput } from '@/components/VariableBaseInput';
import $i18n from '@/i18n';
import { Drawer, Input } from '@spark-ai/design';
import classNames from 'classnames';
import { memo, useMemo } from 'react';
import { IWorkflowDebugInputParamItem } from '../../context';
import { useWorkflowAppStore } from '../../context/WorkflowAppProvider';
import styles from './index.module.less';

function InputParamsForm(props: {
  showPadding?: boolean;
  disableShowQuery?: boolean;
}) {
  const inputParams = useWorkflowAppStore((state) => state.debugInputParams);
  const setDebugInputParams = useWorkflowAppStore(
    (state) => state.setDebugInputParams,
  );

  const changeInputRowItem = (payload: IWorkflowDebugInputParamItem) => {
    const newInputParams = inputParams.map((item) => {
      if (item.key === payload.key && item.source === payload.source)
        return payload;
      return item;
    });
    setDebugInputParams(newInputParams);
  };

  const { userParams, systemParams } = useMemo(() => {
    const userParamList = [] as IWorkflowDebugInputParamItem[];
    const systemParamList = [] as IWorkflowDebugInputParamItem[];
    inputParams.forEach((item) => {
      if (item.source === 'user') {
        userParamList.push(item);
      } else {
        if (!props.disableShowQuery || item.key !== 'query') {
          systemParamList.push(item);
        }
      }
    });
    return { userParams: userParamList, systemParams: systemParamList };
  }, [inputParams, props.disableShowQuery]);

  return (
    <div
      className={classNames('flex flex-1 overflow-y-auto flex-col gap-[24px]', {
        'px-[20px]': props.showPadding,
      })}
    >
      <div className="flex flex-col gap-[12px]">
        <div className={styles['var-group-name']}>
          {$i18n.get({
            id: 'main.pages.App.Workflow.components.TaskTestPanel.InputParamsForm.index.customVariable',
            dm: '自定义变量',
          })}
        </div>
        {userParams.map((item) => (
          <div key={item.key} className="flex flex-col gap-2">
            <div className={'flex flex-col gap-[4px]'}>
              <div className="flex gap-[4px] items-center">
                <span className={styles['var-key']}>{item.key}</span>
                <span className={styles['var-key-type']}>
                  {`[${item.type}]`}
                </span>
              </div>
              <div className={styles['var-key-desc']}>{item.desc}</div>
            </div>
            <VariableBaseInput
              type={item.type}
              value={item.value}
              onChange={(val) => {
                changeInputRowItem({
                  ...item,
                  value: val.value,
                });
              }}
            />
          </div>
        ))}
      </div>
      {!!systemParams.length && (
        <div className="flex flex-col gap-[12px]">
          <div className={styles['var-group-name']}>
            {$i18n.get({
              id: 'main.pages.App.Workflow.components.TaskTestPanel.InputParamsForm.index.builtinVariable',
              dm: '内置变量',
            })}
          </div>
          {systemParams.map((item) => (
            <div key={item.key} className="flex flex-col gap-2">
              <div className={'flex flex-col gap-[4px]'}>
                <div className="flex gap-[4px] items-center">
                  <span className={styles['var-key']}>{item.key}</span>
                  <span className={styles['var-key-type']}>
                    {`[${item.type}]`}
                  </span>
                </div>
                <div className={styles['var-key-desc']}>{item.desc}</div>
              </div>
              <Input
                placeholder={$i18n.get({
                  id: 'main.pages.App.Workflow.components.TaskTestPanel.InputParamsForm.index.enter',
                  dm: '请输入',
                })}
                value={item.value}
                onChange={(e) =>
                  changeInputRowItem({
                    ...item,
                    value: e.target.value,
                  })
                }
              />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export function InputParamsFormDrawer(props: {
  onClose: () => void;
  disableShowQuery?: boolean;
}) {
  return (
    <Drawer
      open
      placement="bottom"
      height="100%"
      onClose={props.onClose}
      getContainer={false}
      title={$i18n.get({
        id: 'main.pages.App.Workflow.components.TaskTestPanel.InputParamsForm.index.inputParamConfiguration',
        dm: '入参变量配置',
      })}
    >
      <InputParamsForm disableShowQuery={props.disableShowQuery} />
    </Drawer>
  );
}

export default memo(InputParamsForm);
