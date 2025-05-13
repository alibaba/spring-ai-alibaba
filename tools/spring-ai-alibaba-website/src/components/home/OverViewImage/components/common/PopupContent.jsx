import React from "react";
import {  useState,useContext, useEffect } from "preact/hooks";
import { twMerge } from "tailwind-merge";
import { AppContext } from "../../context";
import useCustomSWR from "@/utils/useCustomSWR";

export const PopupContent = ({ image, title, labels, links, direction = 'top', isHovering }) => {
  const { swrData={}, fetchData } = useCustomSWR(links?.Github?.apiLink);
  const appContext = useContext(AppContext);
  const [arrow, setArrow] = useState('');
  const [startCount, setStartCount] = useState(0);
  const [forkCount, setForkCount] = useState(0);

  useEffect(()=>{
    const { stargazers_count = 0, forks_count = 0 } = swrData || {};
    setStartCount(stargazers_count || 0);
    setForkCount(forks_count || 0);
  },[swrData]);

  useEffect(()=>{
    if(isHovering) {
      fetchData();
    };
  },[isHovering])

  if (direction === 'bottom') {
    // hover弹框在下面,箭头在上面
    setArrow("after:absolute after:bottom-full after:left-2/4 after:ml-[-5px] after:border-[5px] after:border-b-base-100 after:border-x-transparent after:border-t-transparent  after:content-['']")
  } else {
    // hover弹框在上面，箭头在下面
    setArrow("after:absolute after:top-full after:left-2/4 after:ml-[-5px] after:border-[5px] after:border-t-base-100 after:border-x-transparent after:border-b-transparent  after:content-['']")
  }
  return (
    <div
      className={`relative bg-base-100 text-base-100 p-6 rounded-xl backdrop-opacity-96 shadow-lg min-w-[400px] max-w-md mx-auto ${arrow}`}
    >
      <div className="flex items-center justify-between border-b border-success mb-4 pb-4">
        {/* <div className="flex-1 flex justify-start items-center"> */}
        <img src={image} alt="logo" className="max-h-16 max-w-[130px] " />
        {/* </div> */}
        <div >
          {labels.map((label, idx) => (
            <span
              key={idx}
              className={` bg-opacity-60 border text-xs px-2 py-1 rounded mr-2 mb-2 whitespace-nowrap`}
              style={`color:${appContext.colors.tagFontColor}; background-color:${appContext.colors.tagBgColor}; border-color:${appContext.colors.tagBorderColor};`}
            >
              {label}
            </span>
          ))}
        </div>
      </div>
      <div>
        <div className="text-sm text-success mb-4 ">{title}</div>
        {Object.entries(links).map(
          (
            [key, { link }],
            idx
          ) => (
            <div key={idx} className="mb-2 flex justify-between">
              <div
                className={twMerge(
                  "text-success text-sm mb-1",
                )}
              >
                {key}:
              </div>
              <div className="w-[70%] overflow-hidden ">
                <a 
                href={link} 
                className={twMerge("line-clamp-1 no-underline",appContext.linkStyle )}
                style={`color:${appContext.colors.linkColor};`}
                >
                  {link}
                </a>
                {key==='Github' &&<div className="flex justify-start mt-1">
                  <div 
                  className={`flex items-center text-xs py-1 px-2 rounded`}
                  style={`color:${appContext.colors.starForkFontColor};background-color:${appContext.colors.starForkBgColor};`}
                  >
                    <svg
                      t="1711507787559"
                      class="icon w-4 h-4 mr-1"
                      viewBox="0 0 1024 1024"
                      version="1.1"
                      xmlns="http://www.w3.org/2000/svg"
                      p-id="5263"
                      width="200"
                      height="200"
                      fill="currentColor"
                    >
                      <path
                        d="M960 384l-313.6-40.96L512 64 377.6 343.04 64 384l230.4 208.64L234.88 896 512 746.88 789.12 896l-59.52-303.36L960 384z"
                        p-id="5264"
                      ></path>
                    </svg>
                    <span>{startCount}</span>
                  </div>
                  <div 
                  className={`ml-2 flex items-center text-xs py-1 px-2 rounded`}
                  style={`color:${appContext.colors.starForkFontColor};background-color:${appContext.colors.starForkBgColor};`}
                  >
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      version="1.1"
                      width="16"
                      height="18"
                      viewBox="0 0 1024 1024"
                      class="icon w-4 h-4 mr-1"
                    ><g>
                        <path
                          d="M384 160a32 32 0 0 1 32-32h192a32 32 0 0 1 32 32v192a32 32 0 0 1-32 32h-64v128h192a64 64 0 0 1 64 64v64h64a32 32 0 0 1 32 32v192a32 32 0 0 1-32 32h-192a32 32 0 0 1-32-32v-192a32 32 0 0 1 32-32h64V576h-448v64h64a32 32 0 0 1 32 32v192a32 32 0 0 1-32 32h-192a32 32 0 0 1-32-32v-192a32 32 0 0 1 32-32h64V576a64 64 0 0 1 64-64h192V384h-64a32 32 0 0 1-32-32v-192zM448 320h128V192H448v128z m-256 384v128h128v-128H192z m512 0v128h128v-128h-128z"
                          fill="currentColor"
                          fill-opacity="1">
                        </path>
                      </g>
                    </svg>
                    <span>{forkCount}</span>
                  </div>
                </div>}
              </div>
            </div>
          )
        )}
      </div>
    </div>
  );
};
