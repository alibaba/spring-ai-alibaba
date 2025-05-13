import { useEffect, useState } from "preact/hooks";
import PluginCard from "./PluginCard";
import { pluginEmums } from "./PluginEnum";
import cloneDeep from "lodash.clonedeep";

const componentList = [
  "全部",
  "Example",
  "Tool Calling",
  "Vector Store",
  "Document Reader",
  "LLM",
  "Embedding",
  "Memory",
];

const scenarioObj = {
  "Tool Calling": ["全部", "搜索", "生产效率", "网页浏览", "生活服务", "其他"],
  "Example": ["全部", "Playground", "MCP", "文生文聊天", "文生图", "多模态", "ToolCalling", "RAG", "可观测"],
  "Vector Store": ["全部"],
  "Document Reader": ["全部", "数据格式", "数据来源"],
  "LLM": ["全部", "ChatClient", "ImageModel", "AudioModel"],
  "Embedding": ["全部"],
  "Memory": ["全部"],
};

const PluginHub = (props) => {
  const [cardData, setCardData] = useState(pluginEmums);
  const [filterText, setFilterText] = useState("");
  const [currentComponent, setCurrentComponent] = useState("全部");
  const [currentScenario, setCurrentScenario] = useState("全部");
  const [currentScenarioList, setCurrentScenarioList] = useState([]);

  useEffect(() => {
    handleCheckData();
    handleSwitchScenario();
  }, [filterText, currentComponent, currentScenario]);

  const handleCheckData = () => {
    const copyData = cloneDeep(pluginEmums);
    const newData = [];
    const firstData = [];
    const secondData = [];
    const thirdData = [];
    if (filterText) {
      copyData.forEach((item) => {
        if (item?.title?.toLowerCase().includes(filterText.toLowerCase()) ||
            item?.desc?.toLowerCase().includes(filterText.toLowerCase())) {
          firstData.push(item);
        }
      });
    } else {
      firstData.push(...copyData);
    }
    if (currentComponent === "全部") {
      secondData.push(...firstData);
    } else {
      firstData.forEach((item) => {
        if (item.class === currentComponent) {
          secondData.push(item);
        }
      });
    }
    if (currentScenario === "全部") {
      thirdData.push(...secondData);
    } else {
      secondData.forEach((item) => {
        if (item.childrenClass === currentScenario) {
          thirdData.push(item);
        }
      });
    }
    setCardData(thirdData);
  };

  const handleSwitchScenario = () => {
    if (currentComponent !== "全部") {
      setCurrentScenarioList(scenarioObj[currentComponent]);
    }
  };

  return (
    <div class="flex flex-col justify-center items-center bg-secondary">
      {/* 过滤器 */}
      <div class="md:w-[85.125rem]">
        <label className="input input-bordered flex items-center gap-2 rounded-3xl mt-5 bg-[#EBEFEF] w-96">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 16 16"
            fill="currentColor"
            className="h-4 w-4 opacity-70"
          >
            <path
              fillRule="evenodd"
              d="M9.965 11.026a5 5 0 1 1 1.06-1.06l2.755 2.754a.75.75 0 1 1-1.06 1.06l-2.755-2.754ZM10.5 7a3.5 3.5 0 1 1-7 0 3.5 3.5 0 0 1 7 0Z"
              clipRule="evenodd"
            />
          </svg>
          <input
            type="text"
            class="grow rounded-3xl"
            placeholder="搜索Package..."
            value={filterText}
            onChange={(e) => setFilterText(e.target.value)}
          />
        </label>
        <div className="rounded-3xl mt-5 bg-[#EBEFEF] leading-10 pb-2 pt-2">
          <div className="inline-block ml-5 mr-5">按组件</div>
          {componentList?.map((item, index) => (
            <button
              key={`${item}${index}`}
              className={`text-white cursor-pointer rounded-3xl leading-10 h-10 flex-1 w-36 hover:bg-[#418B5A] hover:text-[#F6F8F8] ml-2 mr-2 ${
                currentComponent === item ? "bg-[#418B5A] text-[#F6F8F8]" : ""
              }`}
              value={item}
              onClick={(e) => setCurrentComponent(e.target.value)}
            >
              {item}
            </button>
          ))}
        </div>
        {currentComponent !== "全部" && (
          <div className="rounded-3xl mt-5 bg-[#EBEFEF] leading-10 pb-2 pt-2">
            <div className="inline-block ml-5 mr-5">按场景</div>
            {currentScenarioList?.map((item, index) => (
              <button
                key={item}
                className={`text-white cursor-pointer rounded-3xl leading-10 h-10 flex-1 w-36 hover:bg-[#418B5A] hover:text-[#F6F8F8] ml-2 mr-2 ${
                  currentScenario === item ? "bg-[#418B5A] text-[#F6F8F8]" : ""
                }`}
                value={item}
                onClick={(e) => setCurrentScenario(e.target.value)}
              >
                {item}
              </button>
            ))}
          </div>
        )}
      </div>
      <div class="mb-20 w-full md:w-[85.125rem]">
        <PluginCard dataSource={cardData} />
      </div>
    </div>
  );
};

export default PluginHub;
