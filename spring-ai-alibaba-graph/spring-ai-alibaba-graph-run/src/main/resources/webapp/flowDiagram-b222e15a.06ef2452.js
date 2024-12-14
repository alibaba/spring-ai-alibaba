function e(e,t,l,r){Object.defineProperty(e,t,{get:l,set:r,enumerable:!0,configurable:!0})}var t=globalThis.parcelRequire0031,l=t.register;l("fUGrY",function(l,r){e(l.exports,"isSubgraph",()=>i),e(l.exports,"edgeToId",()=>n),e(l.exports,"applyStyle",()=>c),e(l.exports,"applyClass",()=>p),e(l.exports,"applyTransition",()=>b);var a=t("4VY3C"),o=t("3rtvu");function i(e,t){return!!e.children(t).length}function n(e){return d(e.v)+":"+d(e.w)+":"+d(e.name)}var s=/:/g;function d(e){return e?String(e).replace(s,"\\:"):""}function c(e,t){t&&e.attr("style",t)}function p(e,t,l){t&&e.attr("class",t).attr("class",l+" "+e.attr("class"))}function b(e,t){var l=t.graph();if(o.default(l)){var r=l.transition;if(a.default(r))return r(e)}return e}}),l("ilMBJ",function(l,r){e(l.exports,"addHtmlLabel",()=>o);var a=t("fUGrY");function o(e,t){var l=e.append("foreignObject").attr("width","100000"),r=l.append("xhtml:div");r.attr("xmlns","http://www.w3.org/1999/xhtml");var o=t.label;switch(typeof o){case"function":r.insert(o);break;case"object":r.insert(function(){return o});break;default:r.html(o)}a.applyStyle(r,t.labelStyle),r.style("display","inline-block"),r.style("white-space","nowrap");var i=r.node().getBoundingClientRect();return l.attr("width",i.width).attr("height",i.height),l}}),l("5MNkI",function(l,r){e(l.exports,"f",()=>w),e(l.exports,"a",()=>h);var a=t("4LkSm"),o=t("2YFJl"),i=t("4jcZX"),n=t("4FlE0"),s=t("ilMBJ"),d=t("jDsny"),c=t("b1Vti");let p={},b=async function(e,t,l,r,a,o){let n=r.select(`[id="${l}"]`);for(let l of Object.keys(e)){let r;let d=e[l],c="default";d.classes.length>0&&(c=d.classes.join(" ")),c+=" flowchart-label";let p=(0,i.k)(d.styles),b=void 0!==d.text?d.text:d.id;if((0,i.l).info("vertex",d,d.labelType),"markdown"===d.labelType)(0,i.l).info("vertex",d,d.labelType);else if((0,i.m)((0,i.c)().flowchart.htmlLabels)){let e={label:b};(r=(0,s.addHtmlLabel)(n,e).node()).parentNode.removeChild(r)}else{let e=a.createElementNS("http://www.w3.org/2000/svg","text");for(let t of(e.setAttribute("style",p.labelStyle.replace("color:","fill:")),b.split(i.e.lineBreakRegex))){let l=a.createElementNS("http://www.w3.org/2000/svg","tspan");l.setAttributeNS("http://www.w3.org/XML/1998/namespace","xml:space","preserve"),l.setAttribute("dy","1em"),l.setAttribute("x","1"),l.textContent=t,e.appendChild(l)}r=e}let u=0,f="";switch(d.type){case"round":u=5,f="rect";break;case"square":case"group":default:f="rect";break;case"diamond":f="question";break;case"hexagon":f="hexagon";break;case"odd":case"odd_right":f="rect_left_inv_arrow";break;case"lean_right":f="lean_right";break;case"lean_left":f="lean_left";break;case"trapezoid":f="trapezoid";break;case"inv_trapezoid":f="inv_trapezoid";break;case"circle":f="circle";break;case"ellipse":f="ellipse";break;case"stadium":f="stadium";break;case"subroutine":f="subroutine";break;case"cylinder":f="cylinder";break;case"doublecircle":f="doublecircle"}let w=await (0,i.r)(b,(0,i.c)());t.setNode(d.id,{labelStyle:p.labelStyle,shape:f,labelText:w,labelType:d.labelType,rx:u,ry:u,class:c,style:p.style,id:d.id,link:d.link,linkTarget:d.linkTarget,tooltip:o.db.getTooltip(d.id)||"",domId:o.db.lookUpDomId(d.id),haveCallback:d.haveCallback,width:"group"===d.type?500:void 0,dir:d.dir,type:d.type,props:d.props,padding:(0,i.c)().flowchart.padding}),(0,i.l).info("setNode",{labelStyle:p.labelStyle,labelType:d.labelType,shape:f,labelText:w,rx:u,ry:u,class:c,style:p.style,id:d.id,domId:o.db.lookUpDomId(d.id),width:"group"===d.type?500:void 0,type:d.type,dir:d.dir,props:d.props,padding:(0,i.c)().flowchart.padding})}},u=async function(e,t,l){let r,a;(0,i.l).info("abc78 edges = ",e);let n=0,s={};if(void 0!==e.defaultStyle){let t=(0,i.k)(e.defaultStyle);r=t.style,a=t.labelStyle}for(let l of e){n++;let d="L-"+l.start+"-"+l.end;void 0===s[d]?s[d]=0:s[d]++,(0,i.l).info("abc78 new entry",d,s[d]);let c=d+"-"+s[d];(0,i.l).info("abc78 new link id to be used is",d,c,s[d]);let b="LS-"+l.start,u="LE-"+l.end,f={style:"",labelStyle:""};switch(f.minlen=l.length||1,"arrow_open"===l.type?f.arrowhead="none":f.arrowhead="normal",f.arrowTypeStart="arrow_open",f.arrowTypeEnd="arrow_open",l.type){case"double_arrow_cross":f.arrowTypeStart="arrow_cross";case"arrow_cross":f.arrowTypeEnd="arrow_cross";break;case"double_arrow_point":f.arrowTypeStart="arrow_point";case"arrow_point":f.arrowTypeEnd="arrow_point";break;case"double_arrow_circle":f.arrowTypeStart="arrow_circle";case"arrow_circle":f.arrowTypeEnd="arrow_circle"}let w="",g="";switch(l.stroke){case"normal":w="fill:none;",void 0!==r&&(w=r),void 0!==a&&(g=a),f.thickness="normal",f.pattern="solid";break;case"dotted":f.thickness="normal",f.pattern="dotted",f.style="fill:none;stroke-width:2px;stroke-dasharray:3;";break;case"thick":f.thickness="thick",f.pattern="solid",f.style="stroke-width: 3.5px;fill:none;";break;case"invisible":f.thickness="invisible",f.pattern="solid",f.style="stroke-width: 0;fill:none;"}if(void 0!==l.style){let e=(0,i.k)(l.style);w=e.style,g=e.labelStyle}f.style=f.style+=w,f.labelStyle=f.labelStyle+=g,void 0!==l.interpolate?f.curve=(0,i.n)(l.interpolate,o.curveLinear):void 0!==e.defaultInterpolate?f.curve=(0,i.n)(e.defaultInterpolate,o.curveLinear):f.curve=(0,i.n)(p.curve,o.curveLinear),void 0===l.text?void 0!==l.style&&(f.arrowheadStyle="fill: #333"):(f.arrowheadStyle="fill: #333",f.labelpos="c"),f.labelType=l.labelType,f.label=await (0,i.r)(l.text.replace(i.e.lineBreakRegex,"\n"),(0,i.c)()),void 0===l.style&&(f.style=f.style||"stroke: #333; stroke-width: 1.5px;fill:none;"),f.labelStyle=f.labelStyle.replace("color:","fill:"),f.id=c,f.classes="flowchart-link "+b+" "+u,t.setEdge(l.start,l.end,f,n)}},f=async function(e,t,l,r){let s,d;(0,i.l).info("Drawing flowchart");let c=r.db.getDirection();void 0===c&&(c="TD");let{securityLevel:p,flowchart:f}=(0,i.c)(),w=f.nodeSpacing||50,g=f.rankSpacing||50;"sandbox"===p&&(s=(0,o.select)("#i"+t));let h="sandbox"===p?(0,o.select)(s.nodes()[0].contentDocument.body):(0,o.select)("body"),y="sandbox"===p?s.nodes()[0].contentDocument:document,k=new a.Graph({multigraph:!0,compound:!0}).setGraph({rankdir:c,nodesep:w,ranksep:g,marginx:0,marginy:0}).setDefaultEdgeLabel(function(){return{}}),x=r.db.getSubGraphs();(0,i.l).info("Subgraphs - ",x);for(let e=x.length-1;e>=0;e--)d=x[e],(0,i.l).info("Subgraph - ",d),r.db.addVertex(d.id,{text:d.title,type:d.labelType},"group",void 0,d.classes,d.dir);let v=r.db.getVertices(),m=r.db.getEdges();(0,i.l).info("Edges",m);let S=0;for(S=x.length-1;S>=0;S--){d=x[S],(0,o.selectAll)("cluster").append("text");for(let e=0;e<d.nodes.length;e++)(0,i.l).info("Setting up subgraphs",d.nodes[e],d.id),k.setParent(d.nodes[e],d.id)}await b(v,k,t,h,y,r),await u(m,k);let T=h.select(`[id="${t}"]`),_=h.select("#"+t+" g");if(await (0,n.r)(_,k,["point","circle","cross"],"flowchart",t),(0,i.u).insertTitle(T,"flowchartTitleText",f.titleTopMargin,r.db.getDiagramTitle()),(0,i.o)(k,T,f.diagramPadding,f.useMaxWidth),r.db.indexNodes("subGraph"+S),!f.htmlLabels)for(let e of y.querySelectorAll('[id="'+t+'"] .edgeLabel .label')){let t=e.getBBox(),l=y.createElementNS("http://www.w3.org/2000/svg","rect");l.setAttribute("rx",0),l.setAttribute("ry",0),l.setAttribute("width",t.width),l.setAttribute("height",t.height),e.insertBefore(l,e.firstChild)}Object.keys(v).forEach(function(e){let l=v[e];if(l.link){let r=(0,o.select)("#"+t+' [id="'+e+'"]');if(r){let e=y.createElementNS("http://www.w3.org/2000/svg","a");e.setAttributeNS("http://www.w3.org/2000/svg","class",l.classes.join(" ")),e.setAttributeNS("http://www.w3.org/2000/svg","href",l.link),e.setAttributeNS("http://www.w3.org/2000/svg","rel","noopener"),"sandbox"===p?e.setAttributeNS("http://www.w3.org/2000/svg","target","_top"):l.linkTarget&&e.setAttributeNS("http://www.w3.org/2000/svg","target",l.linkTarget);let t=r.insert(function(){return e},":first-child"),a=r.select(".label-container");a&&t.append(function(){return a.node()});let o=r.select(".label");o&&t.append(function(){return o.node()})}}})},w={setConf:function(e){for(let t of Object.keys(e))p[t]=e[t]},addVertices:b,addEdges:u,getClasses:function(e,t){return t.db.getClasses()},draw:f},g=(e,t)=>{let l=d.default,r=l(e,"r"),a=l(e,"g"),o=l(e,"b");return c.default(r,a,o,t)},h=e=>`.label {
    font-family: ${e.fontFamily};
    color: ${e.nodeTextColor||e.textColor};
  }
  .cluster-label text {
    fill: ${e.titleColor};
  }
  .cluster-label span,p {
    color: ${e.titleColor};
  }

  .label text,span,p {
    fill: ${e.nodeTextColor||e.textColor};
    color: ${e.nodeTextColor||e.textColor};
  }

  .node rect,
  .node circle,
  .node ellipse,
  .node polygon,
  .node path {
    fill: ${e.mainBkg};
    stroke: ${e.nodeBorder};
    stroke-width: 1px;
  }
  .flowchart-label text {
    text-anchor: middle;
  }
  // .flowchart-label .text-outer-tspan {
  //   text-anchor: middle;
  // }
  // .flowchart-label .text-inner-tspan {
  //   text-anchor: start;
  // }

  .node .katex path {
    fill: #000;
    stroke: #000;
    stroke-width: 1px;
  }

  .node .label {
    text-align: center;
  }
  .node.clickable {
    cursor: pointer;
  }

  .arrowheadPath {
    fill: ${e.arrowheadColor};
  }

  .edgePath .path {
    stroke: ${e.lineColor};
    stroke-width: 2.0px;
  }

  .flowchart-link {
    stroke: ${e.lineColor};
    fill: none;
  }

  .edgeLabel {
    background-color: ${e.edgeLabelBackground};
    rect {
      opacity: 0.5;
      background-color: ${e.edgeLabelBackground};
      fill: ${e.edgeLabelBackground};
    }
    text-align: center;
  }

  /* For html labels only */
  .labelBkg {
    background-color: ${g(e.edgeLabelBackground,.5)};
    // background-color: 
  }

  .cluster rect {
    fill: ${e.clusterBkg};
    stroke: ${e.clusterBorder};
    stroke-width: 1px;
  }

  .cluster text {
    fill: ${e.titleColor};
  }

  .cluster span,p {
    color: ${e.titleColor};
  }
  /* .cluster div {
    color: ${e.titleColor};
  } */

  div.mermaidTooltip {
    position: absolute;
    text-align: center;
    max-width: 200px;
    padding: 2px;
    font-family: ${e.fontFamily};
    font-size: 12px;
    background: ${e.tertiaryColor};
    border: 1px solid ${e.border2};
    border-radius: 2px;
    pointer-events: none;
    z-index: 100;
  }

  .flowchartTitleText {
    text-anchor: middle;
    font-size: 18px;
    fill: ${e.textColor};
  }
`});
//# sourceMappingURL=flowDiagram-b222e15a.06ef2452.js.map
