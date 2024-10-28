// resort words in en.ts and zh.ts;
// check words exist in en.ts and zh.ts;

import EN_MAP from "./en";
import ZH_MAP from "./zh";

let sortArr: { label: string; value: any }[] = [];
let checkArr: string[] = [];

function mapToArr() {
  for (let enKey in EN_MAP) {
    sortArr.push({
      label: enKey,
      value: EN_MAP[enKey],
    });
    let zh = ZH_MAP[enKey];
    if (!zh) {
      checkArr.push(enKey);
    }
  }
}

mapToArr();
console.log(sortArr.sort((a, b) => (a.label > b.label ? 1 : -1)));
