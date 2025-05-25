import { NODE_TITLE, NODE_TYPE } from '../Common/manageNodes';

const LLMNode = () => {
  return <div>{NODE_TITLE[NODE_TYPE.LLM]}</div>;
};

export default LLMNode;
