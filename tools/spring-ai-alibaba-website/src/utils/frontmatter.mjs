
import { visit } from 'unist-util-visit';


/**
 * @description: 设置图片链接
 */
export function addPrefixImageLink() {
  return (tree) => {
      visit(tree, 'image', (node) => {
        //   将博客的img链接替换。从相对路径替换至从public文件夹取
          if (node.url.startsWith('img')) {
            node.url = '/' + node.url
          } 
    });
  };
}


/**
 * @description: 设置link与image的referrer
 */
export function setLinkReferrer() {
    return (tree) => {
        visit(tree, 'link', (node) => {
          if (node.type === 'link' ) {
              // 为非图片链接设置referrer
              node.data = node.data || {};
              node.data.hProperties = node.data.hProperties || {};
              node.data.hProperties.referrerpolicy = 'unsafe-url';
          }
      });
  
        visit(tree, 'image', (node) => {
        if (node.type === 'image') {
          // 为图片链接不设置referrer
          node.data = node.data || {};
          node.data.hProperties = node.data.hProperties || {};
          node.data.hProperties.referrerpolicy = 'no-referrer';
        }
      });
    };
  }
  
/**
 * @description: remark插件，删除.md后缀内容
 */
export function remarkRemoveMdLinks() {
	return (tree) => {
		visit(tree, 'link', (node) => {
			// http或者 https 开头的
			if (node.url.startsWith("http://") || node.url.startsWith("https://") || node.url.startsWith("//")) {
				return;
			}

			let isMdxLink = node.url.includes('.mdx')
      
      // Process internal markdown links (.md or .mdx)
      if (node.url.includes('.md') || isMdxLink ) {
        const extensionLength = isMdxLink ? 4 : 3;
        const parts = node.url.split('#');
        parts[0] = parts[0].slice(0, -extensionLength) + '/'; // Remove extension and add trailing slash
        
        // Ensure relative links start with ./
        if (!/^[.~/]/.test(parts[0])) {
          parts[0] = './' + parts[0];
        }
        
        node.url = parts.join('#');
      }

      // Adjust relative paths
      if (/^\.\//.test(node.url)) {
        node.url = node.url.replace(/^\.\//, '../');
      } else if (/^\.\.\//.test(node.url)) {
        node.url = node.url.replace(/^\.\.\//, '../../');
      }
		});
	};
}

/**
 * @description: remark插件，语法高亮插件不支持 plain的语法
 * @refer: https://docs.astro.build/en/guides/markdown-content/#syntax-highlighting
 * @refer: https://github.com/shikijs/shiki/blob/main/docs/languages.md
 */
export function remarkRemovePlainLanguageCode() {
  return (tree) => {
    visit(tree, "code", (node) => {
      if(node?.lang){
        const lang = node.lang.toLowerCase() || '';
        switch (lang) {
          case "plain":
          case "basic":
            node.lang = "";
            break;
          case "Shell":
            node.lang = "sh";
            break;
          case "$xslt":
            node.lang = "xsl";
            break;
          case "protobuf":
            node.lang = "java";
            break;
          default:
            node.lang = lang;
        }
      }

    });
  }
}

/**
 * @description: remark插件，删除重复的h1 的 Header
 * @refer: https://github.com/alvinometric/remark-remove-comments/blob/main/transformer.js
 */
export function remarkRemoveRepeatHeader() {
	return (tree, file) => {
		visit(tree, 'heading', (node, index, parent) => {
			if (node.depth === 1) {
				/**
				 * {
					type: 'heading',
					depth: 2,
					children: [ { type: 'text', value: 'Nacos常规问题', position: [Object] } ],
					position: {
							start: { line: 47, column: 1, offset: 1108 },
							end: { line: 47, column: 13, offset: 1120 }
						}
					}
				 */
				const h1HeaderNode = node.children[0];
				if (h1HeaderNode && h1HeaderNode.type === 'text' && h1HeaderNode.value === file.data.astro.frontmatter.title){
					parent.children.splice(index, 1);
				}
			}
		});
	};
}
