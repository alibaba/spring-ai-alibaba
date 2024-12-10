import StartNode from '../StartNode';

export enum NODE_TYPE {
  START = 'start',
  LLM = 'llm',
}

const getNodeElement = (key: NODE_TYPE) => {
  switch (key) {
    case NODE_TYPE.START:
      return <StartNode />;
    case NODE_TYPE.LLM:
      return null;
    default:
      return null;
  }
};

// TODO: const getNodeFormProps = (key: NODE_TYPE) => {};
export const generateNodeFromKey = (
  key: NODE_TYPE,
  position?: { x: number; y: number },
) => {
  const { x = 0, y = 0 } = position || {};
  const nodeElement = getNodeElement(key);

  console.log('generate', x, y);

  return {
    id: `key-${Date.now()}`,
    type: key,
    sourcePosition: 'right',
    targetPosition: 'left',
    data: {
      label: nodeElement,
      form: {
        // TODO: name: 1,
      },
    },
    // position: { x: x - 600, y: y - 450 },
    position: { x, y },
  } as any;
  // TODO: The type `any` will be replaced when the type of node is defined in some future version...
};
