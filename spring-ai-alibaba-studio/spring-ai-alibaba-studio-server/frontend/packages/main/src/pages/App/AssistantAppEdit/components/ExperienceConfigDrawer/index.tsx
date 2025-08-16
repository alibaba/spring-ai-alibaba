import $i18n from '@/i18n';
import { ModalityTypeTexts } from '@/types/appManage';
import { Button, Drawer } from '@spark-ai/design';
import { useSetState } from 'ahooks';
import { Flex } from 'antd';
import { forwardRef, useContext, useImperativeHandle } from 'react';
import { AssistantAppContext } from '../../AssistantAppContext';
import ComponentsMap, { ExperienceConfig } from './components/componentsMap';

interface IProps {
  onClose: () => void;
}

const ExperienceConfigDrawer = forwardRef<any, IProps>((props, ref) => {
  const { appState, onAppChange } = useContext(AssistantAppContext);
  const { prologue } = appState?.appBasicConfig?.config || {};

  // initialize the preset questions from appState.appBasicConfig.recommends
  const [state, setState] = useSetState<ExperienceConfig>({
    prologue: prologue?.prologue_text || '',
    questions: (prologue?.suggested_questions || []).map((q, index) => ({
      id: String(index + 1),
      content: q,
    })),
  });

  // provide the onClear method to the parent component
  useImperativeHandle(ref, () => ({
    onClear: () => {
      setState({
        questions: [],
      });
    },
  }));

  // handle the configuration change
  const handleConfigChange = (partialConfig: Partial<ExperienceConfig>) => {
    setState({ ...state, ...partialConfig });
  };

  function handleSave() {
    // filter out the questions with empty content
    const validQuestions = state.questions
      .filter((q) => q.content.trim())
      .map((q) => q.content);

    // update the app config
    onAppChange({
      config: {
        prologue: {
          prologue_text: state.prologue,
          suggested_questions: validQuestions,
        },
      },
    });

    props.onClose();
  }

  return (
    <Drawer
      title={$i18n.get(
        {
          id: 'main.components.ExperienceConfigDrawer.index.experienceConfiguration',
          dm: '体验配置：{var1}',
        },
        { var1: ModalityTypeTexts[appState?.modalType] },
      )}
      width={480}
      onClose={props.onClose}
      open
      footer={
        <Flex className="w-full" justify="flex-end" align="center" gap={12}>
          <Button
            type="default"
            onClick={() => {
              props.onClose();
            }}
          >
            {$i18n.get({
              id: 'main.components.ExperienceConfigDrawer.index.cancel',
              dm: '取消',
            })}
          </Button>
          <Button type="primary" onClick={handleSave}>
            {$i18n.get({
              id: 'main.components.ExperienceConfigDrawer.index.confirm',
              dm: '确认',
            })}
          </Button>
        </Flex>
      }
    >
      <div>
        <ComponentsMap
          componentNames={['GuideText', 'PresetQuestions']}
          config={state}
          onChange={handleConfigChange}
        />
      </div>
    </Drawer>
  );
});

export default ExperienceConfigDrawer;
