import React from 'react';
import GuideText from './guideText';
import PresetQuestions from './presetQuestions';

export type ComponentName = 'PresetQuestions' | 'GuideText';

export interface ExperienceConfig {
  prologue: string;
  questions: {
    id: string;
    content: string;
  }[];
}

interface ComponentsMapProps {
  componentNames: ComponentName[]; // the order of component names determines the order of component display
  config: ExperienceConfig;
  onChange: (config: Partial<ExperienceConfig>) => void;
}

const ComponentsMap: React.FC<ComponentsMapProps> = ({
  componentNames,
  config,
  onChange,
}) => {
  // return the corresponding component according to the component name
  const renderComponent = (name: ComponentName) => {
    switch (name) {
      case 'PresetQuestions':
        return (
          <PresetQuestions
            key="presetQuestions"
            questions={config.questions}
            onQuestionsChange={(questions) => {
              onChange({
                questions,
              });
            }}
          />
        );
      case 'GuideText':
        return (
          <GuideText
            key="prologue"
            prologue={config.prologue}
            onChange={(prologue) => {
              onChange({
                prologue,
              });
            }}
          />
        );
      default:
        return null;
    }
  };

  return <>{componentNames.map(renderComponent)}</>;
};

export default ComponentsMap;
