import fs from "fs/promises";
import path from "path";
import { fileURLToPath } from 'url';
const curFilename = fileURLToPath(import.meta.url);
const curDirname = path.dirname(curFilename);

const starlightPath = path.join(curDirname, 'node_modules/@astrojs/starlight');

// 获取docs目录下的一级文件夹
let docsFile = await fs.readdir('src/content/docs');
docsFile = docsFile.filter((item) => item !== '.DS_Store')

/**
 * @description: 替换 utils/route-data.ts
 * 传递 categories 参数
 */
const replaceRouteData = async () => {
	const originFile = path.join(starlightPath, "/utils/route-data.ts");
	const originContent = await fs.readFile(originFile);
	const replacedContent = originContent.toString().replace(
		/const sidebar = getSidebar.*?;\n/,
		'const sidebar = getSidebar(url.pathname, locale, props.categories);\n'
	);
	await fs.writeFile(originFile, replacedContent);
}


/**
 * @description: 替换 utils/navigation.ts
 *
 */
const replaceNavigation = async () => {
	/**
	 * 获取当前页面的 sidebar， 左侧菜单动态加载
	 * 根据页面路由获取sidebar
	 */
	const originFile = path.join(starlightPath, "/utils/navigation.ts");
	const originContent = await fs.readFile(originFile);
	const sideBarRegex = /export function getSidebar\(pathname\: string\, locale\: string \| undefined\).+\n(.+)/;
	const sideBarContent = originContent.toString().replace(
		sideBarRegex,
		`export function getSidebar(pathname: string, locale: string | undefined, categories: any): SidebarEntry[] {
		const routes = getLocaleRoutes(locale);
		const versionRegex = /\\/docs\\/(${docsFile.join('|')})\\//;
		const match = versionRegex.exec(pathname);
		const version = match ? match[1] : 'latest';
		if(categories && categories[version]){
			return categories[version].map((group) => configItemToEntry(group, pathname, locale, routes));
		}\n`
	);

	/**
	 * 核心的 sidebar Link链接构建
	 */
	/**
	 * /v2/en/quickstart/quick-start-docker.html => /docs/v2/quickstart/quick-start-docker.html
	 * /v2/zh-cn/quickstart/quick-start-kubernetes.html => /docs/v2/quickstart/quick-start-docker.html
	 */
	const sideBarLinkRegex = /href = formatPath\(href\).+\n.+\}/;
	const sideBarLinkContent = sideBarContent.replace(
		sideBarLinkRegex,
		`href = formatPath(href);
		const regex = /\\/(${docsFile.join('|')})\\/(en|zh-cn)/;
		href = href.replace(regex, '/docs/$1');
		}`
	);

	/**
	 * 核心的 localeDir链接构建
	 */
	/**
	 * /v2/en/quickstart/quick-start-docker.html => /docs/v2/quickstart/quick-start-docker.html
	 * /v2/zh-cn/quickstart/quick-start-kubernetes.html => /docs/v2/quickstart/quick-start-docker.html
	 */
	const localeDirRegex = /const localeDir = locale \? locale \+ \'\/\' \+ directory \: directory\;/;
	const localeDirContent = sideBarLinkContent.replace(
		localeDirRegex,
		`const regex =  /(${docsFile.join('|')})\\/(en|zh-cn)/;
		const localeDir = locale ? locale + '/' + directory.replace(regex, "$1/"+locale) : directory;`
	);


	// 增加slugToPathname的入参，为版本号中有.的版本设置正确的sidebar路径
	// slug.id => docs/2.2.x/quickstart/quick-start-docker.md
	// slug.slug => 22x/quickstart/quick-start-docker
	const linkFromRouteRegex = /function linkFromRoute\(route: Route, currentPathname: string\): Link {[\s\S]*?}/;

	const linkFromRouteContent = localeDirContent.replace(
		linkFromRouteRegex,
		`function linkFromRoute(route: Route, currentPathname: string): Link {
			return makeLink(
				slugToPathname(route.slug,route.id),
				route.entry.data.sidebar.label || route.entry.data.title,
				currentPathname,
				route.entry.data.sidebar.badge,
				route.entry.data.sidebar.attrs
			);
		}\n`
	)

	await fs.writeFile(originFile, linkFromRouteContent);
}

/**
 * @description: 替换 index.astro
 * 1. 动态替换核心路由能力
 * 2. 动态集成siderBar能力
 */
const replaceIndexAstro = async () => {
	const originFile = path.join(starlightPath, "index.astro");
	const replacedContent = await fs.readFile('./template/index.startlight.tpl');
	const updateContent =replacedContent.toString().replaceAll(
		'DOCSREGEX',
		`${docsFile.join('|')}`
	)
	await fs.writeFile(originFile, updateContent);
}

/**
 * @description: 替换 404.astro
 */
const replace404Astro = async () => {
	const originFile = path.join(starlightPath, "404.astro");
	const replacedContent = await fs.readFile('./template/404.startlight.tpl');
	await fs.writeFile(originFile, replacedContent.toString());
}


/**
 * @description: 替换 utils/slugs.ts
 */
const replaceSlugs = async () => {
	const originFile = path.join(starlightPath, "/utils/slugs.ts");
	const originContent = await fs.readFile(originFile);

	/**
	 * 获取当前页面的 id，生成正确的 pathname
	 * 主要为2.2.x版本的autogenerate生成正确的sidebar
	 */
	const linkFromRouteRegex = /export function slugToPathname\(slug\: string\)\: string {[\s\S]*?}/;
	const linkFromRouteContent = originContent.toString().replace(
		linkFromRouteRegex,
		`export function slugToPathname(slug: string, id: string): string {
			// 2.2.x/zh-cn/overview/version-explain.md
			let param = slugToParam(slug);
			const regex = /developer|ebook|1.0.0-M3.2|1.0.0-M5.1|1.0.0-M6.1|dev/;
			const [curVersion,lang, ...rest] = id.split('/');
			const match = regex.exec(curVersion);
			if (match) {
				rest[rest.length-1] = rest[rest.length-1].replace(/.(md|mdx)$/, "")
				param = "docs/" + curVersion + "/" + rest.join("/")
			}
			return param ? '/' + param + '/' : '/';
		}\n`
	)

	await fs.writeFile(originFile, linkFromRouteContent);
}


export default async () => {
	await replaceRouteData();
	await replaceNavigation();
	await replaceIndexAstro();
	await replace404Astro();
	await replaceSlugs()
}