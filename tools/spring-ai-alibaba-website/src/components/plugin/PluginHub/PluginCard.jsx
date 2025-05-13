import React, { useState, useEffect } from "react";
import Button from "@/components/common/ReactComponents/Button.jsx";
import AceEditor from "react-ace";
import Tooltip from "@components/common/Tooltip/index";
import { success, error } from "@components/common/Message/index";
import "ace-builds/src-noconflict/theme-monokai";
const PluginCardList = (props) => {
  const { dataSource } = props;
  const [showTooltip, setShowTooltip] = useState(false);
  const [currentDom, setCurrentDom] = useState("");

  const handleMouseEnter = (e) => {
    setCurrentDom(e.target.id);
    setShowTooltip(true);
  };

  const handleMouseLeave = () => {
    setCurrentDom("");
    setShowTooltip(false);
  };

  return (
    <div className="my-8 mx-auto">
      <div className="grid gap-8 gap-x-12 sm:gap-y-8 not-content lg:grid-cols-3 sm:grid-cols-2">
        {dataSource.map((item) => (
          <a
            key={item.title} // 确保每个子组件都有唯一的键
            class="no-underline hover-transform-box-shadow bg-error nounderline relative flex flex-col hover:shadow-xl transition ease-in-out hover:scale-[102%] cursor-pointer rounded-lg backdrop-blur border border-[#ffffff29]"
          >
            <div class="p-6 rounded-2xl ">
              <div class="flex flex-row justify-between items-center">
                <img
                  class="logo w-12 h-12 rounded-lg"
                  src={item.img}
                  alt="start"
                />
              </div>
              <p class="mt-4 text-[18px] leading-[18px] font-medium text-neutral">
                {item.title}
              </p>
              <p class="mt-[12px] text-[14px] leading-[18px] font-medium text-success">
                {item.desc}
              </p>
              <p class="mt-[12px] flex justify-between items-center">
                {item.link && (
                  <Button
                    class="btn btn-secondary cursor-pointer rounded-3xl min-h-2 h-8 flex-1 w-28"
                    visibility={false}
                    href={item.link}
                    target="_blank"
                  >
                    源码
                  </Button>
                )}
                {item.example && (
                  <Button
                    class="btn btn-secondary cursor-pointer rounded-3xl min-h-2 h-8 flex-1 w-28"
                    visibility={false}
                    href={item.example}
                    target="_blank"
                  >
                    示例
                  </Button>
                )}
                {item.rely && (
                <Tooltip
                  children={
                    <Button
                      id={item.title}
                      class="btn btn-secondary cursor-pointer rounded-3xl min-h-2 h-8 flex-1 w-28 text-neutral"
                      visibility={false}
                      onClick={() => {
                        navigator.clipboard
                          .writeText(item.rely)
                          .then(() => {
                            success("复制成功");
                          })
                          .catch(() => {
                            error("复制失败");
                          });
                      }}
                      type="normal"
                      iconClass="text-neutral"
                      onMouseEnter={handleMouseEnter}
                      onMouseLeave={handleMouseLeave}
                    >
                      依赖
                      {showTooltip && currentDom === item.title && (
                        <span>
                          <svg
                            t="1741165257530"
                            class="icon"
                            viewBox="0 0 1024 1024"
                            version="1.1"
                            xmlns="http://www.w3.org/2000/svg"
                            p-id="4518"
                            width="12"
                            height="12"
                          >
                            <path
                              d="M829.568 53.12H960V1024H194.432v-121.344H64V284.48L361.92 0h467.648v53.12z m0 80.896v768.64H279.488v40.448h595.456V134.016h-45.44zM149.056 317.952v503.808h595.456V80.896H397.248L149.12 317.952z"
                              fill="#262626"
                              p-id="4519"
                            ></path>
                          </svg>
                        </span>
                      )}
                    </Button>
                  }
                  message={
                    <AceEditor
                      mode="yaml"
                      theme="monokai"
                      showPrintMargin={false}
                      value={item.rely}
                      width="350px"
                      height="150px"
                    />
                  }
                />
              )}  
              </p>
            </div>
          </a>
        ))}
      </div>
    </div>
  );
};

export default PluginCardList;
