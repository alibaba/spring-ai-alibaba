import fs from "fs/promises";
import path from "path";
import { fileURLToPath } from "url";
const curFilename = fileURLToPath(import.meta.url);
const curDirname = path.dirname(curFilename);

const indexFile = path.join(curDirname, "dist/index.html");
fs.readFile(indexFile, "utf-8").then((data) => {
  //在第一个<script前添加document.getAnimations=[]
  data = data.replace(
    /<script /,
    `<script>var script = document.createElement('script');script.src = 'https://cdnjs.cloudflare.com/ajax/libs/web-animations/2.3.2/web-animations-next.min.js';script.onload=function(){document.getAnimations=window.Element.prototype.getAnimations;};document.head.appendChild(script);</script><script></script><script `
  );
  fs.writeFile(indexFile, data, "utf-8");
});
