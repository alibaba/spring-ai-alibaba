import React from "react";
import { Panel } from "./common/Panel";
import { Product } from "./common/Product";
import { hoverPrometheusData,hoverOpenTelemetryData,hoveriLogtailData } from "../utils";
import { ProductPanel } from "./common/ProductPanel";

export const TelemetryPanel = () => {
  return (
    <ProductPanel title="可观测">
      <Product
        logo="https://img.alicdn.com/imgextra/i3/O1CN01jYzUO31nQywwUzAAv_!!6000000005085-2-tps-80-80.png"
        label="iLogtail"
        hoverContent={hoveriLogtailData}
      />
      <Product
        logo="https://img.alicdn.com/imgextra/i3/O1CN01hTFdm51Jor72V1UQ9_!!6000000001076-2-tps-80-80.png"
        label="OpenTelemetry"
        hoverContent={hoverOpenTelemetryData}
      />
      <Product
        logo="https://img.alicdn.com/imgextra/i2/O1CN01xWWOPW1YLHmKw5I1Z_!!6000000003042-2-tps-80-80.png"
        label="Prometheus"
        hoverContent={hoverPrometheusData}
      />
    </ProductPanel>
  );
};
