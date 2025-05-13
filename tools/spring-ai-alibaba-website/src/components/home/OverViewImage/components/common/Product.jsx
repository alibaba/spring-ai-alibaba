import React from "react";
import { useContext, useEffect, useRef, useState } from "preact/hooks";
import { PopupContent } from "./PopupContent";
import { AppContext } from "../../context";
import sendFetch from "./sendFetch";

export const Product = ({
  image,
  label,
  hoverable = true,
  hoverContent,
  // 气泡框展示位置 上/下
  direction = 'top',
  logo,
}) => {
  const [isHovering, setHovering] = useState(false);
  const [popupPosition, setPopupPosition] = useState({});
  const [data, setData] = useState({});
  const [hasFetchedData, setHasFetchedData] = useState(false);
  const triggerRef = useRef(null);
  const popupRef = useRef(null);
  const appContext = useContext(AppContext);

  const onMouseEnter = async ({ }) => {
    setHovering(true);
    // 只第一次请求一次
    if (!hasFetchedData) {
      setHovering(true);
      setHasFetchedData(true); // 更新标志，表示数据已被获取
    }
  };
  const onMouseLeave = (event) => {
    //如果鼠标的坐标在弹窗上，不关闭
    const { clientX, clientY } = event;
    const popupRect = popupRef.current.getBoundingClientRect();
    const isOnPopup =
      clientX >= popupRect.left &&
      clientX <= popupRect.right &&
      clientY >= popupRect.top &&
      clientY <= popupRect.bottom;
    if (isOnPopup) {
      return;
    }
    setHovering(false);
  };
  return (
    <div
      class="relative cursor-pointer flex flex-col items-center justify-center p-1 "
      style={{
        marginTop: "0 !important",
      }}
      onMouseLeave={onMouseLeave}
      onMouseEnter={onMouseEnter}
      onTouchStart={onMouseEnter}
      onTouchEnd={onMouseLeave}
    >
      <div
        class="w-16 h-16 "
        ref={triggerRef}
      >
        {image && !logo && (
          <img
            class="w-full object-cover mx-auto"
            src={image}
            alt={label}
          />
        )}
        {logo && (
          <div class="flex flex-col items-center ">
            <div
              class="z-[1] relative"
              style={{
                animation:
                  isHovering && hoverable ? "bounce 4s infinite" : "none",
              }}
            >
              <div class="w-full h-8 flex items-center justify-center">
                <img
                  class="w-[50%] object-cover mx-auto "
                  src={logo}
                  alt={label}
                />
              </div>

              <img
                class="w-full object-cover  mx-auto z-[1] relative -translate-y-[70%] visiblity-visible"
                src="https://gw.alicdn.com/imgextra/i1/O1CN016wUWox1REEMXKzwhg_!!6000000002079-2-tps-168-84.png"
                alt=""
                style={{
                  visibility: isHovering ? "visible" : "hidden",
                }}
              />
            </div>
            <img
              class="w-[90%] object-cover mx-auto -translate-y-[120%]"
              // src="https://gw.alicdn.com/imgextra/i2/O1CN01ENganA1vAVcKZH9Kz_!!6000000006132-2-tps-200-142.png"
              src="https://gw.alicdn.com/imgextra/i2/O1CN01sYFTnE21aVnWHhLGR_!!6000000007001-2-tps-144-112.png"
            />
          </div>
        )}
      </div>

      <p
        class="text-center text-xs"
        style={`color:${appContext.colors.normalFontColor}`}
      >
        {label}
      </p>
      {hoverable && (
        // <div class="absolute left-1/2  opacity-0 group-hover:opacity-100 visibility-hidden group-hover:visibility-visible transition-opacity duration-300 z-10">
        //   <PopupContent {...hoverContent} title={label} />
        // </div>
        <div
          className="absolute z-10 shadow-md -mt-20 ml-2"
          style={{
            visibility: isHovering ? "visible" : "hidden",
            bottom: direction === 'top' ? '95px' : '',
            top: direction === 'top' ? '' : '165px',
            // ...popupPosition,
          }}
          onMouseLeave={() => setHovering(false)}
          ref={popupRef}
        >
          <PopupContent {...hoverContent} direction={direction} data={data} isHovering={isHovering}/>
        </div>
      )}
    </div>
  );
};
