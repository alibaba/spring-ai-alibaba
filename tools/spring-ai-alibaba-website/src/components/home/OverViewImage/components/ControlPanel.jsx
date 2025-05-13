import React from "react";
import { Panel } from "./common/Panel";
import { Product } from "./common/Product";
import { hoverNacosData,hoverOpenSergoData } from "../utils";
import { ProductPanel } from "./common/ProductPanel";

export const ControlPanel = ({ }) => {
  return (
    <ProductPanel title="控制面" panelSkewDir={-1}>
      <Product
        image={
          "https://gw.alicdn.com/imgextra/i3/O1CN01dI5UtV1iMnEY2sB3r_!!6000000004399-2-tps-216-154.png"
        }
        label="Nacos"
        hoverContent={hoverNacosData}
        logo="https://img.alicdn.com/imgextra/i3/O1CN01GdSxST24NB7Yl5Pdx_!!6000000007378-2-tps-80-80.png"
        direction="bottom"
      />
      <Product
        image={
          "https://gw.alicdn.com/imgextra/i3/O1CN01g7SsmO1GHTOMxOpzr_!!6000000000597-2-tps-216-160.png"
        }
        label="OpenSergo"
        hoverContent={hoverOpenSergoData}
        logo="https://img.alicdn.com/imgextra/i3/O1CN01oLvUis1DjSkwk53Tg_!!6000000000252-2-tps-80-80.png"
        direction="bottom"
      />
    </ProductPanel>
  );
};
