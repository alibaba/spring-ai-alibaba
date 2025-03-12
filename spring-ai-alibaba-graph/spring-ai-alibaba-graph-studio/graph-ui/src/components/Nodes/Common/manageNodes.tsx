/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { IEdge, INode } from '@/pages/Graph/Design/types';
import { message } from 'antd';
import { dump as yamlDump, load as yamlLoad } from 'js-yaml';
import LLMNode from '../LLMNode';
import StartNode from '../StartNode';

// TODO: The type `any` will be replaced when the type of node is defined in some future version...

export enum NODE_TYPE {
  START = 'start',
  LLM = 'llm',
}

export const NODE_TITLE = {
  [NODE_TYPE.START]: 'Start',
  [NODE_TYPE.LLM]: 'LLM',
};

const getNodeElement = (key: NODE_TYPE) => {
  switch (key) {
    case NODE_TYPE.START:
      return <StartNode />;
    case NODE_TYPE.LLM:
      return <LLMNode />;
    default:
      return null;
  }
};

const getUniqueId = () => {
  return `key-${Date.now()}`;
};

// TODO: const getNodeFormProps = (key: NODE_TYPE) => {};
export const generateNodeFromKey = (
  key: NODE_TYPE,
  position?: { x: number; y: number },
) => {
  console.log('key', key);
  const { x = 0, y = 0 } = position || {};
  const nodeElement = getNodeElement(key);

  return {
    id: getUniqueId(),
    type: key,
    sourcePosition: 'right',
    targetPosition: 'left',
    data: {
      label: nodeElement,
      form: {
        // TODO: name: 1,
      },
    },
    position: { x, y },
  } as any;
};

export const yaml2Json = (yaml: string) => {
  try {
    return yamlLoad(yaml) as Record<string, any>;
  } catch (error) {
    console.error('Error parsing YAML:', error);
    throw new Error('Invalid YAML format');
  }
};

export const json2Yaml = (json: any) => {
  try {
    return yamlDump(json, {
      indent: 2,
      lineWidth: -1,
      noRefs: true,
      quotingType: '"',
    });
  } catch (error) {
    console.error('Error converting to YAML:', error);
    throw new Error('Failed to convert JSON to YAML');
  }
};
// export const getEdgeDataFromDSL = () => {};

// export const getEdgeDSLFromData = (edge: IEdge) => {
//   return '';
// };

export const getNodeDataFromDSL = (jsonDSL: any) => {
  console.log('jsonDSL', jsonDSL);
  return {
    id: jsonDSL.id || getUniqueId(),
    sourcePosition: jsonDSL.sourcePosition || 'right',
    targetPosition: jsonDSL.targetPosition || 'left',
    type: jsonDSL.type || 'start',
    data: {
      label: jsonDSL.data?.title || 'node',
      form: {
        // TODO: This part will be implemented when the form is implemented
      },
    },
    position: {
      x: jsonDSL.position?.x || 0,
      y: jsonDSL.position?.y || 100,
    },
    measured: {
      width: jsonDSL.measured?.width || 150,
      height: jsonDSL.measured?.height || 36,
    },
  };
};

export const getNodeDSLFromData = (node: INode<any>) => {
  return {
    data: {
      desc: '',
      selected: false,
      title: NODE_TITLE[node.type as NODE_TYPE] ?? 'node',
      type: node.type,
      variables: [],
    },
    height: node.measured?.height || 54,
    id: node.id,
    position: {
      x: node.position.x,
      y: node.position.y,
    },
    positionAbsolute: {
      x: node.position.x,
      y: node.position.y,
    },
    selected: false,
  };
};

type CopyFunction = {
  (data: INode<any>, type: 'node'): void;
  (data: IEdge, type: 'edge'): void;
};
export const copy: CopyFunction = async (data, type) => {
  try {
    let res = '';
    if (type === 'node') {
      res = JSON.stringify(getNodeDSLFromData(data as INode<any>));
    }
    // else {
    //   res = JSON.stringify(getEdgeDSLFromData(data as IEdge));
    // }
    await navigator.clipboard.writeText(res);
  } catch (error) {
    console.error('Error copying data:', error);
    message.error('Error copying data');
  }
};

export const paste = async (position: { x: number; y: number }) => {
  try {
    const jsonData = await navigator.clipboard.readText();
    const dsl = JSON.parse(jsonData);
    const nodeData = getNodeDataFromDSL(dsl);
    nodeData.id = getUniqueId();
    nodeData.position = position;
    return nodeData;
  } catch (error) {
    console.error('Error parsing JSON:', error);
    message.error('Error parsing JSON');
  }
};

export const importDataFromDSL = () => {};
