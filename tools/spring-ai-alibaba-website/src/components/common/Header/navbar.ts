import DocsMenu from "./DocsMenu.astro";
import CommunityMenu from "./CommunityMenu.astro";
import LearnMenu from "./LearnMenu.astro";
import SolutionsMenu from "./SolutionsMenu.astro";


export default [
  {
    label: "文档",
    translations: {
      en: "DOCS",
    },
    trigger: "hover",
    slot: DocsMenu,
    position: "absolute",
    activePath: ["/docs"],
  },
  {
    label: "博客",
    translations: {
      en: "BLOG",
    },
    trigger: "click",
    target: "_self",
    route: "/blog/",
  },
  {
    label: "示例与插件",
    translations: {
      en: "Tools",
    },
    trigger: "click",
    target: "_self",
    route: "/cloud/",
    activePath: ["/cloud"],
  },
  {
    label: "MCP Directory",
    translations: {
      en: "MCP Marketplace",
    },
    trigger: "click",
    target: "_self",
    route: "/mcp/",
    activePath: ["/mcp"],
  },
  {
    label: "社区",
    translations: {
      en: "COMMUNITY",
    },
    trigger: "hover",
    relativePosition: 'page',
    slot: CommunityMenu,
    position:"absolute",
    activePath: ["/news", "/activity", "/blog", "/docs/ebook/", "/download"],
  },
];