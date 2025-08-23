import $i18n from '@/i18n';
import type { IWorkFlowNode } from '@spark-ai/flow';
import React, { ReactElement } from 'react';
import {
  IApiNodeData,
  IClassifierNodeData,
  IEndNodeData,
  IJudgeNodeData,
  ILLMNodeData,
  IOutputNodeData,
  IParameterExtractorNodeData,
  IScriptNodeData,
  IStartNodeData,
  IVariableAssignNodeData,
  IVariableHandleNodeData,
} from '../types/flow';
import ApiPanel from './Api/panel';
import ClassifyPanel from './Classify/panel';
import EndPanel from './End/panel';
import JudgePanel from './Judge/panel';
import LLMPanel from './LLM/panel';
import OutputPanel from './Output/panel';
import ParameterExtractorPanel from './ParameterExtractor/panel';
import ScriptPanel from './Script/panel';
import StartPanel from './Start/panel';
import VariableAssignPanel from './VariableAssign/panel';
import VariableHandlePanel from './VariableHandle/panel';

export default function getConfigPanel(
  selectedNode: IWorkFlowNode,
): ReactElement {
  switch (selectedNode.type) {
    case 'Start':
      return (
        <StartPanel
          id={selectedNode.id}
          data={selectedNode.data as IStartNodeData}
        />
      );

    case 'End':
      return (
        <EndPanel
          id={selectedNode.id}
          data={selectedNode.data as IEndNodeData}
        />
      );

    case 'LLM':
      return (
        <LLMPanel
          id={selectedNode.id}
          data={selectedNode.data as ILLMNodeData}
        />
      );

    case 'Output':
      return (
        <OutputPanel
          id={selectedNode.id}
          data={selectedNode.data as IOutputNodeData}
        />
      );

    case 'Script':
      return (
        <ScriptPanel
          id={selectedNode.id}
          data={selectedNode.data as IScriptNodeData}
        />
      );

    case 'Judge':
      return (
        <JudgePanel
          id={selectedNode.id}
          data={selectedNode.data as IJudgeNodeData}
        />
      );

    case 'Classifier':
      return (
        <ClassifyPanel
          id={selectedNode.id}
          data={selectedNode.data as IClassifierNodeData}
        />
      );

    case 'VariableHandle':
      return (
        <VariableHandlePanel
          id={selectedNode.id}
          data={selectedNode.data as IVariableHandleNodeData}
        />
      );

    case 'VariableAssign':
      return (
        <VariableAssignPanel
          id={selectedNode.id}
          data={selectedNode.data as IVariableAssignNodeData}
        />
      );

    case 'ParameterExtractor':
      return (
        <ParameterExtractorPanel
          id={selectedNode.id}
          data={selectedNode.data as IParameterExtractorNodeData}
        />
      );

    case 'API':
      return (
        <ApiPanel
          id={selectedNode.id}
          data={selectedNode.data as IApiNodeData}
        />
      );

    default:
      return (
        <div>
          {$i18n.get({
            id: 'spark-flow.demos.spark-flow-1.nodes.configPanelsMap.unsupportedNode',
            dm: '未支持的节点',
          })}
        </div>
      );
  }
}
