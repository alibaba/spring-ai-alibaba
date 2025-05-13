import React from "react";
import { Panel } from "./common/Panel";
import { Product } from "./common/Product";
import { hoverSentinelData,hoverChaosBladeData,hoverAppActiveData } from "../utils";
import { ProductPanel } from "./common/ProductPanel";

export const GovernancePanel = ({}) => {
  return (
    <ProductPanel title="治理面">
      <Product
        // image={
        //   "https://gw.alicdn.com/imgextra/i4/O1CN012zo8OT21NEJMg5vvT_!!6000000006972-2-tps-216-174.png"
        // }
        logo="https://img.alicdn.com/imgextra/i2/O1CN01bbN4uH1OXyb3Upgcg_!!6000000001716-2-tps-80-80.png"
        label="Sentinel"
        hoverContent={hoverSentinelData}
        direction="bottom"
      />
      <Product
        image={
          "https://gw.alicdn.com/imgextra/i3/O1CN018gvzgw1GnzVoYQSib_!!6000000000668-2-tps-216-148.png"
        }
        direction="bottom"
        logo="https://img.alicdn.com/imgextra/i3/O1CN01MbnN5j20pL0WhvbDl_!!6000000006898-2-tps-80-80.png"
        label="ChaosBlade"
        hoverContent={hoverChaosBladeData}
      />
      <Product
        image={
          "https://gw.alicdn.com/imgextra/i1/O1CN01yZolgm1U0RHWgJGC6_!!6000000002455-2-tps-216-178.png"
        }
        hoverContent={hoverAppActiveData}
        direction="bottom"
        logo="https://img.alicdn.com/imgextra/i3/O1CN019Nt2qs1eyZRjJMDwi_!!6000000003940-2-tps-80-80.png"
        label="AppActive"
      />
      
    </ProductPanel>
  );
};
