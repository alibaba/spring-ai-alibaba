import { useContext } from "preact/hooks";
import React from "react";
import { twMerge } from "tailwind-merge";
import { AppContext } from "../../context";

export const Arrow = ({
  d = "M0 50 H300",
  width = "100px",
  height = "100px",
  viewBox = "0 0 500 200",
  label = "",
  //是否拐弯
  isTurn = false,
}) => {
  const appContext = useContext(AppContext);
  return (
    <svg
      class="min-w-4"
      viewBox={viewBox}
      style={{
        width,
        height,
        // border: "1px solid",
      }}
      xmlns="http://www.w3.org/2000/svg"
    >
      {/* 背景 */}
      <path
        class="fill-none"
        stroke={
          isTurn ? "url(#backgroundGradient)" : appContext.colors.arrowColor
        }
        stroke-width="5"
        style={{ opacity: isTurn ? 1 : 0.1 }}
        // marker-end={"url(#arrowback)"}
        d={d}
      />

      {/* 箭头 */}
      <path
        class=" fill-none"
        style="animation: dash 2s linear infinite;"
        d={d}
        stroke-width={2}
        stroke={isTurn ? "url(#gradient)" : appContext.colors?.arrowColor}
        stroke-dasharray={5}
        marker-end="url(#arrowhead)"
        stroke-linejoin="round"
      ></path>
      {/* 添加文字 */}
      <text
        x="50%"
        y="60"
        fill={appContext.colors.highlightFontColor}
        text-anchor="middle"
        alignment-baseline="hanging"
        class="text-sm"
      >
        {label}
      </text>

      <defs>
        <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="0%">
          {/* <stop offset="0%" style="stop-color:#D8ECFF"></stop> */}
          <stop
            offset="0%"
            stop-color={appContext.colors.arrowColor}
            stop-opacity="0.1"
          />
          <stop
            offset="100%"
            style={`stop-color:${appContext.colors.arrowColor}`}
          ></stop>
        </linearGradient>
        <linearGradient
          id="backgroundGradient"
          x1="0%"
          y1="100%"
          x2="100%"
          y2="100%"
        >
          {/* <stop offset="0%" stop-color="#DAECFF" stop-opacity="0.6" /> */}
          <stop
            offset="0%"
            stop-color={appContext.colors.arrowColor}
            stop-opacity="0.1"
          />
          <stop
            offset="100%"
            stop-color={appContext.colors.arrowColor}
            stop-opacity="0.3"
          />
        </linearGradient>
        <marker
          id="arrowhead"
          markerWidth="10"
          markerHeight="5"
          refX="0"
          refY="2.5"
          orient="auto"
        >
          <polygon
            points="0 0, 5 2.5, 0 5"
            fill={appContext.colors.arrowColor}
          ></polygon>
        </marker>
      </defs>
    </svg>
  );
};
