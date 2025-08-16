import defaultSettings from '@/defaultSettings';
import $i18n from '@/i18n';
import {
  closestCenter,
  DndContext,
  DragEndEvent,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import {
  restrictToParentElement,
  restrictToVerticalAxis,
} from '@dnd-kit/modifiers';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Button, IconFont, Input, message } from '@spark-ai/design';
import { useRef } from 'react';
import commonStyles from '../common.module.less';
import styles from './index.module.less';
const MAX_PRESET_QUESTIONS = defaultSettings.agentPresetQuestionMaxLimit;

interface IPresetQuestion {
  id: string;
  content: string;
}

interface SortableItemProps {
  id: string;
  content: string;
  onDelete: (id: string) => void;
  onChange: (id: string, content: string) => void;
}

function SortableItem({ id, content, onDelete, onChange }: SortableItemProps) {
  const { attributes, listeners, setNodeRef, transform, transition } =
    useSortable({ id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onChange(id, e.target.value);
  };

  return (
    <div ref={setNodeRef} style={style} className={styles.questionItem}>
      <div className={styles.dragHandle} {...attributes} {...listeners}>
        <IconFont type="spark-dragDot-line" className={styles.dragIcon} />
      </div>
      <Input
        value={content}
        onChange={handleInputChange}
        placeholder={$i18n.get({
          id: 'main.components.ExperienceConfigDrawer.components.presetQuestions.index.inputPresetQuestion',
          dm: '输入预设问题',
        })}
      />

      <IconFont
        type="spark-delete-line"
        className={styles.deleteIcon}
        onClick={() => onDelete(id)}
      />
    </div>
  );
}

interface PresetQuestionsProps {
  questions: IPresetQuestion[];
  onQuestionsChange: (questions: IPresetQuestion[]) => void;
}

const PresetQuestions = ({
  questions,
  onQuestionsChange,
}: PresetQuestionsProps) => {
  const questionListRef = useRef<HTMLDivElement>(null);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 5,
      },
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );

  function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event;

    if (over && active.id !== over.id) {
      onQuestionsChange(
        arrayMove(
          questions,
          questions.findIndex((item) => item.id === active.id),
          questions.findIndex((item) => item.id === over.id),
        ),
      );
    }
  }

  function handleAddQuestion() {
    if (questions.length >= MAX_PRESET_QUESTIONS) {
      message.warning(
        $i18n.get({
          id: 'main.components.ExperienceConfigDrawer.components.presetQuestions.index.maxFiveQuestions',
          dm: '最多只能添加5个预设问题',
        }),
      );
      return;
    }
    const newQuestion: IPresetQuestion = {
      id: `${Date.now()}`,
      content: '',
    };
    onQuestionsChange([...questions, newQuestion]);
  }

  function handleDeleteQuestion(id: string) {
    onQuestionsChange(questions.filter((question) => question.id !== id));
  }

  function handleQuestionChange(id: string, content: string) {
    onQuestionsChange(
      questions.map((question) =>
        question.id === id ? { ...question, content } : question,
      ),
    );
  }

  return (
    <div className={commonStyles.section}>
      <div className={commonStyles.sectionTitle}>
        {$i18n.get({
          id: 'main.components.ExperienceConfigDrawer.components.presetQuestions.index.presetQuestions',
          dm: '预设问题',
        })}

        <span style={{ color: 'var(--efm_ant-color-text-tertiary)' }}>
          ({questions.length}/{MAX_PRESET_QUESTIONS})
        </span>
      </div>

      {questions.length > 0 ? (
        <div className={styles.questionList} ref={questionListRef}>
          <DndContext
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragEnd={handleDragEnd}
            modifiers={[restrictToVerticalAxis, restrictToParentElement]}
          >
            <SortableContext
              items={questions.map((q) => q.id)}
              strategy={verticalListSortingStrategy}
            >
              {questions.map((question) => (
                <SortableItem
                  key={question.id}
                  id={question.id}
                  content={question.content}
                  onDelete={handleDeleteQuestion}
                  onChange={handleQuestionChange}
                />
              ))}
            </SortableContext>
          </DndContext>
        </div>
      ) : (
        <div className={styles.questionList}>
          <div className={styles.empty}>
            {$i18n.get({
              id: 'main.components.ExperienceConfigDrawer.components.presetQuestions.index.noPresetQuestions',
              dm: '暂无预设问题，通过下方按钮添加',
            })}
          </div>
        </div>
      )}
      <Button
        disabled={questions.length >= MAX_PRESET_QUESTIONS}
        onClick={handleAddQuestion}
        iconType="spark-plus-line"
        tooltipContent={
          questions.length >= MAX_PRESET_QUESTIONS
            ? $i18n.get(
                {
                  id: 'main.components.ExperienceConfigDrawer.components.presetQuestions.index.maxQuestions',
                  dm: '最多只能添加{var1}个预设问题',
                },
                { var1: MAX_PRESET_QUESTIONS },
              )
            : ''
        }
      >
        {$i18n.get({
          id: 'main.components.ExperienceConfigDrawer.components.presetQuestions.index.addQuestion',
          dm: '添加问题',
        })}
      </Button>
    </div>
  );
};

export default PresetQuestions;
