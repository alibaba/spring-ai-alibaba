/*
 * @describe: 【 YUQUE_TOKEN=..... node build-ebook.js 】
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from 'url';

const curFilename = fileURLToPath(import.meta.url);
const curDirname = path.dirname(curFilename);
const bookDir = path.resolve(curDirname, 'src/content/docs/ebook/zh-cn');

const request = async (url) => {
    const res = await fetch(url, { headers: { 'X-Auth-Token': process.env.YUQUE_TOKEN } });
    if (!res.ok) throw new Error(res.statusText);
    const data = await res.json();
    if (data.error) {
        throw new Error(data.error)
    } else {
        return data;
    }
};

const toc = await request('https://yuque-api.antfin-inc.com/api/v2/repos/40052648/toc')

const bookToc = toc.data;


let sidebar = [];

const slugList = [
    "srekog", // 推荐序
    "hvraft", // 大纲
    // 第一章
    "pf6ee1", "tnxg5t", "snoxot", "khzqeg", 
    // 第二章
    "hkgsqu", "ngxm99", "bgmd8i", "akq64a", 
    // 第三章
    "somsth", "akszh2", "on0vzt", 
    // 开放到 3.3 小节。以下需要锁定
    // "aaqbnx",
    // "ipeble", "me0tbz", "wh4qgy", "vn6lg3", "ggmmft", "tdbw3b", "pz5pqr", 
    // "euq2fi", "hzzzd9", "assenf", "xqk29p", "gbup3a", "svayai", "muynvi", 
    // "dtdnyy", "ck6xf6", "rv9pn1", "utfh8i", "gg3fvw", "ogtees", "wh1hoh",
    // "wct036", "pd0f3u", "fsdbgy", "cdkqhk", "su8w58", "gamvyg", "uimxlk"
];
// 电子书暂时全部开放、不锁定。
// const slugList = toc.data.map(i => i.slug).filter(i => i !== '#');


/**
 * @description: 生成电子书SideBar侧边栏
 * @param {*} tocItem 
 * @param {*} lock 
 * @returns 
 */
function generateSlugSidebar(tocItem, lock) { let itemToc = {
        label: tocItem.title,
        translations: {
            en: ''
        },
        items: []
    };

    if (tocItem.slug !== '#') {
        itemToc.link = `docs/ebook/${tocItem.slug}/`;
        if (lock) {
            itemToc = {
                ...itemToc,
                attrs: {
                    lock: true
                }
            };
        };
    };
    return itemToc;
};


/**
 * @description: 生成电子书内容
 * @param {*} tocItem 
 */
const generateEbookContent = async (tocItem) => {
    let lock = undefined;
    if (tocItem.slug !== '#') {
        slugList.includes(tocItem.slug) ? lock = false : lock = true;
    };
    if (tocItem.depth == '1') {
        sidebar.push(generateSlugSidebar(tocItem, lock));
    };
    if (tocItem.depth == '2') {
        sidebar[sidebar.length - 1].items.push(generateSlugSidebar(tocItem, lock));
    };
    if (tocItem.depth == '3') {
        // 取最后一个对象
        const lastItem = sidebar[sidebar.length - 1];
        // 取最后一个对象的items中的最后一个item
        const lastSecItem = lastItem.items[lastItem.items.length - 1];
        lastSecItem.items.push(
            generateSlugSidebar(tocItem, lock)
        );
    };

    if (tocItem.slug) {
        const getMdContent = (body, frontMatter={}) => `---
title: ${frontMatter.title || "SCA Eook"}
keywords: [ SCA ]
description: ${frontMatter.description || "SCA Eook"}
---
${body}`;

        if (slugList.includes(tocItem.slug)) {
            const bookItem = await request(`https://yuque-api.antfin-inc.com/api/v2/repos/ad3swf/urevs4/docs/${tocItem.slug}`);
            const body = bookItem.data.body;
            if (body) {
                fs.promises.writeFile(path.resolve(bookDir, `${tocItem.slug}.mdx`), getMdContent(body, bookItem.data), 'utf8')
            };
        } else if(tocItem.slug !== '#') {
            const lockContent = `
import Lock from "@components/common/EbookLock.astro";

<Lock />`;

            fs.promises.writeFile(
                path.resolve(bookDir, `${tocItem.slug}.mdx`), 
                getMdContent(lockContent, tocItem), 
                'utf8'
            );
        };
    };
};


if(fs.existsSync(bookDir)) {
    await fs.promises.rm(bookDir, { recursive: true });
    await fs.promises.mkdir(bookDir, { recursive: true });
} else {
    await fs.promises.mkdir(bookDir, { recursive: true });
};

for (const tocItem of bookToc) {
    await generateEbookContent(tocItem);
};

const filePath = path.resolve(curDirname, 'src/content/docs/ebook/_sidebar.json');

fs.promises.writeFile(
    filePath, 
    JSON.stringify(sidebar, null, 2), 
    'utf8'
).then(() => {
    console.log('Successfully generated JavaScript file:', filePath);
}).catch((err) => {
    console.error('Error writing file:', err);
});
