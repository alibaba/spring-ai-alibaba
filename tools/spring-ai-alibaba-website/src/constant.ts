import type { Customer, Solution, ChooseReason, CommunityLink, ContributeLink, CloudIntroduceCard } from "@/types"
import { getEntries } from "astro:content";

// Place any global data in this file.
// You can import this data from anywhere in your site by using the `import` keyword.

// 吊顶提示文案
export const TOPBAR = {
    "text": "Spring AI Alibaba Graph 预览版来了，快速开发 Dify 工作流、OpenManus 等智能体应用！",
    "mobileText": "Spring AI Alibaba Graph 预览版来了，快速开发 Dify 工作流、OpenManus 等智能体应用！",
    "link": "/blog/spring-ai-alibaba-mcp/",
    "target": "_blank",
    "display": true
}
// TODO: 配置algolia
export const ALGOLIA = {
  appId: 'S5T5EPDFWK',
  apiKey: 'f79c0543654b1070280af9f513bff8ee',
  indexName: 'sca-aliyun'
}

// 文档根据版本区分的提示banner
export const DOCS_BANNER = {
  latest: '',
  next:'',
  v1: '',
  v2:'',
}

// TODO: 文档Header数据
export const DOCS_ITEM = [
  {
    key: "1.0.0-M6.1",
    label: "1.0.0-M6.1",
    target: "_self",
    link: "/docs/1.0.0-M6.1/overview/",
    translations: {
      en: "1.0.0-M6.1",
      link: "/en/docs/1.0.0-M6.1/overview/",
    }
  },
  {
    key: "1.0.0-M5.1",
    label: "1.0.0-M5.1",
    target: "_self",
    link: "/docs/1.0.0-M5.1/overview/",
    translations: {
      en: "1.0.0-M5.1",
      link: "/en/docs/1.0.0-M5.1/overview/",
    }
  },
  {
    key: "1.0.0-M3.2",
    label: "1.0.0-M3.2",
    target: "_self",
    link: "/docs/1.0.0-M3.2/overview/",
    translations: {
      en: "1.0.0-M3.2",
      link: "/en/docs/1.0.0-M3.2/overview/",
    }
  },
  {
    key: "dev",
    label: "dev",
    target: "_self",
    link: "/docs/dev/overview/",
    translations: {
      en: "dev",
      link: "/en/docs/dev/overview/",
    }
  },
];

// 主要特性
export const CHOOSE_REASON_LIST: ChooseReason[] = [
  {
    title: "home.website.edge.1.title",
    svgKey: "easy",
    description: "home.website.edge.1.description",
  },
  {
    title: "home.website.edge.2.title",
    svgKey: "adaptive",
    description: "home.website.edge.2.description",
  },
  {
    title: "home.website.edge.3.title",
    svgKey: "timeTested",
    description: "home.website.edge.3.description",
  },
  {
    title: "home.website.edge.4.title",
    svgKey: "variety",
    description: "home.website.edge.4.description",
  },
]
// 核心特性
export const CORE_FEATURE_LIST: ChooseReason[] = [
  {
    title: "home.website.ai.1.title",
    svgKey: "aibox",
    description: "home.website.ai.1.description",
  },
  {
    title: "home.website.ai.2.title",
    svgKey: "aibook",
    description: "home.website.ai.2.description",
  },
  {
    title: "home.website.ai.3.title",
    svgKey: "aicen",
    description: "home.website.ai.3.description",
  }
];

// 合作客户反馈
export const COMPANY_CUSTOMERS: Customer[] = [
  {
    name: "cloud.feedback.soul.name",
    theme: "gray",
    logo: "https://img.alicdn.com/imgextra/i2/O1CN01GZhEqR1bY3dY5SOuY_!!6000000003476-2-tps-160-130.png",
    href: "https://developer.aliyun.com/article/1174616",
    description: "cloud.feedback.soul.description",
  },
  {
    name: "cloud.feedback.laidian.name",
    theme: "dark",
    logo: "https://img.alicdn.com/imgextra/i3/O1CN010ulPrT1M45UNBCAXe_!!6000000001380-2-tps-160-130.png",
    href: "https://developer.aliyun.com/article/855894",
    description: "cloud.feedback.laidian.description",
  },
  {
    name: "cloud.feedback.zeekr.name",
    theme: "light",
    logo: "https://img.alicdn.com/imgextra/i2/O1CN01zluUCK29BKvOCojPr_!!6000000008029-2-tps-160-130.png",
    href: "https://developer.aliyun.com/article/1173573",
    description: "cloud.feedback.zeekr.description",
  },
  {
    name: "cloud.feedback.ykc.name",
    theme: "gray",
    logo: "https://img.alicdn.com/imgextra/i1/O1CN01LWMfwx1Ggf9VGmXBF_!!6000000000652-2-tps-160-130.png",
    href: "https://developer.aliyun.com/article/1172572",
    description: "cloud.feedback.ykc.description",
  },
  {
    name: "cloud.feedback.bosideng.name",
    theme: "dark",
    logo: "https://img.alicdn.com/imgextra/i2/O1CN01d7EDXs1HLsnXSTgGG_!!6000000000742-2-tps-160-130.png",
    href: "https://developer.aliyun.com/article/1147221",
    description: "cloud.feedback.bosideng.description",
  },
  {
    name: "cloud.feedback.skechers.name",
    theme: "light",
    logo: "https://img.alicdn.com/imgextra/i1/O1CN01P1k9gA1YpVsxPYzAw_!!6000000003108-2-tps-160-130.png",
    href: "https://developer.aliyun.com/article/844814",
    description: "cloud.feedback.skechers.description",
  },
  {
    name: "cloud.feedback.very.name",
    theme: "gray",
    logo: "https://img.alicdn.com/imgextra/i1/O1CN01DevTFA28W7HY1JFC6_!!6000000007939-2-tps-160-130.png",
    href: "https://developer.aliyun.com/article/992090",
    description: "cloud.feedback.very.description",
  },
  {
    name: "cloud.feedback.helian.name",
    theme: "dark",
    logo: "https://img.alicdn.com/imgextra/i3/O1CN01YmUBmh1snwqr4kRot_!!6000000005812-2-tps-544-180.png",
    href: "https://developer.aliyun.com/article/1095573",
    description: "cloud.feedback.helian.description",
  },
  {
    name: "cloud.feedback.zhijin.name",
    theme: "light",
    logo: "https://img.alicdn.com/imgextra/i3/O1CN015GIqM31qsPTObt2CV_!!6000000005551-2-tps-544-180.png",
    href: "https://developer.aliyun.com/article/1064881",
    description: "cloud.feedback.zhijin.description",
  },
];

// 解决方案列表
export const SOLUTION_LIST: Solution[] = [
  {
    checked: true,
    src: "https://help.aliyun.com/zh/mse/user-guide/implement-an-end-to-end-canary-release-by-using-mse-cloud-native-gateways",
    title: "home.solutions.card.1",
    keyword: [
      "home.solutions.card.keyword.high_availability",
      "home.solutions.card.keyword.grayscale_publishing",
      "home.solutions.card.keyword.eliminating_change_risks",
      "home.solutions.card.keyword.service_governance"
    ],
  },
  {
    checked: false,
    src: "https://developer.aliyun.com/article/1128388",
    title: "home.solutions.card.2",
    keyword: [
      "home.solutions.card.keyword.current_limiting_degradation",
      "home.solutions.card.keyword.high_availab",
      "home.solutions.card.keyword.great_promotion_of_stability",
      "home.solutions.card.keyword.eliminating_runtime_risks"
    ],
  },
  {
    checked: false,
    src: "https://developer.aliyun.com/article/1265016",
    title: "home.solutions.card.3",
    keyword: [
      "home.solutions.card.keyword.three_in_one",
      "home.solutions.card.keyword.safety",
      "home.solutions.card.keyword.plugin_ecosystem",
      "home.solutions.card.keyword.application_firewall",
    ],
  },
  {
    checked: false,
    src: "https://help.aliyun.com/zh/mse/use-cases/implement-graceful-start-and-shutdown-of-microservice-applications-by-using-mse",
    title: "home.solutions.card.4",
    keyword: [
      "home.solutions.card.keyword.lossless",
      "home.solutions.card.keyword.multi_availability",
      "home.solutions.card.keyword.grayscale",
      "home.solutions.card.keyword.service_governance",
    ],
  },
];

// 文档贡献板块的链接列表
export const getCommunityLinkList = (t: Function): CommunityLink[] => [
  {
    href: `/blog`,
    text: t("rightSidebar.readBlog"),
    icon: "basil:document-outline",
  },
  {
    href: 'https://github.com/spring-cloud-alibaba-group/spring-cloud-alibaba-group.github.io/blob/develop-astro-sca/src/content/docs',
    text: t("rightSidebar.github"),
    icon: "ant-design:github-filled",
  },
];



// 文档社区板块的链接列表
export const getContributeLinkList = (lang: string, editHref: string, feedbackUrl: string, t: Function): ContributeLink[] => [
  {
    // href: `/${lang}/v2/contribution/contributing`,
    href: '/docs/developer/contributor-guide/new-contributor-guide_dev/',
    text: t("rightSidebar.contributorGuides"),
    depth: 2,
    icon: "tabler:book",
  },
  {
    href: editHref,
    text: t("rightSidebar.editPage"),
    depth: 2,
    icon: "tabler:pencil",
  },
  {
    href: feedbackUrl,
    text: t("rightSidebar.feedbackIssue"),
    depth: 2,
    icon: "ant-design:github-filled",
  },
];

export const i18nMap = {
  "blog": {
    article: 'blog.article.technical',
    case: 'blog.article.case.best.practices',
    ecosystem: 'blog.article.ecosystem.articles',
    all: 'blog.all.articles'
  },
  "news": {
    announcement: 'blog.activity.community.announcement',
    release: 'blog.activity.release.statement',
    committer: 'page.blog.news.personnel.promotion',
    cooperate: 'page.blog.news.community.cooperation',
    all: 'page.blog.news.all'
  },
  "activity": {
    'announcement': 'blog.activity.community.announcement',
    'activity-preview': 'blog.activity.preview.event',
    'activity-detail': 'blog.activity.detail.event',
    'all': 'blog.activity.all.event'
  },
  "learn": {
    'spring': 'learn.spring.title',
    'spring-boot': 'learn.spring-boot.title',
    'spring-cloud': 'learn.spring-cloud.title',
    'spring-cloud-alibaba': 'learn.spring-cloud-alibaba.title',
    'all': 'learn.all.title'
  },
  "wuyi": {
    'all': 'learn.all.title',
    'expertConsultation': 'wuyi.meet-professor.title',
  },
};

export const BLOG_CATEGORY = [
  {
    type: 'all',
    title: '全部文章',
    href: '/blog/'
  },
  {
    type: 'article',
    title: '技术文章',
    href: '/blog/article/'
  },

  {
    type: 'ecosystem',
    title: '生态文章',
    href: '/blog/ecosystem/'
  },
  {
    type: 'case',
    title: '最佳实践',
    href: '/blog/case/'
  },
];

export const LEARN_CATEGORY = [
  {
    type: 'all',
    title: '全部文章',
    href: '/learn/'
  },
  {
    type: 'spring',
    title: 'Spring',
    href: '/learn/spring/'
  },

  {
    type: 'spring-boot',
    title: 'Spring Boot',
    href: '/learn/spring-boot/'
  },
  {
    type: 'spring-cloud',
    title: 'Spring Cloud',
    href: '/learn/spring-cloud/'
  },
  {
    type: 'spring-cloud-alibaba',
    title: 'Spring Cloud Alibaba',
    href: '/learn/spring-cloud-alibaba/'
  },
];

export const WUYI_CATEGORY = [
  {
    type: 'expertConsultation',
    title: '全部文章',
    href: '/faq/'
  },
];

export const HEADER_ACTIVITY_CARD = [
    {
        "collection": "blog",
        "slug": "news/spring-ai-alibaba-atom-programming-contest",
        "description": "Spring AI Alibaba 开放原子基金会编程挑战赛！",
        "imageUrl": "https://img.alicdn.com/imgextra/i2/O1CN01Gh8wq71CApBVywPq3_!!6000000000041-0-tps-800-1000.jpg"
    },
    {
        "collection": "blog",
        "slug": "news/attend-a-meeting",
        "description": "参加社区双周会！",
        "imageUrl": "https://img.alicdn.com/imgextra/i2/O1CN01Gh8wq71CApBVywPq3_!!6000000000041-0-tps-800-1000.jpg"
    }
];

export const HEADER_LEARN_CARD = [
  {
    collection: "learn",
    slug: "spring-boot/core",
    description: "最全面的 Spring 中文系列教程，从这里开启你的 Spring 应用开发之旅！",
    imageUrl:
      "https://img.alicdn.com/imgextra/i1/O1CN01xDVfHk1El7oBMjL3p_!!6000000000391-2-tps-1083-721.png",
  },
];

export const HEADER_SOLUTIONS_CARD = [
  {
    collection: "blog",
    slug: "release-nacos110",
    blankUrl: 'https://www.aliyun.com/product/aliware/mse',
    description: "阿里云 MSE 微服务引擎",
    imageUrl:
      "https://img.alicdn.com/imgextra/i2/O1CN011815Q71wQpLpuKYeC_!!6000000006303-0-tps-1284-721.jpg",
  },
];

export const BLOG_IMAGE_SOURCE = [
  "https://img.alicdn.com/imgextra/i1/O1CN0114MKuq1qKyZ0heOq7_!!6000000005478-2-tps-304-179.png",
  "https://img.alicdn.com/imgextra/i2/O1CN01E4YfjD1WmBmWymUJC_!!6000000002830-2-tps-608-358.png",
  "https://img.alicdn.com/imgextra/i1/O1CN01o9sjZA1Efd1bMrl0C_!!6000000000379-2-tps-608-358.png",
  "https://img.alicdn.com/imgextra/i1/O1CN011wgjV01CZ695M8OEB_!!6000000000094-2-tps-608-358.png",
  "https://img.alicdn.com/imgextra/i2/O1CN01swoIUH1csxKPKfwJE_!!6000000003657-2-tps-608-358.png",
  "https://img.alicdn.com/imgextra/i4/O1CN01nIy1Wf1DjSiy0TCxe_!!6000000000252-2-tps-608-358.png",
  "https://img.alicdn.com/imgextra/i3/O1CN019EjKf11Dj0KQKkP3e_!!6000000000251-2-tps-608-358.png",
  "https://img.alicdn.com/imgextra/i2/O1CN01l7gM7r1Y4L5ngHWb8_!!6000000003005-2-tps-608-358.png",
  "https://img.alicdn.com/imgextra/i2/O1CN01oWfLB51kfENwUFaQw_!!6000000004710-2-tps-608-358.png"
];

export const MICROSERVICE_SOLUTION = [
  { title: 'Nacos', image: '/assets/2-1.jpg', detailTitle: "home.introduction.detailTitle.1", detail: 'home.introduction.detail.1' },
  { title: 'Sentinel', image: '/assets/2-2.jpg', detailTitle: 'home.introduction.detailTitle.2', detail: 'home.introduction.detail.2' },
  { title: 'Seata', image: '/assets/2-3.jpg', detailTitle: 'home.introduction.detailTitle.3', detail: 'home.introduction.detail.3' },
  { title: 'RocketMQ', image: '/assets/2-4.jpg', detailTitle: 'home.introduction.detailTitle.4', detail: 'home.introduction.detail.4' },
  { title: 'AI', image: '/assets/2-5.jpg', detailTitle: 'home.introduction.detailTitle.5', detail: 'home.introduction.detail.5' },
  { title: 'Spring Boot', image: '/assets/2-6.jpg', detailTitle: 'home.introduction.detailTitle.6', detail: 'home.introduction.detail.6' },
  { title: 'Spring', image: '/assets/2-7.jpg', detailTitle: 'home.introduction.detailTitle.7', detail: 'home.introduction.detail.7' },
];

export const ProductDisplayCardData = [
  {
    cover: "https://img.alicdn.com/imgextra/i2/O1CN01psWBwW1tzgeAxapCz_!!6000000005973-0-tps-2448-3672.jpg",
    coverPosition: "bottom",
    title: "Nacos",
    body: "home.introduction.card.Nacos.des",
    href: "docs/2023/user-guide/nacos/quick-start/",
  },
  {
    cover: "https://img.alicdn.com/imgextra/i2/O1CN01l9eXcR1LJN7PxX79e_!!6000000001278-0-tps-1000-1500.jpg",
    coverPosition: "bottom",
    title: "RocketMQ",
    body: "home.introduction.card.RocketMQ.des",
    href: "docs/2023/user-guide/rocketmq/quick-start/",
  },
  {
    cover: "https://img.alicdn.com/imgextra/i2/O1CN01HzKXZY29J7h0UIGJ5_!!6000000008046-0-tps-1000-1500.jpg",
    coverPosition: "bottom",
    title: "Sentinel",
    body: "home.introduction.card.Sentinel.des",
    href: "docs/2023/user-guide/sentinel/quick-start/",
  },
  {
    cover: "https://img.alicdn.com/imgextra/i3/O1CN01bJroU81BzNHfeB3jN_!!6000000000016-0-tps-1000-1500.jpg",
    coverPosition: "bottom",
    title: "Seata",
    body: "home.introduction.card.Seata.des",
    href: "docs/2023/user-guide/seata/quick-start/",
  },
  {
    cover: "https://img.alicdn.com/imgextra/i2/O1CN01HzKXZY29J7h0UIGJ5_!!6000000008046-0-tps-1000-1500.jpg",
    coverPosition: "bottom",
    title: "Spring AI",
    body: "home.introduction.card.AI.des",
    href: "docs/2023/user-guide/ai/quick-start/",
  },
  {
    cover: "https://img.alicdn.com/imgextra/i2/O1CN01k1amBw1U0RHtPPlvH_!!6000000002455-0-tps-1000-1500.jpg",
    coverPosition: "bottom",
    title: "Spring Boot",
    body: "home.introduction.card.Boot.des",
    href: "/learn/spring-boot/",
  },
  {
    cover: "https://img.alicdn.com/imgextra/i3/O1CN01WxXILZ1C0I4pkZUyD_!!6000000000018-0-tps-1000-1500.jpg",
    coverPosition: "bottom",
    title: "Spring Cloud",
    body: "home.introduction.card.Cloud.des",
    href: "/learn/spring-cloud/",
  },
  {
    cover: "https://img.alicdn.com/imgextra/i1/O1CN01bKcEde1xVhBVptyhX_!!6000000006449-2-tps-1312-880.png",
    coverPosition: "bottom",
    title: "Spring Scheduling Tasks",
    body: "home.introduction.card.schedulerx.des",
    href: "docs/2023/user-guide/schedulerx/quick-start/",
  },
];

export const categoryMap = {
  article: "blog_article",
  case: "blog_case",
  ecosystem: "blog_ecosystem",
  release: "news_release",
  committer: "news_personnel",
  announcement: "news_announcement",
  cooperate: "news_cooperate",
  "activity-preview": "activity_activity-preview",
  "activity-detail": "activity_activity-preview",
};

//获取顶部导航悬浮层 博客列表信息
export const getBlogContentList = async (blogList = []) => {

  const simplifiedPosts = blogList.map((item) => ({
    collection: item.collection,
    slug: item.slug,
  }));
  const blogDescrip = blogList.map((item) => item.description);

  const blogImgs = blogList.map((item) => item.imageUrl);
  const posts = (await getEntries(simplifiedPosts as any)) || [];
  const blankUrl = blogList.map((item) => item.blankUrl);

  return {
    blogDescrip,
    blogImgs,
    posts,
    blankUrl
  };
}

export const COMMUNITY_MENU_LIST = [
  {
    label: "社区",
    translations: {
      en: "COMMUNITY",
    },
    children: [
      {
        label: "报告文档问题",
        target: "_blank",
        link: "https://github.com/springaialibaba/spring-ai-alibaba-website/issues",
        translations: {
          en: "Report a doc issue",
        },
      },
      {
        label: "贡献社区",
        target: "_blank",
        link: 'https://github.com/alibaba/spring-ai-alibaba/pulls',
        translations: {
          en: "Contribute community",
        },
      },
      {
        label: "贡献者",
        target: "_blank",
        link: 'https://github.com/alibaba/spring-ai-alibaba/graphs/contributors',
        translations: {
          en: "Contributors",
        },
      },
      {
        label: '贡献者指南',
        target: "_blank",
        link: '/docs/developer/contributor-guide/new-contributor-guide_dev/',
        translations: {
          en: "Contributor Guides",
        },
      },
    ],
  },
  {
    label: "资源",
    translations: {
      en: "Resources",
    },
    children: [
      {
        label: "博客",
        target: "_self",
        link: "/blog/",
        translations: {
          en: "Blog",
        },
      },
      {
        label: "电子书（编写中...）",
        target: "_self",
        link: "#",
        translations: {
          en: "E-book(Coming soon...)",
        },
      },
    ],
  },
];

export const LEARN_CARD_LIST= [
  {
    title: "commmon.header.spring.tutorial",
    description: "commmon.header.spring.tutorial.describe",
    href: "/learn/spring/",
  },
  {
    title: "commmon.header.spring.boot.tutorial",
    description: "commmon.header.spring.boot.tutorial.describe",
    href: "/learn/spring-boot/",
  },
  {
    title: "commmon.header.spring.cloud.alibaba.tutorial",
    description:  "commmon.header.spring.cloud.alibaba.tutorial.describe",
    href: "/learn/spring-cloud/",
  },
  {
    title: "commmon.header.spring.mse.ebook",
    description: "commmon.header.spring.mse.ebook.describe",
    href: "/docs/ebook/srekog/",
  }
];

export const SOLUTIONS_CARD_LIST = [
  {
    title: "commmon.header.microservices.engine",
    description: "commmon.header.microservices.engine.describe",
    href: "https://help.aliyun.com/zh/mse/use-cases/implement-high-availability-capabilities-of-mse-microservices-registry",
  },
  {
    title: "commmon.header.microservices.cloud.native.gateway",
    description: "commmon.header.microservices.cloud.native.gateway.describe",
    href: "https://developer.aliyun.com/article/1265016",
  },
  {
    title: "commmon.header.microservices.governance",
    description: "commmon.header.microservices.governance.describe",
    href: "https://help.aliyun.com/zh/mse/use-cases/implement-an-end-to-end-canary-release-by-using-mse-cloud-native-gateways",
  },
  {
    title: "commmon.header.microservices.application.service",
    description: "commmon.header.microservices.application.service.describe",
    href: "https://help.aliyun.com/zh/arms/",
  },
];

// Cloud页面价格 后付费的卡片内容
export const getCloudPostpaidData = (t: Function): CloudIntroduceCard[] => [
  {
    title: t("cloud.introduce.free.pkg"),
    price: t("cloud.introduce.free.price"),
    unit: "",
    priceDesc: [t("cloud.introduce.free.feature")],
    linkName: t("cloud.introduce.free.link"),
    link: "https://free.aliyun.com/?searchKey=spring%20cloud&spm=sca.cloud.topbar.0.0.0",
    feature: [
      t("cloud.introduce.free.discount.1"),
    ],
  },
  {
    title: t("cloud.introduce.regular.pkg"),
    priceDesc: [t("cloud.introduce.regular.feature")],
    price: t("cloud.introduce.regular.price"),
    priceSupply: t("cloud.introduce.regular.price_supply"),
    unit: "/MONTH",
    linkName: t("cloud.introduce.regular.link"),
    link: "https://www.aliyun.com/product/aliware/mse?spm=sca.cloud.topbar.0.0.0",
    feature: [
      t("cloud.introduce.regular.discount.1"),
    ],
  },
  {
    title: t("cloud.introduce.company.pkg"),
    priceDesc: [
      t("cloud.introduce.company.feature.1"),
    ],
    price: t("cloud.introduce.company.price"),
    priceSupply: t("cloud.introduce.company.price_supply"),
    unit: "/HOUR",
    linkName: t("cloud.introduce.company.link"),
    link: "https://www.aliyun.com/product/aliware/mse?spm=sca.cloud.topbar.0.0.0",
    feature: [t("cloud.introduce.company.discount.1")],
  },
];

// Cloud页面价格 资源包的卡片内容

export const getCloudResourcePackData = (t: Function) => [
  {
    title: t("cloud.introduce.free.pkg"),
    price: t("cloud.introduce.free.price"),
    unit: "",
    priceDesc: [t("cloud.introduce.free.feature")],
    linkName: t("cloud.introduce.free.link"),
    link: "https://free.aliyun.com/?searchKey=spring%20cloud&spm=sca.cloud.topbar.0.0.0",
    feature: [
      t("cloud.introduce.free.discount.1"),
    ],
  },
  {
    title: t("cloud.introduce.regular.pkg"),
    priceDesc: [t("cloud.introduce.regular.feature")],
    price:'¥189',
    priceSupply: t('cloud.introduce.regular.supply.month'),
    priceDes:t('cloud.introduce.price.des'),
    unit: "/MONTH",
    linkName: t("cloud.introduce.regular.link"),
    link: "https://www.aliyun.com/product/aliware/mse?spm=sca.cloud.topbar.0.0.0",
    feature: [
      t("cloud.introduce.regular.discount.1"),
      t("cloud.introduce.regular.discount.2"),
      t("cloud.introduce.regular.discount.3"),
    ],
  },
  {
    title: t("cloud.introduce.company.pkg"),
    priceDesc: [
      t("cloud.introduce.company.feature.1"),
    ],
    price: '¥595',
    priceSupply: t('cloud.introduce.regular.supply.month'),
    priceDes:t('cloud.introduce.price.des'),
    unit: "/HOUR",
    linkName: t("cloud.introduce.company.link"),
    link: "https://www.aliyun.com/product/aliware/mse?spm=sca.cloud.topbar.0.0.0",
    feature: [
      t("cloud.introduce.company.discount.1"),
      t("cloud.introduce.company.discount.2"),
      t("cloud.introduce.company.discount.3"),
    ],
  },
];

// 版本功能对比
export const versionDataSource = [
  {
    title: "易用性", data: [
      {
        name: {
          title: 'Java 和 Go Agent',
        },
        free: {
          checked: false,
          des: ''
        },
        speciality: {
          checked: true,
          des: '无需修改代码，即可接入'
        },
        enterprise: {
          checked: true,
          des: '无需修改代码，即可接入'
        },
      },
      {
        name: {
          title: 'Java 和 Go SDK',
        },
        free: {
          checked: false,
          des: ''
        },
        speciality: {
          checked: true,
          des: ''
        },
        enterprise: {
          checked: true,
          des: ''
        },
      },
      {
        name: {
          title: '自动化运维',
        },
        free: {
          checked: false,
          des: ''
        },
        speciality: {
          checked: true,
          des: '无需自行维护 SDK、控制台，只需关注业务'
        },
        enterprise: {
          checked: true,
          des: '无需自行维护 SDK、控制台，只需关注业务',
        },
      },
      {
        name: {
          title: '可观测',
        },
        free: {
          checked: false,
          des: ''
        },
        speciality: {
          checked: true,
          des: '应用、节点、接口颗粒度的观测大盘'
        },
        enterprise: {
          checked: true,
          des: '应用、节点、接口颗粒度的观测大盘'
        },
      },
    ]
  },
  {
    title: "应用开发测试", data: [
      {
        name: {
          title: '服务契约',
        },
        free: {
          checked: false,
          des: ''
        },
        speciality: {
          checked: true,
          des: '',
        },
        enterprise: {
          checked: true,
          des: ''
        },
      },
      {
        name: {
          title: '服务测试',
          des: ''
        },
        free: {
          checked: false,
          des: ''
        },
        speciality: {
          checked: true,
          des: ''
        },
        enterprise: {
          checked: true,
          des: ''
        },
      },
      {
        name: {
          title: '标签路由',
        },
        free: {
          checked: false,
          des: ''
        },
        speciality: {
          checked: true,
          des: ''
        },
        enterprise: {
          checked: true,
          des: ''
        },
      },
    ]
  },
  {
    title: "应用变更", data: [
      {
        name: {
          title: '无损上线',
        },
        free: {
          checked: false,
          des: ''
        },
        speciality: {
          checked: true,
          des: ''
        },
        enterprise: {
          checked: true,
          des: ''
        },
      },
      {
        name: {
          title: '无损下线',
        },
        free: {
          checked: false,
          des: ''
        },
        speciality: {
          checked: true,
          des: ''
        },
        enterprise: {
          checked: true,
          des: ''
        },
      },
      {
        name: {
          title: '多可区容灾',
        },
        free: {
          checked: false,
          des: ''
        },
        speciality: {
          checked: true,
          des: ''
        },
        enterprise: {
          checked: true,
          des: ''
        },
      },
      {
        name: {
          title: '全链路灰度',
        },
        free: {
          checked: false,
          des: ''
        },
        speciality: {
          checked: true,
          des: ''
        },
        enterprise: {
          checked: true,
          des: ''
        },
      },
    ]
  },
  {
    title: "应用运行", data: [
      {
        name: {
          title: '流量防护',
        },
        free: {
          checked: false,
          des: '',
        },
        speciality: {
          checked: false,
          des: '',
        },
        enterprise: {
          checked: true,
          des: '支持分钟、小时级别的流控'
        },
      },
      {
        name: {
          title: '网关防护',
        },
        free: {
          checked: false,
          des: '',
        },
        speciality: {
          checked: false,
          des: '',
        },
        enterprise: {
          checked: true,
          des: '集群、路由级别流量防护',
        },
      },
      {
        name: {
          title: '离群实例摘除',
        },
        free: {
          checked: false,
          des: '',
        },
        speciality: {
          checked: false,
          des: '',
        },
        enterprise: {
          checked: true,
          des: ''
        },
      },
      {
        name: {
          title: '熔断降级',
        },
        free: {
          checked: false,
          des: '',
        },
        speciality: {
          checked: false,
          des: '',
        },
        enterprise: {
          checked: true,
          des: ''
        },
      },
    ]
  },
  {
    title: "安全性", data: [
      {
        name: {
          title: 'TLS 全链路加密',
        },
        free: {
          checked: false,
          des: ''
        },
        speciality: {
          checked: false,
          des: ''
        },
        enterprise: {
          checked: true,
          des: ''
        },
      },
      {
        name: {
          title: '服务调用时的认证鉴权',
        },
        free: {
          checked: false,
          des: ''
        },
        speciality: {
          checked: false,
          des: ''
        },
        enterprise: {
          checked: true,
          des: ''
        },
      },
    ]
  },
]