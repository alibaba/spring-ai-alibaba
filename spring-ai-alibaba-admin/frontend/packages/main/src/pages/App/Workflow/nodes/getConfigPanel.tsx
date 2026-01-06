import $i18n from '@/i18n';
import type { IWorkFlowNode } from '@spark-ai/flow';
import { ReactElement } from 'react';
import {
  IApiNodeData,
  IAppComponentNodeData,
  IClassifierNodeData,
  IEndNodeData,
  IInputNodeData,
  IIteratorNodeData,
  IJudgeNodeData,
  ILLMNodeData,
  IMCPNodeData,
  IOutputNodeData,
  IParallelNodeData,
  IParameterExtractorNodeData,
  IPluginNodeData,
  IRetrievalNodeData,
  IScriptNodeData,
  IStartNodeData,
  IVariableAssignNodeData,
  IVariableHandleNodeData,
} from '../types';
import ApiPanel from './APINode/panel';
import AppComponentPanel from './AppComponent/panel';
import ClassifyPanel from './Classifier/panel';
import EndPanel from './End/panel';
import InputPanel from './InputNode/panel';
import IteratorPanel from './Iterator/panel';
import JudgePanel from './Judge/panel';
import LLMPanel from './LLM/panel';
import MCPPanel from './MCP/panel';
import OutputPanel from './Output/panel';
import ParallelPanel from './Parallel/panel';
import ParameterExtractorPanel from './ParameterExtractor/panel';
import PluginNodePanel from './PluginNode/panel';
import RetrievalPanel from './Retrieval/panel';
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
          parentId={selectedNode.parentId}
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

    case 'Retrieval':
      return (
        <RetrievalPanel
          id={selectedNode.id}
          data={selectedNode.data as IRetrievalNodeData}
        />
      );

    case 'Input':
      return (
        <InputPanel
          id={selectedNode.id}
          data={selectedNode.data as IInputNodeData}
        />
      );

    case 'AppComponent':
      return (
        <AppComponentPanel
          id={selectedNode.id}
          data={selectedNode.data as IAppComponentNodeData}
        />
      );

    case 'MCP':
      return (
        <MCPPanel
          id={selectedNode.id}
          data={selectedNode.data as IMCPNodeData}
        />
      );

    case 'Plugin':
      return (
        <PluginNodePanel
          id={selectedNode.id}
          data={selectedNode.data as IPluginNodeData}
        />
      );

    case 'Iterator':
      return (
        <IteratorPanel
          id={selectedNode.id}
          data={selectedNode.data as IIteratorNodeData}
        />
      );

    case 'Parallel':
      return (
        <ParallelPanel
          id={selectedNode.id}
          data={selectedNode.data as IParallelNodeData}
        />
      );

    default:
      return (
        <div>
          {$i18n.get({
            id: 'main.pages.App.Workflow.nodes.getConfigPanel.unsupportedNode',
            dm: '未支持的节点',
          })}
        </div>
      );
  }
}
