import defaultSettings from '@/defaultSettings';
import $i18n from '@/i18n';
import { Input } from '@spark-ai/design';
import commonStyles from '../common.module.less';
import styles from './index.module.less';
interface GuideTextProps {
  prologue: string;
  onChange: (prologue: string) => void;
}

const GuideText = ({ prologue, onChange }: GuideTextProps) => {
  const handleTextChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const value = e.target.value;
    if (value.length <= 2000) {
      onChange(value);
    }
  };

  return (
    <div className={commonStyles.section}>
      <div className={commonStyles.sectionTitle}>
        {$i18n.get({
          id: 'main.components.ExperienceConfigDrawer.components.guideText.index.welcomeMessage',
          dm: '欢迎语',
        })}
      </div>
      <div className={styles.textAreaWrapper}>
        <Input.TextArea
          value={prologue}
          onChange={handleTextChange}
          placeholder={$i18n.get({
            id: 'main.components.ExperienceConfigDrawer.components.guideText.index.enterWelcomeMessage',
            dm: '请输入欢迎语',
          })}
          rows={3}
          maxLength={defaultSettings.agentWelcomeMessageMaxLength}
          showCount
          className={styles.textArea}
        />
      </div>
    </div>
  );
};

export default GuideText;
