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
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import React, { ReactNode } from 'react';
import CustomIcon from '../CustomIcon';
import './index.less';

interface SortableItemProps {
  id: string;
  children: (listeners: any) => ReactNode;
}

const SortableItem = ({ id, children }: SortableItemProps) => {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...attributes}
      className="draggable-item"
    >
      {children(listeners)}
    </div>
  );
};

interface DraggableWithHandleProps<T> {
  items: T[];
  onChange: (items: T[]) => void;
  renderItem: (item: T, dragHandleProps: ReactNode) => ReactNode;
  getItemId: (item: T) => string;
  dragIconType?: string;
  className?: string;
  disabled?: boolean;
}

export function DraggableWithHandle<T>({
  items,
  onChange,
  renderItem,
  getItemId,
  dragIconType = 'spark-dragDot-line',
  className,
  disabled,
}: DraggableWithHandleProps<T>) {
  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (over && active.id !== over.id) {
      const oldIndex = items.findIndex((item) => getItemId(item) === active.id);
      const newIndex = items.findIndex((item) => getItemId(item) === over.id);

      onChange(arrayMove(items, oldIndex, newIndex));
    }
  };

  const renderHandle = (listeners: any) => (
    <div className="drag-handle" {...listeners}>
      <CustomIcon type={dragIconType} />
    </div>
  );

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCenter}
      onDragEnd={handleDragEnd}
    >
      <SortableContext
        disabled={disabled}
        items={items.map(getItemId)}
        strategy={verticalListSortingStrategy}
      >
        <div className={className}>
          {items.map((item) => (
            <SortableItem key={getItemId(item)} id={getItemId(item)}>
              {(listeners) => renderItem(item, renderHandle(listeners))}
            </SortableItem>
          ))}
        </div>
      </SortableContext>
    </DndContext>
  );
}

export default DraggableWithHandle;
