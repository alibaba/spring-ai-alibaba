import { defineConfig } from 'astro/config';
import sitemap from '@astrojs/sitemap';
import starlight from '@astrojs/starlight';
import partytown from '@astrojs/partytown';
import { autoImportComponents } from "@serverless-cd/goat-ui/src/utils";
import locales from './src/i18n/languages';
import tailwind from "@astrojs/tailwind";
import icon from "astro-icon";
import preact from "@astrojs/preact";
import rehypeExternalLinks from 'rehype-external-links'

import { remarkRemoveMdLinks, remarkRemovePlainLanguageCode, remarkRemoveRepeatHeader, addPrefixImageLink, setLinkReferrer } from './src/utils/frontmatter.mjs';
import { ANALYTICS, SITE } from './src/utils/config.ts';
import goatConfig from './goat.config';
import { starlightAsides } from './node_modules/@astrojs/starlight/integrations/asides';
import topLevelAwait from "vite-plugin-top-level-await";

import icon from 'astro-icon';

const whenExternalScripts = (items = []) =>
  ANALYTICS.vendors.googleAnalytics.id && ANALYTICS.vendors.googleAnalytics.partytown
    ? Array.isArray(items)
      ? items.map((item) => item())
      : [items()]
    : [];

// https://astro.build/config
export default defineConfig({
	site: process.env.DEPLOY_SITE || SITE.site,
	base: SITE.base,
	trailingSlash: SITE.trailingSlash,
	image: {
		domain: ["img.alicdn"]
	},
	integrations: [
		starlight({
			title: SITE.name + '官网',
			favicon: '/sca-icon.svg',
			expressiveCode: {
				themes: ['github-dark'], //TODO: 待调研
			},
			components: {
				TableOfContents: './src/components/starlight/TableOfContents/index.astro',
				Header: './src/components/starlight/Header.astro',
				Head: './src/components/starlight/Head.astro',
				Sidebar: './src/components/starlight/Sidebar.astro',
				PageFrame: "./src/components/starlight/PageFrame.astro",
				SkipLink: "./src/components/starlight/SkipLink.astro",
				TwoColumnContent: "./src/components/starlight/TwoColumnContent.astro",
				PageSidebar: "./src/components/starlight/PageSidebar.astro",
				PageTitle: "./src/components/starlight/PageTitle.astro",
				ContentPanel: "./src/components/starlight/ContentPanel.astro",
				Pagination: "./src/components/starlight/Pagination.astro",
				Banner: "./src/components/starlight/Banner.astro",
			},
			editLink: {
				baseUrl: SITE.websiteGithubUrl,
			},
			locales,
			customCss: ['./src/style/global.css','./src/style/fonts.css'],
		}),
		autoImportComponents(),
		tailwind({ applyBaseStyles: false }),
		icon({
			tabler: ['book', 'pencil'],
			'ant-design':['github-filled'],
			basil:['document-outline']
		}),
		{
			name: '@goat:config',
			hooks: {
				"astro:server:setup": async (options) => {
					await goatConfig();
				},
				"astro:build:setup": async (options) => {
					await goatConfig();
				}
			}
		},
		preact({ compat: true }),
		...whenExternalScripts(() =>
		partytown({
		  config: { forward: ['dataLayer.push'] },
		})
	  ),
	],
	markdown: {
		rehypePlugins: [
			// 在这里添加 rehype-external-links 插件配置
			[rehypeExternalLinks, {
				target: '_blank'
			}]],
		remarkPlugins: [
			remarkRemoveMdLinks,
			remarkRemovePlainLanguageCode,
			remarkRemoveRepeatHeader,
			addPrefixImageLink,
			starlightAsides,
			setLinkReferrer
		]
	},
	vite: {
		build: {
		  target: "chrome68",
		},
		plugins: [topLevelAwait()],
	},
	// TODO: 梳理redirects
	redirects: {
		'/zh-cn/': '/',
		'/en-us/': '/en/',
		'/[...slug].html': '/[...slug]',
		'/docs/': '/docs/dev/overview/',
	}
});