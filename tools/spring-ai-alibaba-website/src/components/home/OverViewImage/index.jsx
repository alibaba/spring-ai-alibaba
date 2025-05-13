import React from "react";
import "tailwindcss/tailwind.css";
import "./style.css";
import { useState } from "preact/hooks";
import { GovernancePanel } from "./components/GovernancePanel";
import { ControlPanel } from "./components/ControlPanel";
import { OpsPanel } from "./components/OpsPanel";
import { TelemetryPanel } from "./components/TelemetryPanel";
import { DataPanel } from "./components/DataPanel";
import { AppContext } from "./context";

const defaultColorConfig = {
  panelColor: "linear-gradient(0deg, #FBFDFF 0%, #F4F8FF 100%)",
  arrowColor: "#2491FF",
  background:
    "url('https://gw.alicdn.com/imgextra/i3/O1CN01kMsmPQ1rqrmbBpNo1_!!6000000005683-0-tps-3600-1440.jpg')",
  normalFontColor: "#5C6170",
  highlightFontColor: "#567BE1",
  panelTitleColor: "#1F36AD",
  tagBgColor: '#e6f4ff',
  tagBorderColor: '#91caff',
  tagFontColor: '#1677ff',
  starForkFontColor: '#8498ee',
  starForkBgColor: '#ced3f1',
  linkColor: '#a3e635'
};

function App({ colors = defaultColorConfig, linkStyle = "text-sm", className = '', ...props }) {
  const [isModalOpen, setModalOpen] = useState(false);
  const handleImageClick = () => {
    setModalOpen(true);
  };

  const handleModalClick = () => {
    setModalOpen(false);
  };
  return (
    <AppContext.Provider
      value={{
        linkStyle,
        colors: {
          ...defaultColorConfig,
          ...colors,
        },
      }}
    >
      <div
        class={`overflow-auto flex flex-col justify-around px-2 pt-4 pb-2 ` + className}
        id="overview-image-root"
        style={{
          background: colors.background,
        }}
        {...props}
      >
        <div class='hidden md:inline-block'>
        <div class="flex justify-around flex-1 items-center">
          <ControlPanel />
          <GovernancePanel />
        </div>
        <div class="flex justify-around flex-[2]">
          <DataPanel />
        </div>
        <div class="flex justify-around flex-1">
          <OpsPanel />
          <TelemetryPanel />
          </div> 
          </div>
          <img class='md:hidden' alt='overview image' onClick={handleImageClick} src='https://img.alicdn.com/imgextra/i2/O1CN01OhhnzT1CtFbbiKTWq_!!6000000000138-0-tps-2792-1502.jpg' />
          {isModalOpen && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            zIndex: 1000,
          }}
          onClick={handleModalClick}
        >
          <div
            style={{
              maxWidth: '80%',
              maxHeight: '90%',
              overflow: 'auto',
            }}
            onClick={(e) => e.stopPropagation()} // 防止点击图片时也关闭遮罩层
          >
            <img
              src="https://img.alicdn.com/imgextra/i2/O1CN01OhhnzT1CtFbbiKTWq_!!6000000000138-0-tps-2792-1502.jpg"
              alt="overview image"
              style={{ maxWidth: '100%' }}
            />
          </div>
        </div>
      )}
      </div>
    </AppContext.Provider>
  );
}

// render(<App height="900px" />, document.getElementById("app"));
export const OverviewImage = App;
