import { expect, test } from "bun:test";


test( 'svg parsing', () => {

  const svgContent = `
  <svg width="100" height="100"><circle cx="50" cy="50" r="40" stroke="black" stroke-width="3" fill="red" /></svg>
  `;
  
  const regex = /(<svg[^>]*>)([\s\S]*?)(<\/svg>)/;
  const match = svgContent.match(regex);
  
  expect( match ).not.toBeNil()
  expect( match?.length ).toBe(4)

  expect( match![1] ).toBe( '<svg width="100" height="100">' )
  expect( match![2] ).toBe( '<circle cx="50" cy="50" r="40" stroke="black" stroke-width="3" fill="red" />')
  expect( match![3] ).toBe( '</svg>' )
  
})