"use strict";(self.webpackChunk_N_E=self.webpackChunk_N_E||[]).push([[8758],{13508:(e,t,a)=>{function i(e,t){e.accDescr&&t.setAccDescription?.(e.accDescr),e.accTitle&&t.setAccTitle?.(e.accTitle),e.title&&t.setDiagramTitle?.(e.title)}a.d(t,{S:()=>i}),(0,a(31905).K)(i,"populateCommonDb")},78758:(e,t,a)=>{a.d(t,{diagram:()=>A});var i=a(13508),l=a(15911),r=a(83900),n=a(11933),s=a(97967),o=a(31905),c=a(86945),d=a(8186),p=a(42828),h=n.UI.pie,g={sections:new Map,showData:!1,config:h},u=g.sections,f=g.showData,m=structuredClone(h),$=(0,o.K)(()=>structuredClone(m),"getConfig"),x=(0,o.K)(()=>{u=new Map,f=g.showData,(0,n.IU)()},"clear"),w=(0,o.K)(({label:e,value:t})=>{if(t<0)throw Error(`"${e}" has invalid value: ${t}. Negative values are not allowed in pie charts. All slice values must be >= 0.`);u.has(e)||(u.set(e,t),s.R.debug(`added new section: ${e}, with value: ${t}`))},"addSection"),S=(0,o.K)(()=>u,"getSections"),y=(0,o.K)(e=>{f=e},"setShowData"),b=(0,o.K)(()=>f,"getShowData"),C={getConfig:$,clear:x,setDiagramTitle:n.ke,getDiagramTitle:n.ab,setAccTitle:n.SV,getAccTitle:n.iN,setAccDescription:n.EI,getAccDescription:n.m7,addSection:w,getSections:S,setShowData:y,getShowData:b},D=(0,o.K)((e,t)=>{(0,i.S)(e,t),t.setShowData(e.showData),e.sections.map(t.addSection)},"populateDb"),T={parse:(0,o.K)(async e=>{let t=await (0,c.qg)("pie",e);s.R.debug(t),D(t,C)},"parse")},k=(0,o.K)(e=>`
  .pieCircle{
    stroke: ${e.pieStrokeColor};
    stroke-width : ${e.pieStrokeWidth};
    opacity : ${e.pieOpacity};
  }
  .pieCircle.highlighted{
    scale: 1.05;
    opacity: 1;
  }
  .pieCircle.highlightedOnHover:hover{
    transition-duration: 250ms;
    scale: 1.05;
    opacity: 1;
  }
  .pieOuterCircle{
    stroke: ${e.pieOuterStrokeColor};
    stroke-width: ${e.pieOuterStrokeWidth};
    fill: none;
  }
  .pieTitleText {
    text-anchor: middle;
    font-size: ${e.pieTitleTextSize};
    fill: ${e.pieTitleTextColor};
    font-family: ${e.fontFamily};
  }
  .slice {
    font-family: ${e.fontFamily};
    fill: ${e.pieSectionTextColor};
    font-size:${e.pieSectionTextSize};
    // fill: white;
  }
  .legend text {
    fill: ${e.pieLegendTextColor};
    font-family: ${e.fontFamily};
    font-size: ${e.pieLegendTextSize};
  }
`,"getStyles"),v=(0,o.K)(e=>{let t=[...e.values()].reduce((e,t)=>e+t,0),a=[...e.entries()].map(([e,t])=>({label:e,value:t})).filter(e=>e.value/t*100>=1);return(0,d.rL)().value(e=>e.value).sort(null)(a)},"createPieArcs"),A={parser:T,db:C,renderer:{draw:(0,o.K)((e,t,a,i)=>{s.R.debug("rendering pie chart\n"+e);let o=i.db,c=(0,n.D7)(),h=(0,r.$t)(o.getConfig(),c.pie),g=(0,l.D)(t),u=g.append("g");u.attr("transform","translate(225,225)");let{themeVariables:f}=c,[m]=(0,r.I5)(f.pieOuterStrokeWidth);m??=2;let $=h.legendPosition,x=h.textPosition,w=h.donutHole>0&&h.donutHole<=.9?h.donutHole:0,S=(0,d.JL)().innerRadius(185*w).outerRadius(185),y=(0,d.JL)().innerRadius(185*x).outerRadius(185*x),b=u.append("g");b.append("circle").attr("cx",0).attr("cy",0).attr("r",185+m/2).attr("class","pieOuterCircle");let C=o.getSections(),D=v(C),T=[f.pie1,f.pie2,f.pie3,f.pie4,f.pie5,f.pie6,f.pie7,f.pie8,f.pie9,f.pie10,f.pie11,f.pie12],k=0;C.forEach(e=>{k+=e});let A=D.filter(e=>"0"!==(e.data.value/k*100).toFixed(0)),K=(0,p.UM)(T).domain([...C.keys()]);b.selectAll("mySlices").data(A).enter().append("path").attr("d",S).attr("fill",e=>K(e.data.label)).attr("class",e=>{let t="pieCircle";return"hover"===h.highlightSlice?t+=" highlightedOnHover":h.highlightSlice===e.data.label&&(t+=" highlighted"),t}),b.selectAll("mySlices").data(A).enter().append("text").text(e=>(e.data.value/k*100).toFixed(0)+"%").attr("transform",e=>"translate("+y.centroid(e)+")").style("text-anchor","middle").attr("class","slice");let R=u.append("text").text(o.getDiagramTitle()).attr("x",0).attr("y",-200).attr("class","pieTitleText"),O=[...C.entries()].map(([e,t])=>({label:e,value:t})),M=u.selectAll(".legend").data(O).enter().append("g").attr("class","legend");M.append("rect").attr("width",18).attr("height",18).style("fill",e=>K(e.label)).style("stroke",e=>K(e.label)),M.append("text").attr("x",22).attr("y",14).text(e=>o.getShowData()?`${e.label} [${e.value}]`:e.label);let z=Math.max(...M.selectAll("text").nodes().map(e=>e?.getBoundingClientRect().width??0)),E=450,F=490,H=22*O.length;switch($){case"center":M.attr("transform",(e,t)=>"translate("+(-z/2-22)+","+(22*t-22*O.length/2)+")");break;case"top":E+=H,M.attr("transform",(e,t)=>`translate(${-z/2-22}, ${22*t-185})`),b.attr("transform",()=>`translate(0, ${H+22})`);break;case"bottom":E+=H,M.attr("transform",(e,t)=>"translate("+(-z/2-22)+","+(22*t- -207)+")");break;case"left":F+=22+z,M.attr("transform",(e,t)=>"translate(-207,"+(22*t-22*O.length/2)+")"),b.attr("transform",()=>`translate(${z+18+4}, 0)`);break;default:F+=22+z,M.attr("transform",(e,t)=>"translate(216,"+(22*t-22*O.length/2)+")")}let L=R.node()?.getBoundingClientRect().width??0,I=Math.min(0,225-L/2),N=Math.max(F,225+L/2)-I;g.attr("viewBox",`${I} 0 ${N} ${E}`),(0,n.a$)(g,E,N,h.useMaxWidth)},"draw")},styles:k}}}]);