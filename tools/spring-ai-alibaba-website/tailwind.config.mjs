const defaultTheme = require('tailwindcss/defaultTheme');
import { goatuiPlugins, getSafelist } from "@serverless-cd/goat-ui/src/plugins"
import { GOAT_UI_CONTENT_PATH } from "@serverless-cd/goat-ui/src/common";
import { UI } from './src/utils/config.ts';

const colorList = UI?.colors;
// 预先设置tailwindcss的safelist，保证动态classname
const safelist = getSafelist({ 
  colorList: Object.keys(colorList)
});

/** @type {import('tailwindcss').Config} */

export default {
  content: [
    "./src/**/*.{astro,html,js,jsx,md,mdx,svelte,ts,tsx,vue}",
    GOAT_UI_CONTENT_PATH,
	],
	safelist: safelist,
  plugins: [...goatuiPlugins,
  ],
  theme: {
		colors: colorList,
    extend: {
      fontFamily: {
				sans: ["Roboto","SourceHanSans","sans-serif"],
				mono: ["Roboto","SourceHanSans","sans-serif"],
      },
      keyframes: {
        fadeByGroup: {
          '0%, 35%, 100%': { opacity: 0 },
    			'5%, 30%': { opacity: 1 }
        }
      },
			animation: {
        'fade-by-group': 'fadeByGroup 9s infinite linear'
      },
			flex: {
				'16': '0 0 16.66%',
			},
			maxWidth: {
				'1/6': '16.66%'
      },
      typography: (theme) => ({
				blogToc: {
					css: {
						a: {
							textDecoration: 'inherit',
							color: theme('colors.info'),
							fontSize: '0.8275rem',
							'&:hover': {
								color: theme('colors.neutral'),
							}
						},
						ul: {
							listStyleType: 'none', /* 去除默认的列表样式 */
							paddingLeft: 0,
							li: {
								borderInlineStart: `1px solid ${theme('colors.error')}`,
								paddingLeft: '0.5rem'
							}
						},
						li: {
							paddingLeft: 0,
						},
					}
				},
				blogTocfold:{
					css: {
						a: {
							textDecoration: 'inherit',
							color: theme('colors.success'),
							fontSize: '0.8275rem',
							'&:hover': {
								color: theme('colors.neutral'),
							}
						},
						ul: {
							listStyleType: 'none', /* 去除默认的列表样式 */
							paddingLeft: 0,
							li: {
								paddingLeft: '0.5rem',
								paddingBottom:'0.5rem'
							}
						},
						li: {
							paddingLeft: 0,
						},
					}
				},
				DEFAULT: {
					css: {
						tbody: {
							tr: {
								borderTop: '1px solid #eee',
								'&:nth-child(odd)': {
									backgroundColor: theme('colors.secondary')
								},
								'&:nth-child(even)': {
									backgroundColor: '#fff',
								},
							}
						}
					}
				}
			})
    }
  },
  daisyui: {
    themes: [
      {
        // you can config light or dark theme color value with an object or as a string, when you set the theme as a string, it means you are using our default theme config.
        // all theme color value can only be a string except 'accent' and 'gray'.
        light: {
          ...colorList
        },
      },
    ],
  },
};