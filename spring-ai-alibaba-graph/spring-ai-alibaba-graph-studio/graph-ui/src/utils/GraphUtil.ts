import { graphState } from '@/store/GraphState';
import { produce } from 'immer';

/**
 * todo
 * re-layout the graph map
 * e.g.
 * Force-Directed Layout
 * Hierarchical Layout
 * Circular Layout
 * Orthogonal Layout
 * Orthogonal Layout
 * @param nodes
 */
export function reLayout(): any {
  let widthGap = 200;
  let heightGap = 100;
  let pre: any = null;
  graphState.nodes = produce(graphState.nodes, (draft: any[]) => {
    draft
      .sort((a: any, b: any) => a.position.x - b.position.x)
      .forEach((x: any) => {
        if (pre === null) {
          pre = x;
          pre.position.x = 0;
          pre.position.y = 150;
          return;
        }
        let offsetW = pre.width || 100;
        let offsetH = pre.height || 100;
        let offsetX = x.position.x - pre.position.x;
        let offsetY = x.position.y - pre.position.y;
        if (Math.abs(offsetX) < offsetW / 2) {
          x.position.x = pre.position.x;
          x.position.y = pre.position.y + offsetH + heightGap;
        } else {
          if (Math.abs(offsetY) < offsetH / 2) {
            x.position.y = pre.position.y;
          } else {
            x.position.y =
              pre.position.y +
              (offsetY > 0 ? offsetH + heightGap : -offsetH - heightGap);
          }
          x.position.x = pre.position.x + offsetW + widthGap;
        }
        pre = x;
      });
  });
}
