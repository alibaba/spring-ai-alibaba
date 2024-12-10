var e=globalThis,t={},a={},s=e.parcelRequire0031;null==s&&((s=function(e){if(e in t)return t[e].exports;if(e in a){var s=a[e];delete a[e];var r={id:e,exports:{}};return t[e]=r,s.call(r.exports,r,r.exports),r.exports}var i=Error("Cannot find module '"+e+"'");throw i.code="MODULE_NOT_FOUND",i}).register=function(e,t){a[e]=t},e.parcelRequire0031=s),s.register;var r=s("hNeh9"),i=s("800sp");const n=async e=>new Promise(t=>setTimeout(t,e));async function*d(e){let t=e.body?.getReader();for(;t;){let{done:e,value:a}=await t.read();if(e)break;yield new TextDecoder().decode(a)}}class o extends i.LitElement{static styles=[r.default,(0,i.css)`
    .container {
      display: flex;
      flex-direction: column;
      row-gap: 10px;
    }

    .commands {
      display: flex;
      flex-direction: row;
      column-gap: 10px;
    }

    .item1 {
      flex-grow: 2;
    }
    .item2 {
      flex-grow: 2;
    }
  `];static properties={url:{type:String,reflect:!0},test:{type:Boolean,reflect:!0},_executing:{state:!0}};url=null;#e;#t=null;constructor(){super(),this.test=!1,this.formMetaData={},this._executing=!1}#a(e){console.debug("thread-updated",e.detail),this.#e=e.detail,this.#t=null,this.requestUpdate()}#s(e){console.debug("onNodeUpdated",e),this.#t=e.detail,this.requestUpdate()}connectedCallback(){if(super.connectedCallback(),this.addEventListener("thread-updated",this.#a),this.addEventListener("node-updated",this.#s),this.test){this.#r();return}this.#i()}disconnectedCallback(){super.disconnectedCallback(),this.removeEventListener("thread-updated",this.#a),this.removeEventListener("node-updated",this.#s)}render(){return(0,i.html)`
        <div class="container">
          ${Object.entries(this.formMetaData).map(([e,t])=>(0,i.html)`<textarea id="${e}" class="textarea textarea-primary" placeholder="${e}"></textarea>`)}
          <div class="commands">
            <button id="submit" ?disabled=${this._executing} @click="${this.#n}" class="btn btn-primary item1">Submit</button>
            <button id="resume" ?disabled=${!this.#t||this._executing} @click="${this.#d}" class="btn btn-secondary item2">
            Resume ${this.#t?"(from "+this.#t?.node+")":""}
            </button>
          </div>
        </div>
        `}async #i(){let e=await fetch(`${this.url}/init`),t=await e.json();console.debug("initData",t),this.dispatchEvent(new CustomEvent("init",{detail:t,bubbles:!0,composed:!0,cancelable:!0})),this.formMetaData=t.args,this.requestUpdate()}async #d(){this._executing=!0;try{if(this.test){await this.#o();return}await this.#c()}finally{this._executing=!1}}async #c(){let e=await fetch(`${this.url}/stream?thread=${this.#e}&resume=true&node=${this.#t?.node}&checkpoint=${this.#t?.checkpoint}`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(this.#t?.data)});for await(let t of(this.#t=null,d(e)))console.debug(t),this.dispatchEvent(new CustomEvent("result",{detail:JSON.parse(t),bubbles:!0,composed:!0,cancelable:!0}))}async #n(){this._executing=!0;try{if(this.test){await this.#o();return}await this.#l()}finally{this._executing=!1}}async #l(){let e=Object.keys(this.formMetaData).reduce((e,t)=>(e[t]=this.shadowRoot?.getElementById(t)?.value,e),{});for await(let t of d(await fetch(`${this.url}/stream?thread=${this.#e}`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(e)})))console.debug(t),this.dispatchEvent(new CustomEvent("result",{detail:JSON.parse(t),bubbles:!0,composed:!0,cancelable:!0}))}async #r(){await n(1e3),this.dispatchEvent(new CustomEvent("init",{detail:{threads:[["default",[]]],title:"SpringAiGraph : TEST",graph:`
---
title: TEST
---        
flowchart TD
  start((start))
  stop((stop))
  web_search("web_search")
  retrieve("retrieve")
  grade_documents("grade_documents")
  generate("generate")
  transform_query("transform_query")
  start:::start -->|web_search| web_search:::web_search
  start:::start -->|vectorstore| retrieve:::retrieve
  web_search:::web_search --> generate:::generate
  retrieve:::retrieve --> grade_documents:::grade_documents
  grade_documents:::grade_documents -->|transform_query| transform_query:::transform_query
  grade_documents:::grade_documents -->|generate| generate:::generate
  transform_query:::transform_query --> retrieve:::retrieve
  generate:::generate -->|not supported| generate:::generate
  generate:::generate -->|not useful| transform_query:::transform_query
  generate:::generate -->|useful| stop:::stop
      `},bubbles:!0,composed:!0,cancelable:!0})),this.formMetaData={input:{type:"string",required:!0}},this.requestUpdate()}async #o(){let e=this.#e,t=async t=>{await n(1e3),this.dispatchEvent(new CustomEvent("result",{detail:[e,{checkpoint:"start"===t||"stop"===t?void 0:`checkpoint-${t}`,node:t,state:{input:"this is input",property1:{value:"value1",valid:!0},property2:{value:"value2",children:{elements:[1,2,3]}}}}],bubbles:!0,composed:!0,cancelable:!0}))};await t("start"),await t("retrieve"),await t("grade_documents"),await t("transform_query"),await t("retrieve"),await t("grade_documents"),await t("generate"),await t("stop")}}window.customElements.define("lg4j-executor",o);
//# sourceMappingURL=index.87a258c2.js.map
