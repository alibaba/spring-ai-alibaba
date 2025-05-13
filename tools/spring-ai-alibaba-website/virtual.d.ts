
declare module 'virtual:starlight/user-config' {
	const Config: import('@astrojs/starlight/types').StarlightConfig;
	export default Config;
}

declare module 'virtual:starlight/user-images' {
	type ImageMetadata = import('astro').ImageMetadata;
	export const logos: {
		dark?: ImageMetadata;
		light?: ImageMetadata;
	};
}

declare module 'qs'

declare module 'virtual:starlight/components' {
	export const Badge: typeof import('@astrojs/starlight/components/Badge.astro').default;
	export const Banner: typeof import('@astrojs/starlight/components/Banner.astro').default;
	export const ContentPanel: typeof import('@astrojs/starlight/components/ContentPanel.astro').default;
	export const PageTitle: typeof import('@astrojs/starlight/components/PageTitle.astro').default;
	export const FallbackContentNotice: typeof import('@astrojs/starlight/components/FallbackContentNotice.astro').default;

	export const Footer: typeof import('@astrojs/starlight/components/Footer.astro').default;
	export const LastUpdated: typeof import('@astrojs/starlight/components/LastUpdated.astro').default;
	export const Pagination: typeof import('@astrojs/starlight/components/Pagination.astro').default;
	export const EditLink: typeof import('@astrojs/starlight/components/EditLink.astro').default;

	export const Header: typeof import('@astrojs/starlight/components/Header.astro').default;
	export const LanguageSelect: typeof import('@astrojs/starlight/components/LanguageSelect.astro').default;
	export const Search: typeof import('@astrojs/starlight/components/Search.astro').default;
	export const SiteTitle: typeof import('@astrojs/starlight/components/SiteTitle.astro').default;
	export const SocialIcons: typeof import('@astrojs/starlight/components/SocialIcons.astro').default;
	export const ThemeSelect: typeof import('@astrojs/starlight/components/ThemeSelect.astro').default;

	export const Head: typeof import('@astrojs/starlight/components/Head.astro').default;
	export const Hero: typeof import('@astrojs/starlight/components/Hero.astro').default;
	export const MarkdownContent: typeof import('@astrojs/starlight/components/MarkdownContent.astro').default;

	export const PageSidebar: typeof import('@astrojs/starlight/components/PageSidebar.astro').default;
	export const TableOfContents: typeof import('@astrojs/starlight/components/TableOfContents.astro').default;

	export const MobileTableOfContents: typeof import('@astrojs/starlight/components/MobileTableOfContents.astro').default;

	export const Sidebar: typeof import('@astrojs/starlight/components/Sidebar.astro').default;
	export const SkipLink: typeof import('@astrojs/starlight/components/SkipLink.astro').default;
	export const ThemeProvider: typeof import('@astrojs/starlight/components/ThemeProvider.astro').default;

	export const PageFrame: typeof import('@astrojs/starlight/components/PageFrame.astro').default;
	export const MobileMenuToggle: typeof import('@astrojs/starlight/components/MobileMenuToggle.astro').default;
	export const MobileMenuFooter: typeof import('@astrojs/starlight/components/MobileMenuFooter.astro').default;

	export const TwoColumnContent: typeof import('@astrojs/starlight/components/TwoColumnContent.astro').default;
}

