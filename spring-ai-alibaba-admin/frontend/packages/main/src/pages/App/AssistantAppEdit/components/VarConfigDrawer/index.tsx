import $i18n from '@/i18n';
import { getAppComponentServersSchemaByCodes } from '@/services/appComponent';
import {
  IAppComponentInputParamItem,
  IAppComponentListItem,
  IAppComponentOutputParamItem,
} from '@/types/appComponent';
import { UserPromptParams } from '@/types/appManage';
import { PluginTool } from '@/types/plugin';
import { Button, Drawer, parseJsonSafely } from '@spark-ai/design';
import classNames from 'classnames';
import {
  CSSProperties,
  forwardRef,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
} from 'react';
import AgentVarConfigForm from './form';
import styles from './index.module.less';

export type IParam = Array<{
  field: string;
  type: string;
  description?: string;
}>;

export type BizVars = {
  user_defined_params: Record<string, Record<string, any>>;
  prompt_variables: Record<string, string>;
};

function isFalsy(value: any) {
  return value === undefined || value === null || value === '';
}

function hasNoEmptyValues(obj: Record<string, any>): boolean {
  return Object.entries(obj).every(([, value]) => {
    if (isFalsy(value)) {
      return false;
    }
    if (typeof value === 'object' && value !== null) {
      return hasNoEmptyValues(value);
    }
    return true;
  });
}

interface IVarConfigDrawerProps {
  onCancel: () => void;
  onBizVarsUpdate: (params: BizVars) => void;
  open: boolean;
  pluginToolsList?: PluginTool[]; // the draft plugin tool list
  agentComponentList?: IAppComponentListItem[];
  workflowComponentList?: IAppComponentListItem[];
  userPromptParams?: Array<UserPromptParams>; // the draft custom variable list
  bizVars: Record<string, Record<string, any>>; // the input parameters state of the outer component
  code: string; // the application code
  style?: CSSProperties;
  className?: string;
  onIsBizVarsCompleteChange?: (isComplete: boolean) => void; // whether the input parameters are filled
}

interface IUseVarConfigProps
  extends Omit<IVarConfigDrawerProps, 'open' | 'onCancel'> {
  needLocalStorage?: boolean; // whether to cache the parameters in the localStorage or not
}

export type BizVarItem = {
  code: string;
  params: IParam;
  name: string;
};
export const useVarConfig = (props: IUseVarConfigProps) => {
  const {
    bizVars,
    pluginToolsList,
    userPromptParams,
    workflowComponentList,
    agentComponentList,
  } = props;
  // the plugin and workflow variables
  const [bizVarList, setBizVarList] = useState<BizVarItem[]>([]);
  // the user custom variables
  const [userPromptParamsList, setUserPromptParamsList] = useState<
    (UserPromptParams & { key: string })[]
  >([]);

  const [loading, setLoading] = useState(true);
  const cachedBizVars = useRef<BizVars | null>(null);
  const updateLocalBizVars = (params: BizVars) => {
    if (cachedBizVars.current === null) {
      // when initializing, update the value of the outer component directly, and do not update it actively afterwards, but update it after clicking confirm button
      props.onBizVarsUpdate(params);
    }
    // update the local cache
    cachedBizVars.current = params;
    if (props.needLocalStorage) {
      // update the value of the localStorage
      localStorage.setItem(`${props.code}-bizVars`, JSON.stringify(params));
    }
    // after clicking confirm, update the value of the outer component, but not here, in order to avoid a dead loop
  };

  // fetch the latest input parameters list from the server, and determine which parameters need to be filled
  const fetchVarList = async (
    {
      pluginToolsList = [],
      userPromptParams = [],
      agentComponentList = [],
      workflowComponentList = [],
    }: {
      pluginToolsList: PluginTool[];
      userPromptParams: UserPromptParams[];
      agentComponentList: IAppComponentListItem[];
      workflowComponentList: IAppComponentListItem[];
    } = {} as any,
  ) => {
    /**
     * refresh the form values based on the return result and the cached bizVars in the localStorage
     */
    const bizVarsCache = parseJsonSafely(
      localStorage.getItem(`${props.code}-bizVars`) || '',
    ) as BizVars | null;
    let isBizVarsComplete = true;

    const user_defined_params: Record<string, Record<string, any>> = {}; // the plugin input parameters

    /**
     * the component input parameters
     */
    let componentSchema: {
      [key: string]: {
        input: IAppComponentInputParamItem[];
        output: IAppComponentOutputParamItem[];
      };
    } = {};
    if (agentComponentList.length || workflowComponentList.length) {
      componentSchema = await getAppComponentServersSchemaByCodes([
        ...agentComponentList.map((item) => item.code || '').filter(Boolean),
        ...workflowComponentList.map((item) => item.code || '').filter(Boolean),
      ]);
    }

    Object.keys(componentSchema).forEach((key) => {
      componentSchema[key].input.forEach((paramItem) => {
        if (!user_defined_params[key]) {
          user_defined_params[key] = {};
        }
        // the parameters that need to be filled manually by the user
        if (paramItem.display && paramItem.source !== 'model') {
          const value =
            bizVarsCache?.user_defined_params[key]?.[paramItem.field] ||
            undefined;
          if (isFalsy(value)) {
            isBizVarsComplete = false;
          }
          user_defined_params[key] = {
            ...user_defined_params[key],
            [paramItem.field]: value,
          };
        }
      });
    });

    /**
     * the plugin input parameters
     */
    pluginToolsList.forEach((item) => {
      if (!item.tool_id) {
        return;
      }
      item.config?.input_params?.forEach((paramItem) => {
        if (!user_defined_params[item.tool_id!]) {
          user_defined_params[item.tool_id!] = {};
        }
        if (paramItem.user_input) {
          // the parameters that need to be filled manually by the user
          const value =
            bizVarsCache?.user_defined_params[item.tool_id!]?.[paramItem.key] ||
            undefined;
          if (isFalsy(value)) {
            isBizVarsComplete = false;
          }
          user_defined_params[item.tool_id!] = {
            ...user_defined_params[item.tool_id!],
            [paramItem.key]: value,
          };
        }
      });
    });

    /**
     * system prompt variables
     */
    const prompt_variables = userPromptParams?.reduce((prev, curr) => {
      const value =
        bizVarsCache?.prompt_variables?.[curr.name] || curr.default_value;
      if (isFalsy(value)) {
        isBizVarsComplete = false;
      }
      return {
        ...prev,
        [curr.name]: value,
      };
    }, {});
    /**
     * update the bizVarsComplete status
     */
    props.onIsBizVarsCompleteChange?.(isBizVarsComplete);
    /**
     * update the local component
     */
    // @ts-ignore
    setBizVarList([
      ...(pluginToolsList
        .filter(
          (item) =>
            item.tool_id &&
            item.config?.input_params?.some((param) => param.user_input),
        )
        .map((tool) => ({
          code: tool.tool_id!,
          name: tool.name,
          params:
            tool.config?.input_params?.map((item) => ({
              field: item.key,
              type: item.type,
              description: item.description,
            })) || [],
        })) || []),
      ...agentComponentList
        .filter((item) => !!item.code)
        .map((item) => ({
          code: item.code,
          name: item.name,
          params: componentSchema[item.code!]?.input
            .filter((item) => item.display && item.source !== 'model')
            .map((item) => ({
              field: item.field,
              type: item.type,
              description: item.description,
            })),
        }))
        .filter((item) => item.params.length > 0),
      ...workflowComponentList
        .filter((item) => !!item.code)
        .map((item) => ({
          code: item.code,
          name: item.name,
          params: componentSchema[item.code!]?.input
            .filter((item) => item.display && item.source !== 'model')
            .map((item) => ({
              field: item.field,
              type: item.type,
              description: item.description,
            })),
        }))
        .filter((item) => item.params.length > 0),
    ]);
    setUserPromptParamsList(
      userPromptParams.map((item) => ({
        ...item,
        key: item.name,
      })),
    );
    /**
     * refresh the loading state
     */
    setLoading(false);
    // return value for updating the outer component and the localStorage status
    return {
      user_defined_params,
      prompt_variables,
    };
  };

  useEffect(() => {
    // when the outer flowCodeList, PluginToolsList, userPromptParams changes, or when the intelligent agent version is switched, refresh the list of parameters to be filled
    const refreshData = async () => {
      const newBizVars = await fetchVarList({
        pluginToolsList: pluginToolsList || [],
        userPromptParams: userPromptParams || [],
        agentComponentList: agentComponentList || [],
        workflowComponentList: workflowComponentList || [],
      });
      updateLocalBizVars(newBizVars);
    };
    refreshData();
  }, [
    pluginToolsList,
    userPromptParams,
    agentComponentList,
    workflowComponentList,
  ]);

  const onBizVarsFormValuesChange = (
    changedValues: Partial<{
      user_defined_params: Record<string, any>;
      prompt_variables: Record<string, string>;
    }>,
    allValues: {
      user_defined_params: Record<string, any>;
      prompt_variables: Record<string, string>;
    },
  ) => {
    const new_user_defined_params = allValues.user_defined_params;
    const new_prompt_variables = allValues.prompt_variables;
    cachedBizVars.current = {
      user_defined_params: new_user_defined_params,
      prompt_variables: new_prompt_variables,
    };
  };
  return {
    loading,
    bizVarList,
    userPromptParamsList,
    bizVars,
    onBizVarsFormValuesChange,
    cachedBizVars,
  };
};

export interface VarConfigComponentRef {
  cachedBizVars: BizVars | null;
}

export interface VarConfigComponentProps {
  loading: boolean;
  bizVarList: BizVarItem[];
  userPromptParamsList: UserPromptParams[];
  bizVars: Record<string, Record<string, any>>;
  onBizVarsFormValuesChange: (changedValues: any, allValues: any) => void;
  cachedBizVars: React.MutableRefObject<BizVars | null>;
}

export const VarConfigComponent = forwardRef<
  VarConfigComponentRef,
  VarConfigComponentProps
>((props, ref) => {
  const {
    loading,
    bizVarList,
    userPromptParamsList,
    bizVars,
    onBizVarsFormValuesChange,
    cachedBizVars,
  } = props;

  // expose the cachedBizVars to the parent component through useImperativeHandle
  useImperativeHandle(
    ref,
    () => ({
      get cachedBizVars() {
        return cachedBizVars.current;
      },
    }),
    [cachedBizVars.current],
  );

  return (
    <AgentVarConfigForm
      loading={loading}
      bizVarList={bizVarList}
      userPromptParamsList={userPromptParamsList}
      bizVars={bizVars}
      onBizVarsFormValuesChange={onBizVarsFormValuesChange}
    />
  );
});

export default function VarConfigDrawer(props: IVarConfigDrawerProps) {
  // create a ref to access the cachedBizVars of the VarConfigComponent component
  const varConfigRef = useRef<VarConfigComponentRef>(null);
  const varConfigComponentProps = useVarConfig({
    ...props,
    needLocalStorage: true,
  });

  return (
    <Drawer
      title={$i18n.get({
        id: 'main.pages.App.AssistantAppEdit.components.VarConfigDrawer.index.paramConfig',
        dm: '入参变量配置',
      })}
      width={480}
      open={props.open}
      onClose={props.onCancel}
      style={props.style}
      className={classNames(props.className)}
      destroyOnClose
      footer={
        <div
          style={{
            width: '100%',
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
          }}
        >
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              gap: 12,
            }}
          >
            <Button
              type="default"
              onClick={() => {
                props.onCancel();
              }}
            >
              {$i18n.get({
                id: 'main.pages.App.AssistantAppEdit.components.VarConfigDrawer.index.cancel',
                dm: '取消',
              })}
            </Button>
            <Button
              type="primary"
              onClick={() => {
                if (varConfigRef.current?.cachedBizVars) {
                  props.onBizVarsUpdate(varConfigRef.current.cachedBizVars);
                  const isComplete = hasNoEmptyValues(
                    varConfigRef.current.cachedBizVars,
                  );
                  props.onIsBizVarsCompleteChange?.(isComplete);
                }
                props.onCancel();
              }}
            >
              {$i18n.get({
                id: 'main.pages.App.AssistantAppEdit.components.VarConfigDrawer.index.confirm',
                dm: '确认',
              })}
            </Button>
          </div>
        </div>
      }
    >
      <div className={styles.desc}>
        {$i18n.get({
          id: 'main.pages.App.AssistantAppEdit.components.VarConfigDrawer.index.description',
          dm: '请填写本轮对话需要传入的变量值，这些参数来自您设置的自定义变量或技能中的业务透传参数。每次修改后，会在新一轮对话中生效。',
        })}
      </div>
      <VarConfigComponent ref={varConfigRef} {...varConfigComponentProps} />
    </Drawer>
  );
}
