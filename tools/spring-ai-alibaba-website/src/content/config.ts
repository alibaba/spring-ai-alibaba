import { defineCollection, z } from 'astro:content';
import { docsSchema, i18nSchema } from '@astrojs/starlight/schema';

const blog = defineCollection({
	schema: z.object({
		title: z.string(),
		description: z.string(),
		keywords: z.string().or(z.array(z.string().or(z.number())).optional()),
		// Transform string to Date object
		date: z.string(),
		updatedDate: z.coerce.date().optional(),
		heroImage: z.string().optional(),
		author: z.string().optional(),
		category: z.string().optional()
	}),
});

const faq = defineCollection({
	schema: z.object({
		title: z.string(),
		description: z.string(),
		keywords: z.string().or(z.array(z.string().or(z.number())).optional()),
		// Transform string to Date object
		date: z.string(),
		updatedDate: z.coerce.date().optional(),
		heroImage: z.string().optional(),
		author: z.string().optional(),
		category: z.string().optional(),
		tags: z.string().or(z.array(z.string().or(z.number())).optional()),
	}),
});

const learn = defineCollection({
	schema: z.object({
		title: z.string(),
		description: z.string(),
		keywords: z.string().or(z.array(z.string().or(z.number())).optional()),
		// Transform string to Date object
		date: z.string(),
		updatedDate: z.coerce.date().optional(),
		heroImage: z.string().optional(),
		author: z.string().optional(),
		category: z.string().optional()
	}),
});

const download = defineCollection({
	schema: z.object({
		title: z.string(),
		order: z.number(),
	}),
});


export const collections = {
	docs: defineCollection({
		schema: docsSchema({
			extend: z.object({
				keywords: z.string().or(z.array(z.string().or(z.number())).optional()),
				position: z.number().optional(),
			}),
		})
	}),
	i18n: defineCollection({ type: 'data', schema: i18nSchema() }),
	blog
};
