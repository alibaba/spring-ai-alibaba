import React from "react";
import { Panel } from "./common/Panel";
import { Product } from "./common/Product";
import { Arrow } from "./common/Arrow";
import { useContext } from "preact/hooks";
import { AppContext } from "../context";
import { hoverHigressData,hoverRocketMQData,hoverDubboData,hoverSCAData,hoverSeataData } from "../utils";

export const DataPanel = () => {
  const appContext = useContext(AppContext);
  return (
    <Panel title="数据面" width="90%">
      <div className="flex items-center justify-evenly w-full pt-1 pr-4">
        <Product
          image={
            "https://gw.alicdn.com/imgextra/i2/O1CN014ZK8OP1msdEMGutsg_!!6000000005010-2-tps-166-160.png"
          }
          label="IoT/PC/Mobile"
          hoverable={false}
        />
        <Arrow d="M0 50 H80" viewBox="0 0 100 100" />
        <Product
          // image={
          //   "https://gw.alicdn.com/imgextra/i2/O1CN014fUi061REEMVnXgBv_!!6000000002079-2-tps-216-166.png"
          // }
          label="网关/Higress"
          hoverContent={hoverHigressData}
          logo="https://img.alicdn.com/imgextra/i4/O1CN01BodpHP1YS9ihnVuRB_!!6000000003057-2-tps-80-80.png"
        />
        <Arrow d="M0 50 H80" viewBox="0 0 100 100" />
        <div className="flex flex-1 flex-col justify-center items-center">
          <div className="flex w-full px-4">
            <Arrow
              d="M10 100  V60 A10 10 0 0 1 20 50 H80"
              viewBox="0 0 100 100"
              isTurn
              width="30%"
            />
            <div className="flex-1">
              <Product
                image={
                  "https://gw.alicdn.com/imgextra/i3/O1CN01ZVqiyi1sSQGBsdTtE_!!6000000005765-2-tps-216-174.png"
                }
                logo="https://img.alicdn.com/imgextra/i4/O1CN01qZ4Kh71Vfwndw8Qoa_!!6000000002681-2-tps-80-80.png"
                label={
                  <span>
                    <span
                      style={`color:${appContext.colors.highlightFontColor}`}
                    >
                      异步调用 /
                    </span>
                    <span>RocketMQ</span>
                  </span>
                }
                hoverContent={hoverRocketMQData}
                direction="bottom"
              />
            </div>
            <Arrow
              d="M10 50 H80 A10 10 0 0 1 90 60 V80"
              viewBox="0 0 100 100"
              isTurn
              width="30%"
            />
          </div>
          <div className="flex w-full items-center">
            <Panel
              title="微服务集群A"
              width="auto"
              panelStyle={{ flex: 1 }}
              height="auto"
              titleStyle={{
                fontSize: "0.75rem",
                color: appContext.colors.normalFontColor,
                textShadow: "none",
              }}
            >
              <Product
                image={
                  "https://gw.alicdn.com/imgextra/i2/O1CN01Qe8woR1OvKmr7JClT_!!6000000001767-2-tps-216-166.png"
                }
                logo="https://img.alicdn.com/imgextra/i2/O1CN01TIWcnX1inMDF8jH9J_!!6000000004457-2-tps-80-80.png"
                label="Dubbo"
                hoverContent={hoverDubboData}
              />
              <Product
                image={
                  "https://gw.alicdn.com/imgextra/i1/O1CN01QS9gNx27T54BcAw5L_!!6000000007797-2-tps-216-166.png"
                }
                logo="https://img.alicdn.com/imgextra/i2/O1CN01kjTZ8b1d4remhTuM6_!!6000000003683-2-tps-80-80.png"
                label="SCA"
                hoverContent={hoverSCAData}
              />
            </Panel>
            <Arrow
              width="30%"
              d="M0 50 H180"
              viewBox="0 0 200 100"
              label="同步调用"
            />
            <Panel
              title="微服务集群B"
              width="auto"
              panelStyle={{ flex: 1 }}
              height="auto"
              titleStyle={{
                fontSize: "0.75rem",
                color: appContext.colors.normalFontColor,
                textShadow: "none",
                textAlign: "right",
              }}
            >
              <Product
                image={
                  "https://gw.alicdn.com/imgextra/i2/O1CN01Qe8woR1OvKmr7JClT_!!6000000001767-2-tps-216-166.png"
                }
                logo="https://img.alicdn.com/imgextra/i2/O1CN01TIWcnX1inMDF8jH9J_!!6000000004457-2-tps-80-80.png"
                label="Dubbo"
                hoverContent={hoverDubboData}
              />
              <Product
                image={
                  "https://gw.alicdn.com/imgextra/i1/O1CN01QS9gNx27T54BcAw5L_!!6000000007797-2-tps-216-166.png"
                }
                logo="https://img.alicdn.com/imgextra/i2/O1CN01kjTZ8b1d4remhTuM6_!!6000000003683-2-tps-80-80.png"
                label="SCA"
                hoverContent={hoverSCAData}
              />
            </Panel>
          </div>

          <div className="flex w-full px-4">
            <Arrow
              d="M10 0 V40 A10 10 0 0 0 20 50 H80"
              viewBox="0 0 100 100"
              isTurn
              width="30%"
            />
            <div className="flex-1">
              <Product
                image={
                  "https://gw.alicdn.com/imgextra/i2/O1CN014fUi061REEMVnXgBv_!!6000000002079-2-tps-216-166.png"
                }
                logo="https://img.alicdn.com/imgextra/i3/O1CN01W3W4PH1qXngVzf7uP_!!6000000005506-2-tps-80-80.png"
                label={
                  <span>
                    <span
                      style={`color:${appContext.colors.highlightFontColor}`}
                    >
                      分布式事务 /
                    </span>
                    <span>Seata</span>
                  </span>
                }
                hoverContent={hoverSeataData}
              />
            </div>

            <Arrow
              d="M100 0 V40 A10 10 0 0 1 90 50 H10"
              viewBox="0 0 100 100"
              isTurn
              width="30%"
            />
          </div>
        </div>

        <Arrow d="M0 50 H80" viewBox="0 0 100 100" />
        <Product
          image={
            "https://gw.alicdn.com/imgextra/i1/O1CN01VfCTpe1gHdXoaboh6_!!6000000004117-2-tps-200-211.png"
          }
          label="数据存储"
          hoverable={false}
        />
      </div>
    </Panel>
  );
};
