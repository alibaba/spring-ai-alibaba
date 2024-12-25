/**
 * Node Interaction
 */
import {NodeBase, NodeChange} from '@xyflow/system';
import {graphState} from '@/store/GraphState';
import produce from 'immer';
import {applyNodeChanges} from '@xyflow/react';

/**
 * e.g.
 * position
 * selected state
 * size
 * @param changes
 */
export function handleNodeChanges(changes: NodeChange[]) {
    graphState.nodes = produce(graphState.nodes, (draft: any) => {
            let tmp_arr: NodeBase[] = applyNodeChanges(changes, draft);
            let idx = 0;
            for (let i = 0; i < draft.length; i++) {
                if (!tmp_arr[i]) {
                    continue;
                }
                draft[idx++] = tmp_arr[i];
            }
        },
    );
}
