import type { Page } from "astro";

export type PageLink = {
    pageNum: number
    text: string
    href: string
}

export type Ellipsis = {
	text: string
	pageNum?: never
	href?: never
}

// clamps the list of pages to show at most 5 pages
// TODO: the use of inline ellipsis text here is a little gross
export function collapseRange(
	page: Page<unknown>,
	pages: Array<PageLink>,
): Array<PageLink | Ellipsis> {
	const total = pages.length
	const max = 5

	// only need ellipsis if we have more pages than we can display
	const needEllipsis = total > max

	// show start ellipsis if the current page is further away than max - 3 from the first page
	const hasStartEllipsis = needEllipsis && page.currentPage > max - 2
	// show end ellipsis if the current page is further than total - total + 2 from the last page
	const hasEndEllipsis = needEllipsis && page.currentPage < total - 2

	if (!needEllipsis) {
		return pages
	}

	if (hasStartEllipsis && !hasEndEllipsis) {
		return [pages[0], { text: "..." }, ...pages.slice(Math.min(page.currentPage - 2, total - 3))]
	}

	if (!hasStartEllipsis && hasEndEllipsis) {
		return [
			...pages.slice(0, Math.max(3, page.currentPage + 1)),
			{ text: "..." },
			pages[pages.length - 1],
		]
	}

	// we have both start and end ellipsis
	return [
		pages[0],
		{ text: "..." },
		...pages.slice(page.currentPage - 2, page.currentPage + 1),
		{ text: "..." },
		pages[pages.length - 1],
	]
}