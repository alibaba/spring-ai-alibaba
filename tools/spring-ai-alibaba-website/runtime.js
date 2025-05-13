import fs from "fs/promises";
import path from "path";
import { fileURLToPath } from 'url';
const curFilename = fileURLToPath(import.meta.url);
const curDirname = path.dirname(curFilename);

const runtimePath = path.join(curDirname, 'node_modules/astro/dist/content/runtime.js');

/**
 * @description: 替换 runtime.js中的 entries取值
 */
const originContent = await fs.readFile(runtimePath);
const replacedContent = originContent.toString().replace(
    'if (!import.meta.env?.DEV && cacheEntriesByCollection.has(collection)) {',
    'if (false && cacheEntriesByCollection.has(collection)) {'
);

await fs.writeFile(runtimePath, replacedContent);
