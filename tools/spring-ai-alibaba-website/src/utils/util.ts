import type { CollectionEntry } from 'astro:content';

export function getLanguageFromURL(pathname: string) {
	// const langCodeMatch = pathname.match(/\/([a-z]{2}-?[a-z]{0,2})\//);
	const langCodeMatch = pathname.match(/\/([a-z]{2}-?[a-z]{0,2})/);
	return langCodeMatch ? langCodeMatch[1] : 'zh-cn';
}

/** Remove \ and / from beginning of string */
export function removeLeadingSlash(path: string) {
	return path.replace(/^[/\\]+/, '');
}

/** Remove \ and / from end of string */
export function removeTrailingSlash(path: string) {
	return path.replace(/[/\\]+$/, '');
}

/** Get a page’s slug, without the language prefix (e.g. `'en/migrate'` => `'migrate'`). */
export const stripLangFromSlug = (slug: CollectionEntry<'docs'>['slug']) =>
	slug.split('/').slice(1).join('/');

/** Get a page’s lang tag from its slug (e.g. `'en/migrate'` => `'en'`). */
export const getLangFromSlug = (slug: CollectionEntry<'docs'>['slug']) => slug.split('/')[0];


// 判断是否为safari浏览器
export const isSafari = () => {
    const userAgent = navigator?.userAgent?.toLowerCase();
    return userAgent.includes('safari') && !userAgent.includes('chrome');
  };