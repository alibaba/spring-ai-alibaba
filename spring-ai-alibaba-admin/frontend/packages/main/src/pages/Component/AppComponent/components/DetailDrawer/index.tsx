import $i18n from '@/i18n';
import { getAppComponentDetailByCode } from '@/services/appComponent';
import { IAppComponentListItem } from '@/types/appComponent';
import { Drawer } from '@spark-ai/design';
import { useMount, useSetState } from 'ahooks';
import { Flex, Spin } from 'antd';
import classNames from 'classnames';
import dayjs from 'dayjs';
import InputParamsComp, {
  IConfigInput,
  IOutputParamItem,
} from '../InputParamsComp';
import OutputParamsComp from '../OutputParamsComp';
import styles from './index.module.less';

interface IProps {
  data: IAppComponentListItem;
  onClose: () => void;
}

export default function DetailDrawer(props: IProps) {
  const [state, setState] = useSetState({
    loading: !!props.data.code,
    input: {
      system_params: [],
      user_params: [],
    } as IConfigInput,
    output: [] as IOutputParamItem[],
  });

  useMount(async () => {
    try {
      const ret = await getAppComponentDetailByCode(props.data.code!);
      const componentDetailCfg = JSON.parse(ret.config);
      setState({
        input: componentDetailCfg.input,
        output: componentDetailCfg.output,
      });
    } finally {
      setState({
        loading: false,
      });
    }
  });

  return (
    <Drawer
      className={styles['form-drawer']}
      width={960}
      open
      onClose={props.onClose}
      title={$i18n.get({
        id: 'main.pages.Component.AppComponent.components.DetailDrawer.index.detailView',
        dm: '查看详情',
      })}
    >
      {state.loading ? (
        <Spin className="loading-center" />
      ) : (
        <>
          <div
            className={classNames(styles['form-con'], 'flex flex-col gap-5')}
          >
            <div className={styles['form-title']}>
              {$i18n.get({
                id: 'main.pages.Component.AppComponent.components.DetailDrawer.index.basicInformation',
                dm: '基础信息',
              })}
            </div>
            <Flex vertical gap={16}>
              <div className={styles['form-item']}>
                <div className={styles.label}>
                  {$i18n.get({
                    id: 'main.pages.Component.AppComponent.components.DetailDrawer.index.componentName',
                    dm: '组件名称',
                  })}
                </div>
                <div className={styles.value}>{props.data.name}</div>
              </div>
              <div className={styles['form-item']}>
                <div className={styles.label}>
                  {$i18n.get({
                    id: 'main.pages.Component.AppComponent.components.DetailDrawer.index.componentId',
                    dm: '组件ID',
                  })}
                </div>
                <div className={styles.value}>{props.data.code}</div>
              </div>
              <div className={styles['form-item']}>
                <div className={styles.label}>
                  {$i18n.get({
                    id: 'main.pages.Component.AppComponent.components.DetailDrawer.index.componentDescription',
                    dm: '组件描述',
                  })}
                </div>
                <div className={styles.value}>{props.data.description}</div>
              </div>
              <div className={styles['form-item']}>
                <div className={styles.label}>
                  {$i18n.get({
                    id: 'main.pages.Component.AppComponent.components.DetailDrawer.index.updateTime',
                    dm: '更新时间',
                  })}
                </div>
                <div className={styles.value}>
                  {dayjs(props.data.gmt_modified).format('YYYY-MM-DD HH:mm:ss')}
                </div>
              </div>
              <div className={styles['form-item']}>
                <div className={styles.label}>
                  {$i18n.get({
                    id: 'main.pages.Component.AppComponent.components.DetailDrawer.index.createTime',
                    dm: '创建时间',
                  })}
                </div>
                <div className={styles.value}>
                  {dayjs(props.data.gmt_create).format('YYYY-MM-DD HH:mm:ss')}
                </div>
              </div>
            </Flex>
          </div>
          <div
            className={classNames(styles['form-con'], 'flex flex-col gap-5')}
          >
            <div className={styles['form-title']}>
              <span>
                {$i18n.get({
                  id: 'main.pages.Component.AppComponent.components.DetailDrawer.index.inputParameters',
                  dm: '输入参数',
                })}
              </span>
            </div>
            <InputParamsComp
              disabled
              input={state.input}
              onChange={(val) =>
                setState({
                  input: {
                    ...state.input,
                    ...val,
                  },
                })
              }
            />
          </div>
          <div
            className={classNames(styles['form-con'], 'flex flex-col gap-5')}
          >
            <div className={styles['form-title']}>
              <span>
                {$i18n.get({
                  id: 'main.pages.Component.AppComponent.components.DetailDrawer.index.outputParameters',
                  dm: '输出参数',
                })}
              </span>
            </div>
            <OutputParamsComp output={state.output} />
          </div>
        </>
      )}
    </Drawer>
  );
}
