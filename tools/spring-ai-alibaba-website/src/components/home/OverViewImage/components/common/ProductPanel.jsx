import React from "react";
import { Panel } from "./Panel";

export const ProductPanel = ({
  title,
  children,
  width = "30%",
  panelSkewDir = 1,
}) => {
  return (
    <Panel title={title} width={width} panelSkewDir={panelSkewDir}>
      {children}
      {/* <div className="flex w-full  justify-around items-center">{children}</div> */}
    </Panel>
  );
};
