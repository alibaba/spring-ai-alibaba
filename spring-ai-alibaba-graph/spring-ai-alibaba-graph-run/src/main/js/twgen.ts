#! /usr/bin/env node

import * as fs from "fs/promises";
import * as path from "path";

const output = path.join("src", "twlit.js");

async function findFirstCssFile( folder: string ) {
  console.log(`Reading css file from ${folder}`);
  try {
    const files = await fs.readdir(folder);
    const cssFile = files.find(file => path.extname(file) === '.css');
    if (cssFile) {
      console.log(`found css ${cssFile}`);
      return path.join( folder, cssFile );
    }
    throw new Error(`No CSS file found in ${folder}`)
  } catch (error) {
    throw error
  }
}

const processCss = async (input:string) => {
  try {
    let contents = await fs.readFile(input, "utf8");

    const cleanContents = contents.replaceAll("`", "").replaceAll("\\", "\\\\");

    const litContents = `
import { css } from 'lit';
const TWStyles = css\` 
${cleanContents} 
\`;
export default TWStyles;
`
;
    await fs.writeFile(output, litContents);

    console.log(`TWLit - wrote to file ${output}`);

  } catch (e: any) {
    console.error(e.message);
  }

}

async function start( watch = true ) {

  console.log(`Writing to ${output}`);

  const input = await findFirstCssFile('dist');

  processCss(input)

  if( !watch ) return

  const watcher = fs.watch('./dist', {})

  for await (const event of watcher) {
    // console.debug( event.filename, event.eventType  )
    
    if( event.filename === path.basename(input) ) { 
      console.debug( event.filename, event.eventType  )
      processCss(input)
    }
  }
}

const args = process.argv.slice(2);

if( args.length > 0 && args[0] === "--no-watch") {
  start( false )
}
else {
  start()

}
