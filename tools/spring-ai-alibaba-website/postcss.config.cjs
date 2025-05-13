const path = require('path');

module.exports = {
	plugins: {
		//配置css嵌套能力
		'tailwindcss/nesting': '',
		tailwindcss: {
			config: path.join(__dirname, 'tailwind.config.mjs'),
		},
		autoprefixer: {},
	},
};
